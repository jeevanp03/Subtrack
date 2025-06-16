package com.example.subtrack

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class AddSubscriptionActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subscription)

        nameEditText = findViewById(R.id.nameEditText)
        amountEditText = findViewById(R.id.amountEditText)
        dateButton = findViewById(R.id.dateButton)
        categorySpinner = findViewById(R.id.categorySpinner)
        saveButton = findViewById(R.id.saveButton)

        setupDatePicker()
        setupCategorySpinner()

        saveButton.setOnClickListener {
            saveSubscription()
        }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        dateButton.setOnClickListener {
            val datePicker = DatePickerDialog(this,
                { _, year, month, day ->
                    selectedDate = "$year-${month+1}-$day"
                    dateButton.text = selectedDate
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

    private fun saveSubscription() {
        val name = nameEditText.text.toString()
        val amount = amountEditText.text.toString().toDoubleOrNull()
        val category = categorySpinner.selectedItem.toString()

        if (name.isEmpty() || amount == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val subscription = Subscription(
            name = name,
            amount = amount,
            date = selectedDate,
            category = category
        )

        val viewModel = SubscriptionViewModel(application)
        viewModel.insert(subscription)

        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

}
