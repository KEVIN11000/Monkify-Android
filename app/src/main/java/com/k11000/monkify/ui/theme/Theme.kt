package com.k11000.monkify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MonkifyColorScheme = darkColorScheme(
    primary = MonkifyPrimary,
    onPrimary = MonkifyOnPrimary,
    background = MonkifyBackground,
    onBackground = MonkifyOnBackground,
    surface = MonkifySurface,
    onSurface = MonkifyOnSurface,
    surfaceVariant = MonkifyDarkGray,
    onSurfaceVariant = MonkifyGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = MonkifyColorScheme,
        typography = Typography,
        content = content
    )
}
