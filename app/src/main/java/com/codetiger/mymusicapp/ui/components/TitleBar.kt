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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

/**
 * The top of a tab screen's page (Home, Add Song). Tab screens have no fixed bar: the header
 * scrolls with the page and the selected tab says where you are. [leading] is the drawing on Home.
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Size.RowTall).padding(top = Space.S4),
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        // Wraps, never ellipsised.
        Text(title, style = MusicType.SongHero, modifier = Modifier.weight(1f).semantics { heading() })
    }
}

/** The fixed bar on every screen that is not a tab: Back, then the screen title. */
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
            .heightIn(min = Size.RowTall)
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
