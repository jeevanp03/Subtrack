package com.example.subtrack

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

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

        // Initialize views
        upcomingRenewalsRecyclerView = findViewById(R.id.upcomingRenewalsRecyclerView)
        totalSubscriptionsText = findViewById(R.id.totalSubscriptionsText)
        totalMonthlyCostText = findViewById(R.id.totalMonthlyCostText)
        addSubscriptionButton = findViewById(R.id.addSubscriptionButton)
        viewCalendarButton = findViewById(R.id.viewCalendarButton)
        fab = findViewById(R.id.fab)

        // Setup RecyclerView
        upcomingRenewalsRecyclerView.layoutManager = LinearLayoutManager(this)
        // TODO: Set up adapter for upcoming renewals

        // Setup click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        addSubscriptionButton.setOnClickListener {
            val intent = Intent(this, AddSubscriptionActivity::class.java)
            startActivity(intent)
        }


        viewCalendarButton.setOnClickListener {
            // TODO: Launch calendar view
            Snackbar.make(fab, "View calendar clicked", Snackbar.LENGTH_SHORT).show()
        }

        fab.setOnClickListener {
            val intent = Intent(this, AddSubscriptionActivity::class.java)
            startActivity(intent)
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