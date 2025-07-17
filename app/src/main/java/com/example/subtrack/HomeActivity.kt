@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.subtrack

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.subtrack.ui.account.LoginScreen
import com.example.subtrack.ui.account.CreateAccountScreen
import com.example.subtrack.ui.account.ScreenState
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.Image
import com.example.subtrack.ui.calendar.CalendarUtils
import com.example.subtrack.BiometricAuthHelper
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import com.example.subtrack.ui.theme.SubtrackTheme

// --------------------------------------
// MAIN ACTIVITY: Entry point for Subtrak app
// Manages Login, Account Creation, and Home screens
// --------------------------------------

class HomeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = SubscriptionDatabase.getDatabase(applicationContext)

        setContent {
            SubtrackTheme {
                // Initialize biometric auth helper
                val biometricAuthHelper = remember { BiometricAuthHelper(applicationContext)}
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
                                screenState =
                                    ScreenState.CREATE_ACCOUNT // Navigate to account creation screen
                            },
                            onBiometricLogin = {
                                if (biometricAuthHelper.isBiometricAvailable()) {
                                    biometricAuthHelper.showBiometricPrompt(
                                        activity = this@HomeActivity,
                                        onSuccess = {
                                            // For demo purposes, we'll use a default user ID
                                            // In a real app, you'd store the user ID securely and retrieve it here
                                            loggedInUserId = 1L
                                            screenState = ScreenState.HOME
                                            Toast.makeText(
                                                applicationContext,
                                                "Biometric authentication successful!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(
                                                applicationContext,
                                                "Biometric error: $error",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onFailed = {
                                            Toast.makeText(
                                                applicationContext,
                                                "Biometric authentication failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        applicationContext,
                                        "Biometric authentication not available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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

                    ScreenState.HOME -> {
                        loggedInUserId?.let { uid ->
                            val viewModelFactory = remember(uid) {
                                SubscriptionViewModelFactory(application, uid)
                            }
                            val viewModel: SubscriptionViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = "user_$uid",
                                    factory = viewModelFactory
                                )

                            HomeScreen(
                                viewModel = viewModel,
                                userId = uid,
                                onLogout = {
                                    loggedInUserId = null
                                    screenState = ScreenState.LOGIN
                                }
                            )
                        }
                    }
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
        val subscriptions by viewModel.subscriptions.collectAsState(emptyList())
        val context = LocalContext.current

        var searchQuery by rememberSaveable { mutableStateOf("") }
        var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedAmountRange by rememberSaveable { mutableStateOf<AmountRange?>(null) }
        var selectedPaymentStatus by rememberSaveable { mutableStateOf<PaymentStatus?>(null) }
        var sortOrder by rememberSaveable { mutableStateOf(SortOrder.NEXT_PAYMENT) }

        // Get unique categories for filter chips
        val categories = subscriptions.map { it.category }.distinct().sorted()

        // Filter and sort subscriptions
        val filteredSubscriptions = subscriptions
            .filter { subscription ->
                // Search filter
                val matchesSearch = searchQuery.isBlank() ||
                        subscription.name.contains(searchQuery, ignoreCase = true) ||
                        subscription.category.contains(searchQuery, ignoreCase = true) ||
                        subscription.amount.toString().contains(searchQuery)

                // Category filter
                val matchesCategory = selectedCategory == null || subscription.category == selectedCategory

                // Amount range filter
                val matchesAmountRange = when (selectedAmountRange) {
                    AmountRange.LOW -> subscription.amount < 10.0
                    AmountRange.MEDIUM -> subscription.amount >= 10.0 && subscription.amount < 50.0
                    AmountRange.HIGH -> subscription.amount >= 50.0
                    null -> true
                }

                // Payment status filter
                val matchesPaymentStatus = when (selectedPaymentStatus) {
                    PaymentStatus.OVERDUE -> PaymentDateUtil.isPaymentDatePassed(subscription.nextPaymentDate)
                    PaymentStatus.DUE_SOON -> {
                        val daysUntil = PaymentDateUtil.getDaysUntilPayment(subscription.nextPaymentDate)
                        daysUntil <= 7 && daysUntil > 0
                    }
                    PaymentStatus.ACTIVE -> {
                        val daysUntil = PaymentDateUtil.getDaysUntilPayment(subscription.nextPaymentDate)
                        daysUntil > 7
                    }
                    null -> true
                }

                matchesSearch && matchesCategory && matchesAmountRange && matchesPaymentStatus
            }
            .let { filtered ->
                when (sortOrder) {
                    SortOrder.NAME -> filtered.sortedBy { it.name }
                    SortOrder.AMOUNT -> filtered.sortedBy { it.amount }
                    SortOrder.NEXT_PAYMENT -> filtered.sortedBy { it.nextPaymentDate }
                    SortOrder.CATEGORY -> filtered.sortedBy { it.category }
                }
            }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    val subscription = Subscription(
                        name = intent.getStringExtra("SUB_NAME") ?: "",
                        amount = intent.getStringExtra("SUB_AMOUNT")?.toDoubleOrNull() ?: 0.0,
                        date = intent.getStringExtra("SUB_DATE") ?: "",
                        category = intent.getStringExtra("SUB_CATEGORY") ?: "",
                        renewalsPerYear = intent.getIntExtra("SUB_RENEWALS", 12),
                        frequencyInDays = intent.getIntExtra("SUB_FREQ_DAYS", 30),
                        nextPaymentDate = intent.getLongExtra(
                            "SUB_NEXT_PAYMENT",
                            System.currentTimeMillis()
                        ),
                        remindDaysBefore = intent.getIntExtra("SUB_REMIND_BEFORE", 1),
                        userId = userId ?: -1L
                    )

                    viewModel.insert(subscription)
                    Log.d("HomeActivity", "Inserted subscription: ${subscription.name}")
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.refreshPaymentDates()
            Log.d("HomeActivity", "Refreshed payment dates on startup")
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Subtrak", color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = {
                            viewModel.clearAllData()
                            Log.d("HomeActivity", "Database cleared")
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Database")
                        }
                        IconButton(onClick = { /* Future profile */ }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(context, AddSubscriptionActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            launcher.launch(intent)  // ✅ Launch for result
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Add Subscription")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(context, ChatBotActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("💰 Financial Assistant")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onLogout() },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Logout")
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    item {
                        Text(
                            text = "Upcoming Renewals",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Search bar
                        SubscriptionSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Collapsible filter section
                        CollapsibleFilterSection(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            selectedAmountRange = selectedAmountRange,
                            onAmountRangeChange = { selectedAmountRange = it },
                            selectedPaymentStatus = selectedPaymentStatus,
                            onPaymentStatusChange = { selectedPaymentStatus = it },
                            currentSort = sortOrder,
                            onSortChange = { sortOrder = it },
                            filteredCount = filteredSubscriptions.size,
                            totalCount = subscriptions.size
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (filteredSubscriptions.isEmpty()) {
                        item {
                            Text(
                                text = if (subscriptions.isEmpty()) "No subscriptions yet. Add your first subscription!" else "No results found.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(filteredSubscriptions) { sub ->
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

        var showMarkPaidDialog by remember { mutableStateOf(false) }
        var showCancelDialog by remember { mutableStateOf(false) }

        val cardColor = when {
            daysUntilPayment <= 5 -> MaterialTheme.colorScheme.errorContainer // red
            daysUntilPayment <= 10 -> MaterialTheme.colorScheme.tertiaryContainer // yellow
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                id = getSubscriptionIcon(
                                    sub.name,
                                    sub.category
                                )
                            ),
                            contentDescription = sub.name,
                            modifier = Modifier
                                .size(32.dp)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (isOverdue) {
                        Text(
                            text = "OVERDUE",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text("Next Payment: $formattedDate")
                Text(
                    text = if (isOverdue) "Overdue by ${-daysUntilPayment} days" else "Due in $daysUntilPayment days",
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text("Amount: $${"%.2f".format(sub.amount)}")
                Text("Frequency: $frequencyLabel")

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (!isOverdue) {
                        Button(
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Mark as Paid")
                        }
                    }
                    Button(onClick = { showCancelDialog = true }) {
                        Text("Cancel")
                    }
                }
            }
        }

        // Mark as Paid confirmation
        if (showMarkPaidDialog) {
            AlertDialog(
                onDismissRequest = { showMarkPaidDialog = false },
                title = { Text("Mark as Paid") },
                text = { Text("Are you sure you want to mark this subscription as paid for the upcoming payment?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.markPaymentCompleted(sub.id)
                        showMarkPaidDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMarkPaidDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Cancel subscription confirmation
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Subscription") },
                text = { Text("Are you sure you want to cancel (delete) this subscription? This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(sub)
                        showCancelDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    fun getSubscriptionIcon(name: String, category: String): Int {
        val context = LocalContext.current
        val normalized = name.lowercase()
            .replace(Regex("[^a-z0-9]"), "") // removes spaces, punctuation, special chars
        val resId = remember(normalized) {
            context.resources.getIdentifier(normalized, "drawable", context.packageName)
        }

        return if (resId != 0) {
            resId
        } else {
            when (category.lowercase()) {
                "streaming" -> R.drawable.ic_streaming
                "productivity" -> R.drawable.ic_productivity
                "fitness" -> R.drawable.ic_fitness
                else -> R.drawable.ic_generic
            }
        }
    }


    // --------------------------------------
    // Displays summary of all subscriptions grouped by frequency
    // --------------------------------------
    @Composable
    fun MonthlyOverview(subscriptions: List<Subscription>) {
        val categoryCostMap = subscriptions.groupBy { it.category }
            .mapValues { entry ->
                entry.value.sumOf { sub ->
                    when (sub.frequencyInDays) {
                        7 -> sub.amount * 4.0                 // Weekly
                        14 -> sub.amount * 2.0                // Bi-weekly
                        30 -> sub.amount                      // Monthly
                        90 -> sub.amount / 3.0                // Quarterly
                        180 -> sub.amount / 6.0               // Semi-annually
                        365 -> sub.amount / 12.0              // Annually
                        else -> sub.amount                    // Default Monthly
                    }
                }
            }

        val totalCost = categoryCostMap.values.sum()
        val categories = listOf("Streaming", "Productivity", "Fitness", "Other")

        val colors = listOf(
            Color(0xFF4CAF50), // Green - Streaming
            Color(0xFF2196F3), // Blue - Productivity
            Color(0xFFFFC107), // Amber - Fitness
            Color(0xFF9E9E9E)  // Grey - Other
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Monthly Subscription Spending",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f

                        categories.forEachIndexed { index, category ->
                            val cost = categoryCostMap[category] ?: 0.0
                            val sweepAngle = if (totalCost > 0)
                                (cost.toFloat() / totalCost.toFloat()) * 360f
                            else 0f

                            drawArc(
                                color = colors[index],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val radius = size.minDimension / 2.5f

                        categories.forEachIndexed { index, category ->
                            val cost = categoryCostMap[category] ?: 0.0
                            val sweepAngle = if (totalCost > 0)
                                (cost.toFloat() / totalCost.toFloat()) * 360f
                            else 0f

                            if (sweepAngle > 0f) {
                                val medianAngleRad = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                                val textX = (size.width / 2 + radius * kotlin.math.cos(medianAngleRad)).toFloat()
                                val textY = (size.height / 2 + radius * kotlin.math.sin(medianAngleRad)).toFloat()

                                drawContext.canvas.nativeCanvas.apply {
                                    drawText(
                                        "$category: ${"%.2f".format(cost)}",
                                        textX,
                                        textY,
                                        android.graphics.Paint().apply {
                                            color = android.graphics.Color.BLACK
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            textSize = 24f
                                            isFakeBoldText = true
                                        }
                                    )
                                }
                            }
                            startAngle += sweepAngle
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------
    // ADD NAV BAR (Clickable/ minimlaist)
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
                .padding(vertical = 4.dp)
        )
    }

    // --------------------------------------
    // SORT OPTIONS
    // --------------------------------------
    @Composable
    fun SortOptions(
        currentSort: SortOrder,
        onSortChange: (SortOrder) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = currentSort.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort by") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOrder.values().forEach { sortOrder ->
                    DropdownMenuItem(
                        text = { Text(sortOrder.displayName) },
                        onClick = {
                            onSortChange(sortOrder)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // --------------------------------------
    // FILTER CHIPS
    // --------------------------------------
    @Composable
    fun FilterChips(
        categories: List<String>,
        selectedCategory: String?,
        onCategoryChange: (String?) -> Unit,
        selectedAmountRange: AmountRange?,
        onAmountRangeChange: (AmountRange?) -> Unit,
        selectedPaymentStatus: PaymentStatus?,
        onPaymentStatusChange: (PaymentStatus?) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Category filters
            if (categories.isNotEmpty()) {
                CategoryDropdown(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = onCategoryChange
                )
            }

            // Amount range filters
            AmountRangeDropdown(
                selectedAmountRange = selectedAmountRange,
                onAmountRangeChange = onAmountRangeChange
            )

            // Payment status filters
            PaymentStatusDropdown(
                selectedPaymentStatus = selectedPaymentStatus,
                onPaymentStatusChange = onPaymentStatusChange
            )
        }
    }

    // --------------------------------------
    // INDIVIDUAL DROPDOWN COMPONENTS
    // --------------------------------------
    @Composable
    fun CategoryDropdown(
        categories: List<String>,
        selectedCategory: String?,
        onCategoryChange: (String?) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory ?: "Any category",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Any category") },
                    onClick = {
                        onCategoryChange(null)
                        expanded = false
                    }
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            onCategoryChange(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun AmountRangeDropdown(
        selectedAmountRange: AmountRange?,
        onAmountRangeChange: (AmountRange?) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedAmountRange?.displayName ?: "Any amount",
                onValueChange = {},
                readOnly = true,
                label = { Text("Monthly cost") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Any amount") },
                    onClick = {
                        onAmountRangeChange(null)
                        expanded = false
                    }
                )
                AmountRange.values().forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.displayName) },
                        onClick = {
                            onAmountRangeChange(range)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun PaymentStatusDropdown(
        selectedPaymentStatus: PaymentStatus?,
        onPaymentStatusChange: (PaymentStatus?) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedPaymentStatus?.displayName ?: "Any status",
                onValueChange = {},
                readOnly = true,
                label = { Text("Payment due") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Any status") },
                    onClick = {
                        onPaymentStatusChange(null)
                        expanded = false
                    }
                )
                PaymentStatus.values().forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.displayName) },
                        onClick = {
                            onPaymentStatusChange(status)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // --------------------------------------
    // COLLAPSIBLE FILTER SECTION
    // --------------------------------------
    @Composable
    fun CollapsibleFilterSection(
        categories: List<String>,
        selectedCategory: String?,
        onCategoryChange: (String?) -> Unit,
        selectedAmountRange: AmountRange?,
        onAmountRangeChange: (AmountRange?) -> Unit,
        selectedPaymentStatus: PaymentStatus?,
        onPaymentStatusChange: (PaymentStatus?) -> Unit,
        currentSort: SortOrder,
        onSortChange: (SortOrder) -> Unit,
        filteredCount: Int,
        totalCount: Int
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            // Filter toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters & Sort",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Show active filter count
                    val activeFilters = listOfNotNull(
                        selectedCategory,
                        selectedAmountRange?.displayName,
                        selectedPaymentStatus?.displayName
                    ).size

                    if (activeFilters > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(
                                text = "$activeFilters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Results count
                Text(
                    text = "$filteredCount of $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expand/collapse icon
                Icon(
                    imageVector = if (expanded)
                        androidx.compose.material.icons.Icons.Default.KeyboardArrowUp
                    else
                        androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse filters" else "Expand filters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Collapsible content
            AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort options
                    SortOptions(
                        currentSort = currentSort,
                        onSortChange = onSortChange
                    )

                    // Filter options
                    FilterChips(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryChange = onCategoryChange,
                        selectedAmountRange = selectedAmountRange,
                        onAmountRangeChange = onAmountRangeChange,
                        selectedPaymentStatus = selectedPaymentStatus,
                        onPaymentStatusChange = onPaymentStatusChange
                    )
                }
            }
        }
    }

    // --------------------------------------
    // ENUM CLASSES FOR FILTERING AND SORTING
    // --------------------------------------
    enum class SortOrder(val displayName: String) {
        NAME("Name"),
        AMOUNT("Amount"),
        NEXT_PAYMENT("Next Payment"),
        CATEGORY("Category")
    }

    enum class AmountRange(val displayName: String) {
        LOW("$0.00 - $9.99"),
        MEDIUM("$10.00 - $49.99"),
        HIGH("$50.00+")
    }

    enum class PaymentStatus(val displayName: String) {
        OVERDUE("Overdue"),
        DUE_SOON("Due in 7 days"),
        ACTIVE("Due later")
    }
}