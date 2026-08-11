package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BitwardenBlueLight,
    onPrimary = Color.White,
    primaryContainer = BitwardenBlueDark,
    onPrimaryContainer = Color.White,
    secondary = BitwardenBlueLight,
    onSecondary = Color.White,
    background = BitwardenDarkBackground,
    onBackground = Color(0xFFE2E2E2),
    surface = BitwardenDarkSurface,
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = BitwardenDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C4C4),
    outline = BitwardenDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = BitwardenBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = BitwardenBlueDark,
    secondary = BitwardenBlue,
    onSecondary = Color.White,
    background = BitwardenLightBackground,
    onBackground = Color(0xFF1C1C1E),
    surface = BitwardenLightSurface,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = BitwardenLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    outline = BitwardenLightOutline
)

@Composable
fun LsPassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

