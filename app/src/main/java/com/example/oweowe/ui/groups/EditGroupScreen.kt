package com.example.oweowe.ui.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.AppBottomSheet
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.SheetHeader
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.errorReasonMessage

/** S7 Edit group details (FR3) as a bottom sheet; Done dismisses it. */
@Composable
fun EditGroupSheet(
    container: AppContainer,
    groupId: String,
    onDismiss: () -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "edit-group-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onDismiss()
        }
    }

    AppBottomSheet(onDismiss = onDismiss) {
        SheetHeader(stringResource(R.string.edit_group_title))
        EditGroupContent(
            state = state,
            onNameChange = viewModel::updateEditName,
            onDescriptionChange = viewModel::updateEditDescription,
            onSave = viewModel::saveGroupDetails,
        )
    }
}

/** Full-screen S7 kept as a fallback while the sheet route is validated. */
@Composable
fun EditGroupScreen(
    container: AppContainer,
    groupId: String,
    onBack: () -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "group-details-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onBack()
        }
    }

    Scaffold(topBar = {
        AppTopBar(
            title = stringResource(R.string.edit_group_title),
            showBack = true,
            onBack = onBack,
        )
    }) { padding ->
        EditGroupContent(
            state = state,
            onNameChange = viewModel::updateEditName,
            onDescriptionChange = viewModel::updateEditDescription,
            onSave = viewModel::saveGroupDetails,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun EditGroupContent(
    state: EditGroupUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.edit_group_name_label)) },
            isError = state.nameInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.edit_group_description_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        if (state.writeState is WriteState.Failed) {
            Text(
                text = errorReasonMessage(state.writeState.reason),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        SubmitButton(
            state = state.writeState,
            label = stringResource(R.string.action_save),
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Edit group")
@Composable
private fun EditGroupContentPreview() {
    MaterialTheme {
        EditGroupContent(
            state = EditGroupUiState(
                name = "Flat 4B",
                description = "Shared flat expenses",
                loadedFromGroup = true,
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "Edit group failed")
@Composable
private fun EditGroupFailedPreview() {
    MaterialTheme {
        EditGroupContent(
            state = EditGroupUiState(
                name = "Flat 4B",
                writeState = WriteState.Failed(ErrorReason.NETWORK),
                loadedFromGroup = true,
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onSave = {},
        )
    }
}
