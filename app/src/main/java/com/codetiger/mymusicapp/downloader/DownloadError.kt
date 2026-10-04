package com.codetiger.mymusicapp.downloader

import com.codetiger.mymusicapp.data.db.FailureReason
import java.io.IOException

/** Why reading or downloading a link failed, in the terms the app acts on (ADD-12, ADD-13). */
sealed class DownloadError(message: String) : IOException(message) {
    /** Can never work: no retries. */
    class Permanent(val reason: FailureReason, detail: String) : DownloadError(detail)

    /** No internet, or the phone is full: retry later; not the site's fault. */
    class Offline(detail: String) : DownloadError(detail)
    class NoSpace(detail: String) : DownloadError(detail)

    /** The site refused or changed: retry later, and count towards switching yt-dlp back (UPD-6). */
    class Site(detail: String) : DownloadError(detail)

    companion object {
        private val private = listOf(
            "private video", "this video is private", "login required", "log in", "login_required",
            "sign in to confirm your age", "age-restricted", "requested content is not available",
            "only available for registered users", "members-only", "join this channel",
            "you must be logged in", "this content isn't available",
            // Instagram without a login (ADD-6).
            "empty media response", "without being logged-in", "not available without logging in",
        )
        private val removed = listOf(
            "video unavailable", "has been removed", "no longer available", "account associated with this video has been terminated",
            "this video does not exist", "http error 404", "content is no longer available", "page not found",
        )
        private val unsupported = listOf(
            "unsupported url", "is not a valid url", "no video formats found", "live event will begin",
            "this live event", "is live", "premieres in", "the channel is not currently live",
        )
        private val offline = listOf(
            "network is unreachable", "temporary failure in name resolution", "name or service not known",
            "no address associated with hostname", "connection refused", "failed to resolve", "timed out",
            "connection reset", "unable to connect", "no route to host", "getaddrinfo failed",
        )
        private val noSpace = listOf("no space left on device", "disk full")

        /** Turns yt-dlp's error output into a [DownloadError]. Anything unknown counts as a site problem. */
        fun fromYtDlp(stderr: String): DownloadError {
            val text = stderr.lowercase()
            val errorLines = text.lines().filter { it.startsWith("error:") }.joinToString("\n").ifEmpty { text }
            return when {
                noSpace.any { it in text } -> NoSpace(errorLines)
                offline.any { it in errorLines } -> Offline(errorLines)
                private.any { it in errorLines } -> Permanent(FailureReason.PRIVATE, errorLines)
                removed.any { it in errorLines } -> Permanent(FailureReason.REMOVED, errorLines)
                unsupported.any { it in errorLines } -> Permanent(FailureReason.UNSUPPORTED, errorLines)
                else -> Site(errorLines)
            }
        }
    }
}
