package com.example.oweowe.data.model

data class User(
    val uid: String = "",          // Firebase Auth UID = document ID
    val fullName: String = "",
    val email: String = "",        // always lower case
    val phone: String = "",
    val pushEnabled: Boolean = true,
    val createdAt: Long = 0,
    /**
     * FR2: the profile photo, or null to fall back to the initials avatar.
     * Today it is the absolute path of a copy in app-private storage (nothing
     * is uploaded); with Firebase it becomes the download URL from Storage.
     */
    val photoUri: String? = null,
)