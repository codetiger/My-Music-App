package com.codetiger.mymusicapp.add

import com.codetiger.mymusicapp.data.db.SourceType

/** "YouTube" in "From YouTube · 4:05". */
val SourceType.displayName: String
    get() = when (this) {
        SourceType.YOUTUBE -> "YouTube"
        SourceType.FACEBOOK -> "Facebook"
        SourceType.INSTAGRAM -> "Instagram"
        SourceType.MP3_LINK -> "a link"
        SourceType.WHATSAPP -> "WhatsApp"
        SourceType.FILE -> "your phone"
    }

/** Title used until a link that couldn't be read yet is read. */
fun pendingTitle(type: SourceType): String = "Song from ${type.displayName}"
