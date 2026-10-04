package com.codetiger.mymusicapp.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codetiger.mymusicapp.ui.theme.TextSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class AppSettings(
    val userName: String? = null,
    /** Changes whenever the photo drawing changes; 0 means the music-note logo. */
    val drawingVersion: Long = 0,
    val textSize: TextSize = TextSize.Normal,
    val welcomeDone: Boolean = false,
    val homeShortcutAdded: Boolean = false,
    /** Null means Favourites (PL-7). */
    val defaultList: ListRef? = null,
    val dismissedCards: Set<String> = emptySet(),
    val restoreSkippedSongs: List<String> = emptyList(),
    val songSort: SongSort = SongSort.AZ,
    val autostartDone: Boolean = false,
    val androidAutoDone: Boolean = false,
    val lastAppUpdateCheck: Long = 0,
    val lastSeenVersionCode: Int = 0,
) {
    val hasDrawing: Boolean get() = drawingVersion != 0L
}

private val Context.settingsStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val drawingVersion = longPreferencesKey("drawing_version")
        val textSize = stringPreferencesKey("text_size")
        val welcomeDone = booleanPreferencesKey("welcome_done")
        val homeShortcutAdded = booleanPreferencesKey("home_shortcut_added")
        val defaultList = stringPreferencesKey("default_list")
        val dismissedCards = stringSetPreferencesKey("dismissed_cards")
        val restoreSkipped = stringPreferencesKey("restore_skipped_songs")
        val songSort = stringPreferencesKey("song_sort")
        val autostartDone = booleanPreferencesKey("autostart_done")
        val androidAutoDone = booleanPreferencesKey("android_auto_done")
        val lastAppUpdateCheck = longPreferencesKey("last_app_update_check")
        val lastSeenVersionCode = intPreferencesKey("last_seen_version_code")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map(::read)

    suspend fun current(): AppSettings = settings.first()

    private fun read(p: Preferences) = AppSettings(
        userName = p[Keys.userName]?.takeIf { it.isNotBlank() },
        drawingVersion = p[Keys.drawingVersion] ?: 0,
        textSize = p[Keys.textSize]?.let { runCatching { TextSize.valueOf(it) }.getOrNull() } ?: TextSize.Normal,
        welcomeDone = p[Keys.welcomeDone] ?: false,
        homeShortcutAdded = p[Keys.homeShortcutAdded] ?: false,
        defaultList = ListRef.decode(p[Keys.defaultList]),
        dismissedCards = p[Keys.dismissedCards] ?: emptySet(),
        restoreSkippedSongs = p[Keys.restoreSkipped]?.split('\n')?.filter { it.isNotEmpty() } ?: emptyList(),
        songSort = p[Keys.songSort]?.let { runCatching { SongSort.valueOf(it) }.getOrNull() } ?: SongSort.AZ,
        autostartDone = p[Keys.autostartDone] ?: false,
        androidAutoDone = p[Keys.androidAutoDone] ?: false,
        lastAppUpdateCheck = p[Keys.lastAppUpdateCheck] ?: 0,
        lastSeenVersionCode = p[Keys.lastSeenVersionCode] ?: 0,
    )

    suspend fun setUserName(name: String?) = context.settingsStore.edit {
        if (name.isNullOrBlank()) it.remove(Keys.userName) else it[Keys.userName] = name.trim()
    }

    suspend fun setDrawingVersion(version: Long) = context.settingsStore.edit { it[Keys.drawingVersion] = version }
    suspend fun setTextSize(size: TextSize) = context.settingsStore.edit { it[Keys.textSize] = size.name }
    suspend fun setWelcomeDone() = context.settingsStore.edit { it[Keys.welcomeDone] = true }
    suspend fun setHomeShortcutAdded(added: Boolean) = context.settingsStore.edit { it[Keys.homeShortcutAdded] = added }
    suspend fun setSongSort(sort: SongSort) = context.settingsStore.edit { it[Keys.songSort] = sort.name }
    suspend fun setAutostartDone() = context.settingsStore.edit { it[Keys.autostartDone] = true }
    suspend fun setAndroidAutoDone() = context.settingsStore.edit { it[Keys.androidAutoDone] = true }
    suspend fun setLastAppUpdateCheck(at: Long) = context.settingsStore.edit { it[Keys.lastAppUpdateCheck] = at }
    suspend fun setLastSeenVersionCode(code: Int) = context.settingsStore.edit { it[Keys.lastSeenVersionCode] = code }

    suspend fun setDefaultList(ref: ListRef?) = context.settingsStore.edit {
        if (ref == null) it.remove(Keys.defaultList) else it[Keys.defaultList] = ref.encode()
    }

    suspend fun dismissCard(card: String) = context.settingsStore.edit {
        it[Keys.dismissedCards] = (it[Keys.dismissedCards] ?: emptySet()) + card
    }

    suspend fun setRestoreSkippedSongs(names: List<String>) = context.settingsStore.edit {
        if (names.isEmpty()) it.remove(Keys.restoreSkipped) else it[Keys.restoreSkipped] = names.joinToString("\n")
    }
}
