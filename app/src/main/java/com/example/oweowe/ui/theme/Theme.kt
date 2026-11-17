package com.example.oweowe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF00315F),
    primaryContainer = BlueContainer80,
    onPrimaryContainer = OnBlueContainer80,
    secondary = WarmBrown80,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = WarmBrownContainer80,
    onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = Sand80,
    onTertiary = Color(0xFF3E2D16),
    background = Color(0xFF1A1712),
    onBackground = Color(0xFFEDE1D1),
    surface = Color(0xFF1A1712),
    onSurface = Color(0xFFEDE1D1),
    surfaceVariant = Color(0xFF4A4034),
    onSurfaceVariant = Color(0xFFD6C4AD),
    surfaceContainerLowest = Color(0xFF14110D),
    surfaceContainerLow = Color(0xFF221E18),
    surfaceContainer = Color(0xFF26221C),
    surfaceContainerHigh = Color(0xFF312C25),
    surfaceContainerHighest = Color(0xFF3C372F),
    outline = Color(0xFF9F8E79),
    outlineVariant = Color(0xFF4A4034),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = BlueContainer40,
    onPrimaryContainer = OnBlueContainer40,
    secondary = WarmBrown40,
    onSecondary = Color.White,
    secondaryContainer = WarmBrownContainer40,
    onSecondaryContainer = Color(0xFF2F1500),
    tertiary = Sand40,
    onTertiary = Color.White,
    background = Color(0xFFF5EBD7),
    onBackground = Color(0xFF2E2418),
    surface = Color(0xFFF5EBD7),
    onSurface = Color(0xFF2E2418),
    surfaceVariant = Color(0xFFEADFC7),
    onSurfaceVariant = Color(0xFF5C4A35),
    surfaceContainerLowest = Color(0xFFFFFBF2),
    surfaceContainerLow = Color(0xFFFAF1DF),
    surfaceContainer = Color(0xFFF1E5CE),
    surfaceContainerHigh = Color(0xFFE8DBC2),
    surfaceContainerHighest = Color(0xFFE0D1B6),
    outline = Color(0xFF8A775C),
    outlineVariant = Color(0xFFD7C7AB),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun OweOweTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use the app's warm brand colors for a consistent look.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}