package com.example.subtrack

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.subtrack.ui.calendar.CalendarUtils
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class AddSubscriptionActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var remindDaysBeforeEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var frequencySpinner: Spinner
    private lateinit var saveButton: Button

    private var selectedDate: String = ""
    private var selectedStartDateMillis: Long = System.currentTimeMillis()

    // Frequency options: label to days and renewals per year
    private val frequencyOptions = listOf(
        Triple("Weekly", 7, 52),
        Triple("Biweekly", 14, 26),
        Triple("Monthly", 30, 12),
        Triple("Quarterly", 90, 4),
        Triple("Semiannually", 180, 2),
        Triple("Annually", 365, 1)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subscription)

        val userId = intent.getLongExtra("USER_ID", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "User ID missing. Cannot save subscription.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameEditText = findViewById(R.id.nameEditText)
        amountEditText = findViewById(R.id.amountEditText)
        remindDaysBeforeEditText = findViewById(R.id.remindDaysBeforeEditText)
        dateButton = findViewById(R.id.dateButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        frequencySpinner = findViewById(R.id.renewalFrequencySpinner)
        saveButton = findViewById(R.id.saveButton)

        requestCalendarPermissions()
        setupDatePicker()
        setupCategorySpinner()
        setupFrequencySpinner()

        saveButton.setOnClickListener {
            saveSubscription(userId)
        }

        val cancelButton = findViewById<Button>(R.id.cancelButton)
        cancelButton?.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        dateButton.setOnClickListener {
            val datePicker = DatePickerDialog(this,
                { _, year, month, day ->
                    selectedDate = "$year-${month + 1}-$day"
                    dateButton.text = selectedDate
                    calendar.set(year, month, day)
                    selectedStartDateMillis = calendar.timeInMillis
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("Streaming", "Productivity", "Fitness", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        categorySpinner.adapter = adapter
    }

    private fun setupFrequencySpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            frequencyOptions.map { it.first }
        )
        frequencySpinner.adapter = adapter
    }

    private fun requestCalendarPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CALENDAR
                ),
                101
            )
        }
    }

    private fun saveSubscription(userId: Long) {
        val name = nameEditText.text.toString().trim()
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val remindDaysBefore = remindDaysBeforeEditText.text.toString().toIntOrNull() ?: 1

        val category = categorySpinner.selectedItem.toString()
        val selectedFrequencyIndex = frequencySpinner.selectedItemPosition
        val (frequencyLabel, frequencyInDays, renewalsPerYear) =
            frequencyOptions.getOrNull(selectedFrequencyIndex) ?: Triple("Monthly", 30, 12)

        if (name.isEmpty() || amount == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        val nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(
            selectedStartDateMillis,
            frequencyInDays
        )

        val nextPaymentDateFormatted = PaymentDateUtil.formatDateForDisplay(nextPaymentDate)
        val daysUntilPayment = PaymentDateUtil.getDaysUntilPayment(nextPaymentDate)

        SubscriptionDebugUtil.testRecurringDateCalculation(selectedDate, frequencyInDays)

        Log.d("DEBUG", "Returning subscription for userId: $userId")

        // Return data to caller instead of inserting here
        val resultIntent = Intent().apply {
            putExtra("SUB_NAME", name)
            putExtra("SUB_AMOUNT", amount.toString())
            putExtra("SUB_DATE", selectedDate)
            putExtra("SUB_CATEGORY", category)
            putExtra("SUB_RENEWALS", renewalsPerYear)
            putExtra("SUB_FREQ_DAYS", frequencyInDays)
            putExtra("SUB_NEXT_PAYMENT", nextPaymentDate)
            putExtra("SUB_REMIND_BEFORE", remindDaysBefore)
        }

        setResult(RESULT_OK, resultIntent)

        // Optionally add calendar event (can still do this here)
        CalendarUtils.insertRecurringEvent(
            context = this,
            title = "Payment: $name",
            description = "Subscription due: $category - $$amount",
            startTimeMillis = selectedStartDateMillis,
            frequencyInDays = frequencyInDays
        )

        Toast.makeText(
            this,
            "Subscription saved! Next payment: $nextPaymentDateFormatted (in $daysUntilPayment days)",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}
