package com.codetiger.mymusicapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Spacing, radius and size tokens from design-system/tokens.json (1px = 1dp). */
object Space {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S5 = 24.dp
    val S6 = 32.dp
    val S7 = 48.dp
}

object Radius {
    val Sm = RoundedCornerShape(8.dp)
    val Md = RoundedCornerShape(16.dp)
    val Lg = RoundedCornerShape(24.dp)
    val Full = RoundedCornerShape(percent = 50)
}

object Size {
    val Target = 64.dp
    val Play = 96.dp
    val Transport = 72.dp
    val Avatar = 48.dp
    val AvatarLg = 64.dp
    val ArtRow = 64.dp
    val ArtCard = 96.dp
    val ArtHero = 240.dp
    val IconSm = 24.dp
    val Icon = 28.dp
    val IconMd = 32.dp
    val IconLg = 40.dp
    val IconXl = 56.dp
    val Row = 72.dp
    val RowTall = 88.dp
    val Handle = 32.dp
    val Track = 8.dp
    val Progress = 4.dp
    val Tile = 128.dp
    val FocusWidth = 3.dp
}
