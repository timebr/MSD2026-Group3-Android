package com.example.oweowe.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.oweowe.data.model.Group
import com.example.oweowe.domain.formatKr
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.ErrorState
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import androidx.compose.foundation.shape.CircleShape

/** S4 Groups (FR3). */
@Composable
fun GroupsScreen(
    container: AppContainer,
    onCreateGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onEditGroup: (String) -> Unit,
    onManageMembers: (String) -> Unit,
) {
    val viewModel: GroupsViewModel = viewModel(factory = GroupsViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.groups_title),
                actions = {
                    IconButton(onClick = onCreateGroup) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.groups_create),
                        )
                    }
                },
            )
        },
    ) { padding ->
        GroupsContent(
            state = state,
            onOpenGroup = onOpenGroup,
            onSelectGroup = viewModel::selectGroup,
            onEditGroup = onEditGroup,
            onManageMembers = onManageMembers,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun GroupsContent(
    state: GroupsUiState,
    onOpenGroup: (String) -> Unit,
    onSelectGroup: (String) -> Unit,
    onEditGroup: (String) -> Unit,
    onManageMembers: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.loadError -> ErrorState(message = stringResource(R.string.common_error_generic))
            state.groups.isEmpty() -> EmptyState(
                title = stringResource(R.string.groups_empty_title),
                message = stringResource(R.string.groups_empty_message),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val selected = state.selectedGroup
                if (selected != null) {
                    item {
                        SelectedGroupCard(
                            summary = selected,
                            onOpenGroup = { onOpenGroup(selected.group.id) },
                            onEditGroup = { onEditGroup(selected.group.id) },
                            onManageMembers = { onManageMembers(selected.group.id) },
                        )
                    }
                }
                // The selected group is shown once, above; the rest stay as rows.
                items(
                    state.groups.filter { it.group.id != selected?.group?.id },
                    key = { it.group.id },
                ) { summary ->
                    GroupCard(
                        summary = summary,
                        onClick = { onOpenGroup(summary.group.id) },
                        onSelect = { onSelectGroup(summary.group.id) },
                    )
                }
            }
        }
    }
}

/** S4: the selected group shown as an inline overview (FR3, FR5). */
@Composable
private fun SelectedGroupCard(
    summary: GroupSummary,
    onOpenGroup: () -> Unit,
    onEditGroup: () -> Unit,
    onManageMembers: () -> Unit,
) {
    val group = summary.group
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenGroup),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.groups_selected_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text(
                text = groupStatusText(summary),
                style = MaterialTheme.typography.bodyMedium,
                color = groupStatusColor(summary),
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                OutlinedButton(onClick = onEditGroup, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.group_overview_edit_details))
                }
                OutlinedButton(onClick = onManageMembers, shape = CircleShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.group_overview_manage_members))
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    summary: GroupSummary,
    onClick: () -> Unit,
    onSelect: (() -> Unit)? = null,
) {
    val group = summary.group
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
            if (group.description.isNotBlank()) {
                Text(
                    group.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.common_member_count, group.memberIds.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = groupStatusText(summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = groupStatusColor(summary),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (onSelect != null) {
                    TextButton(onClick = onSelect) {
                        Text(stringResource(R.string.groups_select))
                    }
                }
            }
        }
    }
}

@Composable
private fun groupStatusText(summary: GroupSummary): String = when {
    summary.myNetOre > 0 -> stringResource(R.string.groups_you_are_owed, formatKr(summary.myNetOre))
    summary.myNetOre < 0 -> stringResource(R.string.groups_you_owe, formatKr(-summary.myNetOre))
    else -> stringResource(R.string.groups_settled)
}

@Composable
private fun groupStatusColor(summary: GroupSummary) = when {
    summary.myNetOre > 0 -> MaterialTheme.colorScheme.primary
    summary.myNetOre < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Preview(showBackground = true, name = "Groups list")
@Composable
private fun GroupsContentPreview() {
    MaterialTheme {
        GroupsContent(
            state = GroupsUiState(
                loading = false,
                groups = listOf(
                    GroupSummary(
                        Group(id = "g1", name = "Flat 4B", description = "Shared flat expenses", memberIds = listOf("u1", "u2", "u3")),
                        myNetOre = 12_500,
                    ),
                    GroupSummary(
                        Group(id = "g2", name = "Trip to Berlin", description = "", memberIds = listOf("u1", "u4")),
                        myNetOre = -3_250,
                    ),
                ),
                selectedGroup = GroupSummary(
                    Group(id = "g1", name = "Flat 4B", description = "Shared flat expenses", memberIds = listOf("u1", "u2", "u3")),
                    myNetOre = 12_500,
                ),
            ),
            onOpenGroup = {},
            onSelectGroup = {},
            onEditGroup = {},
            onManageMembers = {},
        )
    }
}

@Preview(showBackground = true, name = "Groups loading")
@Composable
private fun GroupsLoadingPreview() {
    MaterialTheme {
        GroupsContent(state = GroupsUiState(loading = true), onOpenGroup = {}, onSelectGroup = {}, onEditGroup = {}, onManageMembers = {})
    }
}

@Preview(showBackground = true, name = "Groups empty")
@Composable
private fun GroupsEmptyPreview() {
    MaterialTheme {
        GroupsContent(state = GroupsUiState(loading = false), onOpenGroup = {}, onSelectGroup = {}, onEditGroup = {}, onManageMembers = {})
    }
}

@Preview(showBackground = true, name = "Groups offline")
@Composable
private fun GroupsOfflinePreview() {
    MaterialTheme {
        GroupsContent(
            state = GroupsUiState(loading = false, offline = true, groups = emptyList()),
            onOpenGroup = {},
            onSelectGroup = {},
            onEditGroup = {},
            onManageMembers = {},
        )
    }
}

@Preview(showBackground = true, name = "Groups load error")
@Composable
private fun GroupsLoadErrorPreview() {
    MaterialTheme {
        GroupsContent(
            state = GroupsUiState(loading = false, loadError = true),
            onOpenGroup = {},
            onSelectGroup = {},
            onEditGroup = {},
            onManageMembers = {},
        )
    }
}
