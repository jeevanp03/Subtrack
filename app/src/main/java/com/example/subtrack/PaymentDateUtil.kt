package com.example.subtrack

import java.util.Calendar
import java.util.concurrent.TimeUnit

object PaymentDateUtil {
    fun calculateNextPaymentDate(startDate: String, frequencyInDays: Int): Long {
        val calendar = Calendar.getInstance()
        val startDateParts = startDate.split("-")
        calendar.set(
            startDateParts[0].toInt(),  // Year
            startDateParts[1].toInt() - 1,  // Month (0-based)
            startDateParts[2].toInt()  // Day
        )
        
        val today = System.currentTimeMillis()
        var nextDate = calendar.timeInMillis
        
        // Keep adding frequency until we find a date in the future
        while (nextDate < today) {
            nextDate = addFrequencyToDate(nextDate, frequencyInDays)
        }
        
        return nextDate
    }
    
    fun calculateNextPaymentDateFromCurrent(currentPaymentDate: Long, frequencyInDays: Int): Long {
        return addFrequencyToDate(currentPaymentDate, frequencyInDays)
    }
    
    private fun addFrequencyToDate(date: Long, frequencyInDays: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        
        when (frequencyInDays) {
            30 -> {
                // Monthly - add one month while preserving day of month when possible
                calendar.add(Calendar.MONTH, 1)
            }
            90 -> {
                // Quarterly - add 3 months
                calendar.add(Calendar.MONTH, 3)
            }
            180 -> {
                // Semiannually - add 6 months
                calendar.add(Calendar.MONTH, 6)
            }
            365 -> {
                // Annually - add one year
                calendar.add(Calendar.YEAR, 1)
            }
            else -> {
                // For other frequencies, add the exact number of days
                calendar.add(Calendar.DAY_OF_MONTH, frequencyInDays)
            }
        }
        
        return calendar.timeInMillis
    }
    
    fun isPaymentDatePassed(paymentDate: Long): Boolean {
        val today = System.currentTimeMillis()
        return paymentDate < today
    }
    
    fun getDaysUntilPayment(paymentDate: Long): Int {
        val today = System.currentTimeMillis()
        val diffInMillis = paymentDate - today
        return TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
    }
    
    fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    fun formatDateForDisplay(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return String.format(
            "%d/%d/%d",
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.YEAR)
        )
    }
    
    fun getFrequencyLabel(frequencyInDays: Int): String {
        return when (frequencyInDays) {
            7 -> "Weekly"
            14 -> "Biweekly"
            30 -> "Monthly"
            90 -> "Quarterly"
            180 -> "Semiannually"
            365 -> "Annually"
            else -> "$frequencyInDays days"
        }
    }
} 