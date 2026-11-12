package com.example.oweowe.ui.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.groups.contacts.hasContactsPermission

/**
 * S6 Settings (FR2): the notification preference for FR7 / FR8 notifications and
 * the contacts permission used to suggest group members (FR3).
 *
 * The preference is stored on the user (`pushEnabled`). Delivering the actual
 * notifications is the separate FR7 feature and is not implemented yet, so the
 * switch only records what the user wants.
 */
@Composable
fun NotificationSettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The contacts permission lives in Android, not in our data layer: the
    // switch mirrors it and sends the user to the system settings to revoke it.
    val context = LocalContext.current
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

    Scaffold(topBar = {
        AppTopBar(
            title = stringResource(R.string.notification_settings_title),
            showBack = true,
            onBack = onBack,
        )
    }) { padding ->
        NotificationSettingsContent(
            state = state,
            onSetPushEnabled = viewModel::setPushEnabled,
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
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun NotificationSettingsContent(
    state: ProfileUiState,
    onSetPushEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contactsAllowed: Boolean = false,
    contactsDenied: Boolean = false,
    onSetContactsAllowed: (Boolean) -> Unit = {},
    onOpenContactsSettings: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        if (state.loading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.notification_settings_push),
                        hint = stringResource(R.string.notification_settings_push_hint),
                        checked = state.me?.pushEnabled ?: false,
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

@Preview(showBackground = true, name = "Settings")
@Composable
private fun NotificationSettingsPreview() {
    MaterialTheme {
        NotificationSettingsContent(
            state = ProfileUiState(
                loading = false,
                me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk", pushEnabled = true),
            ),
            onSetPushEnabled = {},
        )
    }
}

@Preview(showBackground = true, name = "Settings contacts allowed")
@Composable
private fun NotificationSettingsContactsPreview() {
    MaterialTheme {
        NotificationSettingsContent(
            state = ProfileUiState(
                loading = false,
                me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk", pushEnabled = true),
            ),
            onSetPushEnabled = {},
            contactsAllowed = true,
        )
    }
}

@Preview(showBackground = true, name = "Settings contacts denied")
@Composable
private fun NotificationSettingsContactsDeniedPreview() {
    MaterialTheme {
        NotificationSettingsContent(
            state = ProfileUiState(
                loading = false,
                me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk"),
            ),
            onSetPushEnabled = {},
            contactsDenied = true,
        )
    }
}

@Preview(showBackground = true, name = "Settings loading")
@Composable
private fun NotificationSettingsLoadingPreview() {
    MaterialTheme {
        NotificationSettingsContent(state = ProfileUiState(loading = true), onSetPushEnabled = {})
    }
}
