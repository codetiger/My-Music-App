package com.codetiger.mymusicapp.add

import com.codetiger.mymusicapp.downloader.MediaInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Details read for the preview card, reused when the download starts a moment later so the
 * link isn't read twice. Direct audio URLs expire after a few hours, so entries are short-lived.
 */
class InfoCache {
    private data class Entry(val info: MediaInfo, val at: Long)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun put(url: String, info: MediaInfo) {
        entries[url] = Entry(info, System.currentTimeMillis())
    }

    fun get(url: String): MediaInfo? {
        val e = entries[url] ?: return null
        return if (System.currentTimeMillis() - e.at < MAX_AGE_MS) e.info else null.also { entries.remove(url) }
    }

    fun drop(url: String) {
        entries.remove(url)
    }

    private companion object {
        const val MAX_AGE_MS = 20 * 60 * 1000L
    }
}
