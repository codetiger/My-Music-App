package com.codetiger.mymusicapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.codetiger.mymusicapp.R

private fun atkinson(weight: Int) = Font(
    R.font.atkinson_hyperlegible_next,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Atkinson Hyperlegible Next, bundled as one variable font. */
val Atkinson = FontFamily(atkinson(400), atkinson(500), atkinson(700))

private fun style(size: Int, lineHeight: Int, weight: Int) = TextStyle(
    fontFamily = Atkinson,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
)

/** Type scale from design-system/tokens.json. Nothing smaller than 16sp. */
object MusicType {
    val SongHero = style(28, 34, 700)
    val Title = style(24, 30, 700)
    val ButtonHero = style(22, 28, 700)
    val Heading = style(20, 26, 700)
    val Body = style(18, 26, 400)
    val BodyStrong = style(18, 26, 700)
    val Time = style(18, 26, 500)
    val Button = style(18, 24, 700)
    val ControlLabel = style(16, 20, 700)
}

/** Material slots mapped onto the scale, so stock components pick up the same type. */
val MusicTypography = Typography(
    displayLarge = MusicType.SongHero,
    displayMedium = MusicType.SongHero,
    displaySmall = MusicType.SongHero,
    headlineLarge = MusicType.SongHero,
    headlineMedium = MusicType.Title,
    headlineSmall = MusicType.Title,
    titleLarge = MusicType.Title,
    titleMedium = MusicType.Heading,
    titleSmall = MusicType.BodyStrong,
    bodyLarge = MusicType.Body,
    bodyMedium = MusicType.Body,
    bodySmall = MusicType.Body,
    labelLarge = MusicType.Button,
    labelMedium = MusicType.ControlLabel,
    labelSmall = MusicType.ControlLabel,
)
