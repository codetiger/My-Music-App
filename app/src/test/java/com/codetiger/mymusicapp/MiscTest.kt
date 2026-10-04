package com.codetiger.mymusicapp

import com.codetiger.mymusicapp.add.FileImporter
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.photo.HomeShortcut
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.update.AppUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MiscTest {
    @Test
    fun shortcutLabels() {
        assertEquals("Ravi's Music" to "Ravi's Music App", HomeShortcut.labels("Ravi"))
        // "Murali's Music" is 14 characters, over the 12 that fit: the name alone (FL-8).
        assertEquals("Murali" to "Murali's Music App", HomeShortcut.labels("Murali"))
        assertEquals("Subramaniam" to "Subramaniam's Music App", HomeShortcut.labels("Subramaniam"))
        assertEquals("My Music" to "My Music App", HomeShortcut.labels(null))
    }

    @Test
    fun versionCompare() {
        assertTrue(AppUpdater.isNewer("1.10.0", "1.9.2"))
        assertTrue(AppUpdater.isNewer("0.2", "0.1.0"))
        assertFalse(AppUpdater.isNewer("0.1.0", "0.1.0"))
        assertFalse(AppUpdater.isNewer("0.0.9", "0.1.0"))
    }

    @Test
    fun voiceNoteTitles() {
        assertEquals("Voice note · 4 Oct", FileImporter.titleFor("PTT-20261004-WA0012.opus"))
        assertEquals("My favourite song", FileImporter.titleFor("My_favourite_song.mp3"))
        assertEquals("AUD-20261004-WA0001", FileImporter.titleFor("AUD-20261004-WA0001.m4a", LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun formats() {
        assertEquals("4:05", Format.time(245_000))
        assertEquals("1:02:03", Format.time(3_723_000))
        assertEquals("2 h 10 min", Format.longDuration(7_800_000))
        assertEquals("150 MB", Format.size(150L * 1024 * 1024))
        assertEquals("2.1 GB", Format.size((2.1 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun listRefsRoundTrip() {
        listOf(ListRef.AllSongs, ListRef.RecentlyPlayed, ListRef.Stored(42)).forEach {
            assertEquals(it, ListRef.decode(it.encode()))
        }
    }
}
