package com.example.subtrack

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SubscriptionListScreen(viewModel: SubscriptionViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingSubscription = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Text(
                text = "Subscriptions",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.subscriptions) { sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.name, style = MaterialTheme.typography.titleMedium)
                                Text("$${sub.cost} - ${sub.renewalDate}", style = MaterialTheme.typography.bodyMedium)
                                Text(sub.category, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                editingSubscription = sub
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteSubscription(sub.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
        if (showDialog) {
            val sub = editingSubscription
            SubscriptionFormDialog(
                initialName = sub?.name ?: "",
                initialCost = sub?.cost?.toString() ?: "",
                initialRenewalDate = sub?.renewalDate ?: "",
                initialCategory = sub?.category ?: "",
                onDismiss = { showDialog = false },
                onSave = { name, cost, renewalDate, category ->
                    if (sub == null) {
                        viewModel.addSubscription(name, cost, renewalDate, category)
                    } else {
                        viewModel.updateSubscription(sub.copy(name = name, cost = cost, renewalDate = renewalDate, category = category))
                    }
                    showDialog = false
                }
            )
        }
    }
} 