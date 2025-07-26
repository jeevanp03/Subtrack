package com.example.subtrack

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.subtrack.ui.calendar.CalendarUtils
import com.example.subtrack.ui.theme.SubtrackTheme
import java.util.*

class AddSubscriptionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getLongExtra("USER_ID", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "User ID missing. Cannot save subscription.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            SubtrackTheme {
                AddSubscriptionScreen(userId = userId, onFinish = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(userId: Long, onFinish: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var remindDays by remember { mutableStateOf("1") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedStartDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var category by remember { mutableStateOf("Streaming") }
    var frequencyIndex by remember { mutableStateOf(2) } // Default Monthly

    val frequencyOptions = listOf(
        Triple("Weekly", 7, 52),
        Triple("Biweekly", 14, 26),
        Triple("Monthly", 30, 12),
        Triple("Quarterly", 90, 4),
        Triple("Semiannually", 180, 2),
        Triple("Annually", 365, 1)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Add Subscription",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Text(
                        text = "New Subscription",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subscription Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Amount Field
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Remind Days Field
                    OutlinedTextField(
                        value = remindDays,
                        onValueChange = { remindDays = it },
                        label = { Text("Remind Days Before") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Date Selection Button
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            val themedContext = ContextThemeWrapper(context, R.style.SubtrackDatePickerTheme)

                            val picker = DatePickerDialog(
                                themedContext,
                                { _, year, month, day ->
                                    selectedDate = "$year-${month + 1}-$day"
                                    calendar.set(year, month, day)
                                    selectedStartDateMillis = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            picker.show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            if (selectedDate.isEmpty()) "Select Payment Date" else "Date: $selectedDate",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Category Dropdown
                    DropdownMenuField(
                        "Category",
                        listOf("Streaming", "Productivity", "Fitness", "Other"),
                        category
                    ) { category = it }

                    // Frequency Dropdown
                    DropdownMenuField(
                        "Frequency",
                        frequencyOptions.map { it.first },
                        frequencyOptions[frequencyIndex].first
                    ) {
                        frequencyIndex = frequencyOptions.indexOfFirst { option -> option.first == it }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFinish,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val amountDouble = amount.toDoubleOrNull()
                                val remindBefore = remindDays.toIntOrNull() ?: 1

                                if (name.isBlank() || amountDouble == null || selectedDate.isBlank()) {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val (_, frequencyDays, renewalsPerYear) = frequencyOptions[frequencyIndex]
                                val nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(selectedStartDateMillis, frequencyDays)
                                val nextFormatted = PaymentDateUtil.formatDateForDisplay(nextPaymentDate)
                                val daysUntil = PaymentDateUtil.getDaysUntilPayment(nextPaymentDate)

                                SubscriptionDebugUtil.testRecurringDateCalculation(selectedDate, frequencyDays)

                                CalendarUtils.insertRecurringEvent(
                                    context,
                                    title = "Payment: $name",
                                    description = "Subscription due: $category - \$$amountDouble",
                                    startTimeMillis = selectedStartDateMillis,
                                    frequencyInDays = frequencyDays
                                )

                                val resultIntent = Intent().apply {
                                    putExtra("SUB_NAME", name)
                                    putExtra("SUB_AMOUNT", amountDouble.toString())
                                    putExtra("SUB_DATE", selectedDate)
                                    putExtra("SUB_CATEGORY", category)
                                    putExtra("SUB_RENEWALS", renewalsPerYear)
                                    putExtra("SUB_FREQ_DAYS", frequencyDays)
                                    putExtra("SUB_NEXT_PAYMENT", nextPaymentDate)
                                    putExtra("SUB_REMIND_BEFORE", remindBefore)
                                }

                                (context as? AddSubscriptionActivity)?.setResult(Activity.RESULT_OK, resultIntent)
                                Toast.makeText(context, "Saved! Next: $nextFormatted ($daysUntil days)", Toast.LENGTH_LONG).show()
                                onFinish()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Save",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}