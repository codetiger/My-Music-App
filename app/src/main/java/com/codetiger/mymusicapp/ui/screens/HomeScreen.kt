package com.codetiger.mymusicapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.AppSettings
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.SongSort
import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.PlaylistSummary
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.matching
import com.codetiger.mymusicapp.data.sortedFor
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.TabScreen
import com.codetiger.mymusicapp.ui.appTitle
import com.codetiger.mymusicapp.ui.components.Avatar
import com.codetiger.mymusicapp.ui.components.ChoicePills
import com.codetiger.mymusicapp.ui.components.ListDialog
import com.codetiger.mymusicapp.ui.components.ListTile
import com.codetiger.mymusicapp.ui.components.MusicButton
import com.codetiger.mymusicapp.ui.components.NameDialog
import com.codetiger.mymusicapp.ui.components.NewListTile
import com.codetiger.mymusicapp.ui.components.NoticeCard
import com.codetiger.mymusicapp.ui.components.PageHeader
import com.codetiger.mymusicapp.ui.components.SearchField
import com.codetiger.mymusicapp.ui.components.SectionHeading
import com.codetiger.mymusicapp.ui.components.SongRow
import com.codetiger.mymusicapp.ui.components.Tab
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.rememberDrawing
import com.codetiger.mymusicapp.ui.rememberSettings
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import com.codetiger.mymusicapp.update.AppUpdater
import kotlinx.coroutines.launch

/** The cards Home can show, highest first; at most one at a time (HOME-4). */
private enum class HomeCard { Update, Setup, RestoreSkipped, Empty, Shortcut }

/** Home: the one screen for lists and songs. */
@Composable
fun HomeScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val settings = rememberSettings()
    val songs by app.library.library.collectAsStateWithLifecycle(initialValue = null)
    val lists by app.library.playlistSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
    val recent by app.library.recentlyPlayed.collectAsStateWithLifecycle(initialValue = emptyList())
    val update by app.appUpdater.state.collectAsStateWithLifecycle()
    val drawing = rememberDrawing(settings.drawingVersion)
    var query by rememberSaveable { mutableStateOf("") }
    var askNewList by remember { mutableStateOf(false) }
    var showSkipped by remember { mutableStateOf(false) }
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }

    val library = songs ?: emptyList()
    val shown = remember(library, settings.songSort, query) { library.matching(query).sortedFor(settings.songSort) }
    val setupOff = remember(resumes, settings) { app.phoneSetup.checkableStepOff(settings) }
    val card = remember(update, setupOff, settings, songs, resumes) {
        pickCard(update, setupOff, settings, songs, app.shortcut.isSupported && !app.shortcut.isPinned)
    }

    // Home has no main button of its own: the bar's Play / Pause is it (HOME-2).
    TabScreen(Tab.Home, primaryPlay = true) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Space.S4, vertical = Space.S2)) {
            // Whose app this is greets you on opening, then scrolls away with the page (HOME-1).
            item(key = "header") {
                PageHeader(appTitle(settings), leading = { Avatar(drawing, size = Size.AvatarLg) })
            }
            if (card != null) {
                item(key = "card") {
                    HomeNotice(card, settings, onSeeNames = { showSkipped = true })
                }
            }
            item(key = "lists-heading") { SectionHeading("My Lists", Modifier.padding(bottom = Space.S4)) }
            val tiles = buildTiles(lists, recent)
            items(tiles.chunked(2), key = { row -> row.joinToString { it.key } }) { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = Space.S4), horizontalArrangement = Arrangement.spacedBy(Space.S4)) {
                    row.forEach { tile ->
                        if (tile.ref == null) {
                            NewListTile({ askNewList = true }, Modifier.weight(1f))
                        } else {
                            ListTile(tile.name, tile.count, tile.icon, tile.picture, { nav.navigate(Routes.list(tile.ref)) }, Modifier.weight(1f))
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item(key = "songs-heading") { SectionHeading("Songs", Modifier.padding(bottom = Space.S4)) }
            if (songs != null && library.isEmpty()) {
                item(key = "no-songs") { Text("Songs you add will be listed here.", style = MusicType.Body) }
            } else if (songs != null) {
                item(key = "search") { SearchField(query, { query = it }, Modifier.padding(bottom = Space.S4)) }
                item(key = "sort") {
                    ChoicePills(
                        "Sort",
                        listOf(SongSort.AZ to "A–Z", SongSort.NEWEST to "Newest", SongSort.MOST_PLAYED to "Most Played"),
                        settings.songSort,
                        { app.scope.launch { app.settings.setSongSort(it) } },
                        Modifier.padding(bottom = Space.S4),
                    )
                }
                if (shown.isEmpty()) {
                    item(key = "no-match") {
                        Text("No songs match “$query”.", style = MusicType.Body, modifier = Modifier.padding(bottom = Space.S4))
                        MusicButton("Clear Search", R.drawable.ic_close, { query = "" })
                    }
                }
                items(shown, key = { it.id }) { song ->
                    SongRow(
                        song,
                        onOpen = { nav.navigate(Routes.song(song.id)) },
                        onPlay = { app.player.play(shown, shown.indexOf(song)) },
                        modifier = Modifier.padding(bottom = Space.S2),
                    )
                }
            }
            // Settings is set up once, so it waits at the end of Home rather than at the top (SET-1).
            item(key = "settings") {
                WideButton(stringResource(R.string.action_settings), R.drawable.ic_settings, { nav.navigate(Routes.SETTINGS) }, Modifier.padding(top = Space.S6))
            }
            item(key = "end") { Spacer(Modifier.height(Space.S4)) }
        }
    }

    if (askNewList) {
        NameDialog("New List", "Name of the list", "", "Make List", R.drawable.ic_add, onConfirm = { name ->
            app.scope.launch {
                val id = app.library.createList(name)
                nav.navigate(Routes.list(ListRef.Stored(id)))
            }
        }, onDismiss = { askNewList = false })
    }
    if (showSkipped) {
        ListDialog("Songs that couldn't be moved", settings.restoreSkippedSongs) { showSkipped = false }
    }
}

private fun pickCard(
    update: AppUpdater.State,
    setupOff: Boolean,
    settings: AppSettings,
    songs: List<Song>?,
    shortcutPossible: Boolean,
): HomeCard? = when {
    update is AppUpdater.State.NeedsTap -> HomeCard.Update
    setupOff && settings.welcomeDone -> HomeCard.Setup
    settings.restoreSkippedSongs.isNotEmpty() -> HomeCard.RestoreSkipped
    songs != null && songs.isEmpty() -> HomeCard.Empty
    shortcutPossible && settings.hasDrawing && !settings.homeShortcutAdded && "shortcut" !in settings.dismissedCards -> HomeCard.Shortcut
    else -> null
}

@Composable
private fun HomeNotice(card: HomeCard, settings: AppSettings, onSeeNames: () -> Unit) {
    val app = LocalApp.current
    val nav = LocalNav.current
    when (card) {
        HomeCard.Update -> NoticeCard(R.drawable.ic_system_update, "A new version is ready") {
            MusicButton("Install", R.drawable.ic_download, { app.scope.launch { app.appUpdater.installNow() } }, onFill = true)
        }
        HomeCard.Setup -> NoticeCard(R.drawable.ic_battery_alert, "Music may stop when the screen is off") {
            MusicButton("Finish Setup", R.drawable.ic_settings, { nav.navigate(Routes.setup(Routes.FROM_SETTINGS)) }, onFill = true)
        }
        HomeCard.RestoreSkipped -> {
            val n = settings.restoreSkippedSongs.size
            NoticeCard(R.drawable.ic_info, if (n == 1) "1 song from WhatsApp or files couldn't be moved to this phone" else "$n songs from WhatsApp or files couldn't be moved to this phone") {
                MusicButton("See Names", R.drawable.ic_list, onSeeNames, onFill = true)
                MusicButton("OK", R.drawable.ic_check, { app.scope.launch { app.settings.setRestoreSkippedSongs(emptyList()) } }, onFill = true)
            }
        }
        HomeCard.Empty -> NoticeCard(R.drawable.ic_add_circle, "Tap Add Song to save your first song") {
            MusicButton("Add Song", R.drawable.ic_add_circle, { nav.navigate(Routes.ADD) { launchSingleTop = true } }, onFill = true)
        }
        HomeCard.Shortcut -> NoticeCard(R.drawable.ic_add_to_home_screen, "Put your picture on the home screen?") {
            MusicButton("Add", R.drawable.ic_add, {
                if (app.shortcut.request(settings.userName)) app.scope.launch { app.settings.setHomeShortcutAdded(true) }
            }, onFill = true)
            MusicButton("No Thanks", R.drawable.ic_close, { app.scope.launch { app.settings.dismissCard("shortcut") } }, onFill = true)
        }
    }
}

private data class Tile(val key: String, val ref: ListRef?, val name: String, val count: Int, val icon: Int, val picture: String?)

/** Favourites, Recently Played, your own lists, then New List. No All Songs tile: every song is below. */
private fun buildTiles(lists: List<PlaylistSummary>, recent: List<Song>): List<Tile> = buildList {
    lists.firstOrNull { it.builtIn == BuiltIn.FAVOURITES }?.let {
        add(Tile("fav", ListRef.Stored(it.id), it.name, it.songCount, R.drawable.ic_favorite_filled, it.firstThumbnail))
    }
    add(Tile("recent", ListRef.RecentlyPlayed, "Recently Played", recent.size, R.drawable.ic_history, recent.firstOrNull()?.thumbnailPath))
    lists.filter { it.builtIn == BuiltIn.NONE }.forEach {
        add(Tile("list-${it.id}", ListRef.Stored(it.id), it.name, it.songCount, R.drawable.ic_music_note, it.firstThumbnail))
    }
    add(Tile("new", null, "New List", 0, R.drawable.ic_add, null))
}
