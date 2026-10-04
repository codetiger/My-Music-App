package com.codetiger.mymusicapp.downloader

import com.codetiger.mymusicapp.data.db.SourceType
import java.io.File

/** Reads links and downloads audio with yt-dlp (ADD-5). */
class YtDlp(private val tools: NativeTools, private val scripts: ScriptStore) {

    /** Reads title, artist, picture, length and the best audio-only format. No download. */
    suspend fun readInfo(url: String, fallbackType: SourceType, script: File = scripts.activeScript()): MediaInfo {
        val result = tools.ytDlp(
            script,
            listOf("-J", "--no-playlist", "--no-warnings", "-f", AUDIO_FORMAT, url),
        )
        if (result.exitCode != 0 || result.out.isBlank()) throw DownloadError.fromYtDlp(result.err)
        return try {
            MediaInfo.parse(result.out.trim().lines().last(), fallbackType)
        } catch (e: Exception) {
            throw DownloadError.Site("Unreadable details: ${e.message}")
        }
    }

    /** Can [script] read [url]? Details only (UPD-5). */
    suspend fun canRead(url: String, script: File): Boolean = runCatching {
        val result = tools.ytDlp(script, listOf("--simulate", "--no-playlist", "--no-warnings", "--print", "id", url))
        result.exitCode == 0 && result.out.isNotBlank()
    }.getOrDefault(false)

    suspend fun version(script: File): String? = runCatching {
        tools.ytDlp(script, listOf("--version")).out.trim().lines().firstOrNull()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /**
     * Downloads with yt-dlp itself, for formats our own downloader can't fetch (HLS, DASH
     * fragments). The audio is kept as it is, without re-encoding. Returns the saved file.
     */
    suspend fun download(
        url: String,
        outDir: File,
        baseName: String,
        onProgress: (downloaded: Long, total: Long?) -> Unit,
    ): File {
        var saved: String? = null
        val result = tools.ytDlp(
            scripts.activeScript(),
            listOf(
                "-f", AUDIO_FORMAT, "-x", "--audio-format", "best",
                "--no-playlist", "--no-warnings", "--newline", "--progress",
                "--progress-template", "download:$PROGRESS|%(progress.downloaded_bytes)s|%(progress.total_bytes)s|%(progress.total_bytes_estimate)s",
                "--print", "after_move:$SAVED|%(filepath)s",
                "-o", File(outDir, "$baseName.%(ext)s").absolutePath,
                url,
            ),
        ) { line ->
            when {
                line.startsWith("$PROGRESS|") -> {
                    val parts = line.split('|')
                    val done = parts.getOrNull(1)?.toDoubleOrNull()?.toLong() ?: return@ytDlp
                    val total = parts.getOrNull(2)?.toDoubleOrNull()?.toLong()
                        ?: parts.getOrNull(3)?.toDoubleOrNull()?.toLong()
                    onProgress(done, total)
                }
                line.startsWith("$SAVED|") -> saved = line.substringAfter('|')
            }
        }
        if (result.exitCode != 0) throw DownloadError.fromYtDlp(result.err)
        return saved?.let(::File)?.takeIf { it.exists() }
            ?: throw DownloadError.Site("yt-dlp finished without a file")
    }

    companion object {
        /** Highest audio-only quality, or the whole file when a site has no audio-only version (ADD-10). */
        const val AUDIO_FORMAT = "bestaudio/best"
        private const val PROGRESS = "MMA-PROGRESS"
        private const val SAVED = "MMA-SAVED"
    }
}
