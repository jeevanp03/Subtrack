package com.example.subtrack

import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class SubscriptionRepository(private val dao: SubscriptionDao, private val userId: Long) {

    fun getAll(): Flow<List<Subscription>> = dao.getSubscriptionsByUser(userId)

    fun getUpcomingPayments(daysAhead: Int = 30) = dao.getUpcomingPaymentsByUser(
        userId,System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysAhead.toLong())
    )

    suspend fun insert(subscription: Subscription) {
        val startDateMillis = PaymentDateUtil.parseDateToMillis(subscription.date)
        val subscriptionWithNextPayment = subscription.copy(
            nextPaymentDate = PaymentDateUtil.calculateNextPaymentDate(
                startDateMillis,
                subscription.frequencyInDays
            )
        )
        dao.insert(subscriptionWithNextPayment)
    }


    suspend fun delete(subscription: Subscription) {
        dao.delete(subscription)
    }

    suspend fun updateNextPaymentDate(subscriptionId: Int, newDate: Long) {
        dao.updateNextPaymentDate(subscriptionId, newDate)
    }

    suspend fun markPaymentCompleted(subscriptionId: Int) {
        val sub = dao.getSubscriptionById(subscriptionId)
        sub?.let {
            val newDate = PaymentDateUtil.calculateNextPaymentDate(it.nextPaymentDate, it.frequencyInDays)
            dao.updateNextPaymentDate(subscriptionId, newDate)
        }
    }

    suspend fun updateOverduePayments() {
        val now = System.currentTimeMillis()
        val subscriptions = dao.getOverdueSubscriptions(userId, now)
        subscriptions.forEach {
            val newDate = PaymentDateUtil.calculateNextPaymentDate(it.nextPaymentDate, it.frequencyInDays)
            dao.updateNextPaymentDate(it.id, newDate)
        }
    }


    suspend fun clearAllData() {
        dao.deleteAllForUser(userId)
    }
}

