package com.example.oweowe.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ExpenseRepository
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One row in the S4 list: the group plus my net balance in it (FR3, FR5). */
data class GroupSummary(
    val group: Group,
    /** > 0 = I get money back, < 0 = I owe money, 0 = settled up. */
    val myNetOre: Long,
)

/** S4: my groups, dynamic from the repository (FR3). */
data class GroupsUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val groups: List<GroupSummary> = emptyList(),
)

/** S9: the create-group form. */
data class CreateGroupUiState(
    val name: String = "",
    val description: String = "",
    val nameInvalid: Boolean = false,
    val writeState: WriteState = WriteState.Idle,
)

/** S4 list + S9 create (FR3, FR5). */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModel(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> = combine(
        groupRepository.observeMyGroups(),
        userRepository.observeMe(),
        connectivity.isOnline,
    ) { groups, me, online -> Triple(groups, me?.uid.orEmpty(), online) }
        .flatMapLatest { (groups, meId, online) ->
            if (groups.isEmpty()) {
                flowOf(GroupsUiState(loading = false, offline = !online))
            } else {
                combine(groups.map { group -> myNet(group, meId) }) { nets ->
                    GroupsUiState(
                        loading = false,
                        offline = !online,
                        groups = groups.zip(nets) { group, net -> GroupSummary(group, net) },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    private fun myNet(group: Group, meId: String): Flow<Long> =
        expenseRepository.observeExpenses(group.id).map { expenses ->
            BalanceCalculator.balances(expenses)[meId] ?: 0L
        }

    private val _createState = MutableStateFlow(CreateGroupUiState())
    val createState: StateFlow<CreateGroupUiState> = _createState.asStateFlow()

    /** Emitted once with the new group's ID; the screen navigates to its overview. */
    private val _createdGroupId = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val createdGroupId: SharedFlow<String> = _createdGroupId.asSharedFlow()

    fun updateName(name: String) =
        _createState.update { it.copy(name = name, nameInvalid = false, writeState = WriteState.Idle) }

    fun updateDescription(description: String) =
        _createState.update { it.copy(description = description, writeState = WriteState.Idle) }

    fun createGroup() {
        val state = _createState.value
        if (state.writeState == WriteState.Busy) return
        if (state.name.isBlank()) {
            _createState.update { it.copy(nameInvalid = true) }
            return
        }
        _createState.update { it.copy(writeState = WriteState.Busy, nameInvalid = false) }
        viewModelScope.launch {
            when (val result = groupRepository.createGroup(state.name.trim(), state.description.trim())) {
                is AppResult.Success -> {
                    _createState.update { it.copy(writeState = WriteState.Done) }
                    _createdGroupId.emit(result.value)
                    _createState.update { it.copy(writeState = WriteState.Idle) }
                }
                is AppResult.Error ->
                    _createState.update { it.copy(writeState = WriteState.Failed(result.reason)) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    GroupsViewModel(
                        groupRepository = container.groupRepository,
                        userRepository = container.userRepository,
                        expenseRepository = container.expenseRepository,
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
