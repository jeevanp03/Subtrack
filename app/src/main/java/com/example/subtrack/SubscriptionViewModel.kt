package com.example.subtrack

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = SubscriptionDatabase.getDatabase(application).subscriptionDao()
    private val repository = SubscriptionRepository(dao)

    val subscriptions: StateFlow<List<Subscription>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(subscription: Subscription) = viewModelScope.launch {
        repository.insert(subscription)
    }

    fun delete(subscription: Subscription) = viewModelScope.launch {
        repository.delete(subscription)
    }
}
