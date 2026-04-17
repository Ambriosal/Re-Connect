package com.example.reconnect.data.model

import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.InteractionEntity

// Converts a raw database entity into a UI-safe model
// Takes the last interaction separately since it comes from a different table
fun ContactEntity.toUiModel(lastInteraction: InteractionEntity?): ContactUiModel {
    return ContactUiModel(
        id = this.id,
        name = this.name,
        phoneNumber = this.phoneNumber,
        relationshipLabel = this.relationshipLabel,
        reminderFrequencyDays = this.reminderFrequencyDays,
        notes = this.notes,
        lastContactedAt = lastInteraction?.occurredAt,
        nativeContactId = this.nativeContactId
    )
}