package com.example.reconnect.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────
// TABLE 1: Contact
// One row per person you want to stay in touch with
// ─────────────────────────────────────────

@Entity(tableName = "contacts")
data class ContactEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val phoneNumber: String?,           // nullable — some contacts may be Instagram-only, etc.

    val relationshipLabel: String = "", // "Family", "Friend", "Colleague", etc.

    val reminderFrequencyDays: Int = 14, // default: remind every 2 weeks

    val notes: String = "",             // general notes about the person (not per-interaction)

    val nativeContactId: String? = null, // ID from phone's contacts app — for "refresh" feature

    val countsAsContactPlatforms: String = "call,sms,instagram,whatsapp,in_person,email,other",
    // Comma-separated list of which platforms reset the reminder clock
    // Stored as a string for simplicity — parsed when needed

    val createdAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────
// TABLE 2: Interaction
// One row per time you connected with someone
// ─────────────────────────────────────────
@Entity(
    tableName = "interactions",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE  // if contact is deleted, their interactions go too
        )
    ],
    indices = [Index("contactId")]         // speeds up "get all interactions for contact X"
)
data class InteractionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val contactId: Long,                  // which contact this interaction belongs to

    val type: String,
    // "call" | "sms" | "instagram" | "WhatsApp" |
    // "in_person" | "email" | "FaceTime" | "LinkedIn" | "other"
    val source: String = "manual",        // "manual" | "auto_detected"

    val occurredAt: Long= System.currentTimeMillis(),                 // timestamp in milliseconds — when the contact happened

    val notes: String? = "",               // Optional typed or voice-transcribed notes about the conversation

    val countsAsContact: Boolean = true   // does this interaction reset the reminder clock?
)
