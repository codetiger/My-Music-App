package com.codetiger.mymusicapp.add

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.codetiger.mymusicapp.MyMusicApplication
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.FailureReason
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.downloader.DownloadError
import com.codetiger.mymusicapp.downloader.HttpDownloader
import com.codetiger.mymusicapp.downloader.MediaInfo
import com.codetiger.mymusicapp.util.DebugLog
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Downloads one song. Can't-download-now problems retry on their own for 7 days; can-never-
 * download problems stop at once (ADD-12, ADD-13). While it runs, the player can read the file.
 */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val app = (context.applicationContext as MyMusicApplication).container
    private val songs = app.db.songs()

    override suspend fun doWork(): Result {
        val songId = inputData.getLong(KEY_SONG_ID, -1)
        val song = songs.get(songId) ?: return Result.success()
        if (song.removedAt != null || song.downloadStatus == DownloadStatus.DONE) return Result.success()
        val url = song.sourceUrl ?: return Result.success()

        runCatching { setForeground(foregroundInfo()) }
        songs.setStatus(songId, DownloadStatus.DOWNLOADING, song.downloadProgress)
        return try {
            download(song, url)
            app.ytDlpUpdater.recordSiteSuccess()
            Result.success()
        } catch (e: DownloadError.Permanent) {
            DebugLog.e("Song $songId can never download", e)
            fail(songId, e.reason)
            Result.success()
        } catch (e: IOException) {
            DebugLog.e("Song $songId will retry", e)
            if (e is DownloadError.Site) app.infoCache.drop(url)
            if (e is DownloadError.Site) app.ytDlpUpdater.recordSiteFailure()
            waitOrGiveUp(songId)
        }
    }

    private suspend fun download(song: Song, url: String) = coroutineScope {
        // One writer for "Downloading 40%", stopped before the final state is written.
        val writer = launch {
            progress.filter { it >= 0 }.collect { songs.setStatus(song.id, DownloadStatus.DOWNLOADING, it) }
        }
        try {
            downloadAndSave(song, url)
        } finally {
            writer.cancelAndJoin()
        }
        markDone(song.id)
    }

    private suspend fun downloadAndSave(song: Song, url: String) {
        val info = app.infoCache.get(url) ?: app.ytDlp.readInfo(url, song.sourceType).also { app.infoCache.put(url, it) }
        if (info.isLive || info.isPlaylist) throw DownloadError.Permanent(FailureReason.UNSUPPORTED, "live or playlist")
        fillDetails(song, info)
        checkSpace(info.estimatedBytes)

        val format = info.format
        val viaYtDlp = suspend {
            app.ytDlp.download(url, app.files.audioDir, song.id.toString()) { done, total -> reportProgress(done, total) }
        }
        val saved: File = if (format != null && format.isDirect && !format.hasVideo) {
            try {
                downloadDirect(song.id, format)
            } catch (e: DownloadError.Site) {
                // The site refused our request; yt-dlp's own downloader knows more tricks.
                app.files.partFile(song.id, format.ext).delete()
                viaYtDlp()
            }
        } else {
            viaYtDlp()
        }
        savedFile = saved
    }

    private var savedFile: File? = null

    private suspend fun markDone(songId: Long) {
        val saved = checkNotNull(savedFile)
        val current = songs.get(songId) ?: return
        songs.update(
            current.copy(
                filePath = saved.absolutePath,
                fileSize = saved.length(),
                downloadStatus = DownloadStatus.DONE,
                downloadProgress = 100,
                failureReason = null,
                firstFailedAt = null,
            ),
        )
    }

    /** Our own downloader, so the song plays while it arrives (ADD-11). */
    private suspend fun downloadDirect(songId: Long, format: MediaInfo.AudioFormat): File {
        val part = app.files.partFile(songId, format.ext)
        val target = app.files.audioFile(songId, format.ext)
        val entry = app.activeDownloads.start(songId, part, if (part.exists()) part.length() else 0, format.sizeBytes)
        try {
            app.httpDownloader.download(format, part) { written, total ->
                app.activeDownloads.progress(entry, written, total)
                reportProgress(written, total)
            }
        } catch (e: IOException) {
            app.activeDownloads.fail(entry, e)
            throw e
        } catch (e: Exception) {
            app.activeDownloads.fail(entry, IOException(e))
            throw e
        }
        if (!part.renameTo(target)) {
            app.activeDownloads.fail(entry, IOException("Can't save file"))
            throw DownloadError.NoSpace("rename failed")
        }
        app.activeDownloads.finish(entry)
        return target
    }

    private val progress = MutableStateFlow(-1)

    private fun reportProgress(done: Long, total: Long?) {
        progress.value = if (total != null && total > 0) (done * 100 / total).toInt().coerceIn(0, 99) else 0
    }

    /** Fills in what the preview couldn't (a link saved while offline) and saves the picture. */
    private suspend fun fillDetails(song: Song, info: MediaInfo) {
        var updated = song
        if (song.title == pendingTitle(song.sourceType)) updated = updated.copy(title = info.title)
        if (song.artist.isEmpty()) updated = updated.copy(artist = info.artist)
        if (song.durationMs == 0L) updated = updated.copy(durationMs = info.durationMs)
        if (song.sourceId == null) updated = updated.copy(sourceId = info.sourceId)
        if (song.thumbnailPath == null && info.thumbnailUrl != null) {
            runCatching {
                val file = app.files.picture(song.id)
                val connection = URL(info.thumbnailUrl).openConnection().apply { connectTimeout = 10_000; readTimeout = 15_000 }
                connection.getInputStream().use { input -> file.outputStream().use { input.copyTo(it) } }
                updated = updated.copy(thumbnailPath = file.absolutePath)
            }
        }
        if (updated != song) songs.update(updated.copy(downloadStatus = DownloadStatus.DOWNLOADING))
    }

    private fun checkSpace(needed: Long) {
        val free = app.files.audioDir.usableSpace
        if (free < needed + MIN_FREE_BYTES) throw DownloadError.NoSpace("free $free, need $needed")
    }

    private suspend fun fail(songId: Long, reason: FailureReason) {
        val song = songs.get(songId) ?: return
        songs.update(song.copy(downloadStatus = DownloadStatus.FAILED, failureReason = reason, downloadProgress = 0))
    }

    private suspend fun waitOrGiveUp(songId: Long): Result {
        val song = songs.get(songId) ?: return Result.success()
        val now = System.currentTimeMillis()
        val firstFailed = song.firstFailedAt ?: now
        if (now - firstFailed > TimeUnit.DAYS.toMillis(RETRY_DAYS)) {
            songs.update(song.copy(downloadStatus = DownloadStatus.FAILED, failureReason = FailureReason.GAVE_UP))
            return Result.success()
        }
        songs.update(song.copy(downloadStatus = DownloadStatus.WAITING_RETRY, firstFailedAt = firstFailed))
        return Result.retry()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo()

    private fun foregroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, applicationContext.getString(R.string.channel_downloads), NotificationManager.IMPORTANCE_LOW),
        )
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(applicationContext.getString(R.string.notification_saving_songs))
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_SONG_ID = "song_id"
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION_ID = 2001
        private const val RETRY_DAYS = 7L
        private const val MIN_FREE_BYTES = 50L * 1024 * 1024
    }
}
