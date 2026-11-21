package com.example.oweowe.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.formatOreForInput
import com.example.oweowe.domain.parseKr
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Editable form fields of S11; kept separate from the loaded data. */
private data class AddExpenseForm(
    val description: String = "",
    val descriptionInvalid: Boolean = false,
    val category: String = "",
    val amountText: String = "",
    val amountInvalid: Boolean = false,
    val payerId: String? = null,
    val splitAmong: Set<String> = emptySet(),
    val selectedGroupId: String? = null,
    val receiptUri: String? = null,
    /** True once an existing expense has been copied into the form (edit only). */
    val prefilled: Boolean = false,
    /** The requested expense no longer exists, is a settlement or is not in the group. */
    val expenseNotFound: Boolean = false,
)

data class AddExpenseUiState(
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val offline: Boolean = false,
    val me: User? = null,
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val members: List<User> = emptyList(),
    val description: String = "",
    val descriptionInvalid: Boolean = false,
    val category: String = "",
    val amountText: String = "",
    val amountInvalid: Boolean = false,
    val payerId: String? = null,
    val splitAmong: Set<String> = emptySet(),
    /** FR10: the receipt already saved with the expense (edit only). */
    val receiptUri: String? = null,
    val expenseNotFound: Boolean = false,
    val writeState: WriteState = WriteState.Idle,
)

/**
 * S11 Add expense (FR4). Everything dynamic from the repositories: groups,
 * members, payer and split selection. Amounts are parsed to øre (Long) by
 * [parseKr] — never a Double.
 *
 * With an [expenseId] the same form edits an existing expense: it is prefilled
 * once from the stored expense and saving updates it instead of adding one.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddExpenseViewModel(
    initialGroupId: String?,
    private val expenseId: String? = null,
    private val expenseRepository: ExpenseRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val form = MutableStateFlow(AddExpenseForm(selectedGroupId = initialGroupId))
    private val writeState = MutableStateFlow<WriteState>(WriteState.Idle)

    init {
        val groupId = initialGroupId
        if (expenseId == null) {
            form.update { it.copy(prefilled = true) }
        } else if (groupId == null) {
            form.update { it.copy(prefilled = true, expenseNotFound = true) }
        } else {
            viewModelScope.launch {
                val expense = expenseRepository.observeExpenses(groupId).first()
                    .find { it.id == expenseId && it.type == ExpenseType.EXPENSE }
                form.update {
                    if (expense == null) {
                        it.copy(prefilled = true, expenseNotFound = true)
                    } else {
                        it.copy(
                            description = expense.description,
                            category = expense.category,
                            amountText = formatOreForInput(expense.amountOre),
                            payerId = expense.paidBy,
                            splitAmong = expense.splitAmong.toSet(),
                            selectedGroupId = expense.groupId,
                            receiptUri = expense.receiptUri,
                            prefilled = true,
                        )
                    }
                }
            }
        }
    }

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
        .catch { e ->
            if (e is CancellationException) throw e
            emit(AddExpenseUiState(loading = false, loadError = true))
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
        // Editing stays in the expense's group, so only that group is offered.
        val groups = if (expenseId == null) {
            inputs.groups
        } else {
            inputs.groups.filter { it.id == inputs.form.selectedGroupId }
        }
        return AddExpenseUiState(
            loading = expenseId != null && !inputs.form.prefilled,
            offline = !inputs.online,
            me = inputs.me,
            groups = groups,
            selectedGroupId = group?.id,
            members = members,
            description = inputs.form.description,
            descriptionInvalid = inputs.form.descriptionInvalid,
            category = inputs.form.category,
            amountText = inputs.form.amountText,
            amountInvalid = inputs.form.amountInvalid,
            payerId = payerId,
            splitAmong = splitAmong,
            receiptUri = inputs.form.receiptUri,
            expenseNotFound = inputs.form.expenseNotFound,
            writeState = inputs.writeState,
        )
    }

    fun updateDescription(description: String) =
        form.update { it.copy(description = description, descriptionInvalid = false) }

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

        // FR4: the purpose is required; the category stays optional.
        val amount = parseKr(state.amountText)
        if (state.description.isBlank() || amount == null || amount <= 0) {
            form.update {
                it.copy(
                    descriptionInvalid = state.description.isBlank(),
                    amountInvalid = amount == null || amount <= 0,
                )
            }
            return
        }
        if (state.splitAmong.isEmpty()) return                  // the UI disables the button anyway

        writeState.value = WriteState.Busy
        viewModelScope.launch {
            val result = if (expenseId == null) {
                expenseRepository.addExpense(
                    groupId = groupId,
                    description = state.description.trim(),
                    category = state.category.trim(),
                    amountOre = amount,
                    paidBy = payerId,
                    splitAmong = state.splitAmong.toList(),
                    receiptUri = receiptUri,
                )
            } else {
                expenseRepository.updateExpense(
                    groupId = groupId,
                    expenseId = expenseId,
                    description = state.description.trim(),
                    category = state.category.trim(),
                    amountOre = amount,
                    paidBy = payerId,
                    splitAmong = state.splitAmong.toList(),
                    receiptUri = receiptUri,
                )
            }
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
        fun factory(
            container: AppContainer,
            initialGroupId: String?,
            expenseId: String? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AddExpenseViewModel(
                        initialGroupId = initialGroupId,
                        expenseId = expenseId,
                        expenseRepository = container.expenseRepository,
                        userRepository = container.userRepository,
                        groupRepository = container.groupRepository,
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
