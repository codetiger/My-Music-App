package com.codetiger.mymusicapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM song WHERE removed_at IS NULL")
    fun observeLibrary(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE removed_at IS NULL")
    suspend fun library(): List<Song>

    @Query("SELECT * FROM song WHERE id = :id")
    fun observe(id: Long): Flow<Song?>

    @Query("SELECT * FROM song WHERE id = :id")
    suspend fun get(id: Long): Song?

    @Query("SELECT * FROM song WHERE id IN (:ids)")
    suspend fun getAll(ids: List<Long>): List<Song>

    @Query("SELECT * FROM song")
    suspend fun everything(): List<Song>

    @Query("SELECT * FROM song WHERE source_type = :type AND source_id = :sourceId LIMIT 1")
    suspend fun findBySource(type: SourceType, sourceId: String): Song?

    @Query("SELECT * FROM song WHERE file_hash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): Song?

    @Query("SELECT * FROM song WHERE removed_at IS NULL AND last_played_at IS NOT NULL ORDER BY last_played_at DESC LIMIT 50")
    fun observeRecentlyPlayed(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE removed_at IS NULL AND last_played_at IS NOT NULL ORDER BY last_played_at DESC LIMIT 50")
    suspend fun recentlyPlayed(): List<Song>

    @Query("SELECT * FROM song WHERE removed_at IS NOT NULL ORDER BY removed_at DESC")
    fun observeRemoved(): Flow<List<Song>>

    @Query("SELECT * FROM song WHERE removed_at IS NOT NULL AND removed_at < :before")
    suspend fun removedBefore(before: Long): List<Song>

    @Query("SELECT * FROM song WHERE removed_at IS NOT NULL")
    suspend fun removed(): List<Song>

    @Query("SELECT * FROM song WHERE download_status IN ('QUEUED', 'DOWNLOADING', 'WAITING_RETRY') AND removed_at IS NULL")
    suspend fun pendingDownloads(): List<Song>

    @Query("SELECT COALESCE(SUM(file_size), 0) FROM song WHERE file_path IS NOT NULL")
    fun observeBytesUsed(): Flow<Long>

    @Insert
    suspend fun insert(song: Song): Long

    @Update
    suspend fun update(song: Song)

    @Query("UPDATE song SET download_status = :status, download_progress = :progress WHERE id = :id")
    suspend fun setStatus(id: Long, status: DownloadStatus, progress: Int = 0)

    @Query("UPDATE song SET title = :title, artist = :artist WHERE id = :id")
    suspend fun rename(id: Long, title: String, artist: String)

    @Query("UPDATE song SET play_count = play_count + 1, last_played_at = :at WHERE id = :id")
    suspend fun recordPlay(id: Long, at: Long)

    @Query("UPDATE song SET removed_at = :at WHERE id = :id")
    suspend fun setRemoved(id: Long, at: Long?)

    @Query("DELETE FROM song WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)
}

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.name, p.built_in,
            (SELECT COUNT(*) FROM playlist_song ps JOIN song s ON s.id = ps.song_id
                WHERE ps.playlist_id = p.id AND s.removed_at IS NULL) AS song_count,
            (SELECT s.thumbnail_path FROM playlist_song ps JOIN song s ON s.id = ps.song_id
                WHERE ps.playlist_id = p.id AND s.removed_at IS NULL ORDER BY ps.position LIMIT 1) AS first_thumbnail
        FROM playlist p
        WHERE p.removed_at IS NULL
        ORDER BY CASE p.built_in WHEN 'FAVOURITES' THEN 0 ELSE 1 END, p.position, p.id
        """,
    )
    fun observeSummaries(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlist WHERE removed_at IS NULL ORDER BY CASE built_in WHEN 'FAVOURITES' THEN 0 ELSE 1 END, position, id")
    suspend fun active(): List<Playlist>

    @Query("SELECT * FROM playlist WHERE id = :id")
    fun observe(id: Long): Flow<Playlist?>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun get(id: Long): Playlist?

    @Query("SELECT * FROM playlist WHERE built_in = 'FAVOURITES' LIMIT 1")
    suspend fun favourites(): Playlist?

    @Query("SELECT * FROM playlist WHERE removed_at IS NOT NULL ORDER BY removed_at DESC")
    fun observeRemoved(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE removed_at IS NOT NULL AND removed_at < :before")
    suspend fun removedBefore(before: Long): List<Playlist>

    @Query("SELECT * FROM playlist WHERE removed_at IS NOT NULL")
    suspend fun removed(): List<Playlist>

    @Query("SELECT COALESCE(MAX(position), 0) FROM playlist")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Query("UPDATE playlist SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE playlist SET removed_at = :at WHERE id = :id")
    suspend fun setRemoved(id: Long, at: Long?)

    @Query("DELETE FROM playlist WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    // Songs in a list

    @Query(
        """
        SELECT s.* FROM song s JOIN playlist_song ps ON ps.song_id = s.id
        WHERE ps.playlist_id = :playlistId AND s.removed_at IS NULL ORDER BY ps.position
        """,
    )
    fun observeSongs(playlistId: Long): Flow<List<Song>>

    @Query(
        """
        SELECT s.* FROM song s JOIN playlist_song ps ON ps.song_id = s.id
        WHERE ps.playlist_id = :playlistId AND s.removed_at IS NULL ORDER BY ps.position
        """,
    )
    suspend fun songs(playlistId: Long): List<Song>

    @Query("SELECT * FROM playlist_song WHERE playlist_id = :playlistId ORDER BY position")
    suspend fun entries(playlistId: Long): List<PlaylistSong>

    @Query("SELECT playlist_id FROM playlist_song WHERE song_id = :songId")
    fun observeListsContaining(songId: Long): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_song WHERE playlist_id = :playlistId")
    suspend fun maxEntryPosition(playlistId: Long): Int

    @Insert
    suspend fun insertEntry(entry: PlaylistSong)

    @Query("DELETE FROM playlist_song WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun deleteEntry(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_song SET position = :position WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun setEntryPosition(playlistId: Long, songId: Long, position: Int)

    @Query("DELETE FROM playlist_song WHERE song_id IN (:songIds)")
    suspend fun deleteEntriesForSongs(songIds: List<Long>)

    @Query("DELETE FROM playlist_song WHERE playlist_id IN (:playlistIds)")
    suspend fun deleteEntriesForLists(playlistIds: List<Long>)

    /** Swaps a song with its neighbour among the songs still in the library. */
    @Transaction
    suspend fun move(playlistId: Long, songId: Long, up: Boolean) {
        val visible = songs(playlistId).map { it.id }
        val index = visible.indexOf(songId)
        val target = if (up) index - 1 else index + 1
        if (index < 0 || target !in visible.indices) return
        val entries = entries(playlistId).associateBy { it.songId }
        val a = entries.getValue(songId)
        val b = entries.getValue(visible[target])
        setEntryPosition(playlistId, a.songId, b.position)
        setEntryPosition(playlistId, b.songId, a.position)
    }
}
