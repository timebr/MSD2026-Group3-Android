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
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S4: my groups, dynamic from the repository (FR3). */
data class GroupsUiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val groups: List<Group> = emptyList(),
)

/** S9: the create-group form. */
data class CreateGroupUiState(
    val name: String = "",
    val description: String = "",
    val nameInvalid: Boolean = false,
    val writeState: WriteState = WriteState.Idle,
)

/** S4 list + S9 create (FR3). */
class GroupsViewModel(
    private val groupRepository: GroupRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> = combine(
        groupRepository.observeMyGroups(),
        connectivity.isOnline,
    ) { groups, online ->
        GroupsUiState(loading = false, offline = !online, groups = groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

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
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
