package com.example.reconnect.data.model

// A sealed class is like a strict enum — but each entry can carry data if needed later
// For now they're simple objects. Think of it as a type-safe list of constants.
// Safer than raw strings because the compiler catches typos —
// "Phonne Call" as a string compiles fine, but InteractionType.PhoneCall always resolves
sealed class InteractionType(
    val label: String,   // shown in the UI
    val key: String,     // stored in the database — keep these stable, never rename them
    val emoji: String    // visual aid in the selector
) {
    object PhoneCall  : InteractionType("Phone Call",  "phone_call",  "📞")
    object Text       : InteractionType("Text",        "text",        "💬")
    object InPerson   : InteractionType("In Person",   "in_person",   "🤝")
    object Instagram  : InteractionType("Instagram",   "instagram",   "📱")
    object WhatsApp   : InteractionType("WhatsApp",    "whatsapp",    "💚")
    object Snapchat   : InteractionType("Snapchat",    "snapchat",    "👻")
    object Email      : InteractionType("Email",       "email",       "✉️")
    object Other      : InteractionType("Other",       "other",       "🌐")

    companion object {
        // All types as an ordered list — used to build the selector UI
        val all = listOf(PhoneCall, Text, InPerson, Instagram, WhatsApp, Snapchat, Email, Other)

        // Convert a stored key back to a type — used when displaying history rows
        // Returns null if the key is unrecognized (e.g. old "manual" entries)
        fun fromKey(key: String): InteractionType? = all.find { it.key == key }
    }
}