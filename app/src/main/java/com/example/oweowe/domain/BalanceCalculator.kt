package com.example.oweowe.domain

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType

/** [from] owes [to] exactly [amountOre]. Always > 0. */
data class Debt(val from: String, val to: String, val amountOre: Long)

object BalanceCalculator {

    /**
     * Net debts between every pair of people in a group.
     * Only pairs that are not even are returned, so "Anna owes Tim 120 kr." never appears
     * together with "Tim owes Anna ...".
     */
    fun pairwiseDebts(expenses: List<Expense>): List<Debt> {
        // gross[a to b] = everything a owes b, before netting out what b owes a
        val gross = mutableMapOf<Pair<String, String>, Long>()
        for (expense in expenses) {
            val shares = SplitCalculator.split(expense.amountOre, expense.splitAmong)
            for ((uid, share) in shares) {
                if (uid == expense.paidBy) continue          // you never owe yourself
                val key = uid to expense.paidBy
                gross[key] = (gross[key] ?: 0L) + share
            }
        }

        val people = gross.keys.flatMap { listOf(it.first, it.second) }.distinct().sorted()
        val debts = mutableListOf<Debt>()
        for (i in people.indices) {
            for (j in i + 1 until people.size) {
                val a = people[i]
                val b = people[j]
                val net = (gross[a to b] ?: 0L) - (gross[b to a] ?: 0L)
                if (net > 0) debts += Debt(from = a, to = b, amountOre = net)
                if (net < 0) debts += Debt(from = b, to = a, amountOre = -net)
            }
        }
        return debts
    }

    /** How much [from] owes [to] right now. 0 if they are even or if [to] owes [from]. */
    fun amountOwed(expenses: List<Expense>, from: String, to: String): Long =
        pairwiseDebts(expenses).find { it.from == from && it.to == to }?.amountOre ?: 0L

    /** Net balance per person: positive = gets money back, negative = owes money. Sums to 0. */
    fun balances(expenses: List<Expense>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (expense in expenses) {
            result[expense.paidBy] = (result[expense.paidBy] ?: 0L) + expense.amountOre
            for ((uid, share) in SplitCalculator.split(expense.amountOre, expense.splitAmong)) {
                result[uid] = (result[uid] ?: 0L) - share
            }
        }
        return result
    }

    /** "Total spent" in Activity: real expenses only, settlements are not spending. */
    fun totalSpent(expenses: List<Expense>): Long =
        expenses.filter { it.type == ExpenseType.EXPENSE }.sumOf { it.amountOre }

    /** True if [userId] owes nobody and nobody owes them. Required before removing a member. */
    fun isSettled(expenses: List<Expense>, userId: String): Boolean =
        pairwiseDebts(expenses).none { it.from == userId || it.to == userId }
}