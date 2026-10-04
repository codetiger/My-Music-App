package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

/**
 * The `fill` strip above the tabs that always says what is playing; tap opens Now Playing. On the
 * tab screens its Play / Pause is the screen's main action, so [primary] makes it `accent`.
 */
@Composable
fun NowPlayingBar(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val kind = if (primary) ButtonKind.Primary else ButtonKind.Secondary
    Bar(song.thumbnailPath, song.title, song.artist.ifEmpty { null }, "Open Now Playing", onOpen, progress, modifier) {
        if (isPlaying) {
            StackedButton("Pause", R.drawable.ic_pause, onPlayPause, kind = kind, onFill = true)
        } else {
            StackedButton("Play", R.drawable.ic_play_arrow, onPlayPause, kind = kind, onFill = true)
        }
    }
}

/** The same strip when nothing is playing: it offers the default list, and a tap plays it (HOME-2). */
@Composable
fun PlayListBar(name: String, picture: String?, onPlay: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false) {
    Bar(picture, "Play $name", "Nothing playing", "Play $name", onPlay, null, modifier) {
        StackedButton("Play", R.drawable.ic_play_arrow, onPlay, kind = if (primary) ButtonKind.Primary else ButtonKind.Secondary, onFill = true)
    }
}

@Composable
private fun Bar(
    picture: String?,
    title: String,
    subtitle: String?,
    openLabel: String,
    onOpen: () -> Unit,
    progress: Float?,
    modifier: Modifier,
    button: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S4)
            .clip(Radius.Md)
            .background(MusicColors.Fill),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(Space.S3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.S3),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .focusRing(Radius.Sm)
                    .clip(Radius.Sm)
                    .clickable(role = Role.Button, onClickLabel = openLabel, onClick = onOpen)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.S3),
            ) {
                SongArt(picture, onFill = true)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MusicType.BodyStrong)
                    // At large text the bar would crowd out the screen; the title alone says what plays.
                    if (subtitle != null && LocalDensity.current.fontScale < 1.25f) {
                        Text(subtitle, style = MusicType.Body)
                    }
                }
            }
            button()
        }
        if (progress != null) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(Size.Progress)
                    .background(MusicColors.Accent),
            )
        }
    }
}
