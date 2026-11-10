package com.example.oweowe.data.model

data class Reminder(
    val id: String = "",
    val groupId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val amountOre: Long = 0,
    val createdAt: Long = 0,
)