package com.example.oweowe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Default avatar (FR1): a circle with the member's initials, tinted by a hash
 * of the name so members are distinguishable without photos.
 */
@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier, size: Int = 40) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
    )
    val onPalette = listOf(
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val index = (name.hashCode().let { if (it < 0) -it else it }) % palette.size

    Box(
        modifier = modifier
            .size(size.dp)
            .background(palette[index], CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            color = onPalette[index],
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, name = "Initials avatars")
@Composable
private fun InitialsAvatarPreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Row {
            InitialsAvatar("Tim Test")
            InitialsAvatar("Anna")
            InitialsAvatar("")
        }
    }
}
