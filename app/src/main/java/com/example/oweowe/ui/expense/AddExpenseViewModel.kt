package com.example.oweowe.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.parseKr
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable form fields of S11; kept separate from the loaded data. */
private data class AddExpenseForm(
    val description: String = "",
    val category: String = "",
    val amountText: String = "",
    val amountInvalid: Boolean = false,
    val payerId: String? = null,
    val splitAmong: Set<String> = emptySet(),
    val selectedGroupId: String? = null,
)

data class AddExpenseUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val me: User? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val members: List<User> = emptyList(),
    val description: String = "",
    val category: String = "",
    val amountText: String = "",
    val amountInvalid: Boolean = false,
    val payerId: String? = null,
    val splitAmong: Set<String> = emptySet(),
    val writeState: WriteState = WriteState.Idle,
)

/**
 * S11 Add expense (FR4). Everything dynamic from the repositories: groups,
 * members, payer and split selection. Amounts are parsed to øre (Long) by
 * [parseKr] — never a Double.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddExpenseViewModel(
    initialGroupId: String?,
    private val expenseRepository: ExpenseRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val form = MutableStateFlow(AddExpenseForm(selectedGroupId = initialGroupId))
    private val writeState = MutableStateFlow<WriteState>(WriteState.Idle)

    val uiState: StateFlow<AddExpenseUiState> = combine(
        userRepository.observeMe(),
        groupRepository.observeMyGroups(),
        connectivity.isOnline,
        form,
        writeState,
    ) { me, groups, online, currentForm, currentWriteState ->
        AddExpenseInputs(me, groups, online, currentForm, currentWriteState)
    }
        .flatMapLatest { inputs ->
            val group = inputs.groups.find { it.id == inputs.form.selectedGroupId }
                ?: inputs.groups.firstOrNull()
            if (group == null) {
                flowOf(toUiState(inputs, group = null, members = emptyList()))
            } else {
                userRepository.observeUsers(group.memberIds)
                    .map { users -> toUiState(inputs, group, users.sortedBy { it.fullName }) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddExpenseUiState())

    private fun toUiState(
        inputs: AddExpenseInputs,
        group: Group?,
        members: List<User>,
    ): AddExpenseUiState {
        val payerId = inputs.form.payerId ?: inputs.me?.uid
        val splitAmong = inputs.form.splitAmong.ifEmpty {
            members.mapTo(mutableSetOf()) { it.uid }
        }
        return AddExpenseUiState(
            loading = false,
            offline = !inputs.online,
            me = inputs.me,
            groups = inputs.groups,
            selectedGroupId = group?.id,
            members = members,
            description = inputs.form.description,
            category = inputs.form.category,
            amountText = inputs.form.amountText,
            amountInvalid = inputs.form.amountInvalid,
            payerId = payerId,
            splitAmong = splitAmong,
            writeState = inputs.writeState,
        )
    }

    fun updateDescription(description: String) =
        form.update { it.copy(description = description) }

    fun updateCategory(category: String) =
        form.update { it.copy(category = category) }

    fun updateAmount(amount: String) =
        form.update { it.copy(amountText = amount, amountInvalid = false) }

    /** Switching a group resets payer and split to sensible defaults. */
    fun selectGroup(groupId: String) =
        form.update { it.copy(selectedGroupId = groupId, payerId = null, splitAmong = emptySet()) }

    fun selectPayer(userId: String) =
        form.update { it.copy(payerId = userId) }

    /** The split the UI shows: the chosen set, or everyone while untouched. */
    private fun effectiveSplit(currentForm: AddExpenseForm): Set<String> =
        currentForm.splitAmong.ifEmpty {
            membersOf(currentForm).mapTo(mutableSetOf()) { user -> user.uid }
        }

    /**
     * Toggles one member. "Untouched" means everyone is selected, so the first
     * toggle must deselect that member, not select only them.
     */
    fun toggleSplitMember(userId: String) = form.update {
        val current = effectiveSplit(it)
        val next = if (userId in current) current - userId else current + userId
        it.copy(splitAmong = next)
    }

    fun selectAllMembers() = form.update {
        it.copy(splitAmong = membersOf(it).mapTo(mutableSetOf()) { user -> user.uid })
    }

    private fun membersOf(currentForm: AddExpenseForm): List<User> {
        val group = uiState.value.groups.find { it.id == currentForm.selectedGroupId }
            ?: uiState.value.groups.firstOrNull()
        return uiState.value.members.filter { group == null || it.uid in group.memberIds }
    }

    fun submit(receiptUri: String? = null) {
        if (writeState.value == WriteState.Busy) return
        val state = uiState.value
        val groupId = state.selectedGroupId
        val payerId = state.payerId
        if (groupId == null || payerId == null) return

        val amount = parseKr(state.amountText)
        if (amount == null || amount <= 0) {
            form.update { it.copy(amountInvalid = true) }
            return
        }
        if (state.splitAmong.isEmpty()) return                  // the UI disables the button anyway

        writeState.value = WriteState.Busy
        viewModelScope.launch {
            val result = expenseRepository.addExpense(
                groupId = groupId,
                description = state.description.trim(),
                category = state.category.trim(),
                amountOre = amount,
                paidBy = payerId,
                splitAmong = state.splitAmong.toList(),
                receiptUri = receiptUri,
            )
            writeState.value = when (result) {
                is AppResult.Success -> WriteState.Done
                is AppResult.Error -> WriteState.Failed(result.reason)
            }
        }
    }

    /** Called by the screen after it reacted to Done (e.g. navigated back). */
    fun resetWriteState() {
        writeState.value = WriteState.Idle
    }

    private data class AddExpenseInputs(
        val me: User?,
        val groups: List<Group>,
        val online: Boolean,
        val form: AddExpenseForm,
        val writeState: WriteState,
    )

    companion object {
        fun factory(container: AppContainer, initialGroupId: String?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddExpenseViewModel(
                        initialGroupId = initialGroupId,
                        expenseRepository = container.expenseRepository,
                        userRepository = container.userRepository,
                        groupRepository = container.groupRepository,
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
