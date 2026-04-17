package com.example.reconnect.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class ImportedContact(
    val nativeId: String,
    val name: String,
    val phoneNumber: String?
)

fun readContactFromUri(context: Context, contactUri: Uri): ImportedContact? {
    val contentResolver = context.contentResolver

    // ── Step 1: Get the contact's ID and display name
    val contactCursor = contentResolver.query(
        contactUri,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        ),
        null, null, null
    ) ?: return null

    val nativeId: String
    val name: String
    val hasPhone: Boolean

    contactCursor.use { cursor ->
        if (!cursor.moveToFirst()) return null
        nativeId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
        name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)) ?: return null
        hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
    }

    // ── Step 2: Get the phone number if one exists
    var phoneNumber: String? = null
    if (hasPhone) {
        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(nativeId),
            null
        )
        phoneCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                phoneNumber = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )?.normalizePhoneNumber()
            }
        }
    }

    return ImportedContact(
        nativeId = nativeId,
        name = name,
        phoneNumber = phoneNumber
    )
}

// Strips all formatting so numbers store consistently
// +1 (519) 555-0123 and 5195550123 both become 15195550123
fun String.normalizePhoneNumber(): String {
    return this.filter { it.isDigit() || it == '+' }
}

fun readAllPhoneContacts(context: Context): List<ImportedContact> {
    val contacts = mutableListOf<ImportedContact>()
    val contentResolver = context.contentResolver

    // ── Get all contacts that have at least one phone number
    val cursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER
        ),
        "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
        null,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
    ) ?: return emptyList()

    cursor.use {
        while (it.moveToNext()) {
            val nativeId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                ?: continue

            // ── Get first phone number for this contact
            var phoneNumber: String? = null
            val phoneCursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(nativeId),
                null
            )
            phoneCursor?.use { pc ->
                if (pc.moveToFirst()) {
                    phoneNumber = pc.getString(
                        pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )?.normalizePhoneNumber()
                }
            }

            contacts.add(ImportedContact(nativeId, name, phoneNumber))
        }
    }

    return contacts
}