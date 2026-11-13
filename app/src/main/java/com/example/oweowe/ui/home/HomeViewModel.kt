package com.example.oweowe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.ReminderRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.components.UiMessage
import com.example.oweowe.ui.components.errorReasonRes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row in the S3 balances list. [balanceOre] > 0 = gets money back, < 0 = owes. */
data class MemberBalance(
    val user: User,
    val balanceOre: Long,
    /** > 0 when this member owes the signed-in user money (Remind button shows, FR8). */
    val owesMeOre: Long,
)

data class HomeUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val me: User? = null,
    val group: Group? = null,
    val memberBalances: List<MemberBalance> = emptyList(),
    val myBalanceOre: Long = 0,
    val reminderBusy: Boolean = false,
    /** When the shown group last changed: newest expense, else the group's creation. */
    val lastActivityAt: Long = 0,
)

/**
 * S3 Home dashboard (FR5, FR6, FR8): current group, own balance, balances per
 * member, reminders. Balances are always computed by [BalanceCalculator], never
 * stored. Offline (NFR3): the same flows keep serving the last data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val reminderBusy = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    /** The group the S3 card shows: the one with the most recent activity (FR5). */
    private val currentGroup: Flow<Group?> =
        groupRepository.observeMyGroups().flatMapLatest { groups ->
            if (groups.isEmpty()) {
                flowOf(null)
            } else {
                combine(groups.map { group -> lastActivity(group) }) { scored ->
                    scored.maxByOrNull { it.second }?.first
                }
            }
        }

    private fun lastActivity(group: Group): Flow<Pair<Group, Long>> =
        expenseRepository.observeExpenses(group.id).map { expenses ->
            group to (expenses.maxOfOrNull { it.createdAt } ?: 0L).coerceAtLeast(group.createdAt)
        }

    // Must come after currentGroup / _messages: `launch` on Main.immediate can
    // start the collector before the constructor returns.
    init {
        watchBackgroundSync()
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.observeMe(),
        currentGroup,
        connectivity.isOnline,
        reminderBusy,
    ) { me, group, online, busy -> HomeInputs(me, group, online, busy) }
        .flatMapLatest { inputs ->
            val group = inputs.group
            if (group == null) {
                flowOf(HomeUiState(loading = false, offline = !inputs.online, me = inputs.me, reminderBusy = inputs.busy))
            } else {
                combine(
                    expenseRepository.observeExpenses(group.id),
                    userRepository.observeUsers(group.memberIds),
                ) { expenses, users ->
                    buildUiState(inputs, group, expenses, users)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildUiState(
        inputs: HomeInputs,
        group: Group,
        expenses: List<Expense>,
        users: List<User>,
    ): HomeUiState {
        val balances = BalanceCalculator.balances(expenses)
        val meId = inputs.me?.uid.orEmpty()
        val rows = users
            .sortedBy { it.fullName }
            .map { user ->
                MemberBalance(
                    user = user,
                    balanceOre = balances[user.uid] ?: 0,
                    owesMeOre = BalanceCalculator.amountOwed(expenses, from = user.uid, to = meId),
                )
            }
        return HomeUiState(
            loading = false,
            offline = !inputs.online,
            me = inputs.me,
            group = group,
            memberBalances = rows,
            myBalanceOre = balances[meId] ?: 0,
            reminderBusy = inputs.busy,
            lastActivityAt = (expenses.maxOfOrNull { it.createdAt } ?: 0L)
                .coerceAtLeast(group.createdAt),
        )
    }

    /** FR8: remind one member who owes the signed-in user money. */
    fun sendReminder(member: MemberBalance) {
        if (reminderBusy.value) return
        val group = uiState.value.group ?: return
        reminderBusy.value = true
        viewModelScope.launch {
            val result = reminderRepository.sendReminder(group.id, member.user.uid, member.owesMeOre)
            reminderBusy.value = false
            val message = when (result) {
                is AppResult.Success ->
                    UiMessage(R.string.home_reminder_sent, listOf(member.user.fullName))
                is AppResult.Error ->
                    UiMessage(errorReasonRes(result.reason))
            }
            _messages.emit(message)
        }
    }

    /**
     * NFR5: when one of the user's own expenses finishes syncing in the
     * background (pending -> synced), tell them with a snackbar message.
     */
    private fun watchBackgroundSync() {
        viewModelScope.launch {
            var pendingIds = emptySet<String>()
            combine(
                authRepository.currentUserId,
                currentGroup.flatMapLatest { group ->
                    if (group == null) flowOf(emptyList()) else expenseRepository.observeExpenses(group.id)
                },
            ) { meId, expenses -> meId to expenses }
                .collect { (meId, expenses) ->
                    val nowPending = expenses
                        .filter { it.syncStatus == SyncStatus.PENDING }
                        .mapTo(mutableSetOf()) { it.id }
                    expenses
                        .filter { it.syncStatus == SyncStatus.SYNCED && it.id in pendingIds && it.createdBy == meId }
                        .firstOrNull()
                        ?.let { expense ->
                            val label = expense.description.ifBlank { expense.category }
                            _messages.emit(UiMessage(R.string.home_synced_in_background, listOf(label)))
                        }
                    pendingIds = nowPending
                }
        }
    }

    private data class HomeInputs(
        val me: User?,
        val group: Group?,
        val online: Boolean,
        val busy: Boolean,
    )

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    HomeViewModel(
                        authRepository = container.authRepository,
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
