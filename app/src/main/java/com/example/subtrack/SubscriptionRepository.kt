package com.example.subtrack

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class SubscriptionRepository(private val dao: SubscriptionDao) {
    fun getAll() = dao.getAll()
    
    fun getUpcomingPayments(daysAhead: Int = 30) = dao.getUpcomingPayments(
        System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysAhead.toLong())
    )
    
    suspend fun insert(subscription: Subscription) {
        val subscriptionWithNextPayment = subscription.copy(
            nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(
                subscription.date,
                subscription.frequencyInDays
            )
        )
        dao.insert(subscriptionWithNextPayment)
    }
    
    suspend fun updateNextPaymentDate(subscriptionId: Int, newDate: Long) {
        dao.updateNextPaymentDate(subscriptionId, newDate)
    }
    
    suspend fun markPaymentCompleted(subscriptionId: Int) {
        val subscription = dao.getSubscriptionById(subscriptionId)
        subscription?.let {
            val nextPaymentDate = PaymentDateUtil.calculateNextPaymentDateFromCurrent(
                it.nextPaymentDate,
                it.frequencyInDays
            )
            dao.updateNextPaymentDate(subscriptionId, nextPaymentDate)
        }
    }
    
    suspend fun updateOverduePayments() {
        val allSubscriptions = dao.getAllSync()
        for (subscription in allSubscriptions) {
            if (PaymentDateUtil.isPaymentDatePassed(subscription.nextPaymentDate)) {
                val nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(
                    PaymentDateUtil.formatDate(subscription.nextPaymentDate),
                    subscription.frequencyInDays
                )
                dao.updateNextPaymentDate(subscription.id, nextPaymentDate)
            }
        }
    }
    
    suspend fun delete(subscription: Subscription) = dao.delete(subscription)

    suspend fun clearAllData() = dao.deleteAll()
}

