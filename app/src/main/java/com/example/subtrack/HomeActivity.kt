package com.example.subtrack

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import android.widget.TextView
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class HomeActivity : AppCompatActivity() {
    private lateinit var upcomingRenewalsRecyclerView: RecyclerView
    private lateinit var totalSubscriptionsText: TextView
    private lateinit var totalMonthlyCostText: TextView
    private lateinit var addSubscriptionButton: MaterialButton
    private lateinit var viewCalendarButton: MaterialButton
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Schedule repeating notifications every 10 seconds
        scheduleNotification(this)

        // Initialize views
        upcomingRenewalsRecyclerView = findViewById(R.id.upcomingRenewalsRecyclerView)
        totalSubscriptionsText = findViewById(R.id.totalSubscriptionsText)
        totalMonthlyCostText = findViewById(R.id.totalMonthlyCostText)
        addSubscriptionButton = findViewById(R.id.addSubscriptionButton)
        viewCalendarButton = findViewById(R.id.viewCalendarButton)
        fab = findViewById(R.id.fab)
        createNotificationChannel(this)

        // Setup RecyclerView
        upcomingRenewalsRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Set up adapter for upcoming renewals

        // Setup click listeners
        setupClickListeners()
    }
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "subtrack_channel_id"
            val channelName = "SubTrack Notifications"
            val channelDescription = "Notifications for subscription tracking"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun scheduleNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val interval = 2_000L // 2 seconds

        // Cancel any existing alarms first
        alarmManager.cancel(pendingIntent)

        // Set a repeating alarm every 10 seconds
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + interval,
            interval,
            pendingIntent
        )
    }

    private fun setupClickListeners() {
        addSubscriptionButton.setOnClickListener {
            // TODO: Launch add subscription activity
            Snackbar.make(fab, "Add subscription clicked", Snackbar.LENGTH_SHORT).show()
        }

        viewCalendarButton.setOnClickListener {
            // TODO: Launch calendar view
            Snackbar.make(fab, "View calendar clicked", Snackbar.LENGTH_SHORT).show()
        }

        fab.setOnClickListener {
            // TODO: Launch add subscription activity
            Snackbar.make(fab, "Add subscription clicked", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                // TODO: Show notifications
                Snackbar.make(fab, "Notifications clicked", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_settings -> {
                // TODO: Launch settings activity
                Snackbar.make(fab, "Settings clicked", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.action_profile -> {
                // TODO: Launch profile activity
                Snackbar.make(fab, "Profile clicked", Snackbar.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
