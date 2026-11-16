package com.srmap.campusnavigator.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = DarkBackground,
    primaryContainer = DeepTeal,
    onPrimaryContainer = Color.White,
    secondary = SrmCrimsonLight,
    onSecondary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outline = SlateGrey
)

private val LightColorScheme = lightColorScheme(
    primary = SrmCrimson,
    onPrimary = PureWhite,
    primaryContainer = SrmCrimsonLight,
    onPrimaryContainer = PureWhite,
    secondary = DeepTeal,
    onSecondary = PureWhite,
    background = CloudWhite,
    surface = PureWhite,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = MidnightNavy,
    onSurface = MidnightNavy,
    onSurfaceVariant = SlateGrey,
    outline = BorderGrey
)

@Composable
fun SrmCampusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
