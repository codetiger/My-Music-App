package com.codetiger.mymusicapp.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SourceType { YOUTUBE, FACEBOOK, INSTAGRAM, MP3_LINK, WHATSAPP, FILE;

    /** Songs from a link can be downloaded again; WhatsApp and file songs cannot. */
    val isLink: Boolean get() = this == YOUTUBE || this == FACEBOOK || this == INSTAGRAM || this == MP3_LINK
}

enum class DownloadStatus { QUEUED, DOWNLOADING, WAITING_RETRY, DONE, FAILED }

enum class FailureReason { PRIVATE, REMOVED, UNSUPPORTED, GAVE_UP;

    /** Gave up after 7 days of retries: Try Again is offered. The others can never work. */
    val canRetry: Boolean get() = this == GAVE_UP
}

@Entity(
    tableName = "song",
    indices = [Index("source_type", "source_id"), Index("file_hash"), Index("removed_at")],
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "",
    @ColumnInfo(name = "source_type") val sourceType: SourceType,
    @ColumnInfo(name = "source_url") val sourceUrl: String? = null,
    /** The site's video ID (yt-dlp's extractor key + id), used to spot duplicates. */
    @ColumnInfo(name = "source_id") val sourceId: String? = null,
    @ColumnInfo(name = "file_hash") val fileHash: String? = null,
    @ColumnInfo(name = "file_path") val filePath: String? = null,
    @ColumnInfo(name = "file_size") val fileSize: Long = 0,
    @ColumnInfo(name = "download_status") val downloadStatus: DownloadStatus,
    @ColumnInfo(name = "download_progress") val downloadProgress: Int = 0,
    @ColumnInfo(name = "failure_reason") val failureReason: FailureReason? = null,
    @ColumnInfo(name = "first_failed_at") val firstFailedAt: Long? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0,
    @ColumnInfo(name = "thumbnail_path") val thumbnailPath: String? = null,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "play_count") val playCount: Int = 0,
    @ColumnInfo(name = "last_played_at") val lastPlayedAt: Long? = null,
    @ColumnInfo(name = "removed_at") val removedAt: Long? = null,
)

enum class BuiltIn { NONE, FAVOURITES }

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "built_in") val builtIn: BuiltIn = BuiltIn.NONE,
    val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "removed_at") val removedAt: Long? = null,
)

/** Rows stay while a song or list is in Recently Removed, so Put Back restores them. */
@Entity(
    tableName = "playlist_song",
    primaryKeys = ["playlist_id", "song_id"],
    indices = [Index("song_id")],
)
data class PlaylistSong(
    @ColumnInfo(name = "playlist_id") val playlistId: Long,
    @ColumnInfo(name = "song_id") val songId: Long,
    val position: Int,
)

/** A list tile: the list, how many songs it holds and its first song's picture. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "built_in") val builtIn: BuiltIn,
    @ColumnInfo(name = "song_count") val songCount: Int,
    @ColumnInfo(name = "first_thumbnail") val firstThumbnail: String?,
)
