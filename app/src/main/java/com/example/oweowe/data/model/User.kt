package com.example.oweowe.data.model

data class User(
    val uid: String = "",          // Firebase Auth UID = document ID
    val fullName: String = "",
    val email: String = "",        // always lower case
    val phone: String = "",
    /**
     * [phone] reduced to its digits (a leading Danish +45 is dropped), so
     * "add member by phone" can query it without depending on formatting.
     */
    val phoneNormalized: String = "",
    val pushEnabled: Boolean = true,
    val createdAt: Long = 0,
    /**
     * FR2: the profile photo, or null to fall back to the initials avatar.
     * Today it is the absolute path of a copy in app-private storage (nothing
     * is uploaded); with Firebase it becomes the download URL from Storage.
     */
    val photoUri: String? = null,
) {
    companion object {
        /**
         * Shown for a group member whose profile document has not arrived (or is
         * gone). Balances and splits still use [uid]; only the label is a stand-in,
         * so the list must not leak the raw Firestore UID as a name.
         */
        const val UNKNOWN_FULL_NAME = "Unknown member"
    }
}