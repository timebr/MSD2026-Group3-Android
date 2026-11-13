package com.example.oweowe.ui.profile

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.errorReasonMessage
import com.example.oweowe.ui.components.rememberImageBitmap
import com.example.oweowe.ui.components.rememberImagePicker

/** Edit profile (FR2): avatar, editable name; email and phone are read-only here. */
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
            onSave = viewModel::saveProfile,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    val pickPhoto = rememberImagePicker { picked -> photoUri = picked?.toString() }
    val photo = rememberImageBitmap(photoUri?.let(Uri::parse))

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .clickable(
                    onClickLabel = stringResource(R.string.cd_change_profile_photo),
                    onClick = pickPhoto,
                ),
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            } else {
                InitialsAvatar(name = state.fullName, size = 72)
            }
        }
        Text(
            text = stringResource(R.string.edit_profile_photo_preview_only),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.fullName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.profile_name_label)) },
            isError = state.nameInvalid,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = {},
            enabled = false,
            label = { Text(stringResource(R.string.profile_email_label)) },
            supportingText = { Text(stringResource(R.string.edit_profile_email_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = {},
            enabled = false,
            label = { Text(stringResource(R.string.profile_phone_label)) },
            supportingText = { Text(stringResource(R.string.edit_profile_phone_locked)) },
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
                email = "tim@test.dk",
                phone = "+45 11 11 11 11",
                loadedFromMe = true,
            ),
            onNameChange = {},
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
                email = "tim@test.dk",
                phone = "+45 11 11 11 11",
                writeState = WriteState.Failed(ErrorReason.NETWORK),
                loadedFromMe = true,
            ),
            onNameChange = {},
            onSave = {},
        )
    }
}
