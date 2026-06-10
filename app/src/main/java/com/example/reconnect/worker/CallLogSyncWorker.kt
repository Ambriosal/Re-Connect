package com.example.reconnect.worker

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reconnect.REConnectApplication
import com.example.reconnect.data.local.InteractionEntity
import com.example.reconnect.util.PhoneNumberUtils

class CallLogSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        // 1. Guard — if permission was revoked, bail out cleanly
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success() // not a failure — just nothing to do
        }

        android.util.Log.d("CallLogSync", "Worker started") //log check

        // 2. Get repository from Application (same pattern as your ViewModels)
        val repository = (context.applicationContext as REConnectApplication).repository

        // 3. Load all contacts that have a phone number
        val contacts = repository.getAllContactsOnce()
            .filter { it.phoneNumber != null }

        // 4. Build a lookup map: normalizedNumber → ContactEntity
        //    This way we do O(1) lookups instead of looping contacts for every call log row
        val numberToContact = contacts
            .mapNotNull { contact ->
                val normalized = PhoneNumberUtils.normalize(contact.phoneNumber)
                if (normalized != null) normalized to contact else null
            }
            .toMap()

        android.util.Log.d("CallLogSync", "Contacts with phone numbers: ${numberToContact.size}")

        if (numberToContact.isEmpty()) return Result.success()

        // 5. Query the call log
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,       // the phone number
                CallLog.Calls.DATE,         // timestamp in ms
                CallLog.Calls.TYPE          // incoming / outgoing / missed
            ),
            null, null,
            "${CallLog.Calls.DATE} DESC"    // newest first
        ) ?: return Result.success()

        // 6. Count calls per contact per day
// Key: contactId+date string for dedup, Value: Triple(contactId, timestamp, count)
        data class DayCallSummary(val contactId: Long, val timestamp: Long, var count: Int)

        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val summaries = mutableMapOf<String, DayCallSummary>()

        cursor.use { c ->
            val numberCol = c.getColumnIndex(CallLog.Calls.NUMBER)
            val dateCol   = c.getColumnIndex(CallLog.Calls.DATE)
            val typeCol   = c.getColumnIndex(CallLog.Calls.TYPE)

            while (c.moveToNext()) {
                val rawNumber = c.getString(numberCol)
                val callDate  = c.getLong(dateCol)
                val callType  = c.getInt(typeCol)

                if (callType == CallLog.Calls.MISSED_TYPE) continue

                val normalized = PhoneNumberUtils.normalize(rawNumber) ?: continue
                val contact    = numberToContact[normalized] ?: continue
                if (callDate < contact.createdAt) continue

                val dayKey = "${contact.id}_${dateFormatter.format(java.util.Date(callDate))}"

                val existing = summaries[dayKey]
                if (existing == null) {
                    summaries[dayKey] = DayCallSummary(contact.id, callDate, 1)
                } else {
                    existing.count++  // same contact, same day — just increment
                }
            }
        }

        android.util.Log.d("CallLogSync", "Found ${summaries.size} contact-day pairs to sync")

        // 7. Upsert each summary into the DB
        summaries.values.forEach { summary ->
            repository.upsertAutoCallInteraction(summary.contactId, summary.timestamp, summary.count)
        }

        return Result.success()
    }
}