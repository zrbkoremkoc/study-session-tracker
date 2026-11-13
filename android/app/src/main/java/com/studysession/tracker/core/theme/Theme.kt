package com.studysession.tracker.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudyColors = darkColorScheme(
    primary = Accent, onPrimary = Background, primaryContainer = Primary, onPrimaryContainer = OnSurface,
    secondary = Primary, onSecondary = OnSurface, secondaryContainer = Secondary, onSecondaryContainer = OnSurface,
    tertiary = Accent, background = Background, onBackground = OnSurface, surface = Background, onSurface = OnSurface,
    surfaceVariant = Secondary, onSurfaceVariant = OnSurface.copy(alpha = 0.76f),
    outline = OnSurface.copy(alpha = 0.2f), outlineVariant = OnSurface.copy(alpha = 0.1f),
    error = OnSurface, onError = Background, errorContainer = Secondary, onErrorContainer = OnSurface,
    onTertiary = Background, tertiaryContainer = Secondary, onTertiaryContainer = OnSurface,
    inverseSurface = OnSurface, inverseOnSurface = Background, inversePrimary = Primary, surfaceTint = Accent,
    surfaceDim = Background, surfaceBright = Secondary, surfaceContainerLowest = Background,
    surfaceContainerLow = Secondary, surfaceContainer = Secondary, surfaceContainerHigh = Secondary,
    surfaceContainerHighest = Secondary, scrim = Background
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StudyColors, typography = AppTypography, content = content)
}
