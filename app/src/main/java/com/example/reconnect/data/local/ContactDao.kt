package com.example.reconnect.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    // Get all contacts, ordered alphabetically
    // Flow - the UI automatically updates when the data changes
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    // Get a single contact by ID
    @Query("SELECT * FROM contacts WHERE id = :contactId")
    suspend fun getContactById(contactId: Long): ContactEntity?

    // Insert a new contact — returns the new row's ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    // Update an existing contact
    @Update
    suspend fun updateContact(contact: ContactEntity)

    // Delete a contact (their interactions delete automatically via CASCADE)
    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: Long)
}