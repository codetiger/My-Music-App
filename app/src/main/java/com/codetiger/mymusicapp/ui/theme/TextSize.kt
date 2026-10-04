package com.codetiger.mymusicapp.ui.theme

/** Settings → Text Size (SET-2). */
enum class TextSize(val factor: Float) {
    Normal(1f),
    Large(1.25f),
    ExtraLarge(1.5f),
}

const val TEXT_SCALE_CAP = 2f

/** The phone's font scale times the app's Text Size, capped so nothing is cut off. */
fun effectiveFontScale(phoneFontScale: Float, textSize: TextSize): Float =
    (phoneFontScale * textSize.factor).coerceAtMost(TEXT_SCALE_CAP)
