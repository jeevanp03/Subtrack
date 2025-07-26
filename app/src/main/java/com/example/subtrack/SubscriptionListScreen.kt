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
fun SubscriptionListScreen(userId: Long, viewModel: SubscriptionViewModel = viewModel()) {
    val subscriptions by viewModel.subscriptions.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<Subscription?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Subscription?>(null) }


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
                items(subscriptions) { sub ->
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
                                Text("$${sub.amount} - ${sub.date}", style = MaterialTheme.typography.bodyMedium)
                                Text(sub.category, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                editingSubscription = sub
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { showDeleteConfirmation = sub }) {
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
                initialCost = sub?.amount?.toString() ?: "",
                initialRenewalDate = sub?.date ?: "",
                initialCategory = sub?.category ?: "",
                onDismiss = { showDialog = false },
                onSave = { name, amount, renewalDate, category, renewalsPerYear ->
                    val subscription = if (sub == null) {
                        Subscription(
                            id = 0,
                            name = name,
                            amount = amount,
                            date = renewalDate,
                            category = category,
                            renewalsPerYear = renewalsPerYear,
                            userId = userId
                        )
                    } else {
                        sub.copy(
                            name = name,
                            amount = amount,
                            date = renewalDate,
                            category = category,
                            renewalsPerYear = renewalsPerYear,
                            userId = userId
                        )
                    }
                    viewModel.insert(subscription)
                    showDialog = false
                }
            )
        }

        // Delete confirmation dialog
        showDeleteConfirmation?.let { subscription ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = null },
                title = { Text("Delete Subscription") },
                text = { Text("Are you sure you want to delete '${subscription.name}'? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(subscription)
                            showDeleteConfirmation = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 