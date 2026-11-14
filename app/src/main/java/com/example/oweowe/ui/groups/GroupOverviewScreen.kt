package com.example.oweowe.ui.groups

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.domain.formatKr
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.ErrorState
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.MyBalanceText
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SignedAmount
import com.example.oweowe.ui.components.TransactionRow
import com.example.oweowe.ui.components.asString
import com.example.oweowe.ui.components.rememberImageBitmap
import java.io.File
import kotlinx.coroutines.launch

/** S4 Group overview (FR3, FR5, FR6, FR8). */
@Composable
fun GroupOverviewScreen(
    container: AppContainer,
    groupId: String,
    onBack: () -> Unit,
    onEditGroup: (String) -> Unit,
    onManageMembers: (String) -> Unit,
    onAddExpense: (String) -> Unit,
    onEditExpense: (String) -> Unit,
) {
    val viewModel: GroupDetailsViewModel = viewModel(
        key = "group-details-$groupId",
        factory = GroupDetailsViewModel.factory(container, groupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noReceiptMessage = stringResource(R.string.group_overview_no_receipt)
    // FR10: the receipt path shown in the dialog, or null when it is closed.
    var receiptToShow by remember { mutableStateOf<String?>(null) }
    // The expense awaiting delete confirmation, or null when no dialog is open.
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
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
            onEditExpense = onEditExpense,
            onDeleteExpense = { expense -> expenseToDelete = expense },
            onRemind = viewModel::sendReminder,
            onTransactionClick = { expense ->
                val receipt = expense.receiptUri
                if (receipt == null) {
                    scope.launch { snackbarHostState.showSnackbar(noReceiptMessage) }
                } else {
                    receiptToShow = receipt
                }
            },
            modifier = Modifier.padding(padding),
        )
    }
    receiptToShow?.let { path ->
        ReceiptDialog(path = path, onDismiss = { receiptToShow = null })
    }
    expenseToDelete?.let { expense ->
        DeleteExpenseDialog(
            expense = expense,
            onConfirm = {
                viewModel.deleteExpense(expense)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null },
        )
    }
}

@Composable
fun GroupOverviewContent(
    state: GroupOverviewUiState,
    onEditGroup: () -> Unit,
    onManageMembers: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onRemind: (MemberRow) -> Unit,
    modifier: Modifier = Modifier,
    onTransactionClick: (Expense) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.notFound -> ErrorState(message = stringResource(R.string.group_overview_not_found))
            else -> GroupOverviewList(
                state = state,
                onEditGroup = onEditGroup,
                onManageMembers = onManageMembers,
                onAddExpense = onAddExpense,
                onEditExpense = onEditExpense,
                onDeleteExpense = onDeleteExpense,
                onRemind = onRemind,
                onTransactionClick = onTransactionClick,
            )
        }
    }
}

@Composable
private fun GroupOverviewList(
    state: GroupOverviewUiState,
    onEditGroup: () -> Unit,
    onManageMembers: () -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (String) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onRemind: (MemberRow) -> Unit,
    onTransactionClick: (Expense) -> Unit,
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
                text = stringResource(R.string.group_overview_balances),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        // FR5: my own balance is one line, not a member row (S4).
        item {
            MyBalanceText(state.members.firstOrNull { it.isMe }?.balanceOre ?: 0L)
        }
        items(state.members.filter { !it.isMe }, key = { it.user.uid }) { member ->
            GroupMemberRow(
                member = member,
                remindEnabled = !state.busy,
                onRemind = onRemind,
            )
        }
        item {
            Text(
                text = stringResource(R.string.group_overview_transactions),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (state.transactions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.group_overview_transactions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.transactions, key = { it.expense.id }) { transaction ->
            TransactionRow(
                expense = transaction.expense,
                payerName = transaction.payerName,
                payeeName = transaction.payeeName,
                onClick = { onTransactionClick(transaction.expense) },
                // Settlements are not an expense form; only EXPENSE rows are editable or deletable.
                onEdit = if (transaction.expense.type == ExpenseType.EXPENSE) {
                    { onEditExpense(transaction.expense.id) }
                } else {
                    null
                },
                onDelete = if (transaction.expense.type == ExpenseType.EXPENSE) {
                    { onDeleteExpense(transaction.expense) }
                } else {
                    null
                },
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
            Text(member.user.fullName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    owesMe -> MaterialTheme.colorScheme.primary
                    member.iOweOre > 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        SignedAmount(ore = member.owesMeOre - member.iOweOre)
        if (owesMe) {
            TextButton(onClick = { onRemind(member) }, enabled = remindEnabled) {
                Text(stringResource(R.string.home_remind))
            }
        }
    }
}

/** Confirms that the expense is really deleted, for the whole group. */
@Composable
private fun DeleteExpenseDialog(
    expense: Expense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = expense.description.ifBlank { expense.category }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_expense_title)) },
        text = {
            Text(
                stringResource(
                    R.string.delete_expense_message,
                    name,
                    formatKr(expense.amountOre),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** FR10: the receipt image of one transaction, read-only. */
@Composable
private fun ReceiptDialog(path: String, onDismiss: () -> Unit) {
    val bitmap = rememberImageBitmap(Uri.fromFile(File(path)))
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.group_overview_receipt_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (bitmap == null) {
                    Text(
                        text = stringResource(R.string.group_overview_receipt_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.group_overview_receipt_title),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                            .padding(top = 12.dp),
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.action_close))
                }
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
                transactions = listOf(
                    GroupTransaction(
                        expense = Expense(
                            id = "e2", groupId = "g1", description = "Pizza night", category = "Food",
                            amountOre = 45_000, paidBy = "u2", createdBy = "u2", createdAt = 2,
                        ),
                        payerName = "Anna Test",
                        payeeName = "",
                    ),
                    GroupTransaction(
                        expense = Expense(
                            id = "e1", groupId = "g1", description = "Groceries", category = "Food",
                            amountOre = 30_000, paidBy = "u1", createdBy = "u1", createdAt = 1,
                        ),
                        payerName = "Tim Test",
                        payeeName = "",
                    ),
                    GroupTransaction(
                        expense = Expense(
                            id = "s1", groupId = "g1", type = ExpenseType.SETTLEMENT,
                            amountOre = 3_333, paidBy = "u3", createdBy = "u3", createdAt = 0,
                            splitAmong = listOf("u1"),
                        ),
                        payerName = "Jonas Test",
                        payeeName = "Tim Test",
                    ),
                ),
            ),
            onEditGroup = {},
            onManageMembers = {},
            onAddExpense = {},
            onEditExpense = {},
            onDeleteExpense = {},
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
            onEditExpense = {},
            onDeleteExpense = {},
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
            onEditExpense = {},
            onDeleteExpense = {},
            onRemind = {},
        )
    }
}
