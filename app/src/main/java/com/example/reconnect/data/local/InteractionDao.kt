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



    //Delete 1 interaction
    @Delete
    suspend fun deleteInteraction(interaction: InteractionEntity)

    //Delete interaction from specific contact
    @Query("SELECT * FROM interactions WHERE id = :id")
    suspend fun getInteractionById(id: Long): InteractionEntity?

    //Delete all
    @Query("DELETE FROM interactions")
    suspend fun deleteAllInteractions()

    // Check if an auto-detected call interaction already exists for a contact on a given day
    // Used by CallLogSyncWorker to avoid duplicate inserts on repeated syncs
    @Query("""
    SELECT COUNT(*) FROM interactions
    WHERE contactId = :contactId
    AND source = 'auto_detected'
    AND occurredAt BETWEEN :dayStart AND :dayEnd""")
    suspend fun countAutoInteractionsOnDay(
        contactId: Long,
        dayStart: Long,
        dayEnd: Long
    ): Int


    // Finds an existing auto-detected call entry for this contact on this day
    // Returns null if none exists yet
    @Query("""
    SELECT * FROM interactions
    WHERE contactId = :contactId
    AND source = 'auto_detected'
    AND occurredAt BETWEEN :dayStart AND :dayEnd
    LIMIT 1""")
    suspend fun getAutoInteractionOnDay(
        contactId: Long,
        dayStart: Long,
        dayEnd: Long
    ): InteractionEntity?

    @Update
    suspend fun updateInteraction(interaction: InteractionEntity)
}