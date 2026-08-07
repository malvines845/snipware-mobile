package com.snipware.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The original app uses 'Syne' for UI text and 'JetBrains Mono' for code
// (--font-ui / --font-code in pierce.html). Those are bundled TTFs in the
// web project's assets/fonts/ folder; drop matching .ttf files under
// app/src/main/res/font/ and swap FontFamily.Default / FontFamily.Monospace
// below for pixel-perfect parity. Monospace is used as a safe, always
// present fallback so the project builds without extra font assets.
val CodeFontFamily = FontFamily.Monospace
val UiFontFamily = FontFamily.Default

val SnipTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CodeFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

/** Dedicated style for code previews/editors -- always monospace regardless of app theme. */
val CodeTextStyle = TextStyle(
    fontFamily = CodeFontFamily,
    fontSize = 13.sp,
    lineHeight = 19.sp
)
