package com.example.oweowe.data.repository
import kotlinx.coroutines.flow.Flow
import com.example.oweowe.data.model.Expense
interface ExpenseRepository {
    /** Expenses and settlements of a group, newest first, each with its syncStatus. */
    fun observeExpenses(groupId: String): Flow<List<Expense>>

    suspend fun addExpense(
        groupId: String,
        description: String,
        category: String,
        amountOre: Long,
        paidBy: String,
        splitAmong: List<String>,
    ): AppResult<Unit>

    /** Only the creator may delete; otherwise NOT_ALLOWED. */
    suspend fun deleteExpense(groupId: String, expenseId: String): AppResult<Unit>

    /** Current user settles their whole balance with withUserId. NOTHING_TO_SETTLE if 0. */
    suspend fun settleUp(groupId: String, withUserId: String): AppResult<Unit>
}