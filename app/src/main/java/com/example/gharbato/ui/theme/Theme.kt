package com.example.gharbato.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GharBatoPalette.brandLight,
    onPrimary = Color(0xFF111A36),
    primaryContainer = GharBatoPalette.brandContainerDark,
    onPrimaryContainer = GharBatoPalette.textDark,
    secondary = GharBatoPalette.brandLight,
    onSecondary = GharBatoPalette.backgroundDark,
    secondaryContainer = GharBatoPalette.brandContainerDark,
    onSecondaryContainer = GharBatoPalette.textDark,
    tertiary = GharBatoPalette.brandLight,
    onTertiary = GharBatoPalette.backgroundDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = GharBatoPalette.backgroundDark,
    surface = GharBatoPalette.surfaceDark,
    surfaceVariant = GharBatoPalette.surfaceVariantDark,
    onBackground = GharBatoPalette.textDark,
    onSurface = GharBatoPalette.textDark,
    onSurfaceVariant = GharBatoPalette.mutedTextDark,
    outline = GharBatoPalette.mutedTextDark,
    outlineVariant = GharBatoPalette.outlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = GharBatoPalette.brand,
    onPrimary = Color.White,
    primaryContainer = GharBatoPalette.brandContainerLight,
    onPrimaryContainer = Color(0xFF202947),
    secondary = GharBatoPalette.brand,
    onSecondary = Color.White,
    secondaryContainer = GharBatoPalette.brandContainerLight,
    onSecondaryContainer = Color(0xFF202947),
    tertiary = GharBatoPalette.brand,
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = GharBatoPalette.backgroundLight,
    surface = GharBatoPalette.surfaceLight,
    surfaceVariant = GharBatoPalette.surfaceVariantLight,
    onBackground = GharBatoPalette.textLight,
    onSurface = GharBatoPalette.textLight,
    onSurfaceVariant = GharBatoPalette.mutedTextLight,
    outline = GharBatoPalette.mutedTextLight,
    outlineVariant = GharBatoPalette.outlineLight
)

@Composable
fun GharBatoTheme(
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
