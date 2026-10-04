package com.codetiger.mymusicapp.data

import com.codetiger.mymusicapp.data.db.Song
import java.text.Normalizer
import java.util.Locale

/** Sort buttons on Home (LIB-3). */
enum class SongSort { AZ, NEWEST, MOST_PLAYED }

fun List<Song>.sortedFor(sort: SongSort): List<Song> = when (sort) {
    SongSort.AZ -> sortedWith(compareBy<Song> { searchKey(it.title) }.thenBy { it.id })
    SongSort.NEWEST -> sortedWith(compareByDescending<Song> { it.addedAt }.thenByDescending { it.id })
    SongSort.MOST_PLAYED -> sortedWith(
        compareByDescending<Song> { it.playCount }.thenBy { searchKey(it.title) }.thenBy { it.id },
    )
}

/** Matches title and artist, ignoring case and accents (LIB-2). */
fun List<Song>.matching(query: String): List<Song> {
    val words = searchKey(query).split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return this
    return filter { song ->
        val haystack = searchKey(song.title + " " + song.artist)
        words.all { it in haystack }
    }
}

internal fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
