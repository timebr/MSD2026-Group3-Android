package com.example.oweowe.ui.activity

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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.model.User
import com.example.oweowe.domain.formatKr
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.MyBalanceText
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.TransactionRow

/** S5 Activity (FR5, FR6, FR7). */
@Composable
fun ActivityScreen(container: AppContainer) {
    val viewModel: ActivityViewModel = viewModel(factory = ActivityViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.activity_title)) },
    ) { padding ->
        ActivityContent(state = state, modifier = Modifier.padding(padding))
    }
}

@Composable
fun ActivityContent(
    state: ActivityUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.transactions.isEmpty() && state.reminders.isEmpty() -> EmptyState(
                title = stringResource(R.string.activity_empty_title),
                message = stringResource(R.string.activity_empty_message),
            )
            else -> ActivityList(state)
        }
    }
}

@Composable
private fun ActivityList(state: ActivityUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.reminders.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.activity_reminders_title),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        state.reminders.forEach { reminder ->
                            Text(
                                text = stringResource(
                                    R.string.activity_reminder_received,
                                    reminder.fromName,
                                    formatKr(reminder.reminder.amountOre),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.activity_total_spent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatKr(state.totalSpentOre),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    MyBalanceText(
                        balanceOre = state.myBalanceOre,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.activity_transactions_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        items(state.transactions, key = { it.expense.id }) { transaction ->
            TransactionRow(
                expense = transaction.expense,
                payerName = transaction.payerName,
                groupName = transaction.groupName,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Preview(showBackground = true, name = "Activity")
@Composable
private fun ActivityPreview() {
    val me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk")
    MaterialTheme {
        ActivityContent(
            state = ActivityUiState(
                loading = false,
                me = me,
                totalSpentOre = 129_000,
                myBalanceOre = 1_667,
                transactions = listOf(
                    ActivityTransaction(
                        expense = Expense(
                            id = "e1", groupId = "g1", description = "Groceries", category = "Food",
                            amountOre = 30_000, paidBy = "u1", createdBy = "u1", createdAt = 1,
                        ),
                        groupName = "Flat 4B",
                        payerName = "Tim Test",
                    ),
                    ActivityTransaction(
                        expense = Expense(
                            id = "e2", groupId = "g1", description = "Pizza night", category = "Food",
                            amountOre = 45_000, paidBy = "u2", createdBy = "u2", createdAt = 2,
                            syncStatus = SyncStatus.PENDING,
                        ),
                        groupName = "Flat 4B",
                        payerName = "Anna Test",
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, name = "Activity empty")
@Composable
private fun ActivityEmptyPreview() {
    MaterialTheme {
        ActivityContent(state = ActivityUiState(loading = false))
    }
}

@Preview(showBackground = true, name = "Activity loading")
@Composable
private fun ActivityLoadingPreview() {
    MaterialTheme {
        ActivityContent(state = ActivityUiState(loading = true))
    }
}
