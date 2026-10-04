package com.codetiger.mymusicapp.data

import com.codetiger.mymusicapp.data.db.DownloadStatus
import com.codetiger.mymusicapp.data.db.Song
import com.codetiger.mymusicapp.data.db.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

class SongOrderTest {
    private fun song(id: Long, title: String, artist: String = "", added: Long = id, plays: Int = 0) =
        Song(id = id, title = title, artist = artist, sourceType = SourceType.FILE, downloadStatus = DownloadStatus.DONE, addedAt = added, playCount = plays)

    private val songs = listOf(
        song(1, "Ennai Konjam", "Hariharan", plays = 3),
        song(2, "alaipayuthey", "Balamuralikrishna", plays = 9),
        song(3, "Kurai Ondrum Illai", "M. S. Subbulakshmi", plays = 3),
    )

    @Test
    fun sorts() {
        assertEquals(listOf(2L, 1L, 3L), songs.sortedFor(SongSort.AZ).map { it.id })
        assertEquals(listOf(3L, 2L, 1L), songs.sortedFor(SongSort.NEWEST).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), songs.sortedFor(SongSort.MOST_PLAYED).map { it.id })
    }

    @Test
    fun searchesTitleAndArtistIgnoringCaseAndAccents() {
        assertEquals(listOf(3L), songs.matching("subbu").map { it.id })
        assertEquals(listOf(1L), songs.matching("HARI konjam").map { it.id })
        assertEquals(listOf(2L), songs.matching("alaipáyuthey").map { it.id })
        assertEquals(3, songs.matching("  ").size)
    }
}
