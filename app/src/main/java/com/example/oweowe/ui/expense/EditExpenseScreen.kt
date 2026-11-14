package com.example.oweowe.ui.expense

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.model.User
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.canTakePhoto
import com.example.oweowe.ui.components.copyReceiptToAppStorage
import com.example.oweowe.ui.components.displayName
import com.example.oweowe.ui.components.rememberCameraCapture
import com.example.oweowe.ui.components.rememberImagePicker
import java.io.File
import kotlinx.coroutines.launch

/**
 * S11 Edit expense (FR4). The exact same form as [AddExpenseScreen], prefilled
 * with the stored expense. Any group member may save the changes.
 */
@Composable
fun EditExpenseScreen(
    container: AppContainer,
    groupId: String,
    expenseId: String,
    onBack: () -> Unit,
) {
    val viewModel: AddExpenseViewModel = viewModel(
        key = "edit-expense-$expenseId",
        factory = AddExpenseViewModel.factory(container, groupId, expenseId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var receiptUri by rememberSaveable { mutableStateOf<String?>(null) }
    var receiptName by rememberSaveable { mutableStateOf<String?>(null) }
    // FR10: only a newly picked receipt is copied; an unchanged one is kept as is.
    var receiptChanged by rememberSaveable { mutableStateOf(false) }
    var receiptInitialized by rememberSaveable { mutableStateOf(false) }
    var savingReceipt by remember { mutableStateOf(false) }

    // The stored receipt is prefilled once, after the expense has loaded.
    LaunchedEffect(state.loading) {
        if (!state.loading && !receiptInitialized) {
            receiptUri = state.receiptUri
            receiptName = state.receiptUri?.let { File(it).name }
            receiptInitialized = true
        }
    }

    val pickReceipt = rememberImagePicker { picked ->
        receiptUri = picked?.toString()
        receiptName = picked?.let { displayName(context, it) }
        receiptChanged = true
    }
    // FR10: taking a photo is the same receipt, just captured with the camera.
    val cameraPhotoName = stringResource(R.string.add_expense_receipt_camera_name)
    val takePhoto = rememberCameraCapture { captured ->
        if (captured != null) {   // null = cancelled: keep whatever was picked before
            receiptUri = captured.toString()
            receiptName = cameraPhotoName
            receiptChanged = true
        }
    }
    val cameraAvailable = remember(context) { canTakePhoto(context) }

    LaunchedEffect(state.writeState) {
        if (state.writeState == WriteState.Done) {
            viewModel.resetWriteState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.edit_expense_title),
                showBack = true,
                onBack = onBack,
            )
        },
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
            onAddReceipt = pickReceipt,
            onTakePhoto = takePhoto,
            cameraAvailable = cameraAvailable,
            receiptUri = receiptUri,
            receiptName = receiptName,
            onClearReceipt = {
                receiptUri = null
                receiptName = null
                receiptChanged = true
            },
            onSubmit = {
                val picked = receiptUri
                when {
                    savingReceipt -> Unit
                    // Nothing picked: keep or clear the receipt that is already stored.
                    !receiptChanged -> viewModel.submit(receiptUri = picked)
                    picked == null -> viewModel.submit(receiptUri = null)
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
            submitLabel = stringResource(R.string.edit_expense_submit),
            modifier = Modifier.padding(padding),
        )
    }
}

@Preview(showBackground = true, name = "Edit expense")
@Composable
private fun EditExpensePreview() {
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
                category = "Food",
                amountText = "300,00",
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
            submitLabel = "Save changes",
        )
    }
}

@Preview(showBackground = true, name = "Edit expense not found")
@Composable
private fun EditExpenseNotFoundPreview() {
    MaterialTheme {
        AddExpenseContent(
            state = AddExpenseUiState(loading = false, expenseNotFound = true),
            onDescriptionChange = {},
            onCategoryChange = {},
            onAmountChange = {},
            onGroupSelected = {},
            onPayerSelected = {},
            onToggleSplitMember = {},
            onSelectAllMembers = {},
            onAddReceipt = {},
            onSubmit = {},
            submitLabel = "Save changes",
        )
    }
}
