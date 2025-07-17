package com.example.subtrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: Long): Flow<List<ChatMessage>>

    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun clearChatHistory(userId: Long)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMessages(userId: Long): List<ChatMessage>
    
}