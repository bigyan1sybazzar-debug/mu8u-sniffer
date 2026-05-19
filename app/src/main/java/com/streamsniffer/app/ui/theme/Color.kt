package com.streamsniffer.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark theme — electric blue + deep purple accents
val ElectricBlue = Color(0xFF00D4FF)
val DeepPurple = Color(0xFF7B2FFF)
val NeonGreen = Color(0xFF00FF88)
val DarkBackground = Color(0xFF0A0E1A)
val DarkSurface = Color(0xFF111827)
val DarkCard = Color(0xFF1A2235)
val OnDark = Color(0xFFE8F0FE)
val OnDarkSecondary = Color(0xFF8B9DC3)
val ErrorRed = Color(0xFFFF4B6A)

val StreamSnifferDarkColors = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = DarkBackground,
    primaryContainer = Color(0xFF003A4A),
    onPrimaryContainer = ElectricBlue,
    secondary = DeepPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1466),
    onSecondaryContainer = Color(0xFFCDB3FF),
    tertiary = NeonGreen,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkCard,
    onSurfaceVariant = OnDarkSecondary,
    outline = Color(0xFF2A3A5C),
    error = ErrorRed,
    onError = Color.White
)
