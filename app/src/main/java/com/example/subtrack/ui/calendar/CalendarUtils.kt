package com.example.subtrack.ui.calendar

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.*

object CalendarUtils {

    private const val TAG = "CalendarUtils"

    fun insertRecurringEvent(
        context: Context,
        title: String,
        description: String,
        startTimeMillis: Long,
        frequencyInDays: Int
    ) {
        if (!hasCalendarPermission(context)) {
            Toast.makeText(context, "Calendar permission not granted", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Missing calendar permission.")
            return
        }

        val calendarId = getPrimaryCalendarId(context)
        if (calendarId == null) {
            Toast.makeText(context, "No available calendar found", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No calendar ID found.")
            return
        }

        val rrule = when (frequencyInDays) {
            7 -> "FREQ=WEEKLY;COUNT=60"
            14 -> "FREQ=WEEKLY;INTERVAL=2;COUNT=60"
            30 -> "FREQ=MONTHLY;COUNT=60"
            90 -> "FREQ=MONTHLY;INTERVAL=3;COUNT=60"
            180 -> "FREQ=MONTHLY;INTERVAL=6;COUNT=60"
            365 -> "FREQ=YEARLY;COUNT=60"
            else -> "FREQ=MONTHLY;COUNT=60"
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startTimeMillis)
            put(CalendarContract.Events.DURATION, "PT1H") // <-- Add this line for a 1-hour duration
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, description)
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.RRULE, rrule)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        if (uri == null) {
            Log.e(TAG, "Failed to insert event into calendar.")
            return
        } else {
            Log.d(TAG, "Event inserted: $uri")
        }

        val eventId = uri.lastPathSegment?.toLongOrNull()
        if (eventId != null) {
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 1440) // 1 day before
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }

            val reminderUri = context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
            if (reminderUri != null) {
                Log.d(TAG, "Reminder set for event ID: $eventId")
            } else {
                Log.e(TAG, "Failed to set reminder for event ID: $eventId")
            }
        } else {
            Log.e(TAG, "Invalid event ID parsed from URI.")
        }
    }

    fun hasCalendarPermission(context: Context): Boolean {
        val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return writeGranted && readGranted
    }

    fun getPrimaryCalendarId(context: Context): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val isPrimary = it.getInt(1) != 0
                val name = it.getString(2)
                Log.d(TAG, "Found calendar: \"$name\" (ID: $id), isPrimary=$isPrimary")
                if (isPrimary) return id
            }

            // Fallback: just return first available calendar
            if (it.moveToFirst()) {
                val fallbackId = it.getLong(0)
                val fallbackName = it.getString(2)
                Log.w(TAG, "No primary calendar. Using fallback: \"$fallbackName\" (ID: $fallbackId)")
                return fallbackId
            }
        }

        Log.e(TAG, "No calendar accounts available.")
        return null
    }
}
