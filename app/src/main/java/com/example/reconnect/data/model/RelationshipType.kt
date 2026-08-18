package com.example.reconnect.data.model

sealed class RelationshipType(
    val label: String,
    val key: String,      // stored in DB — never rename these
    val emoji: String,
    val defaultReminderDays: Int // pre-fills a new contact's reminder interval when this tag is picked
) {
    object Partner       : RelationshipType("Partner",       "partner",       "❤️", 3)
    object CloseFriend   : RelationshipType("Close Friend",  "close_friend",  "👫", 7)
    object Family        : RelationshipType("Family",        "family",        "👨‍👩‍👧", 10)
    object Friend        : RelationshipType("Friend",        "friend",        "🤝", 14)
    object Classmate     : RelationshipType("Classmate",     "classmate",     "🎓", 21)
    object Colleague     : RelationshipType("Colleague",     "colleague",     "🏢", 30)
    object Professional  : RelationshipType("Professional",  "professional",  "💼", 45)
    object Acquaintance  : RelationshipType("Acquaintance",  "acquaintance",  "👋", 60)
    object Other         : RelationshipType("Other",         "other",         "🌐", 30)

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
