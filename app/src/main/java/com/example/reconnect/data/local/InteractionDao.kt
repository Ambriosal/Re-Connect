package com.example.reconnect.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionDao {

    // Get all interactions for a contact, newest first
    @Query("SELECT * FROM interactions WHERE contactId = :contactId ORDER BY occurredAt DESC")
    fun getInteractionsForContact(contactId: Long): Flow<List<InteractionEntity>>

    // Get the single most recent interaction for a contact
    // Used to calculate "days since last contact"
    @Query("""
        SELECT * FROM interactions 
        WHERE contactId = :contactId AND countsAsContact = 1
        ORDER BY occurredAt DESC 
        LIMIT 1
    """)
    suspend fun getLastContactingInteraction(contactId: Long): InteractionEntity?

    // Get the most recent interaction across ALL contacts
    // Useful for dashboard sorting
    @Query("""
        SELECT * FROM interactions 
        WHERE countsAsContact = 1
        ORDER BY occurredAt DESC
    """)
    fun getAllLastInteractions(): Flow<List<InteractionEntity>>

    //Insert a new log
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: InteractionEntity): Long

    @Delete
    suspend fun deleteInteraction(interaction: InteractionEntity)

    @Query("SELECT * FROM interactions WHERE id = :id")
    suspend fun getInteractionById(id: Long): InteractionEntity?
}