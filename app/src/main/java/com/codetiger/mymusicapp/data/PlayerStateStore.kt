package com.codetiger.mymusicapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** What was playing, so the app reopens paused at the same place (PLY-9). */
data class SavedPlayerState(
    val queue: List<Long>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    /** Player.REPEAT_MODE_* */
    val repeatMode: Int,
)

private val Context.playerStore by preferencesDataStore("player_state")

class PlayerStateStore(private val context: Context) {
    private object Keys {
        val queue = stringPreferencesKey("queue")
        val index = intPreferencesKey("index")
        val position = longPreferencesKey("position_ms")
        val shuffle = booleanPreferencesKey("shuffle")
        val repeat = intPreferencesKey("repeat")
    }

    suspend fun load(): SavedPlayerState? {
        val p = context.playerStore.data.first()
        val queue = p[Keys.queue]?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
        if (queue.isEmpty()) return null
        return SavedPlayerState(
            queue = queue,
            index = (p[Keys.index] ?: 0).coerceIn(0, queue.lastIndex),
            positionMs = p[Keys.position] ?: 0,
            shuffle = p[Keys.shuffle] ?: false,
            repeatMode = p[Keys.repeat] ?: 0,
        )
    }

    suspend fun save(state: SavedPlayerState) = context.playerStore.edit {
        it[Keys.queue] = state.queue.joinToString(",")
        it[Keys.index] = state.index
        it[Keys.position] = state.positionMs
        it[Keys.shuffle] = state.shuffle
        it[Keys.repeat] = state.repeatMode
    }

    suspend fun clear() = context.playerStore.edit { it.clear() }
}
