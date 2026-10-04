package com.codetiger.mymusicapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.AppContainer
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.ui.BackScreen
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.UiState
import com.codetiger.mymusicapp.ui.components.ButtonKind
import com.codetiger.mymusicapp.ui.components.ButtonPair
import com.codetiger.mymusicapp.ui.components.ConfirmDialog
import com.codetiger.mymusicapp.ui.components.EditSongDialog
import com.codetiger.mymusicapp.ui.components.MessageAction
import com.codetiger.mymusicapp.ui.components.MusicButton
import com.codetiger.mymusicapp.ui.components.SongArt
import com.codetiger.mymusicapp.ui.components.StatusLabel
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.components.status
import com.codetiger.mymusicapp.ui.rememberSettings
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** The Song screen: picture, title, artist, state and length, and what can be done with it (LIB-5). */
@Composable
fun SongScreen(songId: Long, from: ListRef?) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    val settings = rememberSettings()
    val song by remember(songId) { app.library.song(songId) }.collectAsStateWithLifecycle(initialValue = null)
    val favourite by remember(songId) { app.library.isFavourite(songId) }.collectAsStateWithLifecycle(initialValue = false)
    val fromName by remember(from) { from?.let { app.library.listName(it) } ?: flowOf(null) }.collectAsStateWithLifecycle(initialValue = null)
    val fromList by remember(from) {
        (from as? ListRef.Stored)?.let { app.library.playlist(it.playlistId) } ?: flowOf(null)
    }.collectAsStateWithLifecycle(initialValue = null)
    var askRemove by remember { mutableStateOf(false) }
    var askEdit by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(song) {
        if (song != null) loaded = true
        if (loaded && (song == null || song?.removedAt != null)) nav.popBackStack()
    }
    val s = song ?: return BackScreen("Song") {}

    BackScreen("Song", showNowPlaying = true) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SongArt(s.thumbnailPath, size = Size.ArtHero, shape = Radius.Lg, noteSize = 96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.title, style = MusicType.SongHero, textAlign = TextAlign.Center)
                if (s.artist.isNotEmpty()) Text(s.artist, style = MusicType.Body, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.S2), verticalAlignment = Alignment.CenterVertically) {
                    StatusLabel(s.status())
                    if (s.durationMs > 0) Text("· ${Format.time(s.durationMs)}", style = MusicType.Body)
                }
            }

            if (s.downloadStatus == DownloadStatus.FAILED) {
                // "Can't be saved": Try Again and Remove, or Remove alone when it can never work.
                if (s.failureReason?.canRetry == true) {
                    WideButton("Try Again", R.drawable.ic_download, { app.scope.launch { app.songAdder.retry(s.id) } }, kind = ButtonKind.Primary)
                }
                WideButton("Remove", R.drawable.ic_delete, { askRemove = true })
            } else {
                WideButton("Play", R.drawable.ic_play_arrow, {
                    app.scope.launch {
                        val list = from?.let { app.library.currentSongsOf(it, settings.songSort) }
                            ?: app.library.currentSongsOf(ListRef.AllSongs, settings.songSort)
                        val index = list.indexOfFirst { it.id == s.id }
                        if (index >= 0) app.player.play(list, index) else app.player.play(listOf(s))
                    }
                }, kind = ButtonKind.Primary)
                ButtonPair(
                    { m ->
                        MusicButton(
                            if (favourite) "Remove from Favourites" else "Favourite",
                            if (favourite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
                            { app.scope.launch { app.library.toggleFavourite(s.id) } },
                            m,
                        )
                    },
                    { m -> MusicButton("Add to List", R.drawable.ic_playlist_add, { nav.navigate(Routes.addToList(s.id)) }, m) },
                )
                ButtonPair(
                    { m -> MusicButton("Edit Name", R.drawable.ic_edit, { askEdit = true }, m) },
                    { m -> MusicButton("Remove", R.drawable.ic_delete, { askRemove = true }, m) },
                )
                // Taking a song out of a list never deletes it from the library (PL-6).
                val list = fromList
                if (list != null && fromName != null) {
                    WideButton("Take Out of $fromName", R.drawable.ic_playlist_remove, {
                        app.scope.launch { app.library.removeFromList(list.id, s.id) }
                        ui.messages.show("Taken out of $fromName", R.drawable.ic_playlist_remove, MessageAction("Put Back", R.drawable.ic_restore_from_trash) {
                            app.scope.launch { app.library.addToList(list.id, s.id) }
                        })
                        nav.popBackStack()
                    })
                }
            }
        }
    }

    if (askEdit) {
        EditSongDialog(s.title, s.artist, onConfirm = { t, a -> app.scope.launch { app.library.rename(s.id, t, a) } }, onDismiss = { askEdit = false })
    }
    if (askRemove) {
        ConfirmDialog(
            "Remove this song?",
            "Remove",
            onConfirm = { removeSong(app, ui, s) },
            onDismiss = { askRemove = false },
            detail = "It goes to Recently Removed for 30 days, where you can put it back.",
            confirmIcon = R.drawable.ic_delete,
        )
    }
}

/** LIB-6: to Recently Removed, out of Up Next; the next song starts if it was playing. */
fun removeSong(app: AppContainer, ui: UiState, song: Song) {
    app.player.removeSong(song.id)
    app.scheduler.cancel(song.id)
    app.scope.launch { app.library.removeSong(song.id) }
    ui.messages.show("Song removed", R.drawable.ic_delete, MessageAction("Put Back", R.drawable.ic_restore_from_trash) {
        app.scope.launch {
            app.library.putBackSong(song.id)
            if (song.downloadStatus != DownloadStatus.DONE && song.downloadStatus != DownloadStatus.FAILED) app.scheduler.enqueue(song.id)
        }
    })
}
