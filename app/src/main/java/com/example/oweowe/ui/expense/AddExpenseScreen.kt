package com.example.oweowe.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.launch

/** S11 Add expense (FR4, FR10 placeholder). */
@Composable
fun AddExpenseScreen(
    container: AppContainer,
    initialGroupId: String?,
    onBack: () -> Unit,
) {
    val viewModel: AddExpenseViewModel = viewModel(
        key = "add-expense-$initialGroupId",
        factory = AddExpenseViewModel.factory(container, initialGroupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here, not in the click handler: LocalContext reads don't survive
    // configuration changes.
    val receiptPlaceholder = stringResource(R.string.add_expense_receipt_placeholder)
    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.add_expense_title),
                showBack = true,
                onBack = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AddExpenseContent(
            state = state,
            onDescriptionChange = viewModel::updateDescription,
            onCategoryChange = viewModel::updateCategory,
            onAmountChange = viewModel::updateAmount,
            onGroupSelected = viewModel::selectGroup,
            onPayerSelected = viewModel::selectPayer,
            onToggleSplitMember = viewModel::toggleSplitMember,
            onSelectAllMembers = viewModel::selectAllMembers,
            onAddReceipt = {
                // FR10 is a could-have; the backend has no receipt storage yet.
                scope.launch {
                    snackbarHostState.showSnackbar(receiptPlaceholder)
                }
            },
            onSubmit = viewModel::submit,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    state: AddExpenseUiState,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onGroupSelected: (String) -> Unit,
    onPayerSelected: (String) -> Unit,
    onToggleSplitMember: (String) -> Unit,
    onSelectAllMembers: () -> Unit,
    onAddReceipt: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.groups.isEmpty() -> EmptyState(
                title = stringResource(R.string.add_expense_no_group_title),
                message = stringResource(R.string.add_expense_no_group_message),
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                if (state.groups.size > 1) {
                    FormLabel(stringResource(R.string.add_expense_group_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.groups.forEach { group ->
                            FilterChip(
                                selected = group.id == state.selectedGroupId,
                                onClick = { onGroupSelected(group.id) },
                                label = { Text(group.name) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.add_expense_description_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = state.category,
                    onValueChange = onCategoryChange,
                    label = { Text(stringResource(R.string.add_expense_category_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.add_expense_amount_label)) },
                    isError = state.amountInvalid,
                    supportingText = if (state.amountInvalid) {
                        { Text(stringResource(R.string.add_expense_amount_invalid)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                FormLabel(stringResource(R.string.add_expense_paid_by_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.members.forEach { member ->
                        FilterChip(
                            selected = member.uid == state.payerId,
                            onClick = { onPayerSelected(member.uid) },
                            label = { Text(member.fullName) },
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    FormLabel(
                        stringResource(R.string.add_expense_split_with_label),
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = state.splitAmong.size == state.members.size,
                        onClick = onSelectAllMembers,
                        label = { Text(stringResource(R.string.add_expense_select_all)) },
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.members.forEach { member ->
                        FilterChip(
                            selected = member.uid in state.splitAmong,
                            onClick = { onToggleSplitMember(member.uid) },
                            label = { Text(member.fullName) },
                        )
                    }
                }
                OutlinedButton(onClick = onAddReceipt, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.add_expense_receipt_label))
                }
                SubmitButton(
                    state = state.writeState,
                    label = stringResource(R.string.add_expense_submit),
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Preview(showBackground = true, name = "Add expense")
@Composable
private fun AddExpensePreview() {
    val me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk")
    MaterialTheme {
        AddExpenseContent(
            state = AddExpenseUiState(
                loading = false,
                me = me,
                groups = listOf(Group(id = "g1", name = "Flat 4B", memberIds = listOf("u1", "u2", "u3"))),
                selectedGroupId = "g1",
                members = listOf(
                    me,
                    User(uid = "u2", fullName = "Anna Test", email = "anna@test.dk"),
                    User(uid = "u3", fullName = "Jonas Test", email = "jonas@test.dk"),
                ),
                description = "Groceries",
                amountText = "300",
                payerId = "u1",
                splitAmong = setOf("u1", "u2", "u3"),
            ),
            onDescriptionChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onGroupSelected = {},
            onPayerSelected = {},
            onToggleSplitMember = {},
            onSelectAllMembers = {},
            onAddReceipt = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, name = "Add expense invalid amount")
@Composable
private fun AddExpenseInvalidPreview() {
    MaterialTheme {
        AddExpenseContent(
            state = AddExpenseUiState(
                loading = false,
                me = User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk"),
                groups = listOf(Group(id = "g1", name = "Flat 4B", memberIds = listOf("u1"))),
                selectedGroupId = "g1",
                members = listOf(User(uid = "u1", fullName = "Tim Test", email = "tim@test.dk")),
                amountText = "abc",
                amountInvalid = true,
                payerId = "u1",
                splitAmong = setOf("u1"),
            ),
            onDescriptionChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onGroupSelected = {},
            onPayerSelected = {},
            onToggleSplitMember = {},
            onSelectAllMembers = {},
            onAddReceipt = {},
            onSubmit = {},
        )
    }
}

@Preview(showBackground = true, name = "Add expense no groups")
@Composable
private fun AddExpenseNoGroupsPreview() {
    MaterialTheme {
        AddExpenseContent(
            state = AddExpenseUiState(loading = false),
            onDescriptionChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onGroupSelected = {},
            onPayerSelected = {},
            onToggleSplitMember = {},
            onSelectAllMembers = {},
            onAddReceipt = {},
            onSubmit = {},
        )
    }
}
