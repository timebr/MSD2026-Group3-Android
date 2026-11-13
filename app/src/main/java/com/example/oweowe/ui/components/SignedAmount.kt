package com.example.oweowe.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.example.oweowe.domain.formatKr

/**
 * An amount in øre formatted for the UI (money is never a Double). Colour plus
 * the sign show the direction, so the state never relies on colour alone.
 */
@Composable
fun SignedAmount(
    ore: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    Text(
        text = formatKr(ore),
        style = style,
        color = when {
            ore > 0 -> MaterialTheme.colorScheme.primary
            ore < 0 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
    )
}

@Preview(showBackground = true, name = "Signed amounts")
@Composable
private fun SignedAmountPreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Column {
            SignedAmount(ore = 16_670)
            SignedAmount(ore = 0)
            SignedAmount(ore = -3_333)
        }
    }
}
