package com.codetiger.mymusicapp.add

import com.codetiger.mymusicapp.data.db.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {
    private fun supported(text: String) = LinkParser.find(text).filterIsInstance<FoundLink.Supported>()

    @Test
    fun findsLinkInsideMessage() {
        val links = supported("Listen to this 🙏 https://youtu.be/dQw4w9WgXcQ?si=abc beautiful song")
        assertEquals(1, links.size)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", links[0].url)
        assertEquals("Youtube:dQw4w9WgXcQ", links[0].sourceId)
    }

    @Test
    fun youtubeFormsShareOneId() {
        val text = """
            https://www.youtube.com/watch?v=dQw4w9WgXcQ
            https://music.youtube.com/watch?v=dQw4w9WgXcQ&feature=share
            https://m.youtube.com/watch?v=dQw4w9WgXcQ
        """
        assertEquals(1, supported(text).size)
    }

    @Test
    fun shortsAreSupported() {
        val link = supported("https://youtube.com/shorts/abcdefghijk?feature=share").single()
        assertEquals("Youtube:abcdefghijk", link.sourceId)
    }

    @Test
    fun playlistPartIsDropped() {
        val link = supported("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ&start_radio=1").single()
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", link.url)
    }

    @Test
    fun playlistOnlyAndChannelsAreNotSupported() {
        val found = LinkParser.find("https://www.youtube.com/playlist?list=PL123 https://www.youtube.com/@channel https://example.com/page")
        assertEquals(3, found.size)
        assertTrue(found.all { it is FoundLink.Unsupported })
    }

    @Test
    fun facebookAndInstagram() {
        val links = supported("https://fb.watch/abc123/ and https://www.facebook.com/reel/123456 and https://www.instagram.com/reel/Cxyz/?igsh=1")
        assertEquals(listOf(SourceType.FACEBOOK, SourceType.FACEBOOK, SourceType.INSTAGRAM), links.map { it.type })
        assertEquals("https://www.instagram.com/reel/Cxyz/", links[2].url)
    }

    @Test
    fun directMp3() {
        val link = supported("Song: https://example.com/files/song.MP3.").single()
        assertEquals(SourceType.MP3_LINK, link.type)
        assertEquals("https://example.com/files/song.MP3", link.url)
    }

    @Test
    fun severalLinks() {
        assertEquals(3, supported("a https://youtu.be/aaaaaaaaaaa b https://youtu.be/bbbbbbbbbbb c https://youtu.be/ccccccccccc").size)
    }

    @Test
    fun noLinks() {
        assertTrue(LinkParser.find("Good morning, have a nice day").isEmpty())
    }
}
