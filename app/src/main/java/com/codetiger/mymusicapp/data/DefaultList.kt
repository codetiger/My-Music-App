package com.codetiger.mymusicapp.data

import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.PlaylistSummary

/** The list the Now Playing bar offers when nothing is playing (HOME-2). */
data class DefaultList(val ref: ListRef, val name: String, val picture: String?)

/**
 * The default list falls back to Favourites when the chosen one is gone, then to All Songs when
 * it's empty (PL-7).
 */
fun defaultListOf(chosen: ListRef?, lists: List<PlaylistSummary>): DefaultList {
    val allSongs = DefaultList(ListRef.AllSongs, "All Songs", null)
    if (chosen == ListRef.AllSongs) return allSongs
    val picked = (chosen as? ListRef.Stored)?.let { ref -> lists.firstOrNull { it.id == ref.playlistId } }
        ?: lists.firstOrNull { it.builtIn == BuiltIn.FAVOURITES }
    return if (picked != null && picked.songCount > 0) DefaultList(ListRef.Stored(picked.id), picked.name, picked.firstThumbnail) else allSongs
}
