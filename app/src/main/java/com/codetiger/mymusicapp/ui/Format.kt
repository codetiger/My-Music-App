package com.codetiger.mymusicapp.ui

import java.util.Locale
import kotlin.math.roundToLong

/** Numbers as people say them: "4:05", "2 h 10 min", "about 150 MB" (design-system Writing). */
object Format {
    fun time(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.UK, "%d:%02d:%02d", h, m, s) else String.format(Locale.UK, "%d:%02d", m, s)
    }

    fun longDuration(ms: Long): String {
        val minutes = (ms / 60_000.0).roundToLong()
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    fun size(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024 -> String.format(Locale.UK, "%.1f GB", mb / 1024)
            mb >= 10 -> "${(mb / 10).roundToLong() * 10} MB"
            mb >= 1 -> "${mb.roundToLong()} MB"
            else -> "less than 1 MB"
        }
    }

    fun songCount(n: Int) = if (n == 1) "1 song" else "$n songs"
}
