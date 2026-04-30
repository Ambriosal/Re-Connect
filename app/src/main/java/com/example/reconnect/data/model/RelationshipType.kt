package com.example.reconnect.data.model

sealed class RelationshipType(
    val label: String,
    val key: String,      // stored in DB — never rename these
    val emoji: String
) {
    object Family        : RelationshipType("Family",        "family",        "👨‍👩‍👧")
    object CloseFriend   : RelationshipType("Close Friend",  "close_friend",  "👫")
    object Friend        : RelationshipType("Friend",        "friend",        "🤝")
    object Colleague     : RelationshipType("Colleague",     "colleague",     "🏢")
    object Classmate     : RelationshipType("Classmate",     "classmate",     "🎓")
    object Professional  : RelationshipType("Professional",  "professional",  "💼")
    object Acquaintance  : RelationshipType("Acquaintance",  "acquaintance",  "👋")
    object Partner       : RelationshipType("Partner",       "partner",       "❤️")
    object Other         : RelationshipType("Other",         "other",         "🌐")

    companion object {
        val all = listOf(
            Family, CloseFriend, Friend, Colleague,
            Classmate, Professional, Acquaintance, Partner, Other
        )

        // Convert stored key → type for display
        // Returns null gracefully for any old free-text entries
        fun fromKey(key: String): RelationshipType? = all.find { it.key == key }

        // Formats a stored key for display — falls back to raw string for old entries
        fun displayFrom(key: String): String =
            fromKey(key)?.let { "${it.emoji} ${it.label}" } ?: key
    }
}