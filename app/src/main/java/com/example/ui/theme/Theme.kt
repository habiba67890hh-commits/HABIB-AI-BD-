package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = EmeraldOnSecondaryContainer,
    tertiary = EmeraldTertiary,
    onTertiary = EmeraldOnTertiary,
    background = DarkGreenBackground,
    onBackground = DarkGreenOnSurface,
    surface = DarkGreenSurface,
    onSurface = DarkGreenOnSurface,
    surfaceVariant = DarkGreenSurfaceVariant,
    onSurfaceVariant = DarkGreenOnSurfaceVariant,
    surfaceContainer = DarkGreenSurfaceContainer,
    surfaceContainerHigh = DarkGreenSurfaceContainerHigh,
    outline = DarkGreenOutline,
    outlineVariant = DarkGreenOutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnErrorRedContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LightEmeraldPrimary,
    onPrimary = LightEmeraldOnPrimary,
    primaryContainer = LightEmeraldPrimaryContainer,
    onPrimaryContainer = LightEmeraldOnPrimaryContainer,
    secondary = LightEmeraldSecondary,
    onSecondary = LightEmeraldOnSecondary,
    background = LightGreenBackground,
    onBackground = LightGreenOnSurface,
    surface = LightGreenSurface,
    onSurface = LightGreenOnSurface,
    surfaceVariant = LightGreenSurfaceVariant,
    onSurfaceVariant = LightGreenOnSurfaceVariant,
    outline = LightGreenOutline,
    error = ErrorRed
)

@Composable
fun HabibAITheme(
    darkTheme: Boolean = true, // Default to premium dark-green experience
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
