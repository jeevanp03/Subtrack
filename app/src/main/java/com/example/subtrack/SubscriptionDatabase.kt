package com.example.subtrack

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.subtrack.ui.account.Account
import com.example.subtrack.ui.account.AccountDao

@Database(entities = [Subscription::class, Account::class, ChatMessage::class], version = 16)
abstract class SubscriptionDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun accountDao(): AccountDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile private var instance: SubscriptionDatabase? = null

        fun getDatabase(context: Context): SubscriptionDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SubscriptionDatabase::class.java,
                    "subscription_db"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
    }
}
