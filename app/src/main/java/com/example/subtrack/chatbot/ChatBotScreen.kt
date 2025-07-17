package com.example.subtrack.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subtrack.ChatMessage
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.core.*
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyRow

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val animatedScale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(50)
                    )
                    .scale(animatedScale)
            )
        }
    }
}

@Composable
fun FormattedMessageText(
    text: String,
    isFromUser: Boolean,
    modifier: Modifier = Modifier
) {
    val annotatedText = buildAnnotatedString {
        val lines = text.split("\n")
        
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("**") && line.endsWith("**") -> {
                    // Bold headers
                    withStyle(style = SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                        append(line.removeSurrounding("**"))
                    }
                }
                line.startsWith("🚨") || line.startsWith("💡") || line.startsWith("📺") || 
                line.startsWith("💰") || line.startsWith("🔍") -> {
                    // Insight lines with emphasis
                    withStyle(style = SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)) {
                        append(line)
                    }
                }
                line.startsWith("•") -> {
                    // Bullet points with slight emphasis
                    withStyle(style = SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal)) {
                        append(line)
                    }
                }
                else -> {
                    append(line)
                }
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
    
    Text(
        text = annotatedText,
        modifier = modifier,
        color = if (isFromUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

fun getSmartSuggestions(input: String): List<String> {
    val lowercaseInput = input.lowercase()
    
    return when {
        lowercaseInput.contains("save") || lowercaseInput.contains("money") -> listOf(
            "How can I save 20% on subscriptions?",
            "Show me alternatives to expensive services",
            "Find unused subscriptions to cancel"
        )
        lowercaseInput.contains("budget") -> listOf(
            "Create a monthly subscription budget",
            "What's my ideal spending limit?",
            "How much should I spend per category?"
        )
        lowercaseInput.contains("cancel") || lowercaseInput.contains("expensive") -> listOf(
            "Which subscriptions cost the most?",
            "What can I cancel without missing?",
            "Show me subscription alternatives"
        )
        lowercaseInput.contains("discount") || lowercaseInput.contains("deal") -> listOf(
            "Find student discounts",
            "Annual vs monthly pricing",
            "Bundle deals available"
        )
        else -> listOf(
            "Analyze my spending",
            "Save money tips",
            "Create a budget"
        )
    }.take(3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(
    viewModel: ChatBotViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Assistant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeMessage(onQuickActionClick = { action ->
                            val suggestion = when (action) {
                                "Save Money" -> "How can I save money on my subscriptions?"
                                "Analyze Spending" -> "Analyze my current spending patterns"
                                "Budget Help" -> "Help me create a subscription budget"
                                "Cancel Subscriptions" -> "Which subscriptions should I consider canceling?"
                                else -> action
                            }
                            viewModel.updateCurrentMessage(suggestion)
                        })
                    }
                }
                
                items(uiState.messages) { message ->
                    ChatBubble(message = message)
                }
                
                if (uiState.isLoading) {
                    item {
                        LoadingMessage()
                    }
                }
            }

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            // Quick suggestions when no messages
            if (uiState.messages.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Input field
            ChatInputField(
                message = uiState.currentMessage,
                onMessageChange = viewModel::updateCurrentMessage,
                onSendMessage = viewModel::sendMessage,
                enabled = !uiState.isLoading,
                onQuickSuggestionClick = { suggestion ->
                    viewModel.updateCurrentMessage(suggestion)
                    viewModel.sendMessage()
                }
            )
        }
    }
}

@Composable
fun WelcomeMessage(onQuickActionClick: (String) -> Unit = {}) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Welcome to your Financial Assistant! 💰",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "I can help you with budgeting tips, saving money, and managing your subscriptions. Ask me anything about being more frugal!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        QuickActionSuggestions(onQuickActionClick = onQuickActionClick)
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = dateFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.secondary,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (message.isFromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                )
            ) {
                FormattedMessageText(
                    text = message.message,
                    isFromUser = message.isFromUser,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (message.isFromUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun LoadingMessage() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.secondary,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingIndicator()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionSuggestions(onQuickActionClick: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Default.Info,
                    text = "Save Money",
                    onClick = { onQuickActionClick("Save Money") }
                )
                QuickActionChip(
                    icon = Icons.Default.Search,
                    text = "Analyze Spending",
                    onClick = { onQuickActionClick("Analyze Spending") }
                )
                QuickActionChip(
                    icon = Icons.Default.Star,
                    text = "Budget Help",
                    onClick = { onQuickActionClick("Budget Help") }
                )
                QuickActionChip(
                    icon = Icons.Default.Clear,
                    text = "Cancel Subscriptions",
                    onClick = { onQuickActionClick("Cancel Subscriptions") }
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputField(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    enabled: Boolean,
    onQuickSuggestionClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask about saving money...") },
                enabled = enabled,
                maxLines = 4
            )
            
            if (message.isNotBlank()) {
                val suggestions = getSmartSuggestions(message)
                if (suggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { onQuickSuggestionClick(suggestion) },
                                label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        FloatingActionButton(
            onClick = onSendMessage,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send"
            )
        }
    }
}