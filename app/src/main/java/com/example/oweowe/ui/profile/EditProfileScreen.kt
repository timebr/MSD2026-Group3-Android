package com.example.oweowe.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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

/** Edit profile (FR2): name and phone; the email is the account and cannot change here. */
@Composable
fun EditProfileScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onBack()
        }
    }

    Scaffold(topBar = {
        AppTopBar(
            title = stringResource(R.string.edit_profile_title),
            showBack = true,
            onBack = onBack,
        )
    }) { padding ->
        EditProfileContent(
            state = state,
            onNameChange = viewModel::updateFullName,
            onPhoneChange = viewModel::updatePhone,
            onSave = viewModel::saveProfile,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
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
            value = state.fullName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.profile_name_label)) },
            isError = state.nameInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.profile_phone_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
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

@Preview(showBackground = true, name = "Edit profile")
@Composable
private fun EditProfilePreview() {
    MaterialTheme {
        EditProfileContent(
            state = EditProfileUiState(
                fullName = "Tim Test",
                phone = "+45 11 11 11 11",
                loadedFromMe = true,
            ),
            onNameChange = {},
            onPhoneChange = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, name = "Edit profile failed")
@Composable
private fun EditProfileFailedPreview() {
    MaterialTheme {
        EditProfileContent(
            state = EditProfileUiState(
                fullName = "Tim Test",
                phone = "+45 11 11 11 11",
                writeState = WriteState.Failed(ErrorReason.NETWORK),
                loadedFromMe = true,
            ),
            onNameChange = {},
            onPhoneChange = {},
            onSave = {},
        )
    }
}
