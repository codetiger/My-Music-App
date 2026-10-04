package com.codetiger.mymusicapp.ui.theme

import androidx.compose.ui.graphics.Color

/** The four colours from design-system/tokens.json. Never add a fifth. */
object MusicColors {
    val Surface = Color(0xFFF7F0E4)
    val Fill = Color(0xFFE9DCC6)
    val Ink = Color(0xFF2B1D14)
    val Accent = Color(0xFF6B4423)

    val OnAccent = Surface
    val FocusRing = Ink
    val PressOverlay = Ink.copy(alpha = 0.10f)
    val Scrim = Ink.copy(alpha = 0.55f)
    val DrawingInk = Ink
    val DrawingPaper = Surface
}
