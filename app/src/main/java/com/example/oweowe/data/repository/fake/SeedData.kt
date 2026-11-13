package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User

object SeedData {
    const val TIM = "u1"
    const val ANNA = "u2"
    const val JONAS = "u3"
    const val MAJA = "u4"
    const val NIELS = "u5"
    const val SOFIE = "u6"
    const val FLAT = "g1"

    private const val START = 1_762_000_000_000L   // a fixed point in time
    private const val DAY = 86_400_000L
    private val ALL = listOf(TIM, ANNA, JONAS)

    val users = listOf(
        User(uid = TIM, fullName = "Tim Test", email = "tim@test.dk", phone = "+45 11 11 11 11", createdAt = START),
        User(uid = ANNA, fullName = "Anna Test", email = "anna@test.dk", phone = "+45 22 22 22 22", createdAt = START),
        User(uid = JONAS, fullName = "Jonas Test", email = "jonas@test.dk", phone = "+45 33 33 33 33", createdAt = START),
        // People who are not in FLAT, so they can be added as members.
        User(uid = MAJA, fullName = "Maja Contact", email = "maja@test.dk", phone = "+45 44 44 44 44", createdAt = START),
        User(uid = NIELS, fullName = "Niels Contact", email = "niels@test.dk", phone = "+45 55 55 55 55", createdAt = START),
        User(uid = SOFIE, fullName = "Sofie Contact", email = "sofie@test.dk", phone = "+45 66 66 66 66", createdAt = START),
    )

    val passwords = mapOf(
        "tim@test.dk" to "password",
        "anna@test.dk" to "password",
        "jonas@test.dk" to "password",
        "maja@test.dk" to "password",
        "niels@test.dk" to "password",
        "sofie@test.dk" to "password",
    )

    val groups = listOf(
        Group(id = FLAT, name = "Flat 4B", description = "Shared flat expenses",
            createdBy = TIM, createdAt = START, memberIds = ALL),
    )

    val expenses = listOf(
        expense("e1", "Groceries", "Food", 30_000, TIM, ALL, day = 1),
        expense("e2", "Pizza night", "Food", 45_000, ANNA, ALL, day = 2),
        expense("e3", "Train tickets", "Transport", 20_000, JONAS, listOf(JONAS, TIM), day = 3),
        expense("e4", "Cleaning supplies", "Housing", 10_000, TIM, ALL, day = 4),
        expense("e5", "Cinema", "Entertainment", 24_000, ANNA, listOf(ANNA, JONAS), day = 5),
        expense("s1", "", "", 3_333, JONAS, listOf(TIM), day = 6, type = ExpenseType.SETTLEMENT),
    )

    private fun expense(
        id: String, description: String, category: String, amountOre: Long,
        paidBy: String, splitAmong: List<String>, day: Int,
        type: ExpenseType = ExpenseType.EXPENSE,
    ) = Expense(
        id = id, groupId = FLAT, type = type, description = description, category = category,
        amountOre = amountOre, paidBy = paidBy, splitAmong = splitAmong,
        createdBy = paidBy, createdAt = START + day * DAY,
    )
}