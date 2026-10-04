package com.codetiger.mymusicapp.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.codetiger.mymusicapp.data.LibraryRepository
import com.codetiger.mymusicapp.data.ListRef
import com.codetiger.mymusicapp.data.SettingsRepository
import kotlinx.coroutines.flow.first

/** What the car screen shows: Favourites, your lists, All Songs, Recently Played (PLY-6). */
class LibraryBrowser(
    private val library: LibraryRepository,
    private val settings: SettingsRepository,
) {
    fun root(): MediaItem = browsableItem(MediaIds.ROOT, "My Music", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)

    suspend fun children(parentId: String): List<MediaItem> {
        if (parentId == MediaIds.ROOT) {
            val lists = library.playlistSummaries.first().map { browsableItem(ListRef.Stored(it.id).encode(), it.name) }
            return lists +
                browsableItem(ListRef.AllSongs.encode(), "All Songs") +
                browsableItem(ListRef.RecentlyPlayed.encode(), "Recently Played")
        }
        val ref = ListRef.decode(parentId) ?: return emptyList()
        return library.currentSongsOf(ref, settings.current().songSort)
            .filter { it.isPlayable }
            .map { it.toMediaItem(from = ref) }
    }

    suspend fun item(mediaId: String): MediaItem? {
        if (mediaId == MediaIds.ROOT) return root()
        MediaIds.songId(mediaId)?.let { id -> return library.getSong(id)?.toMediaItem(MediaIds.context(mediaId)) }
        val ref = ListRef.decode(mediaId) ?: return null
        val name = library.listName(ref).first() ?: return null
        return browsableItem(mediaId, name)
    }
}
