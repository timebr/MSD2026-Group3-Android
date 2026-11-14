package com.example.oweowe.ui.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.asString
import com.example.oweowe.ui.components.copyAvatarToAppStorage
import com.example.oweowe.ui.components.errorReasonMessage
import com.example.oweowe.ui.components.rememberImageBitmap
import com.example.oweowe.ui.components.rememberImagePicker
import com.example.oweowe.ui.groups.contacts.hasContactsPermission
import kotlinx.coroutines.launch

/**
 * S6 Profile (FR2) as one editable screen: photo, name and email, the push and
 * contacts preferences, Save changes and Sign out. The phone number stays
 * read-only (changing it needs a new account). Sign out flips the auth
 * repository's `currentUserId`; OweOweApp then switches to Sign in.
 */
@Composable
fun ProfileScreen(container: AppContainer) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    // FR2: the picker only selects the image; it is copied on save so the photo
    // survives leaving this screen and restarting the app.
    val scope = rememberCoroutineScope()
    var pickedPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var savingPhoto by remember { mutableStateOf(false) }
    val pickPhoto = rememberImagePicker { picked -> pickedPhoto = picked?.toString() }

    // FR2: once the save has been acknowledged, return the button to normal.
    LaunchedEffect(editState.writeState) {
        if (editState.writeState == WriteState.Done) viewModel.resetWriteState()
    }

    // The contacts permission lives in Android, not in our data layer: the switch
    // mirrors it and sends the user to the system settings to revoke it.
    val appContext = context.applicationContext
    var contactsAllowed by remember { mutableStateOf(hasContactsPermission(appContext)) }
    var contactsDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        contactsAllowed = granted
        contactsDenied = !granted
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        contactsAllowed = hasContactsPermission(appContext)
    }
    fun openSystemSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.profile_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ProfileContent(
            state = state,
            editState = editState,
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
            onSetPushEnabled = viewModel::setPushEnabled,
            onSignOut = viewModel::signOut,
            contactsAllowed = contactsAllowed,
            contactsDenied = contactsDenied,
            onSetContactsAllowed = { wanted ->
                when {
                    // Android already remembered a refusal: only the system
                    // settings screen can ask again.
                    wanted && contactsDenied -> openSystemSettings()
                    wanted -> permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    // An app cannot revoke its own permission.
                    else -> openSystemSettings()
                }
            },
            onOpenContactsSettings = ::openSystemSettings,
            pickedPhoto = pickedPhoto,
            onPickPhoto = pickPhoto,
            developerOptions = { DeveloperOptions(container) },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    editState: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit,
    onSetPushEnabled: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    contactsAllowed: Boolean = false,
    contactsDenied: Boolean = false,
    onSetContactsAllowed: (Boolean) -> Unit = {},
    onOpenContactsSettings: () -> Unit = {},
    /** A photo picked in this session, not saved yet. */
    pickedPhoto: String? = null,
    onPickPhoto: () -> Unit = {},
    developerOptions: @Composable () -> Unit = {},
) {
    val picked = rememberImageBitmap(pickedPhoto?.toUri())

    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.me == null -> EmptyState(title = stringResource(R.string.profile_not_signed_in))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        InitialsAvatar(
                            name = editState.fullName.ifBlank { state.me.fullName },
                            size = 72,
                            photoPath = state.me.photoUri,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.edit_profile_photo_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                OutlinedTextField(
                    value = editState.fullName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.profile_name_label)) },
                    isError = editState.nameInvalid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editState.email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.profile_email_label)) },
                    isError = editState.emailInvalid,
                    supportingText = { Text(stringResource(R.string.edit_profile_email_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editState.phone,
                    onValueChange = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.profile_phone_label)) },
                    supportingText = { Text(stringResource(R.string.edit_profile_phone_locked)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Card(Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.notification_settings_push),
                        hint = stringResource(R.string.notification_settings_push_hint),
                        checked = state.me.pushEnabled,
                        enabled = !state.pushBusy,
                        onCheckedChange = onSetPushEnabled,
                    )
                }
                Card(Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_contacts_access),
                        hint = stringResource(
                            when {
                                contactsDenied -> R.string.settings_contacts_denied
                                contactsAllowed -> R.string.settings_contacts_hint
                                else -> R.string.settings_contacts_off
                            },
                        ),
                        checked = contactsAllowed,
                        enabled = true,
                        onCheckedChange = onSetContactsAllowed,
                    )
                }
                if (contactsDenied) {
                    TextButton(onClick = onOpenContactsSettings) {
                        Text(stringResource(R.string.settings_open_system_settings))
                    }
                }
                if (editState.writeState is WriteState.Failed) {
                    Text(
                        text = errorReasonMessage(editState.writeState.reason),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SubmitButton(
                    state = editState.writeState,
                    label = stringResource(R.string.profile_save_changes),
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.profile_sign_out))
                }
                developerOptions()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

/**
 * Debug knobs for the FakeBackend (not a product feature): lets anyone demo
 * the pending / failed / offline states without touching code.
 */
@Composable
private fun DeveloperOptions(container: AppContainer) {
    var failNext by rememberSaveable { mutableStateOf(false) }
    var slow by rememberSaveable { mutableStateOf(false) }

    fun applyKnobs() {
        container.fakeBackend.delayMs = if (slow) 3_000L else 300L
        // Only arm the failure when the user asked for it on this screen.
        container.fakeBackend.failNextWith = if (failNext) ErrorReason.NETWORK else null
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dev_options_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.dev_fail_next_write),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = failNext,
                    onCheckedChange = {
                        failNext = it
                        applyKnobs()
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.dev_slow_network),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = slow,
                    onCheckedChange = {
                        slow = it
                        applyKnobs()
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Profile")
@Composable
private fun ProfilePreview() {
    MaterialTheme {
        ProfileContent(
            state = ProfileUiState(
                loading = false,
                me = User(
                    uid = "u1",
                    fullName = "Tim Test",
                    email = "tim@test.dk",
                    phone = "+45 11 11 11 11",
                    pushEnabled = true,
                ),
            ),
            editState = EditProfileUiState(
                fullName = "Tim Test",
                email = "tim@test.dk",
                phone = "+45 11 11 11 11",
                loadedFromMe = true,
            ),
            onNameChange = {},
            onEmailChange = {},
            onSave = {},
            onSetPushEnabled = {},
            onSignOut = {},
            contactsAllowed = true,
        )
    }
}

@Preview(showBackground = true, name = "Profile loading")
@Composable
private fun ProfileLoadingPreview() {
    MaterialTheme {
        ProfileContent(
            state = ProfileUiState(loading = true),
            editState = EditProfileUiState(),
            onNameChange = {},
            onEmailChange = {},
            onSave = {},
            onSetPushEnabled = {},
            onSignOut = {},
        )
    }
}
