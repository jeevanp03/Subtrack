package com.example.subtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.subtrack.chatbot.ChatBotRepository
import com.example.subtrack.chatbot.ChatBotScreen
import com.example.subtrack.chatbot.ChatBotService
import com.example.subtrack.chatbot.ChatBotViewModel
import com.example.subtrack.chatbot.ChatBotViewModelFactory
import com.example.subtrack.ui.theme.SubtrackTheme

class ChatBotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getLongExtra("USER_ID", -1L)
        if (userId == -1L) {
            finish()
            return
        }

        val database = SubscriptionDatabase.getDatabase(applicationContext)
        val chatBotService = ChatBotService()
        val repository = ChatBotRepository(
            chatMessageDao = database.chatMessageDao(),
            subscriptionDao = database.subscriptionDao(),
            accountDao = database.accountDao(),
            chatBotService = chatBotService
        )

        setContent {
            SubtrackTheme {
                val viewModelFactory = ChatBotViewModelFactory(repository, userId)
                val viewModel: ChatBotViewModel = viewModel(factory = viewModelFactory)
                
                ChatBotScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}