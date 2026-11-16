package com.example.oweowe.notifications

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.Reminder

/** What the user should be told about, independent of Android notification APIs. */
sealed interface NotificationAction {
    data class NewExpense(
        val expenseId: String,
        val payerName: String,
        val groupName: String,
        val description: String,
        val amountOre: Long,
    ) : NotificationAction

    data class UpdatedExpense(
        val expenseId: String,
        val editorName: String,
        val groupName: String,
        val description: String,
        val amountOre: Long,
    ) : NotificationAction

    data class ReminderReceived(
        val reminderId: String,
        val fromName: String,
        val groupName: String,
        val amountOre: Long,
    ) : NotificationAction
}

/**
 * Turns successive Firestore snapshots into notifications for someone else's
 * changes. The first snapshot of a group is only used to remember what was
 * already there, so opening the app does not notify about old expenses (FR7).
 */
class NotificationDecider(private val meId: String) {

    private val seenExpenses = mutableMapOf<String, String>()
    private val seededGroups = mutableSetOf<String>()
    private val seenReminders = mutableSetOf<String>()
    private var remindersSeeded = false

    fun onExpenses(
        groupId: String,
        groupName: String,
        expenses: List<Expense>,
        userNames: Map<String, String>,
    ): List<NotificationAction> {
        val seeding = groupId !in seededGroups
        if (seeding) seededGroups += groupId

        val actions = mutableListOf<NotificationAction>()
        for (expense in expenses) {
            val signature = expense.signature()
            val previous = seenExpenses[expense.id]
            if (!seeding) {
                when {
                    previous == null && expense.createdBy != meId -> actions += NotificationAction.NewExpense(
                        expenseId = expense.id,
                        payerName = userNames[expense.paidBy].orEmpty(),
                        groupName = groupName,
                        description = expense.description.ifBlank { expense.category },
                        amountOre = expense.amountOre,
                    )
                    previous != null && previous != signature && expense.updatedBy != meId ->
                        actions += NotificationAction.UpdatedExpense(
                            expenseId = expense.id,
                            editorName = userNames[expense.updatedBy].orEmpty(),
                            groupName = groupName,
                            description = expense.description.ifBlank { expense.category },
                            amountOre = expense.amountOre,
                        )
                }
            }
            seenExpenses[expense.id] = signature
        }
        return actions
    }

    fun onReminders(
        reminders: List<Reminder>,
        userNames: Map<String, String>,
        groupNames: Map<String, String>,
    ): List<NotificationAction> {
        val seeding = !remindersSeeded
        remindersSeeded = true
        val actions = mutableListOf<NotificationAction>()
        for (reminder in reminders) {
            val isNew = seenReminders.add(reminder.id)
            if (!seeding && isNew && reminder.toUserId == meId && reminder.fromUserId != meId) {
                actions += NotificationAction.ReminderReceived(
                    reminderId = reminder.id,
                    fromName = userNames[reminder.fromUserId].orEmpty(),
                    groupName = groupNames[reminder.groupId].orEmpty(),
                    amountOre = reminder.amountOre,
                )
            }
        }
        return actions
    }

    private fun Expense.signature(): String = listOf(
        type.name,
        description,
        category,
        amountOre,
        paidBy,
        splitAmong.joinToString(","),
        createdBy,
        updatedAt,
        receiptUri.orEmpty(),
    ).joinToString("|")
}
