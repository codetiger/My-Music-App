package com.codetiger.mymusicapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.Playlist
import com.codetiger.mymusicapp.ui.BackScreen
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.components.ButtonKind
import com.codetiger.mymusicapp.ui.components.ButtonPair
import com.codetiger.mymusicapp.ui.components.ConfirmDialog
import com.codetiger.mymusicapp.ui.components.ListTile
import com.codetiger.mymusicapp.ui.components.MoveButtons
import com.codetiger.mymusicapp.ui.components.MusicButton
import com.codetiger.mymusicapp.ui.components.NameDialog
import com.codetiger.mymusicapp.ui.components.NewListTile
import com.codetiger.mymusicapp.ui.components.SongRow
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.rememberSettings
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** A list: Play All, Shuffle, its songs, and Change Order for Favourites and own lists (PL-2 to PL-5). */
@Composable
fun PlaylistScreen(ref: ListRef) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    val settings = rememberSettings()
    val name by remember(ref) { app.library.listName(ref) }.collectAsStateWithLifecycle(initialValue = "")
    val playlistFlow = remember(ref) { (ref as? ListRef.Stored)?.let { app.library.playlist(it.playlistId) } ?: flowOf<Playlist?>(null) }
    val playlist by playlistFlow.collectAsStateWithLifecycle(initialValue = null)
    val songs by remember(ref, settings.songSort) { app.library.songsOf(ref, settings.songSort) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var changeOrder by remember { mutableStateOf(false) }
    var askRename by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    // The list was deleted (here or elsewhere): go back.
    LaunchedEffect(name) { if (name == null) nav.popBackStack() }
    val canReorder = playlist != null
    val isOwn = playlist?.builtIn == BuiltIn.NONE

    BackScreen(name.orEmpty(), showNowPlaying = true) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Space.S4, vertical = Space.S2)) {
            item(key = "play") {
                ButtonPair(
                    { m -> MusicButton("Play All", R.drawable.ic_play_arrow, { app.player.play(songs) }, m, kind = ButtonKind.Primary) },
                    { m -> MusicButton("Shuffle", R.drawable.ic_shuffle, { app.player.play(songs, shuffle = true) }, m) },
                    Modifier.padding(bottom = Space.S4),
                )
            }
            if (songs.isEmpty()) {
                item(key = "empty") {
                    Text(
                        when (ref) {
                            ListRef.RecentlyPlayed -> "Songs you play will show here."
                            ListRef.AllSongs -> "Songs you add will show here."
                            else -> "No songs in this list yet. Open a song and tap Add to List."
                        },
                        style = MusicType.Body,
                        modifier = Modifier.padding(bottom = Space.S4),
                    )
                }
            }
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song,
                    onOpen = { nav.navigate(Routes.song(song.id, ref)) },
                    onPlay = { app.player.play(songs, index) },
                    modifier = Modifier.padding(bottom = Space.S2),
                    trailing = if (changeOrder && playlist != null) {
                        {
                            MoveButtons(
                                onUp = { app.scope.launch { app.library.move(playlist!!.id, song.id, up = true) } },
                                onDown = { app.scope.launch { app.library.move(playlist!!.id, song.id, up = false) } },
                                canUp = index > 0,
                                canDown = index < songs.lastIndex,
                                title = song.title,
                            )
                        }
                    } else null,
                )
            }
            item(key = "actions") {
                Spacer(Modifier.height(Space.S4))
                if (canReorder && songs.size > 1) {
                    WideButton(if (changeOrder) "Done" else "Change Order", if (changeOrder) R.drawable.ic_check else R.drawable.ic_swap_vert, { changeOrder = !changeOrder })
                    Spacer(Modifier.height(Space.S4))
                }
                if (isOwn) {
                    ButtonPair(
                        { m -> MusicButton("Rename List", R.drawable.ic_edit, { askRename = true }, m) },
                        { m -> MusicButton("Delete List", R.drawable.ic_delete, { askDelete = true }, m) },
                    )
                }
                Spacer(Modifier.height(Space.S4))
            }
        }
    }

    if (askRename && playlist != null) {
        NameDialog("Rename List", "Name of the list", playlist!!.name, "Rename", onConfirm = { newName ->
            app.scope.launch { app.library.renameList(playlist!!.id, newName) }
        }, onDismiss = { askRename = false })
    }
    if (askDelete && playlist != null) {
        val id = playlist!!.id
        ConfirmDialog(
            "Delete this list? Songs stay in your library.",
            "Delete List",
            onConfirm = {
                app.scope.launch {
                    app.library.deleteList(id)
                    // A deleted default list goes back to Favourites (PL-7).
                    if (app.settings.current().defaultList == ListRef.Stored(id)) app.settings.setDefaultList(null)
                }
                ui.messages.show("List deleted", R.drawable.ic_delete, "Put Back") {
                    app.scope.launch { app.library.putBackList(id) }
                }
            },
            onDismiss = { askDelete = false },
            detail = "It goes to Recently Removed for 30 days, where you can put it back.",
            confirmIcon = R.drawable.ic_delete,
        )
    }
}

/** Add to List: tap a big list tile (PL-4). */
@Composable
fun AddToListScreen(songId: Long) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    val song by remember(songId) { app.library.song(songId) }.collectAsStateWithLifecycle(initialValue = null)
    val lists by app.library.playlistSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
    var askNew by remember { mutableStateOf(false) }

    fun add(listId: Long, listName: String) {
        app.scope.launch {
            val added = app.library.addToList(listId, songId)
            ui.messages.show(if (added) "Added to $listName" else "Already in $listName", R.drawable.ic_playlist_add)
        }
        nav.popBackStack()
    }

    BackScreen("Add to List") {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = Space.S4, vertical = Space.S2)) {
            item(key = "song") {
                Text(song?.title.orEmpty(), style = MusicType.Heading, modifier = Modifier.padding(bottom = Space.S4))
            }
            val tiles: List<Any> = lists + "new"
            items(tiles.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = Space.S4), horizontalArrangement = Arrangement.spacedBy(Space.S4)) {
                    row.forEach { tile ->
                        if (tile is com.codetiger.mymusicapp.data.db.PlaylistSummary) {
                            val icon = if (tile.builtIn == BuiltIn.FAVOURITES) R.drawable.ic_favorite_filled else R.drawable.ic_music_note
                            ListTile(tile.name, tile.songCount, icon, tile.firstThumbnail, { add(tile.id, tile.name) }, Modifier.weight(1f))
                        } else {
                            NewListTile({ askNew = true }, Modifier.weight(1f))
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    if (askNew) {
        NameDialog("New List", "Name of the list", "", "Make List", onConfirm = { name ->
            app.scope.launch {
                val id = app.library.createList(name)
                app.library.addToList(id, songId)
                ui.messages.show("Added to $name", R.drawable.ic_playlist_add)
            }
            nav.popBackStack()
        }, onDismiss = { askNew = false })
    }
}
