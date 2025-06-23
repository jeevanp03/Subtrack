package com.example.subtrack

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "subtrack_channel_id"
        const val NOTIFICATION_ID_BASE = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        val db = SubscriptionDatabase.getDatabase(context)
        val dao = db.subscriptionDao()

        CoroutineScope(Dispatchers.IO).launch {
            val now = Calendar.getInstance()
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val dueSubscriptions = dao.getAllSync().filter { sub ->
                val paymentDateCal = Calendar.getInstance().apply {
                    timeInMillis = sub.nextPaymentDate
                }
                paymentDateCal.add(Calendar.DAY_OF_YEAR, -sub.remindDaysBefore)

                paymentDateCal.timeInMillis in todayStart.timeInMillis..todayEnd.timeInMillis
            }

            if (dueSubscriptions.isNotEmpty()) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                dueSubscriptions.forEachIndexed { index, sub ->
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Upcoming Subscription")
                        .setContentText("Payment for ${sub.name} is due in ${sub.remindDaysBefore} day(s).")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    notificationManager.notify(NOTIFICATION_ID_BASE + index, notification)
                }
            }
        }
    }


    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SubTrack Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming subscription payments"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
