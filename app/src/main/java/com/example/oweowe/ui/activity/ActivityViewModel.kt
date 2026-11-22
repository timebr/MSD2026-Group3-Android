package com.example.oweowe.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.Reminder
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.ReminderRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.components.retryOnReadError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One transaction row in the S5 list, with its sync status (NFR4). */
data class ActivityTransaction(
    val expense: Expense,
    val groupName: String,
    val payerName: String,
    /** Settlement recipient, shown as "A paid B" (FR5). */
    val payeeName: String,
)

/** FR7 / FR8: a reminder addressed to the signed-in user. */
data class ReminderRow(
    val reminder: Reminder,
    val fromName: String,
    val groupName: String,
)

data class ActivityUiState(
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val offline: Boolean = false,
    val me: User? = null,
    val totalSpentOre: Long = 0,
    val myBalanceOre: Long = 0,
    val transactions: List<ActivityTransaction> = emptyList(),
    val reminders: List<ReminderRow> = emptyList(),
)

/**
 * S5 Activity (FR5, FR6, FR7): total spent, own balance and the chronological
 * transaction list across all of the user's groups, plus reminders received.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    /** NFR4: one tap on "Try again" in the error state re-collects the reads. */
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<ActivityUiState> = combine(
        userRepository.observeMe(),
        groupRepository.observeMyGroups(),
        connectivity.isOnline,
    ) { me, groups, online -> ActivityInputs(me, groups, online) }
        .flatMapLatest { inputs ->
            if (inputs.groups.isEmpty()) {
                flowOf(emptyActivity(inputs))
            } else {
                val expenses = expensesOfAll(inputs.groups)
                val users = userRepository.observeUsers(
                    inputs.groups.flatMap { it.memberIds }.distinct(),
                )
                val reminders = reminderRepository.observeRemindersForMe()
                combine(expenses, users, reminders) { e, u, r -> buildUiState(inputs, e, u, r) }
            }
        }
        .retryOnReadError(retryTrigger) {
            // Keep the offline banner from the last good state (NFR3).
            ActivityUiState(loading = false, offline = uiState.value.offline, loadError = true)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    /** NFR4: retry the reads behind the S5 error state. */
    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    private fun emptyActivity(inputs: ActivityInputs) = ActivityUiState(
        loading = false,
        offline = !inputs.online,
        me = inputs.me,
    )

    private fun buildUiState(
        inputs: ActivityInputs,
        expenses: List<Expense>,
        users: List<User>,
        reminders: List<Reminder>,
    ): ActivityUiState {
        val meId = inputs.me?.uid.orEmpty()
        val userById = users.associateBy { it.uid }
        val groupById = inputs.groups.associateBy { it.id }
        return ActivityUiState(
            loading = false,
            offline = !inputs.online,
            me = inputs.me,
            totalSpentOre = BalanceCalculator.totalSpent(expenses),
            myBalanceOre = inputs.groups.sumOf { group ->
                BalanceCalculator
                    .balances(expenses.filter { it.groupId == group.id })[meId] ?: 0
            },
            transactions = expenses
                .sortedByDescending { it.createdAt }
                .map { expense ->
                    ActivityTransaction(
                        expense = expense,
                        groupName = groupById[expense.groupId]?.name.orEmpty(),
                        payerName = userById[expense.paidBy]?.fullName.orEmpty(),
                        // For a settlement, splitAmong holds the recipient.
                        payeeName = if (expense.type == ExpenseType.SETTLEMENT) {
                            userById[expense.splitAmong.firstOrNull()]?.fullName.orEmpty()
                        } else {
                            ""
                        },
                    )
                },
            reminders = reminders
                .sortedByDescending { it.createdAt }
                .map { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        fromName = userById[reminder.fromUserId]?.fullName.orEmpty(),
                        groupName = groupById[reminder.groupId]?.name.orEmpty(),
                    )
                },
        )
    }

    /** Combines the per-group expense flows into one list. */
    private fun expensesOfAll(groups: List<Group>): Flow<List<Expense>> =
        combine(groups.map { group -> expenseRepository.observeExpenses(group.id) }) { lists ->
            lists.toList().flatten()
        }

    private data class ActivityInputs(
        val me: User?,
        val groups: List<Group>,
        val online: Boolean,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ActivityViewModel(
                        userRepository = container.userRepository,
                        groupRepository = container.groupRepository,
                        expenseRepository = container.expenseRepository,
                        reminderRepository = container.reminderRepository,
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
