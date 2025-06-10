package com.example.subtrack

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
    onDismiss: () -> Unit,
    onSave: (name: String, cost: Double, renewalDate: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var cost by remember { mutableStateOf(initialCost) }
    var renewalDate by remember { mutableStateOf(initialRenewalDate) }
    var category by remember { mutableStateOf(initialCategory) }

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
            }
        },
        confirmButton = {
            Button(onClick = {
                val costValue = cost.toDoubleOrNull() ?: 0.0
                onSave(name, costValue, renewalDate, category)
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