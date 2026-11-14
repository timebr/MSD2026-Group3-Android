package com.example.oweowe.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.ReminderRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.components.UiMessage
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.errorReasonRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One member row on the Group overview (S4) and Manage members (S8). */
data class MemberRow(
    val user: User,
    val balanceOre: Long,
    val isMe: Boolean,
    /** Only members with all balances at 0 can be removed (see architecture). */
    val canRemove: Boolean,
    /** > 0 when this member owes the signed-in user money (Remind, FR8). */
    val owesMeOre: Long,
    /** > 0 when the signed-in user owes this member money (FR5, FR6). */
    val iOweOre: Long = 0,
)

/** One transaction row on the Group overview (S4); the group is implied there. */
data class GroupTransaction(
    val expense: Expense,
    val payerName: String,
    /** Settlement recipient, shown as "A paid B" (FR5). */
    val payeeName: String,
)

data class GroupOverviewUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val notFound: Boolean = false,
    val me: User? = null,
    val group: Group? = null,
    val totalSpentOre: Long = 0,
    val members: List<MemberRow> = emptyList(),
    /** The group's expenses and settlements, newest first (FR5). */
    val transactions: List<GroupTransaction> = emptyList(),
    val busy: Boolean = false,
)

/** S7 edit-group form. */
data class EditGroupUiState(
    val name: String = "",
    val description: String = "",
    val nameInvalid: Boolean = false,
    val writeState: WriteState = WriteState.Idle,
    /** True once the form has been pre-filled from the group. */
    val loadedFromGroup: Boolean = false,
)

/**
 * S4 group overview, S7 edit details, S8 member list, S10 add member (FR3, FR5,
 * FR6, FR8). One instance per group, created with the `groupId` route argument.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupDetailsViewModel(
    private val groupId: String,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val reminderRepository: ReminderRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val busy = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    val uiState: StateFlow<GroupOverviewUiState> = combine(
        userRepository.observeMe(),
        groupRepository.observeGroup(groupId),
        connectivity.isOnline,
        busy,
    ) { me, group, online, isBusy -> DetailsInputs(me, group, online, isBusy) }
        .flatMapLatest { inputs ->
            val group = inputs.group
            if (group == null) {
                flowOf(
                    GroupOverviewUiState(
                        loading = false,
                        offline = !inputs.online,
                        notFound = true,
                        me = inputs.me,
                        busy = inputs.busy,
                    ),
                )
            } else {
                combine(
                    userRepository.observeUsers(group.memberIds),
                    expenseRepository.observeExpenses(group.id),
                ) { users, expenses ->
                    buildUiState(inputs, group, users, expenses)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupOverviewUiState())

    private fun buildUiState(
        inputs: DetailsInputs,
        group: Group,
        users: List<User>,
        expenses: List<Expense>,
    ): GroupOverviewUiState {
        val balances = BalanceCalculator.balances(expenses)
        val meId = inputs.me?.uid.orEmpty()
        val userById = users.associateBy { it.uid }
        return GroupOverviewUiState(
            loading = false,
            offline = !inputs.online,
            me = inputs.me,
            group = group,
            totalSpentOre = BalanceCalculator.totalSpent(expenses),
            members = users
                .sortedBy { it.fullName }
                .map { user ->
                    MemberRow(
                        user = user,
                        balanceOre = balances[user.uid] ?: 0,
                        isMe = user.uid == meId,
                        canRemove = user.uid != meId && BalanceCalculator.isSettled(expenses, user.uid),
                        owesMeOre = BalanceCalculator.amountOwed(expenses, from = user.uid, to = meId),
                        iOweOre = BalanceCalculator.amountOwed(expenses, from = meId, to = user.uid),
                    )
                },
            transactions = expenses
                .sortedByDescending { it.createdAt }
                .map { expense ->
                    GroupTransaction(
                        expense = expense,
                        payerName = userById[expense.paidBy]?.fullName.orEmpty(),
                        // For a settlement, splitAmong holds the recipient.
                        payeeName = if (expense.type == ExpenseType.SETTLEMENT) {
                            userById[expense.splitAmong.firstOrNull()]?.fullName.orEmpty()
                        } else {
                            ""
                        },
                    )
                },
            busy = inputs.busy,
        )
    }

    // ---- S7 Edit group ----

    private val _editState = MutableStateFlow(EditGroupUiState())
    val editState: StateFlow<EditGroupUiState> = _editState.asStateFlow()

    init {
        // Pre-fill the edit form from the group once it arrives.
        viewModelScope.launch {
            groupRepository.observeGroup(groupId)
                .filterNotNull()
                .collect { group ->
                    _editState.update {
                        if (it.loadedFromGroup) it else it.prefilled(group)
                    }
                }
        }
    }

    private fun EditGroupUiState.prefilled(group: Group): EditGroupUiState = copy(
        name = group.name,
        description = group.description,
        loadedFromGroup = true,
    )

    fun updateEditName(name: String) =
        _editState.update { it.copy(name = name, nameInvalid = false, writeState = WriteState.Idle) }

    fun updateEditDescription(description: String) =
        _editState.update { it.copy(description = description, writeState = WriteState.Idle) }

    fun saveGroupDetails() {
        val state = _editState.value
        if (state.writeState == WriteState.Busy) return
        if (state.name.isBlank()) {
            _editState.update { it.copy(nameInvalid = true) }
            return
        }
        _editState.update { it.copy(writeState = WriteState.Busy) }
        viewModelScope.launch {
            when (val result = groupRepository.updateDetails(groupId, state.name.trim(), state.description.trim())) {
                is AppResult.Success -> _editState.update { it.copy(writeState = WriteState.Done) }
                is AppResult.Error -> _editState.update { it.copy(writeState = WriteState.Failed(result.reason)) }
            }
        }
    }

    fun resetWriteState() {
        _editState.update { it.copy(writeState = WriteState.Idle) }
    }

    // ---- S8 / S10 members ----

    fun addMemberByEmail(email: String) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            emitAddedMember(groupRepository.addMemberByEmail(groupId, email))
        }
    }

    fun addMemberByPhone(phone: String) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            emitAddedMember(groupRepository.addMemberByPhone(groupId, phone))
        }
    }

    private suspend fun emitAddedMember(result: AppResult<User>) {
        busy.value = false
        val message = when (result) {
            is AppResult.Success -> UiMessage(R.string.manage_members_added, listOf(result.value.fullName))
            is AppResult.Error -> UiMessage(errorReasonRes(result.reason))
        }
        _messages.emit(message)
    }

    fun removeMember(member: MemberRow) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val result = groupRepository.removeMember(groupId, member.user.uid)
            busy.value = false
            val message = when (result) {
                is AppResult.Success -> UiMessage(R.string.manage_members_removed, listOf(member.user.fullName))
                is AppResult.Error -> UiMessage(errorReasonRes(result.reason))
            }
            _messages.emit(message)
        }
    }

    /** FR4: any group member may delete an expense from the group overview. */
    fun deleteExpense(expense: Expense) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val result = expenseRepository.deleteExpense(groupId, expense.id)
            busy.value = false
            val message = when (result) {
                is AppResult.Success -> UiMessage(
                    R.string.group_overview_expense_deleted,
                    listOf(expense.description.ifBlank { expense.category }),
                )
                is AppResult.Error -> UiMessage(errorReasonRes(result.reason))
            }
            _messages.emit(message)
        }
    }

    /** FR8 from the Group overview as well as Home. */
    fun sendReminder(member: MemberRow) {
        if (busy.value) return
        val group = uiState.value.group ?: return
        busy.value = true
        viewModelScope.launch {
            val expenses = expenseRepository.observeExpenses(group.id).first()
            val amount = BalanceCalculator.amountOwed(
                expenses,
                from = member.user.uid,
                to = uiState.value.me?.uid.orEmpty(),
            )
            val result = reminderRepository.sendReminder(group.id, member.user.uid, amount)
            busy.value = false
            val message = when (result) {
                is AppResult.Success -> UiMessage(R.string.group_overview_reminder_sent, listOf(member.user.fullName))
                is AppResult.Error -> UiMessage(errorReasonRes(result.reason))
            }
            _messages.emit(message)
        }
    }

    /**
     * FR5: settle up with a member the signed-in user owes money to. Recorded as
     * a SETTLEMENT expense marked by the debtor (design decision #10).
     */
    fun settleUp(member: MemberRow) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            val result = expenseRepository.settleUp(groupId, member.user.uid)
            busy.value = false
            val message = when (result) {
                is AppResult.Success -> UiMessage(R.string.settle_up_done, listOf(member.user.fullName))
                is AppResult.Error -> UiMessage(errorReasonRes(result.reason))
            }
            _messages.emit(message)
        }
    }

    private data class DetailsInputs(
        val me: User?,
        val group: Group?,
        val online: Boolean,
        val busy: Boolean,
    )

    companion object {
        fun factory(container: AppContainer, groupId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    GroupDetailsViewModel(
                        groupId = groupId,
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
