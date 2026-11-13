package com.example.oweowe.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.domain.formatKr
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.ErrorState
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SignedAmount
import com.example.oweowe.ui.components.asString

/** S4 Group overview (FR3, FR5, FR6, FR8). */
@Composable
fun GroupOverviewScreen(
    container: AppContainer,
    groupId: String,
    onBack: () -> Unit,
    onEditGroup: (String) -> Unit,
    onManageMembers: (String) -> Unit,
    onAddExpense: (String) -> Unit,
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
                title = state.group?.name ?: stringResource(R.string.group_overview_members),
                showBack = true,
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        GroupOverviewContent(
            state = state,
            onEditGroup = { onEditGroup(groupId) },
            onManageMembers = { onManageMembers(groupId) },
            onAddExpense = { onAddExpense(groupId) },
            onRemind = viewModel::sendReminder,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun GroupOverviewContent(
    state: GroupOverviewUiState,
    onEditGroup: () -> Unit,
    onManageMembers: () -> Unit,
    onAddExpense: () -> Unit,
    onRemind: (MemberRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.notFound -> ErrorState(message = stringResource(R.string.group_overview_not_found))
            else -> GroupOverviewList(state, onEditGroup, onManageMembers, onAddExpense, onRemind)
        }
    }
}

@Composable
private fun GroupOverviewList(
    state: GroupOverviewUiState,
    onEditGroup: () -> Unit,
    onManageMembers: () -> Unit,
    onAddExpense: () -> Unit,
    onRemind: (MemberRow) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.group_overview_total_spent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatKr(state.totalSpentOre),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    state.group?.takeIf { it.description.isNotBlank() }?.let { group ->
                        Text(
                            group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEditGroup, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.group_overview_edit_details))
                }
                OutlinedButton(onClick = onManageMembers, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.group_overview_manage_members))
                }
            }
        }
        item {
            Button(onClick = onAddExpense, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_add_expense))
            }
        }
        item {
            Text(
                text = stringResource(R.string.group_overview_members),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(state.members, key = { it.user.uid }) { member ->
            GroupMemberRow(
                member = member,
                remindEnabled = !state.busy,
                onRemind = onRemind,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun GroupMemberRow(
    member: MemberRow,
    remindEnabled: Boolean,
    onRemind: (MemberRow) -> Unit,
) {
    val owesMe = member.owesMeOre > 0
    val status = when {
        member.isMe -> stringResource(R.string.group_member_this_is_you)
        owesMe -> stringResource(R.string.home_member_owes_you, formatKr(member.owesMeOre))
        member.iOweOre > 0 -> stringResource(R.string.home_you_owe_member, formatKr(member.iOweOre))
        else -> stringResource(R.string.home_member_settled)
    }
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
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    member.isMe -> MaterialTheme.colorScheme.onSurfaceVariant
                    owesMe -> MaterialTheme.colorScheme.primary
                    member.iOweOre > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        SignedAmount(ore = if (member.isMe) member.balanceOre else member.owesMeOre - member.iOweOre)
        if (owesMe && !member.isMe) {
            TextButton(onClick = { onRemind(member) }, enabled = remindEnabled) {
                Text(stringResource(R.string.home_remind))
            }
        }
    }
}

@Preview(showBackground = true, name = "Group overview")
@Composable
private fun GroupOverviewPreview() {
    val me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk")
    MaterialTheme {
        GroupOverviewContent(
            state = GroupOverviewUiState(
                loading = false,
                me = me,
                group = Group(id = "g1", name = "Flat 4B", description = "Shared flat expenses", memberIds = listOf("u1", "u2", "u3")),
                totalSpentOre = 129_000,
                members = listOf(
                    MemberRow(me, balanceOre = 1_667, isMe = true, canRemove = false, owesMeOre = 0),
                    MemberRow(
                        User(uid = "u2", fullName = "Anna Test", email = "anna@test.dk"),
                        balanceOre = 2_700, isMe = false, canRemove = false, owesMeOre = 2_700,
                    ),
                    MemberRow(
                        User(uid = "u3", fullName = "Jonas Test", email = "jonas@test.dk"),
                        balanceOre = -4_333, isMe = false, canRemove = false, owesMeOre = 0, iOweOre = 4_333,
                    ),
                ),
            ),
            onEditGroup = {},
            onManageMembers = {},
            onAddExpense = {},
            onRemind = {},
        )
    }
}

@Preview(showBackground = true, name = "Group overview loading")
@Composable
private fun GroupOverviewLoadingPreview() {
    MaterialTheme {
        GroupOverviewContent(
            state = GroupOverviewUiState(loading = true),
            onEditGroup = {},
            onManageMembers = {},
            onAddExpense = {},
            onRemind = {},
        )
    }
}

@Preview(showBackground = true, name = "Group overview not found")
@Composable
private fun GroupOverviewNotFoundPreview() {
    MaterialTheme {
        GroupOverviewContent(
            state = GroupOverviewUiState(loading = false, notFound = true),
            onEditGroup = {},
            onManageMembers = {},
            onAddExpense = {},
            onRemind = {},
        )
    }
}
