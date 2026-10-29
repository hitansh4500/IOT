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
    primary = CyberCyan,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF33206C),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = NeonEmerald,
    onTertiary = Color.White,
    error = CriticalCrimson,
    onError = Color.White,
    background = CyberNavyDark,
    onBackground = TextPrimaryDark,
    surface = CyberNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CyberNavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = CyberNavyBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyberCyanDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7F4FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = CyberPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADBFF),
    onSecondaryContainer = Color(0xFF220B55),
    tertiary = NeonEmerald,
    onTertiary = Color.White,
    error = CriticalCrimson,
    onError = Color.White,
    background = CyberLightBackground,
    onBackground = TextPrimaryLight,
    surface = CyberLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CyberLightCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = CyberLightBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark cyber aesthetic for cybersecurity app
    dynamicColor: Boolean = false, // Keep consistent cyber branded palette
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
        content = content
    )
}
