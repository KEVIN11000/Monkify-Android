package com.k11000.monkify.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val MonkifyColorScheme = darkColorScheme(
    primary = MonkifyPrimary,
    onPrimary = MonkifyOnPrimary,
    primaryContainer = MonkifyElectricBlueDark,
    onPrimaryContainer = Color.White,
    secondary = MonkifyElectricBlueLight,
    onSecondary = Color.Black,
    background = MonkifyBackground,
    onBackground = MonkifyOnBackground,
    surface = MonkifySurface,
    onSurface = MonkifyOnSurface,
    surfaceVariant = MonkifyDarkGray,
    onSurfaceVariant = MonkifyGray
)

@Composable
fun MonkifyTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> MonkifyColorScheme
    }

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
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** Alias retrocompatible con llamadas preexistentes */
@Composable
fun MyApplicationTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MonkifyTheme(dynamicColor = dynamicColor, content = content)
