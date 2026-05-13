package com.vitaai.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.vitaai.app.utils.ThemeManager

// === 深蓝+金色 配色 ===
val DeepBlue = Color(0xFF0D2137)
val NavyBlue = Color(0xFF1A3A5C)
val RoyalBlue = Color(0xFF1E5F8E)
val Gold = Color(0xFFD4A843)
val LightGold = Color(0xFFF0C75A)
val SoftGold = Color(0xFFFFF3CD)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF5F7FA)
val DarkGray = Color(0xFF2C3E50)

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    onPrimary = White,
    primaryContainer = SoftGold,
    onPrimaryContainer = DeepBlue,
    secondary = Gold,
    onSecondary = DeepBlue,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = DarkGray,
    tertiary = RoyalBlue,
    onTertiary = White,
    background = LightGray,
    onBackground = DeepBlue,
    surface = White,
    onSurface = DeepBlue,
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFF8899AA),
    error = Color(0xFFE53E3E)
)

private val DarkColorScheme = darkColorScheme(
    primary = LightGold,
    onPrimary = DeepBlue,
    primaryContainer = NavyBlue,
    onPrimaryContainer = LightGold,
    secondary = Gold,
    onSecondary = DeepBlue,
    secondaryContainer = Color(0xFF2D2000),
    onSecondaryContainer = LightGold,
    tertiary = Color(0xFF64B5F6),
    onTertiary = DeepBlue,
    background = Color(0xFF0A1628),
    onBackground = Color(0xFFE8EDF2),
    surface = Color(0xFF0D2137),
    onSurface = Color(0xFFE8EDF2),
    surfaceVariant = Color(0xFF162840),
    onSurfaceVariant = Color(0xFFAABBCC),
    outline = Color(0xFF445566),
    error = Color(0xFFFF6B6B)
)

@Composable
fun VitaAITheme(
    content: @Composable () -> Unit
) {
    val mode by ThemeManager.mode
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (mode) {
        ThemeManager.MODE_LIGHT -> false
        ThemeManager.MODE_DARK -> true
        else -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}