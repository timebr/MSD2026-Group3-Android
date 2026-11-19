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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = OnIndigo80,
    primaryContainer = IndigoContainer80,
    onPrimaryContainer = OnIndigoContainer80,
    secondary = Lilac80,
    onSecondary = OnLilac80,
    secondaryContainer = LilacContainer80,
    onSecondaryContainer = OnLilacContainer80,
    tertiary = Amber80,
    onTertiary = OnAmber80,
    tertiaryContainer = AmberContainer80,
    onTertiaryContainer = OnAmberContainer80,
    background = Background80,
    onBackground = OnBackground80,
    surface = Surface80,
    onSurface = OnSurface80,
    surfaceVariant = SurfaceVariant80,
    onSurfaceVariant = OnSurfaceVariant80,
    surfaceContainerLowest = SurfaceContainerLowest80,
    surfaceContainerLow = SurfaceContainerLow80,
    surfaceContainer = SurfaceContainer80,
    surfaceContainerHigh = SurfaceContainerHigh80,
    surfaceContainerHighest = SurfaceContainerHighest80,
    outline = Outline80,
    outlineVariant = OutlineVariant80,
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = OnIndigo40,
    primaryContainer = IndigoContainer40,
    onPrimaryContainer = OnIndigoContainer40,
    secondary = Lilac40,
    onSecondary = OnLilac40,
    secondaryContainer = LilacContainer40,
    onSecondaryContainer = OnLilacContainer40,
    tertiary = Amber40,
    onTertiary = OnAmber40,
    tertiaryContainer = AmberContainer40,
    onTertiaryContainer = OnAmberContainer40,
    background = Background40,
    onBackground = OnBackground40,
    surface = Surface40,
    onSurface = OnSurface40,
    surfaceVariant = SurfaceVariant40,
    onSurfaceVariant = OnSurfaceVariant40,
    surfaceContainerLowest = SurfaceContainerLowest40,
    surfaceContainerLow = SurfaceContainerLow40,
    surfaceContainer = SurfaceContainer40,
    surfaceContainerHigh = SurfaceContainerHigh40,
    surfaceContainerHighest = SurfaceContainerHighest40,
    outline = Outline40,
    outlineVariant = OutlineVariant40,
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