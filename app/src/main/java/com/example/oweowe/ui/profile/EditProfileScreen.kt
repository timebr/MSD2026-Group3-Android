package com.example.oweowe.ui.profile

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.copyAvatarToAppStorage
import com.example.oweowe.ui.components.errorReasonMessage
import com.example.oweowe.ui.components.rememberImageBitmap
import com.example.oweowe.ui.components.rememberImagePicker
import kotlinx.coroutines.launch

/** Edit profile (FR2): photo, name, email; the phone stays read-only. */
@Composable
fun EditProfileScreen(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.editState.collectAsStateWithLifecycle()

    // FR2: the picker only selects the image; it is copied on save so the
    // photo survives leaving this screen and restarting the app.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    // Re-entrancy guard while the picked photo is copied off the main thread.
    var savingPhoto by remember { mutableStateOf(false) }
    val pickPhoto = rememberImagePicker { picked -> pickedPhoto = picked?.toString() }

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
            onEmailChange = viewModel::updateEmail,
            onSave = {
                val picked = pickedPhoto
                when {
                    savingPhoto -> Unit
                    picked == null -> viewModel.saveProfile()
                    else -> {
                        savingPhoto = true
                        scope.launch {
                            val path = copyAvatarToAppStorage(context, picked.toUri())
                            savingPhoto = false
                            if (path != null) {
                                pickedPhoto = null
                                viewModel.saveProfile(photoPath = path)
                            } else {
                                viewModel.onPhotoSaveFailed()
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(padding),
            pickedPhoto = pickedPhoto,
            onPickPhoto = pickPhoto,
        )
    }
}

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    /** A photo picked in this session, not saved yet. */
    pickedPhoto: String? = null,
    onPickPhoto: () -> Unit = {},
) {
    val picked = rememberImageBitmap(pickedPhoto?.toUri())

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
                    onClick = onPickPhoto,
                ),
        ) {
            if (picked != null) {
                Image(
                    bitmap = picked,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            } else {
                InitialsAvatar(name = state.fullName, size = 72, photoPath = state.photoUri)
            }
        }
        Text(
            text = stringResource(R.string.edit_profile_photo_hint),
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
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.profile_email_label)) },
            isError = state.emailInvalid,
            supportingText = { Text(stringResource(R.string.edit_profile_email_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
            onEmailChange = {},
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
            onEmailChange = {},
            onSave = {},
        )
    }
}
