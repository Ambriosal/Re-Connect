package com.example.reconnect.data.model

data class ContactUiModel(
    val id: Long,
    val name: String,
    val phoneNumber: String?,
    val relationshipLabel: String,
    val reminderFrequencyDays: Int,
    val notes: String,
    val lastContactedAt: Long?,
    val nativeContactId: String?
)