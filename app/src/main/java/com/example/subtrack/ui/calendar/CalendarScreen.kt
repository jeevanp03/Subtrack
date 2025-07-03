package com.example.subtrack.ui.calendar

import android.app.Activity
import android.widget.CalendarView
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    val activity = context as Activity

    var selectedDate by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Your Calendar",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Calendar in Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = {
                                CalendarView(context).apply {
                                    scaleX = 1.15f
                                    scaleY = 1.2f
                                }
                            },
                            update = { calendarView ->
                                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                                    selectedDate = "$dayOfMonth/${month + 1}/$year"
                                    Toast.makeText(
                                        context,
                                        "Selected: $selectedDate",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                if (selectedDate.isNotEmpty()) {
                    Text(
                        text = "📅 Selected Date: $selectedDate",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom button
            Button(
                onClick = { activity.finish() },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Back to Home", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
