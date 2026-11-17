package com.example.oweowe.ui.groups.contacts

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** One device contact that can be suggested as a group member (S8 / S10). */
data class ContactSuggestion(
    val name: String,
    val email: String,
)

/** True when the user has allowed the app to read contacts. */
fun hasContactsPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Contacts that have an email address, de-duplicated by email and sorted by
 * name. The query runs on the device; nothing is uploaded. Returns an empty list
 * when the permission is missing or is revoked while reading.
 *
 * Contacts are only a shortcut for filling in S10: the member is still added by
 * email through the existing `GroupRepository`, so there is no new backend call.
 */
fun readContactsWithEmail(resolver: ContentResolver): List<ContactSuggestion> {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Email.ADDRESS,
    )
    val byEmail = LinkedHashMap<String, ContactSuggestion>()
    try {
        resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val emailIndex =
                cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                val email = cursor.getString(emailIndex)?.trim()?.lowercase().orEmpty()
                if (email.isEmpty()) continue
                val name = cursor.getString(nameIndex)?.trim().orEmpty().ifEmpty { email }
                byEmail.putIfAbsent(email, ContactSuggestion(name = name, email = email))
            }
        }
    } catch (_: SecurityException) {
        return emptyList()
    }
    return byEmail.values.sortedBy { it.name.lowercase() }
}
