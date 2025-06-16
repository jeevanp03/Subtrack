package com.example.subtrack

class SubscriptionRepository(private val dao: SubscriptionDao) {
    fun getAll() = dao.getAll()
    suspend fun insert(subscription: Subscription) = dao.insert(subscription)
    suspend fun delete(subscription: Subscription) = dao.delete(subscription)
}

