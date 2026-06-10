package com.example.reconnect.util

object PhoneNumberUtils {

    /**
     * Strips everything except digits, then takes the last 10.
     * "+1 (416) 555-0123" → "4165550123"
     * "416-555-0123"      → "4165550123"
     * "+14165550123"      → "4165550123"
     *
     * Last-10 strategy works for North American numbers.
     */
    fun normalize(raw: String?): String? {
        if (raw == null) return null
        val digitsOnly = raw.filter { it.isDigit() }
        return if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else null
    }
}