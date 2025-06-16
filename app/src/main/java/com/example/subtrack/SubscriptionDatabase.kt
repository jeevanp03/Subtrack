package com.example.subtrack

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Subscription::class], version = 1)
abstract class SubscriptionDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        @Volatile private var instance: SubscriptionDatabase? = null

        fun getDatabase(context: Context): SubscriptionDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SubscriptionDatabase::class.java,
                    "subscription_db"
                ).build().also { instance = it }
            }
    }
}
