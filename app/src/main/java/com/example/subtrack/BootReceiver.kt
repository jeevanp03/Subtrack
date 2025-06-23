package com.example.subtrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmHelper.scheduleDailyReminder(context)
            Log.d("BootReceiver", "Alarm rescheduled after reboot")
        }
    }
}
