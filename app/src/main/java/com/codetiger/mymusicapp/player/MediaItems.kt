package com.codetiger.mymusicapp.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.Song
import java.io.File

/**
 * Media IDs: "song:<id>" for a song, optionally "song:<id>@<list>" when chosen from a list in
 * Android Auto, so the rest of that list plays after it. Browsable lists use [ListRef.encode].
 */
object MediaIds {
    const val ROOT = "root"
    private const val SONG = "song:"

    fun song(songId: Long, from: ListRef? = null) = SONG + songId + (from?.let { "@" + it.encode() } ?: "")

    fun songId(mediaId: String): Long? =
        if (mediaId.startsWith(SONG)) mediaId.removePrefix(SONG).substringBefore('@').toLongOrNull() else null

    fun context(mediaId: String): ListRef? =
        if (mediaId.startsWith(SONG) && '@' in mediaId) ListRef.decode(mediaId.substringAfter('@')) else null

    fun songUri(songId: Long): Uri = Uri.parse("mymusic://song/$songId")

    fun songIdFromUri(uri: Uri): Long? = if (uri.scheme == "mymusic" && uri.host == "song") uri.lastPathSegment?.toLongOrNull() else null
}

/** Songs that can be played now: saved, or still arriving (ADD-11). */
val Song.isPlayable: Boolean
    get() = when (downloadStatus) {
        DownloadStatus.DONE -> filePath != null
        DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> true
        DownloadStatus.WAITING_RETRY, DownloadStatus.FAILED -> false
    }

fun Song.toMediaItem(from: ListRef? = null): MediaItem = MediaItem.Builder()
    .setMediaId(MediaIds.song(id, from))
    .setUri(MediaIds.songUri(id))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifEmpty { null })
            .setDisplayTitle(title)
            .setArtworkUri(thumbnailPath?.let { Uri.fromFile(File(it)) })
            .setDurationMs(durationMs.takeIf { it > 0 })
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build(),
    )
    .build()

fun browsableItem(id: String, title: String, mediaType: Int = MediaMetadata.MEDIA_TYPE_PLAYLIST): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(mediaType)
                .build(),
        )
        .build()
