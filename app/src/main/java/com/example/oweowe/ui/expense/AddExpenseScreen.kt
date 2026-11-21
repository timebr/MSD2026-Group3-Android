package com.example.oweowe.ui.expense

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.example.oweowe.ui.components.AppBottomSheet
import com.example.oweowe.ui.components.EmptyState
import com.example.oweowe.ui.components.ErrorState
import com.example.oweowe.ui.components.InitialsAvatar
import com.example.oweowe.ui.components.LoadingState
import com.example.oweowe.ui.components.OfflineBanner
import com.example.oweowe.ui.components.SheetHeader
import com.example.oweowe.ui.components.SubmitButton
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.canTakePhoto
import com.example.oweowe.ui.components.copyReceiptToAppStorage
import com.example.oweowe.ui.components.displayName
import com.example.oweowe.ui.components.errorReasonMessage
import com.example.oweowe.ui.components.rememberCameraCapture
import com.example.oweowe.ui.components.rememberImageBitmap
import com.example.oweowe.ui.components.rememberImagePicker
import kotlinx.coroutines.launch
import com.example.oweowe.ui.components.appTextFieldColors

/**
 * S11 Add expense (FR4) as a bottom sheet. FR10: a picked receipt is copied into
 * app storage and kept with the expense.
 */
@Composable
fun AddExpenseSheet(
    container: AppContainer,
    initialGroupId: String?,
    onDismiss: () -> Unit,
) {
    val viewModel: AddExpenseViewModel = viewModel(
        key = "add-expense-$initialGroupId",
        factory = AddExpenseViewModel.factory(container, initialGroupId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var receiptUri by rememberSaveable { mutableStateOf<String?>(null) }
    var receiptName by rememberSaveable { mutableStateOf<String?>(null) }
    var savingReceipt by remember { mutableStateOf(false) }
    val pickReceipt = rememberImagePicker { picked ->
        receiptUri = picked?.toString()
        receiptName = picked?.let { displayName(context, it) }
    }
    val cameraPhotoName = stringResource(R.string.add_expense_receipt_camera_name)
    val takePhoto = rememberCameraCapture { captured ->
        if (captured != null) {
            receiptUri = captured.toString()
            receiptName = cameraPhotoName
        }
    }
    val cameraAvailable = remember(context) { canTakePhoto(context) }
    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onDismiss()
        }
    }

    AppBottomSheet(onDismiss = onDismiss) {
        SheetHeader(stringResource(R.string.add_expense_title))
        AddExpenseContent(
            state = state,
            onDescriptionChange = viewModel::updateDescription,
            onCategoryChange = viewModel::updateCategory,
            onAmountChange = viewModel::updateAmount,
            onGroupSelected = viewModel::selectGroup,
            onPayerSelected = viewModel::selectPayer,
            onToggleSplitMember = viewModel::toggleSplitMember,
            onSelectAllMembers = viewModel::selectAllMembers,
            onAddReceipt = pickReceipt,
            onTakePhoto = takePhoto,
            cameraAvailable = cameraAvailable,
            receiptUri = receiptUri,
            receiptName = receiptName,
            onClearReceipt = {
                receiptUri = null
                receiptName = null
            },
            onSubmit = {
                val picked = receiptUri
                when {
                    savingReceipt -> Unit
                    picked == null -> viewModel.submit()
                    else -> {
                        savingReceipt = true
                        scope.launch {
                            val path = copyReceiptToAppStorage(context, Uri.parse(picked))
                            savingReceipt = false
                            viewModel.submit(receiptUri = path)
                        }
                    }
                }
            },
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
    receiptUri: String? = null,
    receiptName: String? = null,
    onClearReceipt: () -> Unit = {},
    /** FR10: open a camera app for the receipt photo. */
    onTakePhoto: () -> Unit = {},
    /** False when no camera app is installed, so the button is hidden. */
    cameraAvailable: Boolean = true,
    /** Edit screens override the submit label; adding uses "Split expense". */
    submitLabel: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (state.offline) OfflineBanner()
        when {
            state.loading -> LoadingState()
            state.loadError -> ErrorState(message = stringResource(R.string.common_error_generic))
            state.expenseNotFound -> ErrorState(
                message = stringResource(R.string.edit_expense_not_found),
            )
            state.groups.isEmpty() -> EmptyState(
                title = stringResource(R.string.add_expense_no_group_title),
                message = stringResource(R.string.add_expense_no_group_message),
            )
            else -> Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
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
                    isError = state.descriptionInvalid,
                    supportingText = if (state.descriptionInvalid) {
                        { Text(stringResource(R.string.add_expense_description_invalid)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = appTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = state.category,
                    onValueChange = onCategoryChange,
                    label = { Text(stringResource(R.string.add_expense_category_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = appTextFieldColors(),
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
                    shape = MaterialTheme.shapes.medium,
                    colors = appTextFieldColors(),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    FormLabel(
                        stringResource(R.string.add_expense_split_with_label),

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
                            // FR4: "Split with (avatars)" — the member's initials
                            // next to the name, so the split reads as people.
                            leadingIcon = {
                                InitialsAvatar(name = member.fullName, size = 20)
                            },
                        )
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedButton(onClick = onAddReceipt, shape = CircleShape) {
                        Text(stringResource(R.string.add_expense_receipt_label))
                    }
                    if (cameraAvailable) {
                        OutlinedButton(
                            onClick = onTakePhoto,
                            shape = CircleShape,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(stringResource(R.string.add_expense_take_photo))
                        }
                    }
                }
                val receipt = rememberImageBitmap(receiptUri?.let(Uri::parse))
                if (receipt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Image(
                            bitmap = receipt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Text(
                            text = receiptName.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        IconButton(onClick = onClearReceipt) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_clear_receipt),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.add_expense_receipt_saved_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.writeState is WriteState.Failed) {
                    Text(
                        text = errorReasonMessage(state.writeState.reason),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
                SubmitButton(
                    state = state.writeState,
                    label = submitLabel ?: stringResource(R.string.add_expense_submit),
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

@Preview(showBackground = true, name = "Add expense load error")
@Composable
private fun AddExpenseLoadErrorPreview() {
    MaterialTheme {
        AddExpenseContent(
            state = AddExpenseUiState(loading = false, loadError = true),
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
