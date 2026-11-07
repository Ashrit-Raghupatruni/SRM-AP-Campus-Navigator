package com.srmap.campusnavigator.core.designsystem

import android.app.Activity
import android.os.Build
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
    primary = SrmCrimsonLight,
    onPrimary = Color.White,
    secondary = SrmAccentCyan,
    onSecondary = Color.White,
    tertiary = SrmWalkingGreen,
    background = CampusNavy,
    surface = CampusSlate,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SrmCrimson,
    onPrimary = Color.White,
    secondary = SrmAccentCyan,
    onSecondary = Color.White,
    tertiary = SrmWalkingGreen,
    background = CampusSurfaceLight,
    surface = Color.White,
    onBackground = CampusNavy,
    onSurface = CampusSlate
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
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
