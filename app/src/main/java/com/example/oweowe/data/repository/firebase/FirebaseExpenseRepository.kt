package com.example.oweowe.data.repository.firebase

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.domain.canSettleUp
import com.example.oweowe.domain.canWriteExpense
import com.example.oweowe.sync.SyncStatusStore
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseExpenseRepository(
    private val syncStatusStore: SyncStatusStore = SyncStatusStore(),
    private val auth: FirebaseAuth = Firebase.auth,
    private val db: FirebaseFirestore = Firebase.firestore,
) : ExpenseRepository {

    private fun expenses(groupId: String) =
        db.collection("groups").document(groupId).collection("expenses")

    override fun observeExpenses(groupId: String): Flow<List<Expense>> =
        combine(
            expenses(groupId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots(),
            syncStatusStore.statuses,
        ) { snapshot, statuses ->
            snapshot.documents.map { doc ->
                val status = statuses[doc.id]
                    ?: if (doc.metadata.hasPendingWrites()) SyncStatus.PENDING else SyncStatus.SYNCED
                doc.toExpense(groupId, status)
            }
        }

    override suspend fun addExpense(
        groupId: String,
        description: String,
        category: String,
        amountOre: Long,
        paidBy: String,
        splitAmong: List<String>,
        receiptUri: String?,
    ): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (amountOre <= 0) return AppResult.Error(ErrorReason.INVALID_AMOUNT)
        if (splitAmong.isEmpty()) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val memberIds = when (val members = db.groupMemberIds(groupId)) {
            is AppResult.Success -> members.value
            is AppResult.Error -> return AppResult.Error(members.reason)
        }
        if (!canWriteExpense(uid, memberIds, paidBy, splitAmong)) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }
        val ref = expenses(groupId).document()
        val data = mutableMapOf<String, Any>(
            "id" to ref.id,
            "groupId" to groupId,
            "type" to ExpenseType.EXPENSE.name,
            "description" to description.trim(),
            "category" to category.trim(),
            "amountOre" to amountOre,
            "paidBy" to paidBy,
            "splitAmong" to splitAmong.distinct(),
            "createdBy" to uid,
            "createdAt" to System.currentTimeMillis(),
        )
        if (receiptUri != null) data["receiptUri"] = receiptUri
        // No await(): shown as PENDING from the local cache, synced by Firestore.
        track(ref.id, description.trim().ifBlank { category.trim() }, ref.set(data))
        return AppResult.Success(Unit)
    }

    override suspend fun updateExpense(
        groupId: String,
        expenseId: String,
        description: String,
        category: String,
        amountOre: Long,
        paidBy: String,
        splitAmong: List<String>,
        receiptUri: String?,
    ): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (amountOre <= 0) return AppResult.Error(ErrorReason.INVALID_AMOUNT)
        if (splitAmong.isEmpty()) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val memberIds = when (val members = db.groupMemberIds(groupId)) {
            is AppResult.Success -> members.value
            is AppResult.Error -> return AppResult.Error(members.reason)
        }
        if (!canWriteExpense(uid, memberIds, paidBy, splitAmong)) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }

        val ref = expenses(groupId).document(expenseId)
        val existing = try {
            ref.get().await()
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        }
        if (!existing.exists() || existing.getString("type") != ExpenseType.EXPENSE.name) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }

        val updates = mutableMapOf<String, Any>(
            "description" to description.trim(),
            "category" to category.trim(),
            "amountOre" to amountOre,
            "paidBy" to paidBy,
            "splitAmong" to splitAmong.distinct(),
            "updatedBy" to uid,
            "updatedAt" to System.currentTimeMillis(),
        )
        updates["receiptUri"] = receiptUri ?: FieldValue.delete()
        track(expenseId, description.trim().ifBlank { category.trim() }, ref.update(updates))
        return AppResult.Success(Unit)
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): AppResult<Unit> {
        auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val ref = expenses(groupId).document(expenseId)
        val existing = try {
            ref.get().await()
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        }
        if (!existing.exists() || existing.getString("type") != ExpenseType.EXPENSE.name) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }
        ref.delete()
        return AppResult.Success(Unit)
    }

    override suspend fun settleUp(groupId: String, withUserId: String): AppResult<Unit> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val memberIds = when (val members = db.groupMemberIds(groupId)) {
            is AppResult.Success -> members.value
            is AppResult.Error -> return AppResult.Error(members.reason)
        }
        if (!canSettleUp(uid, memberIds, withUserId)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        val groupExpenses = try {
            expenses(groupId).get().await()
                .documents.map { it.toExpense(groupId, SyncStatus.SYNCED) }
        } catch (e: Exception) {
            return AppResult.Error(e.toErrorReason())
        }
        val amountOre = BalanceCalculator.amountOwed(groupExpenses, from = uid, to = withUserId)
        if (amountOre <= 0) return AppResult.Error(ErrorReason.NOTHING_TO_SETTLE)

        val ref = expenses(groupId).document()
        val data = mapOf(
            "id" to ref.id,
            "groupId" to groupId,
            "type" to ExpenseType.SETTLEMENT.name,
            "description" to "",
            "category" to "",
            "amountOre" to amountOre,
            "paidBy" to uid,
            "splitAmong" to listOf(withUserId),
            "createdBy" to uid,
            "createdAt" to System.currentTimeMillis(),
        )
        track(ref.id, "Settlement", ref.set(data))
        return AppResult.Success(Unit)
    }

    /**
     * Firestore applies the write locally immediately (PENDING); this callback
     * later records the server acknowledgement or the permanent failure (NFR4).
     */
    private fun track(documentId: String, label: String, task: Task<Void>) {
        syncStatusStore.markPending(documentId, label)
        task.addOnSuccessListener { syncStatusStore.markSynced(documentId) }
            .addOnFailureListener { syncStatusStore.markFailed(documentId) }
    }
}
