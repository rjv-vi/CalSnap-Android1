package com.rjvvi.calsnap.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightBg1,
    primaryContainer = LightBg2,
    onPrimaryContainer = LightText0,
    secondary = Color(0xFF605D56),
    onSecondary = LightBg1,
    secondaryContainer = LightBg3,
    onSecondaryContainer = LightText0,
    background = LightBg0,
    onBackground = LightText0,
    surface = LightBg1,
    onSurface = LightText0,
    surfaceVariant = LightBg2,
    onSurfaceVariant = LightText1,
    outline = Color(0x1C141210),
    outlineVariant = Color(0x10141210),
    error = ErrRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBg1,
    primaryContainer = DarkBg2,
    onPrimaryContainer = DarkText0,
    secondary = Color(0xFFCAC4BA),
    onSecondary = DarkBg1,
    secondaryContainer = DarkBg3,
    onSecondaryContainer = DarkText0,
    background = DarkBg0,
    onBackground = DarkText0,
    surface = DarkBg1,
    onSurface = DarkText0,
    surfaceVariant = DarkBg2,
    onSurfaceVariant = DarkText1,
    outline = Color(0x1CF4F2EE),
    outlineVariant = Color(0x0AF4F2EE),
    error = ErrRedDark
)

@Composable
fun CalSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Extension to check dark mode from composable
@Composable
fun isDark() = isSystemInDarkTheme()

@Composable
fun streakColor() = if (isSystemInDarkTheme()) StreakDark else Streak
@Composable
fun okColor() = if (isSystemInDarkTheme()) OkGreenDark else OkGreen
