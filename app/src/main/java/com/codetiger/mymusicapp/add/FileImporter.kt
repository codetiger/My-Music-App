package com.codetiger.mymusicapp.add

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.codetiger.mymusicapp.data.AppFiles
import com.codetiger.mymusicapp.data.LibraryRepository
import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.MusicDatabase
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.db.SourceType
import com.codetiger.mymusicapp.downloader.NativeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A song, video or voice note shared from WhatsApp or picked from the phone (ADD-8, ADD-9). */
data class FilePreview(
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val isVideo: Boolean,
    val sourceType: SourceType,
    val hash: String,
    val ext: String,
)

sealed interface FileCheck {
    data class Ready(val preview: FilePreview) : FileCheck
    data class AlreadyInLibrary(val song: Song, val putBack: Boolean) : FileCheck
    /** Not an audio or video file we can play. */
    data object NotAudio : FileCheck
}

class FileImporter(
    private val context: Context,
    private val db: MusicDatabase,
    private val library: LibraryRepository,
    private val files: AppFiles,
    private val tools: NativeTools,
) {
    private val songs = db.songs()

    suspend fun check(uri: Uri): FileCheck = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri).orEmpty()
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')
        val ext = name.substringAfterLast('.', "").lowercase().ifEmpty { extFor(mime) }
        val isVideo = mime.startsWith("video/") || ext in setOf("mp4", "3gp", "mkv", "webm", "mov")
        val isAudio = mime.startsWith("audio/") || ext in AUDIO_EXTS || mime == "application/ogg"
        if (!isVideo && !isAudio) return@withContext FileCheck.NotAudio

        val hash = sha256(uri)
        songs.findByHash(hash)?.let { existing ->
            val putBack = existing.removedAt != null
            if (putBack) library.putBackSong(existing.id)
            return@withContext FileCheck.AlreadyInLibrary(existing, putBack)
        }

        val duration = runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
        }.getOrNull() ?: 0L
        if (duration <= 0L && !isAudio) return@withContext FileCheck.NotAudio

        val fromWhatsApp = uri.authority?.contains("whatsapp", ignoreCase = true) == true || WA_NAME.containsMatchIn(name)
        FileCheck.Ready(
            FilePreview(
                uri = uri,
                title = titleFor(name),
                durationMs = duration,
                isVideo = isVideo,
                sourceType = if (fromWhatsApp) SourceType.WHATSAPP else SourceType.FILE,
                hash = hash,
                ext = if (isVideo) "m4a" else ext.ifEmpty { "m4a" },
            ),
        )
    }

    /**
     * Copies the file into the app (keeping only the audio of a video) and adds it. The song is
     * added only once its audio is ready, so an import that is stopped part way never leaves a
     * song stuck at "Downloading 0%": nothing would ever finish it.
     */
    suspend fun save(preview: FilePreview): Long = withContext(Dispatchers.IO) {
        val ready = File(files.importDir, "audio-${System.nanoTime()}.${preview.ext}")
        try {
            if (preview.isVideo) extractAudio(preview.uri, ready) else copy(preview.uri, ready)
            val id = songs.insert(
                Song(
                    title = preview.title,
                    artist = artistOf(ready).orEmpty(),
                    sourceType = preview.sourceType,
                    fileHash = preview.hash,
                    downloadStatus = DownloadStatus.DONE,
                    downloadProgress = 100,
                    durationMs = preview.durationMs,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            try {
                val target = files.audioFile(id, preview.ext)
                if (!ready.renameTo(target)) ready.copyTo(target, overwrite = true)
                val picture = savePicture(id, target)
                val song = checkNotNull(songs.get(id))
                songs.update(song.copy(filePath = target.absolutePath, fileSize = target.length(), thumbnailPath = picture?.absolutePath))
                id
            } catch (e: Exception) {
                withContext(NonCancellable) { songs.get(id)?.let { library.deleteSongsNow(listOf(it)) } }
                throw e
            }
        } finally {
            ready.delete()
        }
    }

    private fun copy(uri: Uri, target: File) {
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Can't open file")
        input.use { i -> target.outputStream().use { o -> i.copyTo(o) } }
    }

    /** WhatsApp video: keep the audio track, copied without re-encoding (ADD-8). */
    private suspend fun extractAudio(uri: Uri, target: File) {
        val source = File(files.importDir, "video-${System.nanoTime()}")
        try {
            copy(uri, source)
            val copied = tools.ffmpeg(listOf("-i", source.absolutePath, "-vn", "-c:a", "copy", target.absolutePath))
            if (copied.exitCode != 0 || !target.exists() || target.length() == 0L) {
                // The audio isn't in a form M4A can hold as is: convert it instead.
                target.delete()
                val converted = tools.ffmpeg(
                    listOf("-i", source.absolutePath, "-vn", "-c:a", "aac", "-b:a", "160k", target.absolutePath),
                )
                if (converted.exitCode != 0 || !target.exists()) throw IOException("No audio in this video")
            }
        } finally {
            source.delete()
        }
    }

    private fun savePicture(id: Long, audio: File): File? = runCatching {
        val bytes = MediaMetadataRetriever().use { r ->
            r.setDataSource(audio.absolutePath)
            r.embeddedPicture
        } ?: return null
        files.picture(id).apply { writeBytes(bytes) }
    }.getOrNull()

    private fun artistOf(audio: File): String? = runCatching {
        MediaMetadataRetriever().use { r ->
            r.setDataSource(audio.absolutePath)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        }
    }.getOrNull()

    private fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Can't open file")
        input.use { i ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = i.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val AUDIO_EXTS = setOf("mp3", "m4a", "aac", "opus", "ogg", "oga", "wav", "flac", "amr")
        private val WA_NAME = Regex("""(?i)^(PTT|AUD|VID)-\d{8}-WA\d+""")
        private val VOICE_NOTE = Regex("""(?i)^PTT-(\d{8})-WA\d+""")

        /** "Voice note · 4 Oct" for a WhatsApp voice note, otherwise the file name (ADD-8). */
        fun titleFor(fileName: String, today: LocalDate = LocalDate.now()): String {
            VOICE_NOTE.find(fileName)?.let { m ->
                val date = runCatching { LocalDate.parse(m.groupValues[1], DateTimeFormatter.BASIC_ISO_DATE) }.getOrDefault(today)
                return "Voice note · " + date.format(DateTimeFormatter.ofPattern("d MMM", Locale.UK))
            }
            return fileName.substringBeforeLast('.').replace('_', ' ').trim().ifEmpty { "Song" }
        }

        private fun extFor(mime: String) = when (mime) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/aac" -> "aac"
            "audio/ogg", "audio/opus", "application/ogg" -> "ogg"
            else -> ""
        }
    }
}
