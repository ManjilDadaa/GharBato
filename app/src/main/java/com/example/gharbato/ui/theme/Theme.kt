package com.example.gharbato.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Action fills stay distinct from the lighter accent used for readable text in dark mode. */
@Immutable
data class GharBatoActionColors(
    val container: Color,
    val onContainer: Color,
    val outline: Color
)

private val LightActionColors = GharBatoActionColors(
    container = GharBatoPalette.brand,
    onContainer = Color.White,
    outline = GharBatoPalette.actionOutlineLight
)

private val DarkActionColors = GharBatoActionColors(
    container = GharBatoPalette.actionDark,
    onContainer = GharBatoPalette.onActionDark,
    outline = GharBatoPalette.actionOutlineDark
)

val LocalGharBatoActionColors = staticCompositionLocalOf { LightActionColors }

/** Shared CTA styling; use colorScheme.primary for text links and small accent icons. */
object GharBatoButtonDefaults {
    @Composable
    fun primaryColors(): ButtonColors {
        val actions = LocalGharBatoActionColors.current
        return ButtonDefaults.buttonColors(
            containerColor = actions.container,
            contentColor = actions.onContainer,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }

    @Composable
    fun outlinedColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )

    @Composable
    fun outlinedBorder(enabled: Boolean = true): BorderStroke = BorderStroke(
        1.dp,
        if (enabled) LocalGharBatoActionColors.current.outline
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = GharBatoPalette.brandLight,
    onPrimary = GharBatoPalette.brandDeep,
    primaryContainer = GharBatoPalette.brandContainerDark,
    onPrimaryContainer = GharBatoPalette.textDark,
    secondary = GharBatoPalette.brandLight,
    onSecondary = GharBatoPalette.backgroundDark,
    secondaryContainer = GharBatoPalette.brandContainerDark,
    onSecondaryContainer = GharBatoPalette.textDark,
    tertiary = GharBatoPalette.brandLight,
    onTertiary = GharBatoPalette.backgroundDark,
    tertiaryContainer = GharBatoPalette.brandContainerDark,
    onTertiaryContainer = GharBatoPalette.textDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = GharBatoPalette.backgroundDark,
    surface = GharBatoPalette.surfaceDark,
    surfaceVariant = GharBatoPalette.surfaceVariantDark,
    surfaceDim = GharBatoPalette.backgroundDark,
    surfaceBright = Color(0xFF3B3343),
    surfaceContainerLowest = Color(0xFF110E14),
    surfaceContainerLow = Color(0xFF1B1720),
    surfaceContainer = GharBatoPalette.surfaceDark,
    surfaceContainerHigh = GharBatoPalette.surfaceVariantDark,
    surfaceContainerHighest = Color(0xFF362E3D),
    onBackground = GharBatoPalette.textDark,
    onSurface = GharBatoPalette.textDark,
    onSurfaceVariant = GharBatoPalette.mutedTextDark,
    outline = GharBatoPalette.mutedTextDark,
    outlineVariant = GharBatoPalette.outlineDark,
    surfaceTint = GharBatoPalette.brandLight,
    inverseSurface = GharBatoPalette.surfaceLight,
    inverseOnSurface = GharBatoPalette.textLight,
    inversePrimary = GharBatoPalette.brand
)

private val LightColorScheme = lightColorScheme(
    primary = GharBatoPalette.brand,
    onPrimary = Color.White,
    primaryContainer = GharBatoPalette.brandContainerLight,
    onPrimaryContainer = Color(0xFF1B1028),
    secondary = GharBatoPalette.brand,
    onSecondary = Color.White,
    secondaryContainer = GharBatoPalette.brandContainerLight,
    onSecondaryContainer = Color(0xFF1B1028),
    tertiary = GharBatoPalette.brand,
    onTertiary = Color.White,
    tertiaryContainer = GharBatoPalette.brandContainerLight,
    onTertiaryContainer = GharBatoPalette.brandDeep,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = GharBatoPalette.backgroundLight,
    surface = GharBatoPalette.surfaceLight,
    surfaceVariant = GharBatoPalette.surfaceVariantLight,
    surfaceDim = Color(0xFFE8E2EC),
    surfaceBright = GharBatoPalette.surfaceLight,
    surfaceContainerLowest = GharBatoPalette.surfaceLight,
    surfaceContainerLow = Color(0xFFFAF8FC),
    surfaceContainer = GharBatoPalette.backgroundLight,
    surfaceContainerHigh = GharBatoPalette.surfaceVariantLight,
    surfaceContainerHighest = Color(0xFFE9E4ED),
    onBackground = GharBatoPalette.textLight,
    onSurface = GharBatoPalette.textLight,
    onSurfaceVariant = GharBatoPalette.mutedTextLight,
    outline = GharBatoPalette.mutedTextLight,
    outlineVariant = GharBatoPalette.outlineLight,
    surfaceTint = GharBatoPalette.brand,
    inverseSurface = GharBatoPalette.surfaceDark,
    inverseOnSurface = GharBatoPalette.textDark,
    inversePrimary = GharBatoPalette.brandLight
)

@Composable
fun GharBatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalGharBatoActionColors provides if (darkTheme) DarkActionColors else LightActionColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GharBatoShapes.material,
            content = content
        )
    }
}
