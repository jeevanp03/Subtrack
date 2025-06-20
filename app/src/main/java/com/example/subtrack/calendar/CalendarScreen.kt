package com.example.subtrack.calendar

import android.app.Activity
import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val activity = context as Activity

    var selectedDate by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                Toast.makeText(context, "Selected: $selectedDate", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { activity.finish() }) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("This is the calendar screen", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { datePickerDialog.show() }) {
            Text("Open Date Picker")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedDate.isNotEmpty()) {
            Text("Selected Date: $selectedDate")
        }
    }
}
