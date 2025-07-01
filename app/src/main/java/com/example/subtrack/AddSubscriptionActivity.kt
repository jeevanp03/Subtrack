package com.example.subtrack

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.subtrack.ui.calendar.CalendarUtils
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class AddSubscriptionActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var amountEditText: EditText
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

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameEditText = findViewById(R.id.nameEditText)
        amountEditText = findViewById(R.id.amountEditText)
        dateButton = findViewById(R.id.dateButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        frequencySpinner = findViewById(R.id.renewalFrequencySpinner)
        saveButton = findViewById(R.id.saveButton)

        requestCalendarPermissions()
        setupDatePicker()
        setupCategorySpinner()
        setupFrequencySpinner()

        saveButton.setOnClickListener {
            saveSubscription()
        }

        // Add Cancel button logic
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        cancelButton?.setOnClickListener {
            finish()
        }
    }

    // Handle the up button press
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        dateButton.setOnClickListener {
            val datePicker = DatePickerDialog(this,
                { _, year, month, day ->
                    selectedDate = "$year-${month+1}-$day"
                    dateButton.text = selectedDate
                    calendar.set(year, month, day)
                    selectedStartDateMillis = calendar.timeInMillis
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
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
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CALENDAR
                ),
                101 // your request code
            )
        }
    }

    private fun saveSubscription() {
        val name = nameEditText.text.toString().trim()
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val category = categorySpinner.selectedItem.toString()
        val selectedFrequencyIndex = frequencySpinner.selectedItemPosition
        val (frequencyLabel, frequencyInDays, renewalsPerYear) = frequencyOptions.getOrNull(selectedFrequencyIndex) ?: Triple("Monthly", 30, 12)

        if (name.isEmpty() || amount == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (amount <= 0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        // Calculate the next payment date based on the selected frequency
        val nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(selectedDate, frequencyInDays)
        
        // Format the next payment date for display
        val nextPaymentDateFormatted = PaymentDateUtil.formatDateForDisplay(nextPaymentDate)
        val daysUntilPayment = PaymentDateUtil.getDaysUntilPayment(nextPaymentDate)

        val subscription = Subscription(
            name = name,
            amount = amount,
            date = selectedDate,
            category = category,
            renewalsPerYear = renewalsPerYear,
            frequencyInDays = frequencyInDays,
            nextPaymentDate = nextPaymentDate
        )

        // Debug logging
        SubscriptionDebugUtil.logSubscriptionDetails(subscription)
        SubscriptionDebugUtil.testRecurringDateCalculation(selectedDate, frequencyInDays)

        val viewModel = ViewModelProvider(this).get(SubscriptionViewModel::class.java)
        viewModel.insert(subscription)

        CalendarUtils.insertRecurringEvent(
            context = this,
            title = "Payment: $name",
            description = "Subscription due: $category - $$amount",
            startTimeMillis = nextPaymentDate,
            frequencyInDays = frequencyInDays
        )

        // Show success message with next payment information
        val successMessage = "Subscription saved! Next payment: $nextPaymentDateFormatted (in $daysUntilPayment days)"
        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()
        finish()
    }
}
