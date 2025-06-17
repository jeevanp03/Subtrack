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
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

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
                SubscriptionItem(sub)
            }
        }
    }
}

@Composable
fun SubscriptionItem(sub: Subscription) {
    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        .format(Date(sub.nextPaymentDate))
    val frequencyLabel = FrequencyUtil.getFrequencyLabel(sub.frequencyInDays)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = sub.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Next Payment: $formattedDate")
            Text("Amount: $${"%.2f".format(sub.amount)}")
            Text("Frequency: $frequencyLabel")
        }
    }
}

@Composable
fun MonthlyOverview(subscriptions: List<Subscription>) {
    val frequencyMap = mutableMapOf<String, Double>()

    subscriptions.forEach { sub ->
        val label = FrequencyUtil.getFrequencyLabel(sub.frequencyInDays)
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