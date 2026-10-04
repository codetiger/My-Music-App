package com.codetiger.mymusicapp.data

import androidx.room.withTransaction
import com.codetiger.mymusicapp.data.db.BuiltIn
import com.codetiger.mymusicapp.data.db.MusicDatabase
import com.codetiger.mymusicapp.data.db.Playlist
import com.codetiger.mymusicapp.data.db.PlaylistSong
import com.codetiger.mymusicapp.data.db.PlaylistSummary
import com.codetiger.mymusicapp.data.db.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.concurrent.TimeUnit

/** Songs, playlists, favourites, recently played and Recently Removed. */
class LibraryRepository(
    private val db: MusicDatabase,
    private val files: AppFiles,
) {
    private val songs = db.songs()
    private val playlists = db.playlists()

    val library: Flow<List<Song>> = songs.observeLibrary()
    val recentlyPlayed: Flow<List<Song>> = songs.observeRecentlyPlayed()
    val playlistSummaries: Flow<List<PlaylistSummary>> = playlists.observeSummaries()
    val removedSongs: Flow<List<Song>> = songs.observeRemoved()
    val removedLists: Flow<List<Playlist>> = playlists.observeRemoved()
    val bytesUsed: Flow<Long> = songs.observeBytesUsed()

    fun song(id: Long): Flow<Song?> = songs.observe(id)
    suspend fun getSong(id: Long): Song? = songs.get(id)
    suspend fun getSongs(ids: List<Long>): List<Song> {
        val byId = songs.getAll(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    fun playlist(id: Long): Flow<Playlist?> = playlists.observe(id)
    suspend fun getPlaylist(id: Long): Playlist? = playlists.get(id)
    suspend fun activePlaylists(): List<Playlist> = playlists.active()

    suspend fun favouritesId(): Long = checkNotNull(playlists.favourites()).id

    /** Songs of a list in the order they play. All Songs follows [sort]. */
    fun songsOf(ref: ListRef, sort: SongSort = SongSort.AZ): Flow<List<Song>> = when (ref) {
        ListRef.AllSongs -> library.map { it.sortedFor(sort) }
        ListRef.RecentlyPlayed -> recentlyPlayed
        is ListRef.Stored -> playlists.observeSongs(ref.playlistId)
    }

    suspend fun currentSongsOf(ref: ListRef, sort: SongSort = SongSort.AZ): List<Song> = when (ref) {
        ListRef.AllSongs -> songs.library().sortedFor(sort)
        ListRef.RecentlyPlayed -> songs.recentlyPlayed()
        is ListRef.Stored -> playlists.songs(ref.playlistId)
    }

    fun listName(ref: ListRef): Flow<String?> = when (ref) {
        ListRef.AllSongs -> flowOf("All Songs")
        ListRef.RecentlyPlayed -> flowOf("Recently Played")
        is ListRef.Stored -> playlists.observe(ref.playlistId).map { it?.takeIf { p -> p.removedAt == null }?.name }
    }

    // Favourites and lists

    fun isFavourite(songId: Long): Flow<Boolean> =
        combine(playlists.observeListsContaining(songId), playlistSummaries) { ids, lists ->
            val favId = lists.firstOrNull { it.builtIn == BuiltIn.FAVOURITES }?.id
            favId != null && favId in ids
        }

    /** Returns true when the song is now a favourite. */
    suspend fun toggleFavourite(songId: Long): Boolean {
        val favId = favouritesId()
        val inList = playlists.entries(favId).any { it.songId == songId }
        if (inList) playlists.deleteEntry(favId, songId) else addToList(favId, songId)
        return !inList
    }

    /** Returns false when the song was already in the list. */
    suspend fun addToList(playlistId: Long, songId: Long): Boolean = db.withTransaction {
        if (playlists.entries(playlistId).any { it.songId == songId }) return@withTransaction false
        playlists.insertEntry(PlaylistSong(playlistId, songId, playlists.maxEntryPosition(playlistId) + 1))
        true
    }

    suspend fun removeFromList(playlistId: Long, songId: Long) = playlists.deleteEntry(playlistId, songId)

    suspend fun move(playlistId: Long, songId: Long, up: Boolean) = playlists.move(playlistId, songId, up)

    suspend fun createList(name: String): Long = playlists.insert(
        Playlist(name = name.trim(), position = playlists.maxPosition() + 1, createdAt = now()),
    )

    suspend fun renameList(id: Long, name: String) = playlists.rename(id, name.trim())

    /** Moves an own list to Recently Removed; songs stay in the library (PL-3). */
    suspend fun deleteList(id: Long) {
        val list = playlists.get(id) ?: return
        if (list.builtIn != BuiltIn.NONE) return
        playlists.setRemoved(id, now())
    }

    suspend fun putBackList(id: Long) = playlists.setRemoved(id, null)

    // Songs

    suspend fun rename(songId: Long, title: String, artist: String) =
        songs.rename(songId, title.trim(), artist.trim())

    suspend fun recordPlay(songId: Long) = songs.recordPlay(songId, now())

    /** Moves a song to Recently Removed. Its list rows stay so Put Back restores them (LIB-6). */
    suspend fun removeSong(songId: Long) = songs.setRemoved(songId, now())

    suspend fun putBackSong(songId: Long) = songs.setRemoved(songId, null)

    /** Deletes everything in Recently Removed for good (LIB-7 Empty Now). */
    suspend fun emptyRemoved() {
        deleteForGood(songs.removed(), playlists.removed())
    }

    /** Deletes what has been in Recently Removed for 30 days. */
    suspend fun purgeExpired() {
        val before = now() - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        deleteForGood(songs.removedBefore(before), playlists.removedBefore(before))
    }

    private suspend fun deleteForGood(songList: List<Song>, lists: List<Playlist>) {
        db.withTransaction {
            if (songList.isNotEmpty()) {
                playlists.deleteEntriesForSongs(songList.map { it.id })
                songs.delete(songList.map { it.id })
            }
            if (lists.isNotEmpty()) {
                playlists.deleteEntriesForLists(lists.map { it.id })
                playlists.delete(lists.map { it.id })
            }
        }
        songList.forEach(::deleteFiles)
    }

    fun deleteFiles(song: Song) {
        song.filePath?.let { File(it).delete() }
        song.thumbnailPath?.let { File(it).delete() }
        files.audioDir.listFiles { f -> f.name.startsWith("${song.id}.") }?.forEach { it.delete() }
    }

    /** Drops songs for good without Recently Removed (used when a restored song can't come back). */
    suspend fun deleteSongsNow(list: List<Song>) = deleteForGood(list, emptyList())

    private fun now() = System.currentTimeMillis()

    companion object {
        const val RETENTION_DAYS = 30L
    }
}
