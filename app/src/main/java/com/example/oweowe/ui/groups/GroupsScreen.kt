package com.example.oweowe.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.Group
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner

/** S4 Groups (FR3). */
@Composable
fun GroupsScreen(
    container: AppContainer,
    onCreateGroup: () -> Unit,
    onOpenGroup: (String) -> Unit,
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
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun GroupsContent(
    state: GroupsUiState,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.groups.isEmpty() -> EmptyState(
                title = stringResource(R.string.groups_empty_title),
                message = stringResource(R.string.groups_empty_message),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.groups, key = { it.id }) { group ->
                    GroupCard(group = group, onClick = { onOpenGroup(group.id) })
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: Group, onClick: () -> Unit) {
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
            Text(
                text = stringResource(R.string.common_member_count, group.memberIds.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "Groups list")
@Composable
private fun GroupsContentPreview() {
    MaterialTheme {
        GroupsContent(
            state = GroupsUiState(
                loading = false,
                groups = listOf(
                    Group(id = "g1", name = "Flat 4B", description = "Shared flat expenses", memberIds = listOf("u1", "u2", "u3")),
                    Group(id = "g2", name = "Trip to Berlin", description = "", memberIds = listOf("u1", "u4")),
                ),
            ),
            onOpenGroup = {},
        )
    }
}

@Preview(showBackground = true, name = "Groups loading")
@Composable
private fun GroupsLoadingPreview() {
    MaterialTheme {
        GroupsContent(state = GroupsUiState(loading = true), onOpenGroup = {})
    }
}

@Preview(showBackground = true, name = "Groups empty")
@Composable
private fun GroupsEmptyPreview() {
    MaterialTheme {
        GroupsContent(state = GroupsUiState(loading = false), onOpenGroup = {})
    }
}

@Preview(showBackground = true, name = "Groups offline")
@Composable
private fun GroupsOfflinePreview() {
    MaterialTheme {
        GroupsContent(
            state = GroupsUiState(loading = false, offline = true, groups = emptyList()),
            onOpenGroup = {},
        )
    }
}
