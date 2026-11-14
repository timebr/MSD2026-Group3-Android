package com.example.oweowe.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.oweowe.R
import com.example.oweowe.data.model.Expense
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.SyncStatus

/**
 * One transaction row, shared by S5 Activity and the S4 group overview so the
 * two lists cannot drift apart (FR5). Activity passes the group name; the group
 * overview leaves it null, because there every row belongs to that group.
 *
 * When [onClick] is set the row is tappable (FR10 receipts).
 * [onEdit] and [onDelete] add the actions used by the group overview.
 */
@Composable
fun TransactionRow(
    expense: Expense,
    payerName: String,
    modifier: Modifier = Modifier,
    groupName: String? = null,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = transactionTitle(expense),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (groupName == null) {
                    stringResource(R.string.transaction_paid_by, payerName)
                } else {
                    stringResource(R.string.activity_transaction_subtitle, payerName, groupName)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onClick != null && expense.receiptUri != null) {
                Text(
                    text = stringResource(R.string.transaction_view_receipt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (expense.syncStatus != SyncStatus.SYNCED) {
                SyncStatusChip(
                    status = expense.syncStatus,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        SignedAmount(ore = expense.amountOre)
        if (onEdit != null) {
            // Compact 36 dp buttons keep the two actions tight next to the amount.
            IconButton(onClick = onEdit, modifier = Modifier.requiredSize(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.cd_edit_expense),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.requiredSize(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_delete_expense),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun transactionTitle(expense: Expense): String =
    when {
        expense.type == ExpenseType.SETTLEMENT -> stringResource(R.string.activity_settlement)
        expense.description.isNotBlank() -> expense.description
        expense.category.isNotBlank() -> expense.category
        else -> stringResource(R.string.activity_expense_fallback)
    }
