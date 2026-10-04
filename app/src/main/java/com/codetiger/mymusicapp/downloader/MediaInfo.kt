package com.codetiger.mymusicapp.downloader

import com.codetiger.mymusicapp.data.db.SourceType
import org.json.JSONObject

/** What yt-dlp reads from a link: details for the preview card and how to fetch the audio. */
data class MediaInfo(
    val sourceType: SourceType,
    /** extractor key + id, e.g. "Youtube:dQw4w9WgXcQ"; same video across link forms (ADD-14). */
    val sourceId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val isLive: Boolean,
    val isPlaylist: Boolean,
    val format: AudioFormat?,
) {
    data class AudioFormat(
        val url: String,
        val ext: String,
        val protocol: String,
        val headers: Map<String, String>,
        val sizeBytes: Long?,
        val chunkSize: Long?,
        val hasVideo: Boolean,
    ) {
        /** A plain file over HTTP(S) that our own downloader can fetch, so it can play while downloading. */
        val isDirect: Boolean get() = protocol == "https" || protocol == "http"
    }

    /** "about 150 MB" estimate for the long-recording question (ADD-7). */
    val estimatedBytes: Long
        get() = format?.sizeBytes ?: (durationMs / 1000 * 160_000 / 8)

    companion object {
        fun parse(json: String, fallbackType: SourceType): MediaInfo {
            val o = JSONObject(json)
            val type = o.optString("_type", "video")
            val extractor = o.optString("extractor_key", o.optString("extractor", ""))
            val format = if (o.has("url")) parseFormat(o) else o.optJSONArray("requested_formats")
                ?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.optString("acodec") != "none" } }
                ?.let(::parseFormat)
            val artist = listOf("artist", "creator", "uploader", "channel")
                .firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() && it != "null" } }
                .orEmpty()
            return MediaInfo(
                sourceType = sourceTypeFor(extractor) ?: fallbackType,
                sourceId = "$extractor:${o.optString("id")}",
                title = (o.optString("track").takeIf { it.isNotBlank() } ?: o.optString("title")).trim().ifEmpty { "Song" },
                artist = artist.removeSuffix(" - Topic").trim(),
                durationMs = (o.optDouble("duration", 0.0) * 1000).toLong(),
                thumbnailUrl = o.optString("thumbnail").takeIf { it.startsWith("http") },
                isLive = o.optBoolean("is_live", false) || o.optString("live_status") in setOf("is_live", "is_upcoming"),
                isPlaylist = type == "playlist" || type == "multi_video",
                format = format,
            )
        }

        private fun parseFormat(f: JSONObject): AudioFormat {
            val headers = f.optJSONObject("http_headers")?.let { h -> h.keys().asSequence().associateWith { h.getString(it) } }.orEmpty()
            val size = f.optLong("filesize", 0).takeIf { it > 0 } ?: f.optLong("filesize_approx", 0).takeIf { it > 0 }
            val chunk = f.optJSONObject("downloader_options")?.optLong("http_chunk_size", 0)?.takeIf { it > 0 }
            return AudioFormat(
                url = f.getString("url"),
                ext = f.optString("ext", "m4a"),
                protocol = f.optString("protocol", "https"),
                headers = headers,
                sizeBytes = size,
                chunkSize = chunk,
                hasVideo = f.optString("vcodec", "none").let { it != "none" && it.isNotEmpty() },
            )
        }

        private fun sourceTypeFor(extractor: String): SourceType? = when {
            extractor.startsWith("Youtube", ignoreCase = true) -> SourceType.YOUTUBE
            extractor.startsWith("Facebook", ignoreCase = true) -> SourceType.FACEBOOK
            extractor.startsWith("Instagram", ignoreCase = true) -> SourceType.INSTAGRAM
            extractor.equals("Generic", ignoreCase = true) -> SourceType.MP3_LINK
            else -> null
        }
    }
}
