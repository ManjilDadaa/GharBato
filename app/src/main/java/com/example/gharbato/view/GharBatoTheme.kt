package com.example.gharbato.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.gharbato.ui.theme.Blue
// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = Color.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF001D35),

    secondary = Color(0xFF535E6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F4),
    onSecondaryContainer = Color(0xFF101C2B),

    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color.White,
    onBackground = Color(0xFF2C2C2C),

    surface = Color.White,
    onSurface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF999999),

    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),

    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3C4858),
    onSecondaryContainer = Color(0xFFD7E3F4),

    tertiary = Color(0xFFD9BDE4),
    onTertiary = Color(0xFF3E2D4A),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF121212),
    onBackground = Color(0xFFE1E1E1),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),

    outline = Color(0xFF444444),
    outlineVariant = Color(0xFF333333),
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


