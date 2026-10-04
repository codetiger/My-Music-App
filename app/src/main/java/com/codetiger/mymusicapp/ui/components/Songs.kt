package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import java.io.File

/** A song's picture, or a flat square with a music note. */
@Composable
fun SongArt(
    path: String?,
    modifier: Modifier = Modifier,
    size: Dp = Size.ArtRow,
    shape: Shape = Radius.Sm,
    onFill: Boolean = false,
    noteSize: Dp = 32.dp,
) {
    val box = modifier.size(size).clip(shape).background(if (onFill) MusicColors.Surface else MusicColors.Fill)
    Box(box, contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.ic_music_note), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(noteSize))
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

/** Where a song stands, in words with an icon (StatusLabel). */
data class SongStatus(val words: String, val icon: Int, val bold: Boolean)

fun Song.status(): SongStatus = when (downloadStatus) {
    DownloadStatus.DONE -> SongStatus("On your phone", R.drawable.ic_offline_pin, false)
    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> SongStatus("Downloading $downloadProgress%", R.drawable.ic_downloading, false)
    DownloadStatus.WAITING_RETRY -> SongStatus("Will download later", R.drawable.ic_schedule, false)
    DownloadStatus.FAILED -> SongStatus("Can't be saved", R.drawable.ic_error, true)
}

@Composable
fun StatusLabel(status: SongStatus, modifier: Modifier = Modifier, color: Color = MusicColors.Ink) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S1)) {
        Icon(painterResource(status.icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            status.words,
            style = if (status.bold) MusicType.BodyStrong else MusicType.Body,
            color = color,
        )
    }
}

/**
 * One song: picture, title, "Artist · status", and Play. Tapping the picture, title or artist
 * opens the Song screen. [trailing] replaces Play (Change Order, Up Next).
 */
@Composable
fun SongRow(
    song: Song,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val status = song.status()
    Row(
        modifier.fillMaxWidth().padding(vertical = Space.S1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(Radius.Sm)
                .clickable(role = Role.Button, onClickLabel = "Open song", onClick = onOpen)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.S4),
        ) {
            SongArt(song.thumbnailPath)
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MusicType.BodyStrong)
                if (note != null) Text(note, style = MusicType.Body)
                if (song.artist.isNotEmpty()) Text(song.artist, style = MusicType.Body)
                StatusLabel(status)
            }
        }
        if (trailing != null) trailing() else {
            StackedButton(
                "Play", R.drawable.ic_play_arrow, onPlay,
                description = "Play ${song.title}",
            )
        }
    }
}

/** Move Up / Move Down beside a row, so drag is never the only way (PL-5). */
@Composable
fun MoveButtons(onUp: () -> Unit, onDown: () -> Unit, canUp: Boolean, canDown: Boolean, title: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
        // Buttons stay active; at the ends they simply do nothing (design-system: never disable).
        StackedButton("Up", R.drawable.ic_arrow_upward, { if (canUp) onUp() }, description = "Move $title up")
        StackedButton("Down", R.drawable.ic_arrow_downward, { if (canDown) onDown() }, description = "Move $title down")
    }
}

@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MusicType.Heading,
        modifier = modifier.padding(top = Space.S5).semantics { heading() },
    )
}

/** A screen's own title under the Back bar where one is needed. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MusicType.Title, modifier = modifier.semantics { heading() })
}
