package com.familyguardian.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.Black,
    primaryContainer = WhatsAppGreenDark,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF2A3942)
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppGreenDark,
    onPrimary = Color.White,
    primaryContainer = NavIndicatorLight,
    onPrimaryContainer = WhatsAppGreenDark,
    secondary = WhatsAppTeal,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFE9EDEF)
)

@Composable
fun MonitorTheme(
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
