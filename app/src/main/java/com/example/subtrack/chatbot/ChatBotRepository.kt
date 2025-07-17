package com.example.subtrack.chatbot

import com.example.subtrack.ChatMessage
import com.example.subtrack.ChatMessageDao
import com.example.subtrack.SubscriptionDao
import com.example.subtrack.ui.account.AccountDao
import kotlinx.coroutines.flow.Flow

class ChatBotRepository(
    private val chatMessageDao: ChatMessageDao,
    private val subscriptionDao: SubscriptionDao,
    private val accountDao: AccountDao,
    private val chatBotService: ChatBotService
) {
    
    fun getChatMessages(userId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForUser(userId)
    }

    suspend fun sendMessage(userId: Long, message: String): String {
        // Save user message
        val userMessage = ChatMessage(
            userId = userId,
            message = message,
            isFromUser = true
        )
        chatMessageDao.insertMessage(userMessage)

        // Get user's account information
        val userAccount = accountDao.getAccountById(userId)
        val userEmail = userAccount?.email ?: "user@example.com"
        
        // Get user's subscription data for context
        val userSubscriptions = try {
            subscriptionDao.getSubscriptionsByUserSync(userId)
        } catch (e: Exception) {
            emptyList()
        }
        
        // Get recent conversation history for context
        val recentMessages = try {
            chatMessageDao.getRecentMessages(userId)
        } catch (e: Exception) {
            emptyList()
        }
        
        // Generate AI response with actual user data and conversation context
        val botResponse = chatBotService.generateFinancialAdvice(
            message,
            userSubscriptions,
            userEmail,
            recentMessages
        )

        // Save bot response
        val botMessage = ChatMessage(
            userId = userId,
            message = botResponse,
            isFromUser = false
        )
        chatMessageDao.insertMessage(botMessage)

        return botResponse
    }

    suspend fun clearChatHistory(userId: Long) {
        chatMessageDao.clearChatHistory(userId)
    }
}