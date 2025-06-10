package com.example.subtrack

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class SubscriptionViewModel : ViewModel() {
    private var nextId = 1
    var subscriptions = mutableStateListOf<Subscription>()
        private set

    fun addSubscription(name: String, cost: Double, renewalDate: String, category: String) {
        subscriptions.add(Subscription(nextId++, name, cost, renewalDate, category))
    }

    fun updateSubscription(updated: Subscription) {
        val index = subscriptions.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            subscriptions[index] = updated
        }
    }

    fun deleteSubscription(id: Int) {
        subscriptions.removeAll { it.id == id }
    }
} 