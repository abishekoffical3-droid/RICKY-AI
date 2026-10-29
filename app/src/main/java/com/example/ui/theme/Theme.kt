package com.example.ui.theme

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
    primary = RickyCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E59),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = RickyPurple,
    onSecondary = Color(0xFF260058),
    secondaryContainer = Color(0xFF3B0086),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = RickyNeonGreen,
    onTertiary = Color(0xFF003915),
    background = RickyDarkBg,
    onBackground = TextPrimaryDark,
    surface = RickyDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = RickyDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = RickyError,
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = RickyLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = RickyLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADBFF),
    onSecondaryContainer = Color(0xFF23005B),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = RickyLightBg,
    onBackground = TextPrimaryLight,
    surface = RickyLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = RickyLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun RickyAiTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for AI assistant
    dynamicColor: Boolean = false, // Keep brand identity by default
    content: @Composable () -> Unit
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
        content = content
    )
}
