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
import androidx.compose.ui.unit.dp
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Space

/** The `fill` strip above the tabs that always says what is playing; tap opens Now Playing. */
@Composable
fun NowPlayingBar(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S3)
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
                    .clip(Radius.Sm)
                    .clickable(role = Role.Button, onClickLabel = "Open Now Playing", onClick = onOpen)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.S3),
            ) {
                SongArt(song.thumbnailPath, onFill = true)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MusicType.BodyStrong)
                    // At large text the bar would crowd out the screen; the title alone says what plays.
                    if (song.artist.isNotEmpty() && LocalDensity.current.fontScale < 1.25f) {
                        Text(song.artist, style = MusicType.Body)
                    }
                }
            }
            if (isPlaying) {
                StackedButton("Pause", R.drawable.ic_pause, onPlayPause, onFill = true)
            } else {
                StackedButton("Play", R.drawable.ic_play_arrow, onPlayPause, onFill = true)
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(MusicColors.Accent),
        )
    }
}
