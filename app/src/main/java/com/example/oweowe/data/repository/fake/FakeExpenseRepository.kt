package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.domain.BalanceCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FakeExpenseRepository(private val db: FakeBackend) : ExpenseRepository {

    override fun observeExpenses(groupId: String): Flow<List<Expense>> =
        db.expenses.map { all ->
            all.filter { it.groupId == groupId }.sortedByDescending { it.createdAt }
        }

    override suspend fun addExpense(
        groupId: String, description: String, category: String,
        amountOre: Long, paidBy: String, splitAmong: List<String>,
    ): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (!db.isMember(groupId, me)) return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (amountOre <= 0) return AppResult.Error(ErrorReason.INVALID_AMOUNT)
        val allMembers = (splitAmong + paidBy).all { db.isMember(groupId, it) }
        if (splitAmong.isEmpty() || !allMembers) return AppResult.Error(ErrorReason.NOT_ALLOWED)

        val expense = Expense(
            id = db.newId(), groupId = groupId, type = ExpenseType.EXPENSE,
            description = description.trim(), category = category, amountOre = amountOre,
            paidBy = paidBy, splitAmong = splitAmong.distinct(),
            createdBy = me, createdAt = db.now(), syncStatus = SyncStatus.PENDING,
        )
        db.expenses.update { it + expense }
        markSyncedLater(expense.id)
        return AppResult.Success(Unit)
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val expense = db.expenses.value.find { it.id == expenseId && it.groupId == groupId }
        if (expense == null || expense.createdBy != db.currentUserId.value) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)   // only the creator may delete
        }
        db.expenses.update { all -> all.filterNot { it.id == expenseId } }
        return AppResult.Success(Unit)
    }

    override suspend fun settleUp(groupId: String, withUserId: String): AppResult<Unit> {
        db.simulateCall()?.let { return AppResult.Error(it) }
        val me = db.currentUserId.value ?: return AppResult.Error(ErrorReason.NOT_ALLOWED)
        if (!db.isMember(groupId, me) || !db.isMember(groupId, withUserId)) {
            return AppResult.Error(ErrorReason.NOT_ALLOWED)
        }
        val groupExpenses = db.expenses.value.filter { it.groupId == groupId }
        val amountOre = BalanceCalculator.amountOwed(groupExpenses, from = me, to = withUserId)
        if (amountOre <= 0) return AppResult.Error(ErrorReason.NOTHING_TO_SETTLE)

        val settlement = Expense(
            id = db.newId(), groupId = groupId, type = ExpenseType.SETTLEMENT,
            amountOre = amountOre, paidBy = me, splitAmong = listOf(withUserId),
            createdBy = me, createdAt = db.now(), syncStatus = SyncStatus.PENDING,
        )
        db.expenses.update { it + settlement }
        markSyncedLater(settlement.id)
        return AppResult.Success(Unit)
    }

    /** Imitates Firestore: the write reaches the "server" a bit later. */
    private fun markSyncedLater(expenseId: String) {
        db.scope.launch {
            delay(db.syncDelayMs)
            db.expenses.update { all ->
                all.map { if (it.id == expenseId) it.copy(syncStatus = SyncStatus.SYNCED) else it }
            }
        }
    }
}