package com.codetiger.mymusicapp.player

import android.net.Uri
import androidx.media3.common.C
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.FileDataSource
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.SongDao
import com.codetiger.mymusicapp.downloader.ActiveDownloads
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Plays a song from its saved file, or from the file as it downloads (ADD-11): reads wait for
 * the downloader to write more, so each song uses mobile data only once.
 */
@OptIn(UnstableApi::class)
class SongDataSource(
    private val songs: SongDao,
    private val active: ActiveDownloads,
) : BaseDataSource(false) {

    class Factory(private val songs: SongDao, private val active: ActiveDownloads) : DataSource.Factory {
        override fun createDataSource(): DataSource = SongDataSource(songs, active)
    }

    /** The song isn't on the phone and isn't arriving. */
    class NotOnPhone(message: String) : IOException(message)

    private var uri: Uri? = null
    private var file: FileDataSource? = null
    private var growing: ActiveDownloads.Entry? = null
    private var raf: RandomAccessFile? = null
    private var position = 0L
    private var bytesRemaining = C.LENGTH_UNSET.toLong()
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val songId = MediaIds.songIdFromUri(dataSpec.uri) ?: throw NotOnPhone("Not a song: ${dataSpec.uri}")
        transferInitializing(dataSpec)
        val deadline = System.currentTimeMillis() + WAIT_FOR_START_MS
        while (true) {
            val song = runBlocking { songs.get(songId) } ?: throw NotOnPhone("Song $songId is gone")
            val saved = song.filePath?.let(::File)?.takeIf { song.downloadStatus == DownloadStatus.DONE && it.exists() }
            if (saved != null) {
                val source = FileDataSource()
                file = source
                val length = source.open(dataSpec.buildUpon().setUri(Uri.fromFile(saved)).build())
                opened = true
                transferStarted(dataSpec)
                return length
            }
            val entry = active.get(songId)
            if (entry != null) return openGrowing(dataSpec, entry)
            val waiting = song.downloadStatus == DownloadStatus.QUEUED || song.downloadStatus == DownloadStatus.DOWNLOADING
            if (!waiting || System.currentTimeMillis() > deadline) throw NotOnPhone("Song $songId is not on the phone")
            Thread.sleep(POLL_MS)
        }
    }

    private fun openGrowing(dataSpec: DataSpec, entry: ActiveDownloads.Entry): Long {
        growing = entry
        raf = RandomAccessFile(entry.file, "r").also { it.seek(dataSpec.position) }
        position = dataSpec.position
        val total = entry.total
        bytesRemaining = when {
            dataSpec.length != C.LENGTH_UNSET.toLong() -> dataSpec.length
            total != null -> total - dataSpec.position
            else -> C.LENGTH_UNSET.toLong()
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        file?.let { source ->
            val n = source.read(buffer, offset, length)
            if (n > 0) bytesTransferred(n)
            return n
        }
        val entry = growing ?: throw IOException("Not open")
        val input = raf ?: throw IOException("Not open")
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        while (true) {
            val available = entry.written - position
            if (available > 0) {
                var toRead = minOf(length.toLong(), available)
                if (bytesRemaining != C.LENGTH_UNSET.toLong()) toRead = minOf(toRead, bytesRemaining)
                val n = input.read(buffer, offset, toRead.toInt())
                if (n < 0) return C.RESULT_END_OF_INPUT
                position += n
                if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= n
                bytesTransferred(n)
                return n
            }
            entry.failure?.let { throw IOException("Download stopped", it) }
            if (entry.finished) return C.RESULT_END_OF_INPUT
            entry.awaitBytesBeyond(position, POLL_MS)
            if (Thread.currentThread().isInterrupted) throw IOException("Interrupted")
        }
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        try {
            file?.close()
            raf?.close()
        } finally {
            file = null
            raf = null
            growing = null
            uri = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    private companion object {
        /** A just-saved song: wait while the link is read and the download starts. */
        const val WAIT_FOR_START_MS = 90_000L
        const val POLL_MS = 250L
    }
}
