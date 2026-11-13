package com.example.oweowe.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.asString

/** S8 Group members with S10 Add member (FR3). */
@Composable
fun ManageMembersScreen(
    container: AppContainer,
    groupId: String,
    onBack: () -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "group-details-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.manage_members_title),
                showBack = true,
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ManageMembersContent(
            state = state,
            onAddMember = viewModel::addMemberByEmail,
            onRemoveMember = viewModel::removeMember,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun ManageMembersContent(
    state: GroupOverviewUiState,
    onAddMember: (String) -> Unit,
    onRemoveMember: (MemberRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.manage_members_add_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.manage_members_email_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = {
                            onAddMember(email.trim())
                            email = ""
                        },
                        enabled = !state.busy,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Text(stringResource(R.string.manage_members_add_button))
                        }
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = stringResource(R.string.group_overview_members),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(state.members, key = { it.user.uid }) { member ->
                ManageMemberRow(member, enabled = !state.busy, onRemoveMember = onRemoveMember)
            }
        }
    }
}

@Composable
private fun ManageMemberRow(
    member: MemberRow,
    enabled: Boolean,
    onRemoveMember: (MemberRow) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        InitialsAvatar(name = member.user.fullName)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.user.fullName, style = MaterialTheme.typography.bodyLarge)
                if (member.isMe) {
                    Text(
                        text = stringResource(R.string.common_you_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                text = member.user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = { onRemoveMember(member) },
            enabled = enabled && member.canRemove,
        ) {
            Text(stringResource(R.string.manage_members_remove))
        }
    }
}

@Preview(showBackground = true, name = "Manage members")
@Composable
private fun ManageMembersPreview() {
    MaterialTheme {
        ManageMembersContent(
            state = GroupOverviewUiState(
                loading = false,
                me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk"),
                members = listOf(
                    MemberRow(
                        User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk"),
                        balanceOre = 0, isMe = true, canRemove = false, owesMeOre = 0,
                    ),
                    MemberRow(
                        User(uid = "u2", fullName = "Anna Test", email = "anna@test.dk"),
                        balanceOre = 0, isMe = false, canRemove = true, owesMeOre = 0,
                    ),
                ),
            ),
            onAddMember = {},
            onRemoveMember = {},
        )
    }
}

@Preview(showBackground = true, name = "Manage members busy")
@Composable
private fun ManageMembersBusyPreview() {
    MaterialTheme {
        ManageMembersContent(
            state = GroupOverviewUiState(
                loading = false,
                busy = true,
                members = listOf(
                    MemberRow(
                        User(uid = "u2", fullName = "Anna Test", email = "anna@test.dk"),
                        balanceOre = 0, isMe = false, canRemove = true, owesMeOre = 0,
                    ),
                ),
            ),
            onAddMember = {},
            onRemoveMember = {},
        )
    }
}
