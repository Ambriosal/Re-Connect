package com.example.reconnect.data.local

import com.example.reconnect.util.ImportedContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class REConnectRepository(private val db: REConnectDatabase) {

    // ── Contacts ──────────────────────────────────────
    fun getAllContacts(): Flow<List<ContactEntity>> =
        db.contactDao().getAllContacts()

    // Re-emits whenever EITHER the contacts table OR the interactions table changes —
    // needed so logging an interaction immediately refreshes "last contacted" on the list
    // screen instead of waiting for a contacts-table change (e.g. app restart).
    fun getAllContactsWithLastInteraction(): Flow<List<Pair<ContactEntity, InteractionEntity?>>> =
        combine(
            db.contactDao().getAllContacts(),
            db.interactionDao().getAllLastInteractions()
        ) { contacts, lastInteractions ->
            contacts.map { contact ->
                contact to lastInteractions.firstOrNull { it.contactId == contact.id }
            }
        }

    suspend fun getContactById(id: Long): ContactEntity? =
        db.contactDao().getContactById(id)

    suspend fun insertContact(contact: ContactEntity): Long =
        db.contactDao().insertContact(contact)

    suspend fun updateContact(contact: ContactEntity) =
        db.contactDao().updateContact(contact)

    suspend fun deleteContact(contact: ContactEntity) =
        db.contactDao().deleteContact(contact)

    // ── Interactions ──────────────────────────────────
    fun getInteractionsForContact(contactId: Long): Flow<List<InteractionEntity>> =
        db.interactionDao().getInteractionsForContact(contactId)

    // All countsAsContact interactions across every contact — used by the Stats screen
    // (monthly call totals, weekly goal progress, streak) despite the DAO method's name.
    fun getAllInteractions(): Flow<List<InteractionEntity>> =
        db.interactionDao().getAllLastInteractions()

    suspend fun getLastInteraction(contactId: Long): InteractionEntity? =
        db.interactionDao().getLastContactingInteraction(contactId)

    // ← Entity construction now lives here, not in the ViewModel
    suspend fun logInteraction(
        contactId: Long,
        platform: String,
        notes: String = "",
        countsAsContact: Boolean = true
    ): Long {
        return db.interactionDao().insertInteraction(
            InteractionEntity(
                contactId = contactId,
                type = platform,
                source = "manual",
                occurredAt = System.currentTimeMillis(),
                notes = notes,
                countsAsContact = countsAsContact
            )
        )
    }

    suspend fun deleteInteraction(interaction: InteractionEntity) =
        db.interactionDao().deleteInteraction(interaction)

    // ← Logged from a notification quick-action (Call/Text button tap).
    // Marked needsFollowUp so the "how did it go?" prompt picks it up later.
    suspend fun logInteractionFromNotification(contactId: Long, type: String): Long {
        return db.interactionDao().insertInteraction(
            InteractionEntity(
                contactId = contactId,
                type = type,
                source = "notification",
                occurredAt = System.currentTimeMillis(),
                notes = "",
                countsAsContact = true,
                needsFollowUp = true
            )
        )
    }

    suspend fun getPendingFollowUps(): List<InteractionEntity> =
        db.interactionDao().getPendingFollowUps()

    suspend fun answerFollowUp(interactionId: Long, notes: String) {
        val interaction = db.interactionDao().getInteractionById(interactionId) ?: return
        db.interactionDao().updateInteraction(
            interaction.copy(notes = notes, needsFollowUp = false)
        )
    }

    suspend fun dismissFollowUp(interactionId: Long) {
        val interaction = db.interactionDao().getInteractionById(interactionId) ?: return
        db.interactionDao().updateInteraction(interaction.copy(needsFollowUp = false))
    }

    // Contacts whose last counted interaction is older than their reminderFrequencyDays
    // (or who have never been logged at all — those are overdue immediately).
    suspend fun getContactsDueForReminder(): List<ContactEntity> {
        val now = System.currentTimeMillis()
        return getAllContactsOnce().filter { contact ->
            if (!contact.notificationsEnabled) return@filter false
            val lastInteraction = getLastInteraction(contact.id)
            val daysSince = lastInteraction?.let { (now - it.occurredAt) / 86_400_000L }
            daysSince == null || daysSince >= contact.reminderFrequencyDays
        }
    }

    fun fetchSystemContacts(){
        //query Contacts.Contract.Contacts.CONTENT_URI
    }

    suspend fun deleteContactById(contactId: Long) {
        db.contactDao().deleteContactById(contactId)
    }

    suspend fun importContact(imported: ImportedContact): Long {
        return db.contactDao().insertContact(
            ContactEntity(
                name = imported.name,
                phoneNumber = imported.phoneNumber,
                nativeContactId = imported.nativeId,
                photoUri = imported.photoUri,
                relationshipLabel = "",
                reminderFrequencyDays = 14
            )
        )
    }

    //Delete all!
    // ── Nuclear option — wipes everything, used for dev testing via Settings
    suspend fun clearAllData() {
        db.interactionDao().deleteAllInteractions()  // interactions first
        db.contactDao().deleteAllContacts()          // then contacts
    }

    // Returns all contacts as a plain list (not Flow) — needed for background worker
    suspend fun getAllContactsOnce(): List<ContactEntity> {
        return db.contactDao().getAllContactsOnce()
    }

    // Returns true if an auto-detected interaction already exists for that calendar day
    suspend fun hasInteractionOnDay(contactId: Long, timestampMs: Long): Boolean {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        val dayEnd   = dayStart + 86_400_000L - 1 // end of that same day

        return db.interactionDao().countAutoInteractionsOnDay(contactId, dayStart, dayEnd) > 0
    }

    // Raw insert — used by background workers that construct the entity themselves.
    suspend fun insertInteraction(interaction: InteractionEntity): Long =
        db.interactionDao().insertInteraction(interaction)

    suspend fun upsertAutoCallInteraction(
        contactId: Long,
        timestampMs: Long,
        callCount: Int
    ) {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestampMs
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayStart = calendar.timeInMillis
        val dayEnd   = dayStart + 86_400_000L - 1

        val existing = db.interactionDao().getAutoInteractionOnDay(contactId, dayStart, dayEnd)

        if (existing != null) {
            // Row exists — update it with the new count
            // Only update if count changed (avoids unnecessary writes)
            if (existing.callCount != callCount) {
                db.interactionDao().updateInteraction(existing.copy(callCount = callCount))
            }
        } else {
            // No row yet — insert fresh
            db.interactionDao().insertInteraction(
                InteractionEntity(
                    contactId       = contactId,
                    type            = "call",
                    source          = "auto_detected",
                    occurredAt      = timestampMs,
                    notes           = "",
                    countsAsContact = true,
                    callCount       = callCount
                )
            )
        }
    }

}