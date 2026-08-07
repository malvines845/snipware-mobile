package com.snipware.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SnipDarkColors = darkColorScheme(
    primary = SnipAccent,
    onPrimary = SnipBg,
    secondary = SnipPin,
    onSecondary = SnipBg,
    background = SnipBg,
    onBackground = SnipText,
    surface = SnipSurface,
    onSurface = SnipText,
    surfaceVariant = SnipSurface2,
    onSurfaceVariant = SnipTextMid,
    error = SnipDanger,
    onError = SnipText,
    outline = SnipBorder
)

// Snipware is designed dark-first (like the original web app), but a light
// scheme is included so the app still behaves if the user's system forces
// light mode / for accessibility settings that override app theming.
private val SnipLightColors = lightColorScheme(
    primary = SnipAccentDim,
    secondary = SnipPin,
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    error = SnipDanger
)

@Composable
fun SnipwareTheme(
    darkTheme: Boolean = true, // default to the original app's dark-only aesthetic
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) SnipDarkColors else SnipLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SnipTypography,
        content = content
    )
}
