package com.example.reconnect.ui.screens

object Screen {
    const val Contacts = "contacts"
    const val Picker = "picker"

    // The {contactId} is a path parameter — like "/contact/:id" in Express
    // This is how Compose Navigation passes data between screens
    const val ContactDetail = "contact_detail/{contactId}"

    // Helper function to build the actual route string with a real ID
    // e.g. contactDetailRoute(42) → "contact_detail/42"
    fun contactDetailRoute(contactId: Long) = "contact_detail/$contactId"
}