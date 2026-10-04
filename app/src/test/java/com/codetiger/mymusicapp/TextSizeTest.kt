package com.codetiger.mymusicapp

import com.codetiger.mymusicapp.ui.theme.TextSize
import com.codetiger.mymusicapp.ui.theme.effectiveFontScale
import org.junit.Assert.assertEquals
import org.junit.Test

class TextSizeTest {
    @Test
    fun multipliesPhoneScaleByTextSize() {
        assertEquals(1f, effectiveFontScale(1f, TextSize.Normal), 0f)
        assertEquals(1.25f, effectiveFontScale(1f, TextSize.Large), 0f)
        assertEquals(1.65f, effectiveFontScale(1.1f, TextSize.ExtraLarge), 0.0001f)
    }

    @Test
    fun capsAtTwo() {
        assertEquals(2f, effectiveFontScale(1.5f, TextSize.ExtraLarge), 0f)
        assertEquals(2f, effectiveFontScale(2f, TextSize.Large), 0f)
    }
}
