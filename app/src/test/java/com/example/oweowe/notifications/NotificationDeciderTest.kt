package com.example.oweowe.notifications

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeciderTest {

    private val me = "me"
    private val other = "other"
    private val groupId = "g1"

    private fun expense(
        id: String,
        createdBy: String = other,
        updatedBy: String = "",
        amountOre: Long = 1_000,
    ) = Expense(
        id = id,
        groupId = groupId,
        type = ExpenseType.EXPENSE,
        description = "Dinner",
        category = "Food",
        amountOre = amountOre,
        paidBy = createdBy,
        splitAmong = listOf(me, other),
        createdBy = createdBy,
        createdAt = 1,
        updatedBy = updatedBy,
        updatedAt = if (updatedBy.isBlank()) 0 else 2,
    )

    @Test
    fun firstSnapshotOnlySeedsExistingExpenses() {
        val decider = NotificationDecider(me)
        val actions = decider.onExpenses(groupId, "Flat", listOf(expense("e1")), mapOf(other to "Other"))
        assertTrue(actions.isEmpty())
    }

    @Test
    fun newExpenseByAnotherMemberNotifiesOnce() {
        val decider = NotificationDecider(me)
        decider.onExpenses(groupId, "Flat", emptyList(), emptyMap())

        val actions = decider.onExpenses(groupId, "Flat", listOf(expense("e1")), mapOf(other to "Other"))
        assertEquals(1, actions.size)
        assertTrue(actions.single() is NotificationAction.NewExpense)
        assertTrue(decider.onExpenses(groupId, "Flat", listOf(expense("e1")), emptyMap()).isEmpty())
    }

    @Test
    fun ownNewExpenseDoesNotNotify() {
        val decider = NotificationDecider(me)
        decider.onExpenses(groupId, "Flat", emptyList(), emptyMap())
        assertTrue(decider.onExpenses(groupId, "Flat", listOf(expense("e1", createdBy = me)), emptyMap()).isEmpty())
    }

    @Test
    fun editByAnotherMemberNotifies() {
        val decider = NotificationDecider(me)
        decider.onExpenses(groupId, "Flat", listOf(expense("e1")), mapOf(other to "Other"))

        val actions = decider.onExpenses(
            groupId,
            "Flat",
            listOf(expense("e1", updatedBy = other, amountOre = 2_000)),
            mapOf(other to "Other"),
        )
        assertEquals(1, actions.size)
        assertTrue(actions.single() is NotificationAction.UpdatedExpense)
    }

    @Test
    fun ownEditDoesNotNotify() {
        val decider = NotificationDecider(me)
        decider.onExpenses(groupId, "Flat", listOf(expense("e1")), emptyMap())
        assertTrue(
            decider.onExpenses(
                groupId,
                "Flat",
                listOf(expense("e1", updatedBy = me, amountOre = 2_000)),
                emptyMap(),
            ).isEmpty(),
        )
    }

    @Test
    fun firstReminderSnapshotOnlySeeds() {
        val decider = NotificationDecider(me)
        val reminder = Reminder(id = "r1", groupId = groupId, fromUserId = other, toUserId = me, amountOre = 100)
        assertTrue(decider.onReminders(listOf(reminder), mapOf(other to "Other"), mapOf(groupId to "Flat")).isEmpty())
    }

    @Test
    fun newReminderNotifies() {
        val decider = NotificationDecider(me)
        decider.onReminders(emptyList(), emptyMap(), emptyMap())
        val reminder = Reminder(id = "r1", groupId = groupId, fromUserId = other, toUserId = me, amountOre = 100)
        val actions = decider.onReminders(listOf(reminder), mapOf(other to "Other"), mapOf(groupId to "Flat"))
        assertEquals(1, actions.size)
        assertTrue(actions.single() is NotificationAction.ReminderReceived)
    }
}
