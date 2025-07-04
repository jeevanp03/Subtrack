package com.example.subtrack

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription)

    @Query("SELECT * FROM subscriptions WHERE userId = :userId ORDER BY nextPaymentDate ASC")
    fun getSubscriptionsByUser(userId: Long): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND nextPaymentDate <= :timestamp ORDER BY nextPaymentDate ASC")
    fun getUpcomingPaymentsByUser(userId: Long, timestamp: Long): Flow<List<Subscription>>

    @Query("DELETE FROM subscriptions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)

    @Query("SELECT * FROM subscriptions ORDER BY nextPaymentDate ASC")
    fun getAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY nextPaymentDate ASC")
    suspend fun getAllSync(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE nextPaymentDate <= :timestamp ORDER BY nextPaymentDate ASC")
    fun getUpcomingPayments(timestamp: Long): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId")
    suspend fun getSubscriptionById(subscriptionId: Int): Subscription?

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND nextPaymentDate < :timestamp")
    suspend fun getOverdueSubscriptions(userId: Long, timestamp: Long): List<Subscription>


    @Query("UPDATE subscriptions SET nextPaymentDate = :newDate WHERE id = :subscriptionId")
    suspend fun updateNextPaymentDate(subscriptionId: Int, newDate: Long)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}

