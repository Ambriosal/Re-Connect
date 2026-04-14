package com.example.reconnect.data.local

import kotlinx.coroutines.flow.Flow

class REConnectRepository(private val db: REConnectDatabase) {

    // ── Contacts ──────────────────────────────────────
    fun getAllContacts(): Flow<List<ContactEntity>> =
        db.contactDao().getAllContacts()

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

    fun fetchSystemContacts(){
        //query Contacts.Contract.Contacts.CONTENT_URI
    }

    suspend fun deleteContactById(contactId: Long) {
        db.contactDao().deleteContactById(contactId)
    }
}