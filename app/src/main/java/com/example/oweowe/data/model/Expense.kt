package com.example.oweowe.data.model

enum class ExpenseType { EXPENSE, SETTLEMENT }

enum class SyncStatus { PENDING, SYNCED, FAILED }

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val type: ExpenseType = ExpenseType.EXPENSE,
    val description: String = "",
    val category: String = "",
    val amountOre: Long = 0,
    val paidBy: String = "",
    val splitAmong: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = 0,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
)