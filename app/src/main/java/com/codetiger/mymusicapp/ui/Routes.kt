package com.codetiger.mymusicapp.ui

import android.net.Uri
import com.codetiger.mymusicapp.data.ListRef

object Routes {
    const val WELCOME = "welcome"
    const val WELCOME_PHOTO = "welcome/photo"
    const val PHOTO_PREVIEW = "photo/preview/{from}"
    const val SETUP = "setup/{from}"
    const val HOME = "home"
    const val ADD = "add"
    const val LIST = "list/{ref}"
    const val SONG = "song/{id}?from={from}"
    const val ADD_TO_LIST = "addtolist/{id}"
    const val PLAYER = "player"
    const val UP_NEXT = "upnext"
    const val SETTINGS = "settings"
    const val YOU = "settings/you"
    const val TEXT_SIZE = "settings/text"
    const val DEFAULT_LIST = "settings/default"
    const val STORAGE = "settings/storage"
    const val REMOVED = "settings/removed"

    /** Where the photo and setup flows were started from. */
    const val FROM_WELCOME = "welcome"
    const val FROM_SETTINGS = "settings"

    fun photoPreview(from: String) = "photo/preview/$from"
    fun setup(from: String) = "setup/$from"
    fun list(ref: ListRef) = "list/${Uri.encode(ref.encode())}"
    fun song(id: Long, from: ListRef? = null) = "song/$id" + (from?.let { "?from=${Uri.encode(it.encode())}" } ?: "")
    fun addToList(id: Long) = "addtolist/$id"
}
