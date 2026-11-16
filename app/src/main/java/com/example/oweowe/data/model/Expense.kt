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
    /** Set when a member edits an existing expense (FR7 update notifications). */
    val updatedBy: String = "",
    val updatedAt: Long = 0,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    /**
     * FR10: the receipt image attached to this expense, or null. Today it is the
     * absolute path of a copy in app-private storage (nothing is uploaded); with
     * Firebase it becomes the download URL from Cloud Storage.
     */
    val receiptUri: String? = null,
)