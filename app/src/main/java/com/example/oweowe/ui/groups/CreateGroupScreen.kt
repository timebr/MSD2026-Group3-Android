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

/**
 * S9 Create group (FR3) as a bottom sheet. On success the ViewModel emits the
 * new ID; the host opens its overview and dismisses the sheet.
 */
@Composable
fun CreateGroupSheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit,
) {
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.factory(container))
    val state by viewModel.createState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.createdGroupId.collect { groupId -> onGroupCreated(groupId) }
    }

    AppBottomSheet(onDismiss = onDismiss) {
        SheetHeader(stringResource(R.string.create_group_title))
        CreateGroupContent(
            state = state,
            onNameChange = viewModel::updateName,
            onDescriptionChange = viewModel::updateDescription,
            onCreate = viewModel::createGroup,
        )
    }
}

/** Full-screen S9 kept as a fallback while the sheet route is validated. */
@Composable
fun CreateGroupScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
) {
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.factory(container))
    val state by viewModel.createState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.createdGroupId.collect { groupId -> onGroupCreated(groupId) }
    }

    Scaffold(topBar = {
        AppTopBar(
            title = stringResource(R.string.create_group_title),
            showBack = true,
            onBack = onBack,
        )
    }) { padding ->
        CreateGroupContent(
            state = state,
            onNameChange = viewModel::updateName,
            onDescriptionChange = viewModel::updateDescription,
            onCreate = viewModel::createGroup,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun CreateGroupContent(
    state: CreateGroupUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCreate: () -> Unit,
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
            label = { Text(stringResource(R.string.create_group_name_label)) },
            isError = state.nameInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.create_group_description_label)) },
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
            label = stringResource(R.string.create_group_button),
            onClick = onCreate,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Create group")
@Composable
private fun CreateGroupContentPreview() {
    MaterialTheme {
        CreateGroupContent(
            state = CreateGroupUiState(name = "Flat 4B", description = "Shared flat expenses"),
            onNameChange = {},
            onDescriptionChange = {},
            onCreate = {},
        )
    }
}

@Preview(showBackground = true, name = "Create group failed")
@Composable
private fun CreateGroupFailedPreview() {
    MaterialTheme {
        CreateGroupContent(
            state = CreateGroupUiState(
                name = "Flat 4B",
                writeState = WriteState.Failed(ErrorReason.NETWORK),
            ),
            onNameChange = {},
            onDescriptionChange = {},
            onCreate = {},
        )
    }
}

@Preview(showBackground = true, name = "Create group busy")
@Composable
private fun CreateGroupBusyPreview() {
    MaterialTheme {
        CreateGroupContent(
            state = CreateGroupUiState(name = "Flat 4B", writeState = WriteState.Busy),
            onNameChange = {},
            onDescriptionChange = {},
            onCreate = {},
        )
    }
}
