package com.example.oweowe.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason

/**
 * Standard submit button for form writes: busy shows a spinner and blocks
 * double-submits, failed shows Retry (NFR2, NFR4).
 */
@Composable
fun SubmitButton(
    state: WriteState,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(onClick = onClick, enabled = state != WriteState.Busy, modifier = modifier) {
        if (state == WriteState.Busy) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .height(20.dp)
                    .widthIn(min = 20.dp),
            )
        } else if (state is WriteState.Failed) {
            Text(stringResource(R.string.action_retry))
        } else {
            Text(label)
        }
    }
}

@Preview(showBackground = true, name = "Submit button states")
@Composable
private fun SubmitButtonPreview() {
    androidx.compose.foundation.layout.Column {
        SubmitButton(state = WriteState.Idle, label = "Split expense", onClick = {})
        SubmitButton(state = WriteState.Busy, label = "Split expense", onClick = {})
        SubmitButton(state = WriteState.Failed(ErrorReason.NETWORK), label = "Split expense", onClick = {})
    }
}
