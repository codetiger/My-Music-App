package com.codetiger.mymusicapp.add

import com.codetiger.mymusicapp.data.db.SourceType
import java.net.URI

/** A link found in pasted or shared text. */
sealed interface FoundLink {
    val original: String

    /** A link the app can save. [url] is cleaned up (one video only, no playlist part). */
    data class Supported(
        override val original: String,
        val url: String,
        val type: SourceType,
        /** Known before reading the link for YouTube, so duplicates are caught at once. */
        val sourceId: String?,
    ) : FoundLink

    data class Unsupported(override val original: String) : FoundLink
}

/** Finds supported links inside any text, e.g. a forwarded WhatsApp message (ADD-3, ADD-4). */
object LinkParser {
    private val urlPattern = Regex("""(?i)\b((?:https?://|www\.|m\.|youtu\.be/|fb\.watch/)[^\s<>"'“”‘’]+)""")
    private val youtubeId = Regex("^[A-Za-z0-9_-]{11}$")

    fun find(text: String): List<FoundLink> =
        urlPattern.findAll(text)
            .map { it.value.trimEnd('.', ',', ')', '!', '?', ';', ':', '…', ']') }
            .distinct()
            .map(::classify)
            .distinctBy { (it as? FoundLink.Supported)?.sourceId ?: (it as? FoundLink.Supported)?.url ?: it.original }
            .toList()

    fun classify(raw: String): FoundLink {
        val withScheme = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
        val uri = runCatching { URI(withScheme.replace(" ", "%20")) }.getOrNull()
            ?: return FoundLink.Unsupported(raw)
        val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return FoundLink.Unsupported(raw)
        val path = uri.path.orEmpty()
        val query = parseQuery(uri.rawQuery)

        return when {
            host == "youtu.be" -> youtube(raw, path.trim('/').substringBefore('/'))
            host == "youtube.com" || host == "music.youtube.com" || host == "youtube-nocookie.com" -> when {
                path == "/watch" -> youtube(raw, query["v"])
                path.startsWith("/shorts/") -> youtube(raw, path.removePrefix("/shorts/").substringBefore('/'))
                path.startsWith("/embed/") -> youtube(raw, path.removePrefix("/embed/").substringBefore('/'))
                path.startsWith("/v/") -> youtube(raw, path.removePrefix("/v/").substringBefore('/'))
                // Live streams and playlist-only links are not supported (ADD-4).
                else -> FoundLink.Unsupported(raw)
            }
            host == "fb.watch" -> FoundLink.Supported(raw, "https://fb.watch$path", SourceType.FACEBOOK, null)
            host == "facebook.com" || host.endsWith(".facebook.com") -> when {
                path.contains("/videos/") || path == "/watch" || path == "/watch/" || path.startsWith("/reel/") ||
                    path.startsWith("/share/v/") || path.startsWith("/share/r/") || path == "/video.php" ||
                    path.startsWith("/watch/live") ->
                    FoundLink.Supported(raw, withScheme, SourceType.FACEBOOK, null)
                else -> FoundLink.Unsupported(raw)
            }
            host == "instagram.com" -> when {
                listOf("/reel/", "/reels/", "/p/", "/tv/").any { path.startsWith(it) } ->
                    FoundLink.Supported(raw, "https://www.instagram.com$path", SourceType.INSTAGRAM, null)
                else -> FoundLink.Unsupported(raw)
            }
            path.lowercase().let { it.endsWith(".mp3") || it.endsWith(".m4a") } ->
                FoundLink.Supported(raw, withScheme, SourceType.MP3_LINK, "Direct:${withScheme.substringBefore('#')}")
            else -> FoundLink.Unsupported(raw)
        }
    }

    private fun youtube(raw: String, id: String?): FoundLink =
        if (id != null && youtubeId.matches(id)) {
            FoundLink.Supported(raw, "https://www.youtube.com/watch?v=$id", SourceType.YOUTUBE, "Youtube:$id")
        } else {
            FoundLink.Unsupported(raw)
        }

    private fun parseQuery(raw: String?): Map<String, String> =
        raw?.split('&')?.mapNotNull { part ->
            val k = part.substringBefore('=', "")
            if (k.isEmpty()) null else k to java.net.URLDecoder.decode(part.substringAfter('=', ""), "UTF-8")
        }?.toMap().orEmpty()
}
