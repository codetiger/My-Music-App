package com.codetiger.mymusicapp.downloader

import com.codetiger.mymusicapp.data.db.FailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadErrorTest {
    private fun reason(err: String) = (DownloadError.fromYtDlp(err) as DownloadError.Permanent).reason

    @Test
    fun privateAndLoginVideosCanNeverBeSaved() {
        assertEquals(FailureReason.PRIVATE, reason("ERROR: [youtube] abc: Private video. Sign in if you've been granted access"))
        assertEquals(FailureReason.PRIVATE, reason("ERROR: [Instagram] xyz: Requested content is not available, rate-limit reached or login required"))
        assertEquals(
            FailureReason.PRIVATE,
            reason("ERROR: [Instagram] C2dYhTxsl1x: Instagram sent an empty media response. Check if this post is accessible in your browser without being logged-in."),
        )
    }

    @Test
    fun removedAndUnsupported() {
        assertEquals(FailureReason.REMOVED, reason("ERROR: [youtube] abc: Video unavailable. This video has been removed by the uploader"))
        assertEquals(FailureReason.UNSUPPORTED, reason("ERROR: Unsupported URL: https://example.com/page"))
    }

    @Test
    fun noInternetIsTemporary() {
        assertTrue(DownloadError.fromYtDlp("ERROR: Unable to download webpage: <urlopen error [Errno 7] No address associated with hostname>") is DownloadError.Offline)
    }

    @Test
    fun botCheckIsASiteProblemNotPrivate() {
        val e = DownloadError.fromYtDlp("ERROR: [youtube] abc: Sign in to confirm you're not a bot. Use --cookies-from-browser or --cookies for the authentication.")
        assertTrue(e is DownloadError.Site)
    }

    @Test
    fun fullPhone() {
        assertTrue(DownloadError.fromYtDlp("ERROR: unable to write data: [Errno 28] No space left on device") is DownloadError.NoSpace)
    }
}
