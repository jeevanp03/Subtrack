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
        
        while (nextDate < today) {
            nextDate += TimeUnit.DAYS.toMillis(frequencyInDays.toLong())
        }
        
        return nextDate
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
} 