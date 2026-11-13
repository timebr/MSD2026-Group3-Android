package com.example.oweowe.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.oweowe.ui.components.asString

/** S6 Profile (FR2). */
@Composable
fun ProfileScreen(
    container: AppContainer,
    onEditProfile: () -> Unit,
    onNotificationSettings: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.profile_title)) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ProfileContent(
            state = state,
            onEditProfile = onEditProfile,
            onNotificationSettings = onNotificationSettings,
            onSetPushEnabled = viewModel::setPushEnabled,
            onSignOut = viewModel::signOut,
            developerOptions = { DeveloperOptions(container) },
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onEditProfile: () -> Unit,
    onNotificationSettings: () -> Unit,
    onSetPushEnabled: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    developerOptions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        InitialsAvatar(name = state.me.fullName, size = 56)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                        ) {
                            Text(state.me.fullName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                state.me.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                state.me.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Text(
                        stringResource(R.string.profile_edit),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onNotificationSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Notifications, contentDescription = null)
                    Text(
                        stringResource(R.string.profile_notification_settings),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.profile_sign_out))
                }
                developerOptions()
                Spacer(Modifier.height(24.dp))
            }
        }
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
                ),
            ),
            onEditProfile = {},
            onNotificationSettings = {},
            onSetPushEnabled = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, name = "Profile loading")
@Composable
private fun ProfileLoadingPreview() {
    MaterialTheme {
        ProfileContent(
            state = ProfileUiState(loading = true),
            onEditProfile = {},
            onNotificationSettings = {},
            onSetPushEnabled = {},
            onSignOut = {},
        )
    }
}
