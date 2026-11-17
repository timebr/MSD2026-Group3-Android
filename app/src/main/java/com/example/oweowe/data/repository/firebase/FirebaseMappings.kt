package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.model.SyncStatus
import com.google.firebase.firestore.DocumentSnapshot

internal fun DocumentSnapshot.toExpense(groupId: String, syncStatus: SyncStatus): Expense {
    val type = getString("type")
        ?.let { value -> runCatching { ExpenseType.valueOf(value) }.getOrDefault(ExpenseType.EXPENSE) }
        ?: ExpenseType.EXPENSE
    val splitAmong = (get("splitAmong") as? List<*>)
        ?.mapNotNull { it as? String }
        .orEmpty()
    return Expense(
        id = id,
        groupId = getString("groupId") ?: groupId,
        type = type,
        description = getString("description").orEmpty(),
        category = getString("category").orEmpty(),
        amountOre = getLong("amountOre") ?: 0L,
        paidBy = getString("paidBy").orEmpty(),
        splitAmong = splitAmong,
        createdBy = getString("createdBy").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L,
        updatedBy = getString("updatedBy").orEmpty(),
        updatedAt = getLong("updatedAt") ?: 0L,
        syncStatus = syncStatus,
        receiptUri = getString("receiptUri"),
    )
}

internal fun DocumentSnapshot.toReminder(): Reminder = Reminder(
    id = id,
    groupId = getString("groupId").orEmpty(),
    fromUserId = getString("fromUserId").orEmpty(),
    toUserId = getString("toUserId").orEmpty(),
    amountOre = getLong("amountOre") ?: 0L,
    createdAt = getLong("createdAt") ?: 0L,
)
