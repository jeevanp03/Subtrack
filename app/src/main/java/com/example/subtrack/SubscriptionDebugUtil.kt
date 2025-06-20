package com.example.subtrack

import android.util.Log

object SubscriptionDebugUtil {
    private const val TAG = "SubscriptionDebug"
    
    fun logSubscriptionDetails(subscription: Subscription) {
        Log.d(TAG, "Subscription: ${subscription.name}")
        Log.d(TAG, "  Start Date: ${subscription.date}")
        Log.d(TAG, "  Frequency: ${PaymentDateUtil.getFrequencyLabel(subscription.frequencyInDays)} (${subscription.frequencyInDays} days)")
        Log.d(TAG, "  Next Payment: ${PaymentDateUtil.formatDateForDisplay(subscription.nextPaymentDate)}")
        Log.d(TAG, "  Days Until Payment: ${PaymentDateUtil.getDaysUntilPayment(subscription.nextPaymentDate)}")
        Log.d(TAG, "  Is Overdue: ${PaymentDateUtil.isPaymentDatePassed(subscription.nextPaymentDate)}")
    }
    
    fun testRecurringDateCalculation(startDate: String, frequencyInDays: Int, iterations: Int = 5) {
        Log.d(TAG, "Testing recurring date calculation:")
        Log.d(TAG, "  Start Date: $startDate")
        Log.d(TAG, "  Frequency: ${PaymentDateUtil.getFrequencyLabel(frequencyInDays)}")
        
        var currentDate = PaymentDateUtil.calculateNextPaymentDate(startDate, frequencyInDays)
        
        for (i in 1..iterations) {
            val formattedDate = PaymentDateUtil.formatDateForDisplay(currentDate)
            Log.d(TAG, "  Payment $i: $formattedDate")
            currentDate = PaymentDateUtil.calculateNextPaymentDateFromCurrent(currentDate, frequencyInDays)
        }
    }
} 