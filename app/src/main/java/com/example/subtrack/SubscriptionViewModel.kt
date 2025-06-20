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
        
    val upcomingPayments: StateFlow<List<Subscription>> = repository.getUpcomingPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(subscription: Subscription) = viewModelScope.launch {
        repository.insert(subscription)
    }

    fun delete(subscription: Subscription) = viewModelScope.launch {
        repository.delete(subscription)
    }
    
    fun updateNextPaymentDate(subscriptionId: Int, newDate: Long) = viewModelScope.launch {
        repository.updateNextPaymentDate(subscriptionId, newDate)
    }
    
    fun markPaymentCompleted(subscriptionId: Int) = viewModelScope.launch {
        repository.markPaymentCompleted(subscriptionId)
    }
    
    fun updateOverduePayments() = viewModelScope.launch {
        repository.updateOverduePayments()
    }
    
    // Call this when the app starts or when needed to ensure all payment dates are current
    fun refreshPaymentDates() = viewModelScope.launch {
        repository.updateOverduePayments()
    }

    // WARNING: This will delete all data from the database. Use only in development!
    fun clearAllData() = viewModelScope.launch {
        repository.clearAllData()
    }
}
