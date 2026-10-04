package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Space

/** Title bar on the three tab screens: drawing, personalised title, Settings. */
@Composable
fun TabTitleBar(
    title: String,
    drawing: ImageBitmap?,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitleBarRow(modifier) {
        Avatar(drawing)
        TitleText(title, Modifier.weight(1f))
        StackedButton(stringResource(R.string.action_settings), R.drawable.ic_settings, onSettings)
    }
}

/** Title bar on every other screen: Back, then the screen title. */
@Composable
fun BackTitleBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TitleBarRow(modifier) {
        MusicButton(stringResource(R.string.action_back), R.drawable.ic_arrow_back, onBack)
        TitleText(title, Modifier.weight(1f))
    }
}

@Composable
private fun TitleBarRow(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .padding(horizontal = Space.S4, vertical = Space.S3),
        horizontalArrangement = Arrangement.spacedBy(Space.S3),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// Wraps, never ellipsised.
@Composable
private fun TitleText(title: String, modifier: Modifier) {
    Text(title, style = MusicType.Title, modifier = modifier.semantics { heading() })
}
