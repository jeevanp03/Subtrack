@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.subtrack

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.example.subtrack.ui.account.LoginScreen
import com.example.subtrack.ui.account.CreateAccountScreen
import com.example.subtrack.ui.account.ScreenState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Menu


// --------------------------------------
// MAIN ACTIVITY: Entry point for Subtrak app
// Manages Login, Account Creation, and Home screens
// --------------------------------------

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = SubscriptionDatabase.getDatabase(applicationContext)

        setContent {
            var screenState by rememberSaveable { mutableStateOf(ScreenState.LOGIN) }
            var loggedInUserId by rememberSaveable { mutableStateOf<Long?>(null) }
            val scope = rememberCoroutineScope()

            when (screenState) {
                ScreenState.LOGIN -> {  // Show login screen
                    LoginScreen(
                        onLogin = { email, password, onResult ->
                            scope.launch {
                                val account = db.accountDao().getAccountByEmail(email)
                                if (account != null && account.password == password) {
                                    loggedInUserId = account.id
                                    onResult(true, account.id)
                                    screenState = ScreenState.HOME
                                } else {
                                    onResult(false, null)
                                }
                            }
                        },
                        onNavigateToCreateAccount = {
                            screenState = ScreenState.CREATE_ACCOUNT // Navigate to account creation screen
                        }
                    )
                }

                ScreenState.CREATE_ACCOUNT -> { // Show account creation screen
                    CreateAccountScreen(
                        db = db,
                        onBackToLogin = {
                            screenState = ScreenState.LOGIN // Return to login screen
                        }
                    )
                }

                ScreenState.HOME -> { // Show main app screen
                    val viewModel = ViewModelProvider(this)[SubscriptionViewModel::class.java] // Get ViewModel instance
                    HomeScreen(
                        viewModel = viewModel,
                        userId = loggedInUserId,
                        onLogout = {
                            loggedInUserId = null
                            screenState = ScreenState.LOGIN
                        }
                    )
                }
            }
        }
    }
    
    // --------------------------------------
    // HOME SCREEN UI - Displays subscription list & actions
    // --------------------------------------
    @Composable
    fun HomeScreen(
        viewModel: SubscriptionViewModel,
        userId: Long?,
        onLogout: () -> Unit
    ) {
        val subscriptions by viewModel.subscriptions.collectAsState(emptyList())  // Observe subscription list from ViewModel
        val context = LocalContext.current  // Access current context for navigation

        var searchQuery by rememberSaveable { mutableStateOf("") }

        val filteredSubscriptions = if (searchQuery.isBlank()) {
            subscriptions
        } else {
            subscriptions.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        var menuExpanded by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.refreshPaymentDates()
            Log.d("HomeActivity", "Refreshed payment dates on startup")
        }
        // Scaffold provides Top Bar, Bottom Bar, and Content Layout
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Subtrak") },
                    actions = {
                        // Debug button to clear database
                        IconButton(onClick = {
                            viewModel.clearAllData()
                            Log.d("HomeActivity", "Database cleared")
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Database")
                        }
                        // Placeholder for Profile functionality
                        IconButton(onClick = { /* Profile action */ }) {   
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                )
            },
            bottomBar = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, AddSubscriptionActivity::class.java)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Add Subscription")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onLogout() },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("Logout")
                            }
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = { /* No-op */ }, text = { Text("Button 1") })
                                DropdownMenuItem(onClick = { /* No-op */ }, text = { Text("Button 2") })
                                DropdownMenuItem(onClick = { /* No-op */ }, text = { Text("Button 3") })
                            }
                        }
                    }
                }
            }

        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
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
                        SubscriptionSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )
                    }
                    if (filteredSubscriptions.isEmpty()) {
                        item {
                            Text(
                                text = "No results found.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(filteredSubscriptions.sortedBy { it.nextPaymentDate }) { sub ->
                            SubscriptionItem(sub, viewModel)
                        }
                    }
                }
            }
        }
    }
    // --------------------------------------
    // Renders details for a single subscription entry
    // --------------------------------------
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
    // --------------------------------------
    // Displays summary of all subscriptions grouped by frequency
    // --------------------------------------
    @Composable
    fun MonthlyOverview(subscriptions: List<Subscription>) {
        val frequencyMap = mutableMapOf<String, Double>()
        // Sum total subscription cost per frequency label
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

    
    // --------------------------------------
    // ADD NAV BAR (Clickable/ minimlaist) /
    // --------------------------------------

    // --------------------------------------
    // ADD SEARCH BAR (search subsciptions)
    // --------------------------------------
    @Composable
    fun SubscriptionSearchBar(
        query: String,
        onQueryChange: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search subscriptions...") },
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }

    // --------------------------------------
    // NOTIFCATIONS BANNER (IE incoming payment...for snapchat)
    // --------------------------------------
}
