package com.codetiger.mymusicapp.data

/** A list that can be opened or played: a stored playlist, or one built from the library. */
sealed interface ListRef {
    data object AllSongs : ListRef
    data object RecentlyPlayed : ListRef
    data class Stored(val playlistId: Long) : ListRef

    fun encode(): String = when (this) {
        AllSongs -> "all"
        RecentlyPlayed -> "recent"
        is Stored -> "list:$playlistId"
    }

    companion object {
        fun decode(value: String?): ListRef? = when {
            value == null -> null
            value == "all" -> AllSongs
            value == "recent" -> RecentlyPlayed
            value.startsWith("list:") -> value.removePrefix("list:").toLongOrNull()?.let(::Stored)
            else -> null
        }
    }
}
