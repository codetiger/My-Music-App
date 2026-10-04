package com.codetiger.mymusicapp.downloader

import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Downloads being written right now, so the player can read a song while it arrives (ADD-11).
 * Writers report progress here; readers wait on an entry for more bytes.
 */
class ActiveDownloads {
    class Entry(val songId: Long, val file: File) {
        @Volatile var written: Long = 0
            internal set
        @Volatile var total: Long? = null
            internal set
        @Volatile var finished: Boolean = false
            internal set
        @Volatile var failure: IOException? = null
            internal set
        internal val lock = Object()

        /** Blocks until more than [position] bytes exist, the download ends, or [timeoutMs] passes. */
        fun awaitBytesBeyond(position: Long, timeoutMs: Long) {
            synchronized(lock) {
                if (written > position || finished || failure != null) return
                lock.wait(timeoutMs)
            }
        }
    }

    private val entries = ConcurrentHashMap<Long, Entry>()

    fun get(songId: Long): Entry? = entries[songId]

    fun start(songId: Long, file: File, alreadyWritten: Long, total: Long?): Entry {
        val entry = Entry(songId, file).apply { written = alreadyWritten; this.total = total }
        entries[songId] = entry
        return entry
    }

    fun progress(entry: Entry, written: Long, total: Long?) = synchronized(entry.lock) {
        entry.written = written
        if (total != null) entry.total = total
        entry.lock.notifyAll()
    }

    /** The file is complete. Readers holding it open keep reading; new readers use the saved file. */
    fun finish(entry: Entry) {
        synchronized(entry.lock) {
            entry.finished = true
            entry.total = entry.written
            entry.lock.notifyAll()
        }
        entries.remove(entry.songId, entry)
    }

    fun fail(entry: Entry, error: IOException) {
        synchronized(entry.lock) {
            entry.failure = error
            entry.lock.notifyAll()
        }
        entries.remove(entry.songId, entry)
    }
}
