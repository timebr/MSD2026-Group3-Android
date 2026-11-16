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
        /** FR10: the receipt image to keep with the expense, or null. */
        receiptUri: String? = null,
    ): AppResult<Unit>

    /**
     * Any group member may edit an expense (purpose, category, amount, payer,
     * split and receipt). The expense stays in its group and keeps its creator.
     */
    suspend fun updateExpense(
        groupId: String,
        expenseId: String,
        description: String,
        category: String,
        amountOre: Long,
        paidBy: String,
        splitAmong: List<String>,
        receiptUri: String? = null,
    ): AppResult<Unit>

    /**
     * Any group member may delete an expense; otherwise NOT_ALLOWED. The
     * deleted row disappears for everyone in the group.
     */
    suspend fun deleteExpense(groupId: String, expenseId: String): AppResult<Unit>

    /** Current user settles their whole balance with withUserId. NOTHING_TO_SETTLE if 0. */
    suspend fun settleUp(groupId: String, withUserId: String): AppResult<Unit>
}