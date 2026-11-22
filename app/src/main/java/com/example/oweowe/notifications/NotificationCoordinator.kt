package com.example.oweowe.notifications

import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.ReminderRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.sync.SyncStatusStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

/**
 * Watches the user's groups while the app runs and posts local notifications
 * for other members' new expenses, expense edits and reminders addressed to
 * the user (FR7, FR8), plus the NFR5 "your write synced" feedback.
 *
 * Spark-plan limitation: with no FCM/Cloud Functions this only works while the
 * app process is alive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCoordinator(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    private val syncStatusStore: SyncStatusStore,
    private val presenter: AndroidNotificationPresenter,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @Volatile
    private var pushEnabled = true

    fun start() {
        if (job != null) return
        job = scope.launch {
            launch {
                syncStatusStore.events.collect { event ->
                    if (pushEnabled) presenter.showSynced(event)
                }
            }
            authRepository.currentUserId.collectLatest { uid ->
                if (uid == null) return@collectLatest
                observeUser(uid)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun observeUser(uid: String) {
        val decider = NotificationDecider(uid)
        combine(
            groupRepository.observeMyGroups(),
            userRepository.observeMe(),
        ) { groups, me ->
            pushEnabled = me?.pushEnabled ?: true
            groups
        }.flatMapLatest { groups ->
            val groupNames = groups.associate { it.id to it.name }
            val expenseFlows = groups.map { group -> expenseRepository.observeExpenses(group.id) }
            val expenses: kotlinx.coroutines.flow.Flow<List<Expense>> =
                if (expenseFlows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(expenseFlows) { lists -> lists.flatMap { it } }
                }
            val users = userRepository.observeUsers(groups.flatMap { it.memberIds }.distinct())
            combine(
                expenses,
                users,
                reminderRepository.observeRemindersForMe(),
            ) { allExpenses, allUsers, reminders ->
                NotificationsInput(
                    groupNames = groupNames,
                    userNames = allUsers.associate { it.uid to it.fullName },
                    expenses = allExpenses,
                    reminders = reminders,
                )
            }
        }.retryWhen { cause, _ ->
            // A transient listener failure must not end notifications for the rest
            // of the session; stop() cancels the job, which ends the retry loop.
            if (cause is CancellationException) return@retryWhen false
            delay(RETRY_AFTER_READ_ERROR_MS)
            true
        }.collect { input ->
            val actions = mutableListOf<NotificationAction>()
            input.expenses.groupBy { it.groupId }.forEach { (groupId, expenses) ->
                actions += decider.onExpenses(
                    groupId = groupId,
                    groupName = input.groupNames[groupId].orEmpty(),
                    expenses = expenses,
                    userNames = input.userNames,
                )
            }
            actions += decider.onReminders(input.reminders, input.userNames, input.groupNames)
            if (pushEnabled) actions.forEach(presenter::show)
        }
    }

    private companion object {
        /** NFR4: wait before re-reading after a failed listener, instead of giving up. */
        const val RETRY_AFTER_READ_ERROR_MS = 30_000L
    }

    private data class NotificationsInput(
        val groupNames: Map<String, String>,
        val userNames: Map<String, String>,
        val expenses: List<Expense>,
        val reminders: List<Reminder>,
    )
}
