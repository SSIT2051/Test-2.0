package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Strict 3-color scheme: Pitch Black, Crisp White, Pumpkin Orange
private val DarkColorScheme = darkColorScheme(
    primary = PumpkinOrange,
    onPrimary = Color.Black,
    primaryContainer = ObsidianSurfaceElevated,
    onPrimaryContainer = PumpkinOrange,
    secondary = TextPrimary,
    onSecondary = ObsidianDark,
    secondaryContainer = ObsidianSurfaceBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = PumpkinOrangeLight,
    onTertiary = Color.Black,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianSurfaceBorder,
    outlineVariant = ObsidianSurfaceBorder,
    error = PumpkinOrange,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
