package com.example.oweowe.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.ErrorState
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.MyBalanceText
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SignedAmount
import com.example.oweowe.ui.components.asString

/** S3 Home dashboard (FR5, FR6, FR8, NFR3, NFR4, NFR5). */
@Composable
fun HomeScreen(
    container: AppContainer,
    onAddExpense: (groupId: String?) -> Unit,
    onGoToGroups: () -> Unit,
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(homeViewModel) {
        homeViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message.asString(context))
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        HomeContent(
            state = state,
            onAddExpense = { onAddExpense(state.group?.id) },
            onRemind = homeViewModel::sendReminder,
            onGoToGroups = onGoToGroups,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onAddExpense: () -> Unit,
    onRemind: (MemberBalance) -> Unit,
    onGoToGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.me == null -> ErrorStatePlaceholder()
            state.group == null -> EmptyState(
                title = stringResource(R.string.home_no_groups_title),
                message = stringResource(R.string.home_no_groups_message),
                action = { Button(onClick = onGoToGroups) { Text(stringResource(R.string.home_go_to_groups)) } },
            )
            else -> HomeList(state, onAddExpense, onRemind)
        }
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    onAddExpense: () -> Unit,
    onRemind: (MemberBalance) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.home_greeting, state.me?.fullName.orEmpty()),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            Text(
                text = stringResource(R.string.home_recent_group_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item { CurrentGroupCard(state, onAddExpense) }
        item {
            Text(
                text = stringResource(R.string.home_balances_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(state.memberBalances, key = { it.user.uid }) { member ->
            MemberBalanceRow(member, isMe = member.user.uid == state.me?.uid, onRemind = onRemind)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** S3: current group card with own balance and the Add expense action (FR4, FR6). */
@Composable
private fun CurrentGroupCard(state: HomeUiState, onAddExpense: () -> Unit) {
    val group = state.group ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleLarge)
            if (group.description.isNotBlank()) {
                Text(
                    group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.common_member_count, group.memberIds.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            MyBalanceText(state.myBalanceOre, Modifier.padding(top = 12.dp))
            Button(onClick = onAddExpense, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.home_add_expense))
            }
        }
    }
}

/** Row for one member in the S3 balances list, with a Remind action (FR8). */
@Composable
private fun MemberBalanceRow(
    member: MemberBalance,
    isMe: Boolean,
    onRemind: (MemberBalance) -> Unit,
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
                if (isMe) {
                    Text(
                        text = stringResource(R.string.common_you_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            if (member.owesMeOre > 0) {
                Text(
                    text = stringResource(R.string.home_member_owes_you, formatKr(member.owesMeOre)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SignedAmount(ore = member.balanceOre)
        if (member.owesMeOre > 0 && !isMe) {
            IconButton(onClick = { onRemind(member) }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = stringResource(R.string.cd_remind_member, member.user.fullName),
                )
            }
        }
    }
}

@Composable
private fun ErrorStatePlaceholder() {
    ErrorState(message = stringResource(R.string.common_error_generic))
}

@Preview(showBackground = true, name = "Home content")
@Composable
private fun HomeContentPreview() {
    val me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk")
    val group = Group(id = "g1", name = "Flat 4B", description = "Shared flat expenses", memberIds = listOf("u1", "u2", "u3"))
    MaterialTheme {
        HomeContent(
            state = HomeUiState(
                loading = false,
                me = me,
                group = group,
                memberBalances = listOf(
                    MemberBalance(me, balanceOre = 1_667, owesMeOre = 0),
                    MemberBalance(
                        User(uid = "u2", fullName = "Anna Test", email = "anna@test.dk"),
                        balanceOre = 2_700,
                        owesMeOre = 2_700,
                    ),
                    MemberBalance(
                        User(uid = "u3", fullName = "Jonas Test", email = "jonas@test.dk"),
                        balanceOre = -4_333,
                        owesMeOre = 0,
                    ),
                ),
                myBalanceOre = 1_667,
            ),
            onAddExpense = {},
            onRemind = {},
            onGoToGroups = {},
        )
    }
}

@Preview(showBackground = true, name = "Home loading")
@Composable
private fun HomeLoadingPreview() {
    MaterialTheme {
        HomeContent(state = HomeUiState(loading = true), onAddExpense = {}, onRemind = {}, onGoToGroups = {})
    }
}

@Preview(showBackground = true, name = "Home empty")
@Composable
private fun HomeEmptyPreview() {
    MaterialTheme {
        HomeContent(
            state = HomeUiState(loading = false, me = User(uid = "u1", fullName = "Tim Test")),
            onAddExpense = {},
            onRemind = {},
            onGoToGroups = {},
        )
    }
}

@Preview(showBackground = true, name = "Home offline")
@Composable
private fun HomeOfflinePreview() {
    MaterialTheme {
        HomeContent(
            state = HomeUiState(loading = false, offline = true),
            onAddExpense = {},
            onRemind = {},
            onGoToGroups = {},
        )
    }
}
