package com.snipware.app.ui.theme

import androidx.compose.ui.graphics.Color

// Ported 1:1 from the original web app's :root CSS custom properties
// (see pierce.html / css/global.css) so the Android app keeps the same look.
val SnipBg = Color(0xFF1E1E1E)
val SnipSurface = Color(0xFF252526)
val SnipSurface2 = Color(0xFF2D2D2E)
val SnipSurface3 = Color(0xFF333334)
val SnipBorder = Color(0xFF3A3A3B)
val SnipAccent = Color(0xFFA0FF6F)
val SnipAccentDim = Color(0xFF4CBA28)
val SnipPin = Color(0xFFFFAA3B)
val SnipDanger = Color(0xFFFF4F4F)
val SnipText = Color(0xFFE2E2E8)
val SnipTextMid = Color(0xFF8F8F9A)
val SnipTextDim = Color(0xFF5A5A64)

// Additional tokens pulled from the original app's global.css that aren't
// plain CSS custom properties but are used consistently enough to be
// design-system colors in their own right.
val SnipAccentGlow = Color(0x12A0FF6F)       // rgba(160,255,111,0.07)
val SnipAccentGlowStrong = Color(0x26A0FF6F) // rgba(160,255,111,0.15) -- active filter chip bg
val SnipPreviewText = Color(0xFFABB2BF)      // .cpreview text color
val SnipLockBlue = Color(0xFF60A5FA)         // .lock-badge / locked-card ring
val SnipLockBlueGlow = Color(0x1A60A5FA)     // rgba(96,165,250,0.1)
val SnipMessyTitle = Color(0xFFC8C8D0)       // .card.messy .card-title
val SnipMessyBorder = Color(0x38FF4F4F)      // rgba(255,79,79,0.22)
val SnipSearchBg = Color(0xFF1D1D23)         // #searchBar background
val SnipSearchBgFocus = Color(0xFF22222B)    // #searchBar:focus background
val SnipSearchBorder = Color(0xFF48484F)     // #searchBar border
val SnipSearchPlaceholder = Color(0xFF505060)
