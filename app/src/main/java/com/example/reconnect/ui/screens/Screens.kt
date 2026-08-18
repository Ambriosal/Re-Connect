package com.example.reconnect.ui.screens

object Screen {
    const val Home = "home" // Home screen — overdue / due-this-week digest
    const val Contacts = "contacts" //Contacts screen
    const val Calendar = "calendar" // Calendar screen (placeholder for now)
    const val Stats = "stats" // Stats screen — reached from the Home screen's stats widget
    const val Picker = "picker" // Contacts Picker screen
    const val ContactDetail = "contact_detail/{contactId}" //Contact detail screen for specified contact

    // Helper function to build the actual route string with a real ID
    // e.g. contactDetailRoute(42) → "contact_detail/42"
    fun contactDetailRoute(contactId: Long) = "contact_detail/$contactId"
    const val Settings = "settings" // Settings screen

    const val ContactSettings = "contact_settings/{contactId}" // Per-contact settings screen
    fun contactSettingsRoute(contactId: Long) = "contact_settings/$contactId"
}