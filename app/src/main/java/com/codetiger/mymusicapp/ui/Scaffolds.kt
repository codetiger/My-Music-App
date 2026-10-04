package com.codetiger.mymusicapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.AppSettings
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.defaultListOf
import com.codetiger.mymusicapp.ui.components.BackTitleBar
import com.codetiger.mymusicapp.ui.components.MessageBar
import com.codetiger.mymusicapp.ui.components.NowPlayingBar
import com.codetiger.mymusicapp.ui.components.PlayListBar
import com.codetiger.mymusicapp.ui.components.Tab
import com.codetiger.mymusicapp.ui.components.TabBar
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun rememberSettings(): AppSettings {
    val app = LocalApp.current
    val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    return settings
}

/** "Murali's Music App", or "My Music App" when no name was given (FL-2). */
@Composable
fun appTitle(settings: AppSettings): String =
    settings.userName?.let { stringResource(R.string.app_title_named, it) } ?: stringResource(R.string.app_title_default)

/** The saved photo drawing, reloaded whenever it changes. Null means the music-note logo. */
@Composable
fun rememberDrawing(version: Long): ImageBitmap? {
    val app = LocalApp.current
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(version) {
        bitmap = if (version == 0L) null else app.photoStore.load()?.asImageBitmap()
    }
    return bitmap
}

/**
 * Home and Add Song: the page (which starts with its own PageHeader), Message, Now Playing bar,
 * tabs. No fixed top bar: it would only repeat what the selected tab already says. [primaryPlay]
 * makes the bar's Play / Pause the accent, for a screen with no main action of its own (Home).
 */
@Composable
fun TabScreen(tab: Tab, primaryPlay: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth()) { content() }
        BottomArea(showNowPlaying = true, primaryPlay = primaryPlay, offerDefaultList = tab == Tab.Home)
        TabBar(selected = tab, onSelect = { selected ->
            // Home is the root; Add Song sits on top of it. (Saved tab state would bring Add Song
            // back when Home is chosen, since Home is also the start screen.)
            if (selected == Tab.Home) {
                if (!nav.popBackStack(Routes.HOME, inclusive = false)) {
                    nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                }
            } else {
                nav.navigate(selected.route) {
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
            }
        })
    }
}

/** Every other screen: Back and the screen title, then the content. */
@Composable
fun BackScreen(
    title: String,
    showNowPlaying: Boolean = false,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize()) {
        BackTitleBar(title, onBack = onBack ?: { nav.popBackStack(); Unit })
        Column(Modifier.weight(1f).fillMaxWidth()) { content() }
        BottomArea(showNowPlaying)
    }
}

@Composable
private fun BottomArea(showNowPlaying: Boolean, primaryPlay: Boolean = false, offerDefaultList: Boolean = false) {
    val ui = LocalUi.current
    Column(Modifier.fillMaxWidth().padding(bottom = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S3)) {
        MessageBar(ui.messages)
        if (showNowPlaying) NowPlayingArea(primaryPlay, offerDefaultList)
    }
}

/**
 * The current song while it plays or was left part-way. Otherwise, once the player is ready and on
 * Home only ([offerDefaultList]), the default list to play (HOME-2); a finished list counts as
 * nothing loaded, so Play doesn't restart it.
 */
@Composable
private fun NowPlayingArea(primaryPlay: Boolean, offerDefaultList: Boolean) {
    val app = LocalApp.current
    val ui = LocalUi.current
    val nav = LocalNav.current
    val state by app.player.state.collectAsStateWithLifecycle()
    if (state.hasSong && !state.ended) {
        val songFlow = remember(state.currentSongId) { state.currentSongId?.let { app.library.song(it) } ?: flowOf<Song?>(null) }
        val song by songFlow.collectAsStateWithLifecycle(initialValue = null)
        song?.let {
            NowPlayingBar(
                song = it,
                isPlaying = state.isPlaying,
                progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
                onOpen = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } },
                onPlayPause = { app.player.togglePlay() },
                primary = primaryPlay,
            )
        }
    } else if (offerDefaultList && state.connected) {
        val settings = rememberSettings()
        val lists by app.library.playlistSummaries.collectAsStateWithLifecycle(initialValue = null)
        val default = lists?.let { defaultListOf(settings.defaultList, it) } ?: return
        PlayListBar(default.name, default.picture, primary = primaryPlay, onPlay = {
            app.scope.launch {
                val songs = app.library.currentSongsOf(default.ref, settings.songSort)
                if (songs.isEmpty()) ui.messages.show("Tap Add Song to save your first song", R.drawable.ic_add_circle)
                else app.player.play(songs)
            }
        })
    }
}

/** Standard body padding for scrolling screens. */
val BodyPadding = Modifier.padding(horizontal = Space.S4)
