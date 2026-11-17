package com.example.oweowe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the group the user selected on S4 so S3 Home can show it. */
class SelectedGroupStore {
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    fun select(groupId: String) {
        _selectedGroupId.value = groupId
    }
}
