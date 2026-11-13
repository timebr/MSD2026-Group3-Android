package com.example.oweowe.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * Avatar (FR1, FR2): the profile photo when one is saved, otherwise a circle
 * with the member's initials, tinted by a hash of the name so members are
 * distinguishable without photos.
 *
 * [photoPath] is a file path in app-private storage today (nothing is uploaded
 * yet); it falls back to initials when it is null or the file cannot be read.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    photoPath: String? = null,
) {
    val photo = rememberImageBitmap(photoPath?.let { Uri.fromFile(File(it)) })
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
            .background(palette[index], CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = initials,
                // Chips use a small avatar, so the initials shrink with it
                // instead of overflowing the circle.
                style = if (size >= 40) {
                    MaterialTheme.typography.labelLarge
                } else {
                    MaterialTheme.typography.labelSmall
                },
                color = onPalette[index],
                textAlign = TextAlign.Center,
            )
        }
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
