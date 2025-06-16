package com.example.subtrack

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription)

    @Query("SELECT * FROM subscriptions ORDER BY date ASC")
    fun getAll(): Flow<List<Subscription>>


    @Delete
    suspend fun delete(subscription: Subscription)
}
