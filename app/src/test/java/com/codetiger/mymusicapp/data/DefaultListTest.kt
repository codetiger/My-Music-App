package com.codetiger.mymusicapp.data

import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.PlaylistSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultListTest {
    private val favourites = PlaylistSummary(1, "Favourites", BuiltIn.FAVOURITES, 2, "fav.jpg")
    private val temple = PlaylistSummary(2, "Temple", BuiltIn.NONE, 3, "temple.jpg")
    private val empty = PlaylistSummary(3, "Empty", BuiltIn.NONE, 0, null)
    private val lists = listOf(favourites, temple, empty)

    @Test
    fun playsTheChosenList() {
        assertEquals(DefaultList(ListRef.Stored(2), "Temple", "temple.jpg"), defaultListOf(ListRef.Stored(2), lists))
    }

    @Test
    fun deletedListFallsBackToFavourites() {
        assertEquals(ListRef.Stored(1), defaultListOf(ListRef.Stored(9), lists).ref)
        assertEquals(ListRef.Stored(1), defaultListOf(null, lists).ref)
    }

    @Test
    fun emptyListFallsBackToAllSongs() {
        assertEquals(ListRef.AllSongs, defaultListOf(ListRef.Stored(3), lists).ref)
        assertEquals("All Songs", defaultListOf(null, listOf(favourites.copy(songCount = 0))).name)
        assertEquals(ListRef.AllSongs, defaultListOf(ListRef.AllSongs, lists).ref)
    }
}
