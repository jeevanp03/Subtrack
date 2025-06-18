@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.subtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.util.Log

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = ViewModelProvider(this)[SubscriptionViewModel::class.java]
            HomeScreen(viewModel)
        }
    }
}

@Composable
fun HomeScreen(viewModel: SubscriptionViewModel) {
    val subscriptions by viewModel.subscriptions.collectAsState(emptyList())
    val context = LocalContext.current

    // Refresh payment dates when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.refreshPaymentDates()
        Log.d("HomeActivity", "Refreshed payment dates on startup")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subtrak") },
                actions = {
                    IconButton(onClick = { /* Profile action */ }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val intent = Intent(context, AddSubscriptionActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Text("Add Subscription")
                }
                Button(onClick = { /* TODO: Add navigation to Calendar */ }) {
                    Text("View Calendar")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                MonthlyOverview(subscriptions)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text(
                    text = "Upcoming Renewals",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(subscriptions.sortedBy { it.nextPaymentDate }) { sub ->
                SubscriptionItem(sub, viewModel)
            }
        }
    }
}

@Composable
fun SubscriptionItem(sub: Subscription, viewModel: SubscriptionViewModel) {
    val formattedDate = PaymentDateUtil.formatDateForDisplay(sub.nextPaymentDate)
    val frequencyLabel = PaymentDateUtil.getFrequencyLabel(sub.frequencyInDays)
    val daysUntilPayment = PaymentDateUtil.getDaysUntilPayment(sub.nextPaymentDate)
    val isOverdue = PaymentDateUtil.isPaymentDatePassed(sub.nextPaymentDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sub.name, 
                    style = MaterialTheme.typography.titleMedium
                )
                if (isOverdue) {
                    Text(
                        text = "OVERDUE",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Next Payment: $formattedDate")
            Text(
                text = if (isOverdue) "Overdue by ${-daysUntilPayment} days" else "Due in $daysUntilPayment days",
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text("Amount: $${"%.2f".format(sub.amount)}")
            Text("Frequency: $frequencyLabel")
            
            if (!isOverdue) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.markPaymentCompleted(sub.id) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Mark as Paid")
                }
            }
        }
    }
}

@Composable
fun MonthlyOverview(subscriptions: List<Subscription>) {
    val frequencyMap = mutableMapOf<String, Double>()

    subscriptions.forEach { sub ->
        val label = PaymentDateUtil.getFrequencyLabel(sub.frequencyInDays)
        val current = frequencyMap[label] ?: 0.0
        frequencyMap[label] = current + sub.amount
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Overview",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Total Subscriptions: ${subscriptions.size}")

            Spacer(modifier = Modifier.height(8.dp))

            Text("Recurring Cost:")

            frequencyMap.forEach { (label, total) ->
                Text("• $label: $${"%.2f".format(total)}")
            }
        }
    }
} 