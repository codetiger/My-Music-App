package com.codetiger.mymusicapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.BuildConfig
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.db.SourceType
import com.codetiger.mymusicapp.ui.BackScreen
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.components.Avatar
import com.codetiger.mymusicapp.ui.components.ChoiceRow
import com.codetiger.mymusicapp.ui.components.ConfirmDialog
import com.codetiger.mymusicapp.ui.components.NameDialog
import com.codetiger.mymusicapp.ui.components.NoticeCard
import com.codetiger.mymusicapp.ui.components.SectionHeading
import com.codetiger.mymusicapp.ui.components.SettingsRow
import com.codetiger.mymusicapp.ui.components.SongArt
import com.codetiger.mymusicapp.ui.components.SongRow
import com.codetiger.mymusicapp.ui.components.StackedButton
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.rememberDrawing
import com.codetiger.mymusicapp.ui.rememberSettings
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import com.codetiger.mymusicapp.ui.theme.TextSize
import kotlinx.coroutines.launch

private const val ONE_GB = 1024L * 1024 * 1024

@Composable
private fun SettingsPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    BackScreen(title) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
            content = content,
        )
    }
}

private fun TextSize.label() = when (this) {
    TextSize.Normal -> "Normal"
    TextSize.Large -> "Large"
    TextSize.ExtraLarge -> "Extra Large"
}

/** Settings: five items (SET-1). */
@Composable
fun SettingsScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val settings = rememberSettings()
    val lists by app.library.playlistSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
    val used by app.library.bytesUsed.collectAsStateWithLifecycle(initialValue = 0L)
    val removedSongs by app.library.removedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val removedLists by app.library.removedLists.collectAsStateWithLifecycle(initialValue = emptyList())
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val stepsLeft = remember(resumes, settings) { app.phoneSetup.stepsLeft(settings) }
    val drawing = rememberDrawing(settings.drawingVersion)

    val defaultName = when (val d = settings.defaultList) {
        ListRef.AllSongs -> "All Songs"
        is ListRef.Stored -> lists.firstOrNull { it.id == d.playlistId }?.name
        else -> null
    } ?: "Favourites"
    val removedCount = removedSongs.size + removedLists.size

    BackScreen("Settings") {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S2),
        ) {
            SettingsRow("Your Name and Photo", settings.userName ?: "No name yet", { nav.navigate(Routes.YOU) }, leading = { Avatar(drawing) })
            SettingsRow("Text Size", settings.textSize.label(), { nav.navigate(Routes.TEXT_SIZE) })
            SettingsRow("Default Playlist", defaultName, { nav.navigate(Routes.DEFAULT_LIST) })
            SettingsRow(
                "Storage",
                "${Format.size(used)} used" + if (removedCount > 0) " · $removedCount removed" else "",
                { nav.navigate(Routes.STORAGE) },
            )
            SettingsRow(
                "Phone Setup",
                when (stepsLeft) { 0 -> "All done"; 1 -> "1 step left"; else -> "$stepsLeft steps left" },
                { nav.navigate(Routes.setup(Routes.FROM_SETTINGS)) },
                valueIcon = if (stepsLeft == 0) R.drawable.ic_check_circle else R.drawable.ic_schedule,
            )
            Text("Version ${BuildConfig.VERSION_NAME}", style = MusicType.Body, modifier = Modifier.padding(top = Space.S5))
        }
    }
}

/** Your Name and Photo (SET-3). */
@Composable
fun YourNameAndPhotoScreen() {
    val app = LocalApp.current
    val settings = rememberSettings()
    val drawing = rememberDrawing(settings.drawingVersion)
    val pickers = rememberPhotoPickers(Routes.FROM_SETTINGS)
    var askName by remember { mutableStateOf(false) }
    var askRemove by remember { mutableStateOf(false) }

    SettingsPage("Your Name and Photo") {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.S3)) {
            Avatar(drawing, size = Size.ArtHero, contentDescription = "Your photo drawing")
            Text(settings.userName ?: "No name yet", style = MusicType.SongHero, textAlign = TextAlign.Center)
        }
        WideButton("Change Name", R.drawable.ic_edit, { askName = true })
        WideButton("Take New Photo", R.drawable.ic_photo_camera, pickers.takePhoto)
        WideButton("Choose New Photo", R.drawable.ic_image, pickers.choosePhoto)
        if (settings.hasDrawing) WideButton("Remove Photo", R.drawable.ic_delete, { askRemove = true })
    }

    if (askName) {
        NameDialog("Change Name", "Your name", settings.userName.orEmpty(), "Save", onConfirm = { name ->
            app.scope.launch {
                app.settings.setUserName(name)
                // The home-screen picture's label follows the name (FL-10).
                app.shortcut.update(name)
            }
        }, onDismiss = { askName = false })
    }
    if (askRemove) {
        ConfirmDialog(
            "Remove your photo?", "Remove Photo",
            onConfirm = { app.scope.launch { app.photoStore.remove() } },
            onDismiss = { askRemove = false },
            detail = "The music-note logo comes back, here and on the home screen.",
            confirmIcon = R.drawable.ic_delete,
        )
    }
}

/** Text Size (SET-2): applies at once; a sample song row shows the size. */
@Composable
fun TextSizeScreen() {
    val app = LocalApp.current
    val settings = rememberSettings()
    SettingsPage("Text Size") {
        TextSize.entries.forEach { size ->
            ChoiceRow(size.label(), settings.textSize == size, { app.scope.launch { app.settings.setTextSize(size) } })
        }
        SectionHeading("Sample")
        SongRow(
            Song(
                id = -1, title = "Kurai Ondrum Illai", artist = "M. S. Subbulakshmi", sourceType = SourceType.FILE,
                downloadStatus = DownloadStatus.DONE, addedAt = 0,
            ),
            onOpen = {}, onPlay = {},
        )
    }
}

/** Default Playlist: what the big Play button on Home plays (PL-7). */
@Composable
fun DefaultListScreen() {
    val app = LocalApp.current
    val settings = rememberSettings()
    val lists by app.library.playlistSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
    SettingsPage("Default Playlist") {
        Text("The big Play button on Home plays this list.", style = MusicType.Body)
        lists.forEach { list ->
            val selected = if (list.builtIn == BuiltIn.FAVOURITES) settings.defaultList == null || settings.defaultList == ListRef.Stored(list.id)
            else settings.defaultList == ListRef.Stored(list.id)
            ChoiceRow(list.name, selected, {
                app.scope.launch { app.settings.setDefaultList(if (list.builtIn == BuiltIn.FAVOURITES) null else ListRef.Stored(list.id)) }
            })
        }
        ChoiceRow("All Songs", settings.defaultList == ListRef.AllSongs, { app.scope.launch { app.settings.setDefaultList(ListRef.AllSongs) } })
    }
}

/** Storage (SET-4): space used, free space warning, Recently Removed. */
@Composable
fun StorageScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val used by app.library.bytesUsed.collectAsStateWithLifecycle(initialValue = 0L)
    val removedSongs by app.library.removedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val removedLists by app.library.removedLists.collectAsStateWithLifecycle(initialValue = emptyList())
    val free = remember { app.files.audioDir.usableSpace }
    SettingsPage("Storage") {
        Text("Songs on your phone use ${Format.size(used)}.", style = MusicType.Body)
        Text("Free space on your phone: ${Format.size(free)}.", style = MusicType.Body)
        if (free < ONE_GB) {
            NoticeCard(R.drawable.ic_sd_card_alert, "Your phone is nearly full", detail = "Remove songs you don't need, then empty Recently Removed.")
        }
        val parts = listOfNotNull(
            removedSongs.size.takeIf { it > 0 }?.let { Format.songCount(it) },
            removedLists.size.takeIf { it > 0 }?.let { if (it == 1) "1 list" else "$it lists" },
        )
        SettingsRow("Recently Removed", if (parts.isEmpty()) "Empty" else parts.joinToString(", "), { nav.navigate(Routes.REMOVED) })
    }
}

/** Recently Removed (LIB-7): Put Back, or Empty Now. */
@Composable
fun RecentlyRemovedScreen() {
    val app = LocalApp.current
    val ui = LocalUi.current
    val removedSongs by app.library.removedSongs.collectAsStateWithLifecycle(initialValue = emptyList())
    val removedLists by app.library.removedLists.collectAsStateWithLifecycle(initialValue = emptyList())
    var askEmpty by remember { mutableStateOf(false) }

    SettingsPage("Recently Removed") {
        Text("Removed songs and lists stay here for 30 days. Then they are deleted.", style = MusicType.Body)
        if (removedSongs.isEmpty() && removedLists.isEmpty()) {
            Text("Nothing here.", style = MusicType.BodyStrong)
            return@SettingsPage
        }
        removedSongs.forEach { song ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S4)) {
                SongArt(song.thumbnailPath)
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MusicType.BodyStrong)
                    if (song.artist.isNotEmpty()) Text(song.artist, style = MusicType.Body)
                }
                StackedButton("Put Back", R.drawable.ic_restore_from_trash, {
                    app.scope.launch {
                        app.library.putBackSong(song.id)
                        if (song.downloadStatus != DownloadStatus.DONE && song.downloadStatus != DownloadStatus.FAILED) app.scheduler.enqueue(song.id)
                    }
                    ui.messages.show("Song put back in your library", R.drawable.ic_restore_from_trash)
                }, description = "Put back ${song.title}")
            }
        }
        removedLists.forEach { list ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S4)) {
                SongArt(null)
                Column(Modifier.weight(1f)) {
                    Text(list.name, style = MusicType.BodyStrong)
                    Text("List", style = MusicType.Body)
                }
                StackedButton("Put Back", R.drawable.ic_restore_from_trash, {
                    app.scope.launch { app.library.putBackList(list.id) }
                    ui.messages.show("List put back", R.drawable.ic_restore_from_trash)
                }, description = "Put back ${list.name}")
            }
        }
        WideButton("Empty Now", R.drawable.ic_delete, { askEmpty = true })
    }

    if (askEmpty) {
        ConfirmDialog(
            "Delete these for good?", "Delete for Good",
            onConfirm = { app.scope.launch { app.library.emptyRemoved() } },
            onDismiss = { askEmpty = false },
            detail = "They can't be put back after this.",
            confirmIcon = R.drawable.ic_delete,
        )
    }
}

