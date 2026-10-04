package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.Size

/** The user's photo drawing, or the music-note logo until they add one. Decorative by default. */
@Composable
fun Avatar(
    drawing: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = Size.Avatar,
    contentDescription: String? = null,
) {
    val sized = modifier.size(size).clip(CircleShape)
    if (drawing != null) {
        Image(drawing, contentDescription, sized)
    } else {
        Image(painterResource(R.drawable.logo_mark), contentDescription, sized)
    }
}
