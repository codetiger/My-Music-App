package com.codetiger.mymusicapp.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codetiger.mymusicapp.AppContainer
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.add.FileCheck
import com.codetiger.mymusicapp.add.FilePreview
import com.codetiger.mymusicapp.add.FoundLink
import com.codetiger.mymusicapp.add.LinkParser
import com.codetiger.mymusicapp.add.LinkPreview
import com.codetiger.mymusicapp.data.db.FailureReason
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.ui.IncomingShare
import com.codetiger.mymusicapp.ui.components.MessageAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One card on Add Song: a link being read or read, or a file. */
sealed interface AddItem {
    val key: String
    val ticked: Boolean

    data class Link(val link: FoundLink.Supported, val preview: LinkPreview?, override val ticked: Boolean = true) : AddItem {
        override val key get() = link.url
        val canSave get() = preview is LinkPreview.Ready || preview is LinkPreview.Later
    }

    data class File(val preview: FilePreview, override val ticked: Boolean = true) : AddItem {
        override val key get() = preview.uri.toString()
    }
}

data class AddUiState(
    val items: List<AddItem> = emptyList(),
    val readingFiles: Boolean = false,
    val saving: Boolean = false,
    /** ADD-7: "This is a long recording (2 h 10 min, about 150 MB). Save it?" */
    val askLong: String? = null,
)

class AddSongViewModel(private val app: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()
    private val messages get() = app.ui.messages

    fun handle(share: IncomingShare) = when (share) {
        is IncomingShare.Text -> handleText(share.text)
        is IncomingShare.Files -> handleFiles(share.uris)
    }

    /** Paste Link: reads the clipboard; no typing needed (ADD-1). */
    fun paste(clipboard: String?) {
        if (clipboard.isNullOrBlank()) {
            messages.show("Nothing is copied yet. Copy a song link first, then tap Paste Link.", R.drawable.ic_content_paste)
            return
        }
        handleText(clipboard)
    }

    /** Finds the supported links inside any text (ADD-3). */
    fun handleText(text: String) {
        val found = LinkParser.find(text)
        val supported = found.filterIsInstance<FoundLink.Supported>()
        if (supported.isEmpty()) {
            messages.show(if (found.isEmpty()) "No song link found in this message." else "This link is not supported.")
            return
        }
        _state.value = AddUiState(items = supported.map { AddItem.Link(it, null) })
        viewModelScope.launch {
            // One at a time: each read starts Python, which is heavy on the phone.
            for (link in supported) {
                val preview = app.songAdder.preview(link)
                onPreview(link, preview)
            }
        }
    }

    private fun onPreview(link: FoundLink.Supported, preview: LinkPreview) {
        when (preview) {
            is LinkPreview.AlreadyInLibrary -> {
                removeItem(link.url)
                if (preview.putBack) {
                    messages.show("Song put back in your library", R.drawable.ic_restore_from_trash)
                } else {
                    messages.show("This song is already in your library", R.drawable.ic_library_music_filled, MessageAction("Play", R.drawable.ic_play_arrow) {
                        app.player.play(listOf(preview.song))
                    })
                }
            }
            is LinkPreview.CannotSave -> {
                removeItem(link.url)
                messages.show(messageFor(preview.reason), R.drawable.ic_error)
            }
            else -> _state.update { s ->
                s.copy(items = s.items.map { if (it is AddItem.Link && it.link.url == link.url) it.copy(preview = preview) else it })
            }
        }
    }

    private fun removeItem(key: String) = _state.update { s -> s.copy(items = s.items.filterNot { it.key == key }) }

    fun handleFiles(uris: List<Uri>) {
        _state.value = AddUiState(readingFiles = true)
        viewModelScope.launch {
            val ready = mutableListOf<AddItem.File>()
            for (uri in uris) {
                when (val check = runCatching { app.fileImporter.check(uri) }.getOrElse { FileCheck.NotAudio }) {
                    is FileCheck.Ready -> ready += AddItem.File(check.preview)
                    is FileCheck.AlreadyInLibrary -> if (check.putBack) {
                        messages.show("Song put back in your library", R.drawable.ic_restore_from_trash)
                    } else {
                        messages.show("This song is already in your library", R.drawable.ic_library_music_filled, MessageAction("Play", R.drawable.ic_play_arrow) {
                            app.player.play(listOf(check.song))
                        })
                    }
                    FileCheck.NotAudio -> messages.show("This file isn't a song or a video.", R.drawable.ic_error)
                }
            }
            _state.value = AddUiState(items = ready)
        }
    }

    fun setTicked(key: String, ticked: Boolean) = _state.update { s ->
        s.copy(items = s.items.map {
            if (it.key != key) it else when (it) {
                is AddItem.Link -> it.copy(ticked = ticked)
                is AddItem.File -> it.copy(ticked = ticked)
            }
        })
    }

    fun clear() {
        _state.value = AddUiState()
    }

    private fun toSave(): List<AddItem> {
        val s = _state.value
        val single = s.items.size == 1
        return s.items.filter { (single || it.ticked) && (it !is AddItem.Link || it.canSave) }
    }

    /** Save Song / Save 3 Songs. Recordings over an hour ask first (ADD-7). */
    fun save(confirmedLong: Boolean = false) {
        val items = toSave()
        if (items.isEmpty()) {
            messages.show("Tick the songs you want to save.")
            return
        }
        if (!confirmedLong) {
            val long = items.filterIsInstance<AddItem.Link>().mapNotNull { (it.preview as? LinkPreview.Ready)?.info }
                .firstOrNull { it.durationMs > LONG_MS }
            if (long != null) {
                _state.update {
                    it.copy(askLong = "This is a long recording (${Format.longDuration(long.durationMs)}, about ${Format.size(long.estimatedBytes)}). Save it?")
                }
                return
            }
        }
        _state.update { it.copy(saving = true, askLong = null) }
        // App scope: leaving Add Song while a video is being copied must not stop the save.
        app.scope.launch {
            var saved = 0
            var lastSongId: Long? = null
            var offline = false
            for (item in items) {
                runCatching {
                    when (item) {
                        is AddItem.Link -> {
                            lastSongId = app.songAdder.save(item.preview!!)
                            if ((item.preview as? LinkPreview.Later)?.offline == true) offline = true
                        }
                        is AddItem.File -> lastSongId = app.fileImporter.save(item.preview)
                    }
                    saved++
                }
            }
            _state.value = AddUiState()
            val allFiles = items.all { it is AddItem.File }
            when {
                saved == 0 -> messages.show("The song couldn't be saved. Try again.", R.drawable.ic_error)
                offline -> messages.show("No internet — this song will download later", R.drawable.ic_schedule)
                allFiles -> messages.show(if (saved == 1) "Song saved on your phone" else "$saved songs saved on your phone", R.drawable.ic_offline_pin)
                else -> {
                    val id = lastSongId
                    val text = if (saved == 1) "Song added. It is being saved on your phone." else "$saved songs added. They are being saved on your phone."
                    val play = if (saved == 1 && id != null) {
                        MessageAction("Play", R.drawable.ic_play_arrow) {
                            app.scope.launch { app.library.getSong(id)?.let { app.player.play(listOf(it)) } }
                        }
                    } else null
                    messages.show(text, R.drawable.ic_downloading, play)
                }
            }
        }
    }

    fun cancelLong() = _state.update { it.copy(askLong = null) }

    companion object {
        private const val LONG_MS = 60 * 60 * 1000L

        fun messageFor(reason: FailureReason) = when (reason) {
            FailureReason.PRIVATE -> "This video is private and can't be saved."
            FailureReason.REMOVED -> "This video has been removed from the site and can't be saved."
            FailureReason.UNSUPPORTED -> "This link is not supported."
            FailureReason.GAVE_UP -> "This song can't be saved right now."
        }
    }
}
