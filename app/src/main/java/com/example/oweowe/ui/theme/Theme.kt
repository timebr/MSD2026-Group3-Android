package com.example.oweowe.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Color(0xFF1A208F),
    primaryContainer = IndigoContainer80,
    onPrimaryContainer = OnIndigoContainer80,
    secondary = IndigoGrey80,
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = IndigoGreyContainer80,
    onSecondaryContainer = Color(0xFFE1E0F9),
    tertiary = Teal80,
    onTertiary = Color(0xFF00315E),
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Color.White,
    primaryContainer = IndigoContainer40,
    onPrimaryContainer = OnIndigoContainer40,
    secondary = IndigoGrey40,
    onSecondary = Color.White,
    secondaryContainer = IndigoGreyContainer40,
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Teal40,
    onTertiary = Color.White,
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
)

@Composable
fun OweOweTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Use the app's brand colors so the UI matches the logo.
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
        content = content,
    )
}