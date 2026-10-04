package com.codetiger.mymusicapp.downloader

import com.codetiger.mymusicapp.data.db.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaInfoTest {
    @Test
    fun readsYoutubeAudioFormat() {
        val json = """
            {"id":"dQw4w9WgXcQ","extractor_key":"Youtube","title":"Song Title","uploader":"Singer - Topic",
             "duration":245.0,"thumbnail":"https://i.ytimg.com/vi/x/maxres.jpg","live_status":"not_live",
             "url":"https://rr1.googlevideo.com/videoplayback?x","ext":"webm","protocol":"https","vcodec":"none","acodec":"opus",
             "filesize":4000000,"http_headers":{"User-Agent":"UA"},"downloader_options":{"http_chunk_size":10485760}}
        """.trimIndent()
        val info = MediaInfo.parse(json, SourceType.YOUTUBE)
        assertEquals("Youtube:dQw4w9WgXcQ", info.sourceId)
        assertEquals("Singer", info.artist)
        assertEquals(245_000L, info.durationMs)
        val f = info.format!!
        assertTrue(f.isDirect)
        assertFalse(f.hasVideo)
        assertEquals(10485760L, f.chunkSize)
        assertEquals("UA", f.headers["User-Agent"])
        assertFalse(info.isLive)
    }

    @Test
    fun liveAndPlaylistAreFlagged() {
        assertTrue(MediaInfo.parse("""{"id":"a","extractor_key":"Youtube","title":"t","is_live":true}""", SourceType.YOUTUBE).isLive)
        assertTrue(MediaInfo.parse("""{"_type":"playlist","id":"p","extractor_key":"YoutubeTab","title":"t"}""", SourceType.YOUTUBE).isPlaylist)
    }

    @Test
    fun estimatesSizeFromLengthWhenUnknown() {
        val info = MediaInfo.parse("""{"id":"a","extractor_key":"Facebook","title":"t","duration":3600}""", SourceType.FACEBOOK)
        assertEquals(SourceType.FACEBOOK, info.sourceType)
        assertEquals(72_000_000L, info.estimatedBytes)
    }
}
