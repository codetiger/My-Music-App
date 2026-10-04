package com.codetiger.mymusicapp.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionError
import com.codetiger.mymusicapp.MainActivity
import com.codetiger.mymusicapp.MyMusicApplication
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.SavedPlayerState
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Plays music with the screen off and serves the lock screen, notification, Bluetooth and
 * Android Auto (PLY-5, PLY-6). Reopens paused where it stopped and never starts by itself (PLY-9).
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private val app by lazy { (application as MyMusicApplication).container }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var browser: LibraryBrowser
    private var restored = false
    private var tickJob: Job? = null

    // Play counting (LIB-3): once 30 s have played, or half the song if it is shorter.
    private var listenedMs = 0L
    private var counted = false

    override fun onCreate() {
        super.onCreate()
        browser = LibraryBrowser(app.library, app.settings)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(SongDataSource.Factory(app.db.songs(), app.activeDownloads)))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player.addListener(PlayerListener())

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_PLAYER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaLibrarySession.Builder(this, player, Callback())
            .setSessionActivity(openApp)
            .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_music_note) },
        )
        scope.launch { restore() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        app.playbackActive.set(false)
        runBlocking { saveState() }
        scope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    private suspend fun restore() {
        val saved = app.playerState.load() ?: run { restored = true; return }
        val items = app.library.getSongs(saved.queue).filter { it.removedAt == null }.map { it.toMediaItem() }
        if (items.isNotEmpty() && player.mediaItemCount == 0) {
            val currentId = saved.queue.getOrNull(saved.index)
            val index = items.indexOfFirst { MediaIds.songId(it.mediaId) == currentId }.coerceAtLeast(0)
            player.setMediaItems(items, index, if (index == saved.index) saved.positionMs else 0)
            player.shuffleModeEnabled = saved.shuffle
            player.repeatMode = saved.repeatMode
            player.playWhenReady = false
            player.prepare()
        }
        restored = true
    }

    private suspend fun saveState() {
        if (!restored) return
        val ids = (0 until player.mediaItemCount).mapNotNull { MediaIds.songId(player.getMediaItemAt(it).mediaId) }
        if (ids.isEmpty() || player.playbackState == Player.STATE_ENDED) {
            app.playerState.clear()
            return
        }
        app.playerState.save(
            SavedPlayerState(
                queue = ids,
                index = player.currentMediaItemIndex,
                positionMs = player.currentPosition,
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
            ),
        )
    }

    private inner class PlayerListener : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            listenedMs = 0
            counted = false
            scope.launch { saveState() }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            app.playbackActive.set(isPlaying)
            tickJob?.cancel()
            if (isPlaying) tickJob = scope.launch { tick() }
            scope.launch { saveState() }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            scope.launch { saveState() }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            scope.launch { saveState() }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) scope.launch { saveState() }
        }
    }

    private suspend fun tick() {
        var sinceSave = 0L
        while (scope.isActive) {
            delay(TICK_MS)
            listenedMs += TICK_MS
            sinceSave += TICK_MS
            val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            if (!counted && listenedMs >= minOf(COUNT_AFTER_MS, duration / 2)) {
                counted = true
                MediaIds.songId(player.currentMediaItem?.mediaId.orEmpty())?.let { app.library.recordPlay(it) }
            }
            if (sinceSave >= SAVE_EVERY_MS) {
                sinceSave = 0
                saveState()
            }
        }
    }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            scope.future { LibraryResult.ofItem(this@PlaybackService.browser.root(), params) }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val children = this@PlaybackService.browser.children(parentId)
            val from = (page * pageSize).coerceAtMost(children.size)
            val to = (from + pageSize).coerceAtMost(children.size)
            LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(from, to)), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            this@PlaybackService.browser.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        }

        /** Items from the app or a car arrive as IDs; turn them into playable songs. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = scope.future {
            val ids = mediaItems.mapNotNull { MediaIds.songId(it.mediaId) }
            app.library.getSongs(ids).filter { it.removedAt == null }.map { it.toMediaItem() }
        }

        /** Choosing one song in a car list plays it and then the rest of that list (PLY-2). */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val single = mediaItems.singleOrNull()
            val context = single?.let { MediaIds.context(it.mediaId) }
            if (single != null && context != null) {
                val songId = MediaIds.songId(single.mediaId)
                val list = app.library.currentSongsOf(context, app.settings.current().songSort).filter { it.isPlayable }
                val index = list.indexOfFirst { it.id == songId }.coerceAtLeast(0)
                MediaItemsWithStartPosition(list.map { it.toMediaItem() }, index, C.TIME_UNSET)
            } else {
                val ids = mediaItems.mapNotNull { MediaIds.songId(it.mediaId) }
                val resolved = app.library.getSongs(ids).filter { it.removedAt == null }.map { it.toMediaItem() }
                MediaItemsWithStartPosition(resolved, startIndex.coerceIn(0, (resolved.size - 1).coerceAtLeast(0)), startPositionMs)
            }
        }

        /** Bluetooth play button with the app closed: carry on with what was playing. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaItemsWithStartPosition> = scope.future {
            val saved = app.playerState.load()
            val songs = saved?.let { s -> app.library.getSongs(s.queue).filter { it.removedAt == null } }.orEmpty()
            if (saved == null || songs.isEmpty()) {
                val fallback = app.library.currentSongsOf(ListRef.AllSongs).filter { it.isPlayable }
                MediaItemsWithStartPosition(fallback.map { it.toMediaItem() }, 0, 0)
            } else {
                val currentId = saved.queue.getOrNull(saved.index)
                val index = songs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
                MediaItemsWithStartPosition(songs.map { it.toMediaItem() }, index, saved.positionMs)
            }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
        const val COUNT_AFTER_MS = 30_000L
        const val SAVE_EVERY_MS = 5_000L
    }
}
