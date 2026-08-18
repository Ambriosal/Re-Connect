package com.example.reconnect

import android.app.Application
import androidx.work.*
import com.example.reconnect.data.local.AppPreferences
import com.example.reconnect.data.local.REConnectDatabase
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.util.NotificationHelper
import com.example.reconnect.worker.CallLogSyncWorker
import com.example.reconnect.worker.ReminderCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class REConnectApplication : Application() {

    val database by lazy { REConnectDatabase.getDatabase(this) }
    val repository by lazy { REConnectRepository(database) }
    val appPreferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        scheduleCallLogSync()

        // Read the persisted reminder-check time once at startup and (re)schedule the
        // daily worker to match. SettingsScreen calls scheduleReminderCheck directly
        // whenever the user changes the time, so this only matters for cold starts.
        CoroutineScope(Dispatchers.IO).launch {
            val (hour, minute) = appPreferences.reminderTime.first()
            scheduleReminderCheck(hour, minute)
        }
    }

    // Schedules (or reschedules) the once-a-day overdue-contact check to first run at the
    // next occurrence of hour:minute, then every 24 hours after that. Uses UPDATE so
    // calling this again (e.g. from Settings) replaces the pending schedule instead of
    // stacking a second periodic job.
    fun scheduleReminderCheck(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderRequest
        )
    }

    private fun scheduleCallLogSync() {
        val syncRequest = PeriodicWorkRequestBuilder<CallLogSyncWorker>(
            repeatInterval = 3,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "call_log_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}