package com.example.oweowe.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.errorReasonMessage

/** S9 Create group (FR3). On success the ViewModel emits the new ID; we open its overview. */
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
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
