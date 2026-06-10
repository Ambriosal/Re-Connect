package com.example.reconnect

import android.app.Application
import androidx.work.*
import com.example.reconnect.data.local.REConnectDatabase
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.worker.CallLogSyncWorker
import java.util.concurrent.TimeUnit

class REConnectApplication : Application() {

    val database by lazy { REConnectDatabase.getDatabase(this) }
    val repository by lazy { REConnectRepository(database) }

    override fun onCreate() {
        super.onCreate()
        scheduleCallLogSync()
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