package com.codetiger.mymusicapp.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XdogTest {
    private val n = 128

    private fun flat(value: Float) = FloatArray(n * n) { value }

    /** Black square (0.2) in the middle of a white (0.9) image. */
    private fun squareImage(): FloatArray = FloatArray(n * n) { i ->
        val x = i % n
        val y = i / n
        if (x in 40 until 88 && y in 40 until 88) 0.2f else 0.9f
    }

    @Test
    fun flatImageHasNoInkWhateverItsBrightness() {
        for (v in listOf(0f, 0.1f, 0.5f, 0.9f, 1f)) {
            for (p in listOf(Xdog.DRAWING, Xdog.ICON, Xdog.Params(focus = 0f, joinLevel = 0.5f))) {
                val ink = Xdog.lines(flat(v), n, n, p)
                assertFalse("brightness $v gave ink", ink.any { it })
            }
        }
    }

    @Test
    fun sharpEdgeGivesInkAlongTheEdge() {
        val img = squareImage()
        val ink = Xdog.lines(img, n, n, Xdog.Params(focus = 0f))
        fun at(x: Int, y: Int) = ink[y * n + x]
        // Ink just inside each side of the square, half way along.
        assertTrue((38..43).any { at(it, 64) })
        assertTrue((84..89).any { at(it, 64) })
        assertTrue((38..43).any { at(64, it) })
        assertTrue((84..89).any { at(64, it) })
        // Middle of the square and far outside stay paper.
        assertFalse(at(64, 64))
        assertFalse(at(5, 5))
        assertFalse(at(120, 120))
    }

    @Test
    fun drawingParamsFindTheSquareToo() {
        val ink = Xdog.lines(squareImage(), n, n, Xdog.DRAWING)
        assertTrue(ink.count { it } > 4 * 40)
    }

    @Test
    fun smallSpecksAreRemoved() {
        val img = flat(0.9f)
        // A 2 x 2 dark speck far from anything else.
        for (y in 30..31) for (x in 30..31) img[y * n + x] = 0.1f
        val p = Xdog.Params(focus = 0f)
        val ink = Xdog.lines(img, n, n, p)
        assertFalse(ink.any { it })
        // Without the blob filter the speck would have shown.
        val raw = Xdog.lines(img, n, n, p.copy(minBlob = 0, thicken = 0))
        assertTrue(raw.any { it })
    }

    @Test
    fun removeSmallBlobsKeepsLargeOnes() {
        val w = 20
        val h = 10
        val ink = BooleanArray(w * h)
        ink[2 * w + 2] = true // 1 px
        for (x in 5 until 15) ink[5 * w + x] = true // 10 px line
        Xdog.removeSmallBlobs(ink, w, h, 5)
        assertFalse(ink[2 * w + 2])
        assertEquals(10, ink.count { it })
    }

    @Test
    fun dilateThickensByRadius() {
        val ink = BooleanArray(9 * 9)
        ink[4 * 9 + 4] = true
        assertEquals(9, Xdog.dilate(ink, 9, 9, 1).count { it })
        assertEquals(21, Xdog.dilate(ink, 9, 9, 2).count { it })
    }

    @Test
    fun contrastStretchMapsPercentilesToZeroAndOne() {
        // 1000 evenly spread values between 0.3 and 0.7.
        val grey = FloatArray(1000) { 0.3f + 0.4f * it / 999f }
        val out = Xdog.stretchContrast(grey)
        val sorted = out.sorted()
        assertEquals(0f, sorted[0], 0f)
        assertEquals(0f, sorted[19], 0.01f) // 2nd percentile
        assertEquals(1f, sorted[979], 0.01f) // 98th percentile
        assertEquals(1f, sorted[999], 0f)
        assertEquals(0.5f, out[500], 0.01f)
    }

    @Test
    fun contrastStretchLeavesFlatImageAlone() {
        val out = Xdog.stretchContrast(FloatArray(100) { 0.4f })
        assertTrue(out.all { it == 0.4f })
    }

    @Test
    fun greyUsesLumaWeights() {
        val grey = Xdog.toGrey(intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), -1, 0xFF000000.toInt()))
        assertEquals(0.299f, grey[0], 0.001f)
        assertEquals(0.587f, grey[1], 0.001f)
        assertEquals(0.114f, grey[2], 0.001f)
        assertEquals(1f, grey[3], 0.001f)
        assertEquals(0f, grey[4], 0.001f)
    }

    @Test
    fun bilateralKeepsEdgesSharp() {
        val img = squareImage()
        val out = Xdog.bilateral(img, n, n)
        // Either side of the edge stays close to its own value.
        assertEquals(0.2f, out[64 * n + 41], 0.02f)
        assertEquals(0.9f, out[64 * n + 38], 0.02f)
    }

    @Test
    fun circleIsTransparentOutsideAndRingedAtEdge() {
        val ink = 0xFF2B1D14.toInt()
        val paper = 0xFFF7F0E4.toInt()
        val px = Xdog.renderCircle(FloatArray(64 * 64), 64, 100, 100, ink, paper, null)
        assertEquals(0, px[0]) // corner
        assertEquals(paper, px[50 * 100 + 50]) // centre
        assertEquals(ink, px[50 * 100 + 1]) // ring at the left edge
        val icon = Xdog.renderCircle(FloatArray(64 * 64), 64, 100, 66, ink, paper, paper)
        assertTrue(icon.all { (it ushr 24) == 0xFF }) // fully opaque
        assertEquals(paper, icon[0])
    }
}
