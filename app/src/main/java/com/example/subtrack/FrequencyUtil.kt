package com.example.subtrack

object FrequencyUtil {
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