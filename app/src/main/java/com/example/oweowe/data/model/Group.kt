package com.example.oweowe.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0,
    val memberIds: List<String> = emptyList(),
)