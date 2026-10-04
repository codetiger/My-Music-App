package com.codetiger.mymusicapp.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.codetiger.mymusicapp.data.db.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatSetting { Off, All, One }

/** What the screens show about playback. */
data class PlayerUiState(
    val connected: Boolean = false,
    val currentSongId: Long? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: RepeatSetting = RepeatSetting.Off,
    /** Up Next in play order. */
    val queue: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val ended: Boolean = false,
) {
    val hasSong: Boolean get() = currentSongId != null
}

/** The app's side of the player: a MediaController to [PlaybackService]. */
class PlayerConnection(private val context: Context, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    /** Problems to show as a Message. */
    private val _errors = MutableSharedFlow<PlayerError>(extraBufferCapacity = 4)
    val errors: SharedFlow<PlayerError> = _errors

    enum class PlayerError { NotOnPhone }

    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            update()
            pending.forEach { it(c) }
            pending.clear()
        }, MoreExecutors.directExecutor())
    }

    private fun withController(action: (MediaController) -> Unit) {
        val c = controller
        if (c != null) action(c) else {
            pending += action
            connect()
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update()

        override fun onPlayerError(error: PlaybackException) {
            _errors.tryEmit(PlayerError.NotOnPhone)
        }
    }

    private fun update() {
        val c = controller ?: return
        val order = playOrder(c)
        _state.value = PlayerUiState(
            connected = true,
            currentSongId = c.currentMediaItem?.let { MediaIds.songId(it.mediaId) },
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING && c.playWhenReady,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.takeIf { it > 0 } ?: 0,
            shuffle = c.shuffleModeEnabled,
            repeat = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatSetting.All
                Player.REPEAT_MODE_ONE -> RepeatSetting.One
                else -> RepeatSetting.Off
            },
            queue = order.map { MediaIds.songId(c.getMediaItemAt(it).mediaId) ?: -1 },
            currentIndex = order.indexOf(c.currentMediaItemIndex).coerceAtLeast(0),
            ended = c.playbackState == Player.STATE_ENDED,
        )
        ticker?.cancel()
        if (c.isPlaying) {
            ticker = scope.launch {
                while (isActive) {
                    delay(500)
                    _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0))
                }
            }
        }
    }

    /** Indexes of the timeline in the order they will play (shuffle-aware). */
    private fun playOrder(c: Player): List<Int> {
        val timeline = c.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val order = mutableListOf<Int>()
        var i = timeline.getFirstWindowIndex(c.shuffleModeEnabled)
        while (i != androidx.media3.common.C.INDEX_UNSET && order.size < timeline.windowCount) {
            order += i
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
        }
        return order
    }

    /** Plays [songs] from [startIndex] (PLY-2). Songs that can't play yet are left out. */
    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        val start = songs.getOrNull(startIndex)
        if (start != null && !start.isPlayable) {
            _errors.tryEmit(PlayerError.NotOnPhone)
            return
        }
        val playable = songs.filter { it.isPlayable }
        if (playable.isEmpty()) {
            _errors.tryEmit(PlayerError.NotOnPhone)
            return
        }
        val index = start?.let { s -> playable.indexOfFirst { it.id == s.id } }?.coerceAtLeast(0) ?: 0
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(playable.map { MediaItem.Builder().setMediaId(MediaIds.song(it.id)).build() }, if (shuffle) 0 else index, 0)
            if (shuffle) c.seekToDefaultPosition(c.currentTimeline.getFirstWindowIndex(true).coerceAtLeast(0))
            c.prepare()
            c.play()
        }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() = withController { it.seekToNextMediaItem() }

    /** Previous: back to the start of the song if it has played a few seconds, else the song before. */
    fun previous() = withController { it.seekToPrevious() }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    fun seekBy(deltaMs: Long) = withController { c ->
        val target = (c.currentPosition + deltaMs).coerceIn(0, (c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        c.seekTo(target)
    }

    fun setShuffle(on: Boolean) = withController { it.shuffleModeEnabled = on }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Up Next (PLY-8): positions are in play order. */
    fun playQueueItem(position: Int) = withController { c ->
        playOrder(c).getOrNull(position)?.let { c.seekToDefaultPosition(it); c.play() }
    }

    fun removeQueueItem(position: Int) = withController { c ->
        playOrder(c).getOrNull(position)?.let { c.removeMediaItem(it) }
    }

    /** Moves a song one place up or down in Up Next. */
    fun moveQueueItem(position: Int, up: Boolean) = withController { c ->
        val order = playOrder(c)
        val target = if (up) position - 1 else position + 1
        if (position !in order.indices || target !in order.indices) return@withController
        if (c.shuffleModeEnabled) {
            // A shuffled order can't be rearranged item by item; fix the order first.
            val ids = order.map { c.getMediaItemAt(it) }.toMutableList()
            val currentPos = order.indexOf(c.currentMediaItemIndex)
            val pos = c.currentPosition
            ids.add(target, ids.removeAt(position))
            val newCurrent = when (currentPos) {
                position -> target
                target -> position
                else -> currentPos
            }
            c.shuffleModeEnabled = false
            c.setMediaItems(ids, newCurrent, pos)
        } else {
            c.moveMediaItem(order[position], order[target])
        }
    }

    /** A song left the library: take it out of Up Next; if it was playing, the next one starts (LIB-6). */
    fun removeSong(songId: Long) = withController { c ->
        for (i in c.mediaItemCount - 1 downTo 0) {
            if (MediaIds.songId(c.getMediaItemAt(i).mediaId) == songId) c.removeMediaItem(i)
        }
    }

    fun isPlayingNow(): Boolean = controller?.isPlaying == true
}
