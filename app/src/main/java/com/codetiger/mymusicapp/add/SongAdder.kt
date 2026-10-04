package com.codetiger.mymusicapp.add

import com.codetiger.mymusicapp.data.LibraryRepository
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.FailureReason
import com.codetiger.mymusicapp.data.db.MusicDatabase
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.db.SourceType
import com.codetiger.mymusicapp.downloader.DownloadError
import com.codetiger.mymusicapp.downloader.MediaInfo
import com.codetiger.mymusicapp.downloader.YtDlp
import com.codetiger.mymusicapp.util.DebugLog
import kotlinx.coroutines.CancellationException

/** What the Add Song screen shows for one link. */
sealed interface LinkPreview {
    val link: FoundLink.Supported

    /** Read fine: show picture, title, source, length and Save Song. */
    data class Ready(override val link: FoundLink.Supported, val info: MediaInfo) : LinkPreview

    /** Couldn't be read now (no internet, site trouble): it can still be saved and will download later. */
    data class Later(override val link: FoundLink.Supported, val offline: Boolean) : LinkPreview

    /** Already in the library; [putBack] when it was in Recently Removed and has been put back (ADD-14). */
    data class AlreadyInLibrary(override val link: FoundLink.Supported, val song: Song, val putBack: Boolean) : LinkPreview

    /** Can never be saved. */
    data class CannotSave(override val link: FoundLink.Supported, val reason: FailureReason) : LinkPreview
}

/** Adds songs from links: reads the link, spots duplicates, saves and starts the download. */
class SongAdder(
    private val db: MusicDatabase,
    private val library: LibraryRepository,
    private val ytDlp: YtDlp,
    private val infoCache: InfoCache,
    private val scheduler: DownloadScheduler,
) {
    private val songs = db.songs()

    suspend fun preview(link: FoundLink.Supported): LinkPreview {
        link.sourceId?.let { id -> duplicate(link, id)?.let { return it } }
        val info = try {
            infoCache.get(link.url) ?: ytDlp.readInfo(link.url, link.type)
        } catch (e: DownloadError.Permanent) {
            return LinkPreview.CannotSave(link, e.reason)
        } catch (e: DownloadError.Offline) {
            return LinkPreview.Later(link, offline = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DebugLog.e("Reading ${link.url} failed", e)
            // Site trouble, or the downloader couldn't start: it can still be saved for later.
            return LinkPreview.Later(link, offline = false)
        }
        if (info.isLive || info.isPlaylist) return LinkPreview.CannotSave(link, FailureReason.UNSUPPORTED)
        infoCache.put(link.url, info)
        duplicate(link, sourceIdFor(link, info))?.let { return it }
        return LinkPreview.Ready(link, info)
    }

    private suspend fun duplicate(link: FoundLink.Supported, sourceId: String): LinkPreview.AlreadyInLibrary? {
        val existing = songs.findBySource(link.type, sourceId) ?: return null
        val putBack = existing.removedAt != null
        if (putBack) library.putBackSong(existing.id)
        return LinkPreview.AlreadyInLibrary(link, existing, putBack)
    }

    /** Saves the song and starts downloading it straight away. Returns the new song's id. */
    suspend fun save(preview: LinkPreview): Long {
        val link = preview.link
        val info = (preview as? LinkPreview.Ready)?.info
        val song = Song(
            title = info?.title ?: pendingTitle(link.type),
            artist = info?.artist.orEmpty(),
            sourceType = info?.sourceType ?: link.type,
            sourceUrl = link.url,
            sourceId = info?.let { sourceIdFor(link, it) } ?: link.sourceId,
            downloadStatus = DownloadStatus.QUEUED,
            durationMs = info?.durationMs ?: 0,
            addedAt = System.currentTimeMillis(),
        )
        val id = songs.insert(song)
        scheduler.enqueue(id)
        return id
    }

    /** Try Again on a song that gave up (ADD-12). */
    suspend fun retry(songId: Long) {
        val song = songs.get(songId) ?: return
        songs.update(song.copy(downloadStatus = DownloadStatus.QUEUED, failureReason = null, firstFailedAt = null, downloadProgress = 0))
        scheduler.enqueue(songId, replace = true)
    }

    private fun sourceIdFor(link: FoundLink.Supported, info: MediaInfo): String =
        // A direct file link is identified by its address, not by its file name.
        if (link.type == SourceType.MP3_LINK) link.sourceId ?: link.url else info.sourceId
}
