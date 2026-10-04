package com.codetiger.mymusicapp.data

import com.codetiger.mymusicapp.add.DownloadScheduler
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.MusicDatabase
import java.io.File

/**
 * On start: after Auto Backup restores the library on a new phone, songs from links download
 * again; songs from WhatsApp or files can't come back and are named on a Home card (SET-6).
 * Also makes sure every unfinished download is scheduled.
 */
class RestoreCheck(
    private val db: MusicDatabase,
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
    private val scheduler: DownloadScheduler,
) {
    suspend fun run() {
        val songs = db.songs()
        val lost = mutableListOf<com.codetiger.mymusicapp.data.db.Song>()
        val unfinished = mutableListOf<com.codetiger.mymusicapp.data.db.Song>()
        for (song in songs.everything()) {
            val thumbnailMissing = song.thumbnailPath != null && !File(song.thumbnailPath).exists()
            val audioMissing = song.downloadStatus == DownloadStatus.DONE && (song.filePath == null || !File(song.filePath).exists())
            when {
                // A file import stopped part way (older versions added the song first): nothing can finish it.
                !song.sourceType.isLink && song.downloadStatus != DownloadStatus.DONE -> unfinished += song
                audioMissing && song.sourceType.isLink -> {
                    songs.update(song.copy(filePath = null, fileSize = 0, thumbnailPath = null, downloadStatus = DownloadStatus.QUEUED, downloadProgress = 0))
                    if (song.removedAt == null) scheduler.enqueue(song.id)
                }
                audioMissing -> lost += song
                thumbnailMissing -> songs.update(song.copy(thumbnailPath = null))
            }
        }
        if (unfinished.isNotEmpty()) library.deleteSongsNow(unfinished)
        if (lost.isNotEmpty()) {
            val names = lost.filter { it.removedAt == null }.map { it.title }
            library.deleteSongsNow(lost)
            if (names.isNotEmpty()) settings.setRestoreSkippedSongs(settings.current().restoreSkippedSongs + names)
        }
        songs.pendingDownloads().forEach { scheduler.enqueue(it.id) }
    }
}
