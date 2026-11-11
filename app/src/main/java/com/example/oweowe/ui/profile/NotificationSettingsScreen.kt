package com.example.oweowe.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner

/** Notification settings (FR2): the push preference for FR7 / FR8 notifications. */
@Composable
fun NotificationSettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun NotificationSettingsContent(
    state: ProfileUiState,
    onSetPushEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        if (state.loading) {
            LoadingState()
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.notification_settings_push),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.notification_settings_push_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.me?.pushEnabled ?: false,
                        onCheckedChange = onSetPushEnabled,
                        enabled = !state.pushBusy,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Notification settings")
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

@Preview(showBackground = true, name = "Notification settings loading")
@Composable
private fun NotificationSettingsLoadingPreview() {
    MaterialTheme {
        NotificationSettingsContent(state = ProfileUiState(loading = true), onSetPushEnabled = {})
    }
}
