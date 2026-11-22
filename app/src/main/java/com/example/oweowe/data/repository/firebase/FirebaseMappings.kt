package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.model.User
import com.google.firebase.firestore.DocumentSnapshot

// The document ID *is* the Auth UID (see User.uid), so it wins over a copied
// `uid` field: a stale field must not hide the member or break balance lookups.
internal fun DocumentSnapshot.toUser(): User = User(
    uid = id,
    fullName = getString("fullName").orEmpty(),
    email = getString("email").orEmpty().lowercase(),
    phone = getString("phone").orEmpty(),
    phoneNormalized = getString("phoneNormalized").orEmpty(),
    pushEnabled = getBoolean("pushEnabled") ?: true,
    createdAt = getLong("createdAt") ?: 0L,
    photoUri = getString("photoUri"),
)

internal fun DocumentSnapshot.toGroup(): Group = Group(
    id = id,
    name = getString("name").orEmpty(),
    description = getString("description").orEmpty(),
    createdBy = getString("createdBy").orEmpty(),
    createdAt = getLong("createdAt") ?: 0L,
    memberIds = (get("memberIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
)

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
