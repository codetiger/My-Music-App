package com.codetiger.mymusicapp.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.player.RepeatSetting
import com.codetiger.mymusicapp.ui.BackScreen
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.components.ButtonPair
import com.codetiger.mymusicapp.ui.components.MusicButton
import com.codetiger.mymusicapp.ui.components.MusicSlider
import com.codetiger.mymusicapp.ui.components.SongArt
import com.codetiger.mymusicapp.ui.components.StackedButton
import com.codetiger.mymusicapp.ui.components.ToggleButton
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Now Playing: picture, seek bar, controls, Up Next / Song Details, Volume. */
@Composable
fun NowPlayingScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val state by app.player.state.collectAsStateWithLifecycle()
    val song by remember(state.currentSongId) {
        state.currentSongId?.let { app.library.song(it) } ?: flowOf<Song?>(null)
    }.collectAsStateWithLifecycle(initialValue = null)
    val favourite by remember(state.currentSongId) {
        state.currentSongId?.let { app.library.isFavourite(it) } ?: flowOf(false)
    }.collectAsStateWithLifecycle(initialValue = false)
    val volume = rememberMediaVolume()
    var seeking by remember { mutableStateOf<Float?>(null) }

    BackScreen("Now Playing") {
        val s = song
        if (s == null) {
            Text("Nothing is playing.", style = MusicType.Body, modifier = Modifier.padding(Space.S4))
            return@BackScreen
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongArt(s.thumbnailPath, size = Size.ArtHero, shape = Radius.Lg, noteSize = 96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.title, style = MusicType.SongHero, textAlign = TextAlign.Center)
                if (s.artist.isNotEmpty()) Text(s.artist, style = MusicType.Body, textAlign = TextAlign.Center)
            }

            val duration = state.durationMs.takeIf { it > 0 } ?: s.durationMs
            val fraction = seeking ?: if (duration > 0) state.positionMs.toFloat() / duration else 0f
            Column(Modifier.fillMaxWidth()) {
                MusicSlider(
                    value = fraction,
                    onChange = { seeking = it },
                    onDone = { seeking = null; if (duration > 0) app.player.seekTo((it * duration).toLong()) },
                    label = "Position in song",
                    valueText = "${Format.time((fraction * duration).toLong())} of ${Format.time(duration)}",
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(Format.time((fraction * duration).toLong()), style = MusicType.Time)
                    Text(Format.time(duration), style = MusicType.Time)
                }
            }
            ButtonPair(
                { m -> MusicButton("Back 10 s", R.drawable.ic_replay_10, { app.player.seekBy(-10_000) }, m) },
                { m -> MusicButton("Ahead 10 s", R.drawable.ic_forward_10, { app.player.seekBy(10_000) }, m) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                TransportButton("Previous", R.drawable.ic_skip_previous, Size.Transport, main = false) { app.player.previous() }
                if (state.isPlaying) {
                    TransportButton("Pause", R.drawable.ic_pause, Size.Play, main = true) { app.player.togglePlay() }
                } else {
                    TransportButton("Play", R.drawable.ic_play_arrow, Size.Play, main = true) { app.player.togglePlay() }
                }
                TransportButton("Next", R.drawable.ic_skip_next, Size.Transport, main = false) { app.player.next() }
            }
            ButtonPair(
                { m -> ToggleButton(if (state.shuffle) "Shuffle: On" else "Shuffle: Off", R.drawable.ic_shuffle, state.shuffle, { app.player.setShuffle(!state.shuffle) }, m) },
                { m ->
                    ToggleButton(
                        when (state.repeat) { RepeatSetting.Off -> "Repeat: Off"; RepeatSetting.All -> "Repeat: All"; RepeatSetting.One -> "Repeat: One" },
                        if (state.repeat == RepeatSetting.One) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                        state.repeat != RepeatSetting.Off,
                        { app.player.cycleRepeat() },
                        m,
                    )
                },
            )
            ButtonPair(
                { m -> MusicButton("Up Next", R.drawable.ic_queue_music, { nav.navigate(Routes.UP_NEXT) }, m) },
                { m -> MusicButton("Song Details", R.drawable.ic_info, { nav.navigate(Routes.song(s.id)) }, m) },
            )
            WideButton(
                if (favourite) "Remove from Favourites" else "Favourite",
                if (favourite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
                { app.scope.launch { app.library.toggleFavourite(s.id) } },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
                Text("Volume", style = MusicType.BodyStrong)
                MusicSlider(
                    value = volume.level,
                    onChange = { volume.set(it) },
                    label = "Volume",
                    valueText = "${Math.round(volume.level * 100)} percent",
                    modifier = Modifier.weight(1f),
                    steps = volume.steps,
                )
            }
        }
    }
}

/** Previous / Play-Pause / Next: a disc with the word under it; disc and word are one target. */
@Composable
private fun TransportButton(label: String, @DrawableRes icon: Int, size: androidx.compose.ui.unit.Dp, main: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(Radius.Md)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(Space.S1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(if (main) MusicColors.Accent else MusicColors.Fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon), contentDescription = null,
                tint = if (main) MusicColors.OnAccent else MusicColors.Ink,
                modifier = Modifier.size(if (main) 56.dp else Size.IconLg),
            )
        }
        Text(label, style = MusicType.ControlLabel)
    }
}

/** Up Next (PLY-8): the songs still to play; play, move or remove any of them. */
@Composable
fun UpNextScreen() {
    val app = LocalApp.current
    val state by app.player.state.collectAsStateWithLifecycle()
    val library by app.library.library.collectAsStateWithLifecycle(initialValue = emptyList())
    val byId = remember(library) { library.associateBy { it.id } }

    BackScreen("Up Next") {
        if (state.queue.isEmpty()) {
            Text("Nothing is lined up.", style = MusicType.Body, modifier = Modifier.padding(Space.S4))
            return@BackScreen
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Space.S4, vertical = Space.S2)) {
            itemsIndexed(state.queue, key = { i, id -> "$i-$id" }) { position, id ->
                val song = byId[id] ?: return@itemsIndexed
                val isCurrent = position == state.currentIndex
                Column(Modifier.fillMaxWidth().padding(bottom = Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(Radius.Sm)
                            .clickable(role = Role.Button, onClickLabel = "Play") { app.player.playQueueItem(position) }
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.S4),
                    ) {
                        SongArt(song.thumbnailPath)
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MusicType.BodyStrong)
                            Text(if (isCurrent) "Playing now" else song.artist.ifEmpty { " " }, style = MusicType.Body)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.S2, Alignment.End)) {
                        StackedButton("Up", R.drawable.ic_arrow_upward, { app.player.moveQueueItem(position, up = true) }, description = "Move ${song.title} up")
                        StackedButton("Down", R.drawable.ic_arrow_downward, { app.player.moveQueueItem(position, up = false) }, description = "Move ${song.title} down")
                        StackedButton("Remove", R.drawable.ic_close, { app.player.removeQueueItem(position) }, description = "Take ${song.title} out of Up Next")
                    }
                }
            }
        }
    }
}
