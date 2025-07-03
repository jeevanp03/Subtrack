package com.example.subtrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val date: String,
    val category: String,
    val renewalsPerYear: Int, // Kept for future use, not used in current calculations
    val frequencyInDays: Int = 30,  // Default to monthly (30 days)
    val nextPaymentDate: Long = System.currentTimeMillis(), // Store as timestamp
    val remindDaysBefore: Int = 1 // How many days before the subscription payment date that the notification should trigger
)
