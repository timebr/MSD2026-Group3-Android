package com.example.oweowe.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.oweowe.R
import com.example.oweowe.data.model.SyncStatus

/**
 * Pending / synced / failed marker next to a row that was written locally and
 * is on its way to the backend (or failed to get there). NFR4.
 */
@Composable
fun SyncStatusChip(status: SyncStatus, modifier: Modifier = Modifier) {
    val label = when (status) {
        SyncStatus.PENDING -> stringResource(R.string.sync_pending)
        SyncStatus.SYNCED -> stringResource(R.string.sync_synced)
        SyncStatus.FAILED -> stringResource(R.string.sync_failed)
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when (status) {
            SyncStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
            SyncStatus.SYNCED -> MaterialTheme.colorScheme.surfaceVariant
            SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        },
        contentColor = when (status) {
            SyncStatus.PENDING -> MaterialTheme.colorScheme.onSecondaryContainer
            SyncStatus.SYNCED -> MaterialTheme.colorScheme.onSurfaceVariant
            SyncStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        },
        modifier = modifier.semantics { contentDescription = "" },
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            when (status) {
                SyncStatus.PENDING -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(12.dp),
                )
                SyncStatus.SYNCED -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
                SyncStatus.FAILED -> Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "Sync status chips")
@Composable
private fun SyncStatusChipPreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Column {
            SyncStatusChip(SyncStatus.PENDING)
            SyncStatusChip(SyncStatus.SYNCED)
            SyncStatusChip(SyncStatus.FAILED)
        }
    }
}
