package com.example.oweowe.data.model

data class User(
    val uid: String = "",          // Firebase Auth UID = document ID
    val fullName: String = "",
    val email: String = "",        // always lower case
    val phone: String = "",
    val pushEnabled: Boolean = true,
    val createdAt: Long = 0,
)