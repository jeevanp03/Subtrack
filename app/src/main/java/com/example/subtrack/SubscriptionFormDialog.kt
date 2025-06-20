package com.example.subtrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SubscriptionFormDialog(
    initialName: String = "",
    initialCost: String = "",
    initialRenewalDate: String = "",
    initialCategory: String = "",
    initialRenewalsPerYear: Int = 12, // ✅ added here
    onDismiss: () -> Unit,
    onSave: (name: String, cost: Double, renewalDate: String, category: String, renewalsPerYear: Int) -> Unit
)
 {
    var name by remember { mutableStateOf(initialName) }
    var cost by remember { mutableStateOf(initialCost) }
    var renewalDate by remember { mutableStateOf(initialRenewalDate) }
    var category by remember { mutableStateOf(initialCategory) }
     val frequencyOptions = listOf(
         "Weekly" to 52,
         "Biweekly" to 26,
         "Monthly" to 12,
         "Quarterly" to 4,
         "Yearly" to 1
     )

     var selectedOption by remember {
         mutableStateOf(frequencyOptions.find { it.second == initialRenewalsPerYear } ?: frequencyOptions[2])
     }


     AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "Add Subscription" else "Edit Subscription") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Cost") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = renewalDate,
                    onValueChange = { renewalDate = it },
                    label = { Text("Renewal Date") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Renewal Frequency", style = MaterialTheme.typography.labelLarge)

                var expanded by remember { mutableStateOf(false) }

                Text("Renewal Frequency", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize()) {
                    OutlinedTextField(
                        value = selectedOption.first,
                        onValueChange = {},
                        label = { Text("Frequency") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = true,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true })
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.first) },
                                onClick = {
                                    selectedOption = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val costValue = cost.toDoubleOrNull() ?: 0.0
                onSave(name, costValue, renewalDate, category, selectedOption.second)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 