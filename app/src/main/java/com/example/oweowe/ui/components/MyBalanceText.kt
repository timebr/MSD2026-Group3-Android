package com.example.oweowe.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.example.oweowe.R
import com.example.oweowe.domain.formatKr

/**
 * The signed-in user's own balance line, shared by S3 Home and S5 Activity
 * (FR6). Colour plus wording show the direction, not colour alone.
 */
@Composable
fun MyBalanceText(balanceOre: Long, modifier: Modifier = Modifier) {
    val label = when {
        balanceOre > 0 -> stringResource(R.string.balance_you_get, formatKr(balanceOre))
        balanceOre < 0 -> stringResource(R.string.balance_you_owe, formatKr(-balanceOre))
        else -> stringResource(R.string.balance_even)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        color = if (balanceOre >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = modifier,
    )
}

@Preview(showBackground = true, name = "My balance")
@Composable
private fun MyBalanceTextPreview() {
    MaterialTheme {
        Column {
            MyBalanceText(balanceOre = 1_667, modifier = Modifier.padding(4.dp))
            MyBalanceText(balanceOre = 0, modifier = Modifier.padding(4.dp))
            MyBalanceText(balanceOre = -3_333, modifier = Modifier.padding(4.dp))
        }
    }
}
