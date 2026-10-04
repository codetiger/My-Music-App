package com.codetiger.mymusicapp.photo

import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * The numeric half of the photo-to-line-drawing filter (spec section 7, steps 3-5).
 * Works on greyscale images stored row by row in a [FloatArray] with values 0 (black)
 * to 1 (white). No Android code, so it runs in plain JVM unit tests.
 */
object Xdog {

    data class Params(
        /** Radius of the small blur G1, in pixels. */
        val sigma: Float = 1.2f,
        /** G2 uses radius k * sigma. */
        val k: Float = 2.0f,
        /** How much of G2 is taken away from G1. Below 1 keeps flat areas white. */
        val tau: Float = 0.99f,
        /** D at or above this is paper. */
        val epsilon: Float = -0.005f,
        /** Steepness of the paper-to-ink ramp. */
        val phi: Float = 100f,
        /** Ink blobs with fewer pixels than this are removed. */
        val minBlob: Int = 80,
        /** Lines are thickened by this many pixels. */
        val thicken: Int = 1,
        /**
         * How much weaker edges count near the border than in the middle (0 = no change).
         * The face sits in the middle of the crop, so this hides background clutter.
         */
        val focus: Float = 0.6f,
        /**
         * Pixels with an ink level below 0.5 are ink. Fainter pixels, down to this level, are
         * ink too when they touch a line, so lines stay joined instead of breaking into dashes.
         * 0.5 turns this off.
         */
        val joinLevel: Float = 0.9f,
    )

    /**
     * Tuned on real portraits. Compared with the spec's starting values (k 1.6, tau 0.98,
     * blobs 40) the wider k and tau closer to 1 catch soft outlines such as a pale face on a
     * pale wall; focus and the larger blob limit keep background clutter out.
     */
    val DRAWING = Params()

    /** Broader lines that survive at launcher-icon size, with less background detail. */
    val ICON = Params(sigma = 2.0f, thicken = 2, minBlob = 150, focus = 0.8f)

    /** Step 3 in one call: grey, stretch contrast, two passes of the bilateral blur. */
    fun smoothGrey(argb: IntArray, w: Int, h: Int): FloatArray {
        val grey = stretchContrast(toGrey(argb))
        return bilateral(bilateral(grey, w, h), w, h)
    }

    /** Grey = 0.299 R + 0.587 G + 0.114 B, on a 0-1 scale. Alpha is ignored. */
    fun toGrey(argb: IntArray): FloatArray = FloatArray(argb.size) { i ->
        val c = argb[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        (0.299f * r + 0.587f * g + 0.114f * b) / 255f
    }

    /** Stretches values so the 2nd percentile becomes 0 and the 98th becomes 1 (clamped). */
    fun stretchContrast(grey: FloatArray): FloatArray {
        if (grey.isEmpty()) return FloatArray(0)
        val bins = 1024
        val hist = IntArray(bins)
        for (v in grey) hist[bin(v, bins)]++
        val lo = percentile(hist, grey.size, 0.02f) / (bins - 1).toFloat()
        val hi = percentile(hist, grey.size, 0.98f) / (bins - 1).toFloat()
        val range = hi - lo
        // A flat (or nearly flat) image has nothing to stretch; keep it as it is.
        if (range < 1f / 255f) return grey.copyOf()
        return FloatArray(grey.size) { i -> ((grey[i] - lo) / range).coerceIn(0f, 1f) }
    }

    /**
     * One pass of a 5 x 5 edge-preserving (bilateral) blur. Neighbours that differ a lot in
     * brightness get little weight, so outlines stay sharp while skin texture fades.
     */
    fun bilateral(
        grey: FloatArray,
        w: Int,
        h: Int,
        sigmaSpace: Float = 2.0f,
        sigmaRange: Float = 0.1f,
    ): FloatArray {
        require(grey.size == w * h) { "size ${grey.size} != $w x $h" }
        val r = 2
        val spatial = FloatArray((2 * r + 1) * (2 * r + 1))
        for (dy in -r..r) for (dx in -r..r) {
            spatial[(dy + r) * (2 * r + 1) + dx + r] =
                exp(-(dx * dx + dy * dy) / (2f * sigmaSpace * sigmaSpace))
        }
        // Range weights looked up by |difference| in 1/1024 steps.
        val steps = 1024
        val rangeLut = FloatArray(steps + 1) { i ->
            val d = i / steps.toFloat()
            exp(-(d * d) / (2f * sigmaRange * sigmaRange))
        }
        val out = FloatArray(grey.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val c = grey[y * w + x]
                var sum = 0f
                var wsum = 0f
                for (dy in -r..r) {
                    val yy = clamp(y + dy, h)
                    val row = yy * w
                    val srow = (dy + r) * (2 * r + 1) + r
                    for (dx in -r..r) {
                        val v = grey[row + clamp(x + dx, w)]
                        val diff = v - c
                        val idx = ((if (diff < 0) -diff else diff) * steps).toInt()
                        val wt = spatial[srow + dx] * rangeLut[min(idx, steps)]
                        sum += wt * v
                        wsum += wt
                    }
                }
                out[y * w + x] = sum / wsum
            }
        }
        return out
    }

    /** Separable gaussian blur with edge clamping. */
    fun gaussian(src: FloatArray, w: Int, h: Int, sigma: Float): FloatArray {
        require(src.size == w * h) { "size ${src.size} != $w x $h" }
        if (sigma <= 0f) return src.copyOf()
        val r = max(1, ceil(3f * sigma).toInt())
        val kernel = FloatArray(2 * r + 1) { i ->
            val d = (i - r).toFloat()
            exp(-(d * d) / (2f * sigma * sigma))
        }
        val norm = kernel.sum()
        for (i in kernel.indices) kernel[i] /= norm

        val tmp = FloatArray(src.size)
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var s = 0f
                for (i in -r..r) s += kernel[i + r] * src[row + clamp(x + i, w)]
                tmp[row + x] = s
            }
        }
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0f
                for (i in -r..r) s += kernel[i + r] * tmp[clamp(y + i, h) * w + x]
                out[y * w + x] = s
            }
        }
        return out
    }

    /**
     * XDoG ink level per pixel: 1 = paper, lower = ink.
     * D = G1 - tau * G2; level = 1 if D >= epsilon else 1 + tanh(phi * (D - epsilon)).
     */
    fun inkLevel(smoothedGrey: FloatArray, w: Int, h: Int, p: Params): FloatArray {
        val g1 = gaussian(smoothedGrey, w, h, p.sigma)
        val g2 = gaussian(smoothedGrey, w, h, p.k * p.sigma)
        val size = min(w, h).toFloat()
        return FloatArray(g1.size) { i ->
            var d = g1[i] - p.tau * g2[i]
            if (p.focus > 0f) d *= focusWeight(i % w, i / w, w, h, size, p.focus)
            if (d >= p.epsilon) 1f else 1f + tanh(p.phi * (d - p.epsilon))
        }
    }

    /** 1 within 30 % of the size from the centre, falling to 1 - focus at the edge of the circle. */
    private fun focusWeight(x: Int, y: Int, w: Int, h: Int, size: Float, focus: Float): Float {
        val dx = (x + 0.5f - w / 2f) / size
        val dy = (y + 0.5f - h / 2f) / size
        val r = sqrt(dx * dx + dy * dy)
        val t = ((r - FOCUS_INNER) / (0.5f - FOCUS_INNER)).coerceIn(0f, 1f)
        return 1f - focus * t * t * (3f - 2f * t)
    }

    private const val FOCUS_INNER = 0.3f

    /** Returns a mask, true = ink: XDoG, threshold at 0.5, remove small blobs, thicken. */
    fun lines(smoothedGrey: FloatArray, w: Int, h: Int, p: Params): BooleanArray {
        val level = inkLevel(smoothedGrey, w, h, p)
        val ink = BooleanArray(level.size) { level[it] < 0.5f }
        if (p.joinLevel > 0.5f) growInto(ink, w, h) { level[it] < p.joinLevel }
        removeSmallBlobs(ink, w, h, p.minBlob)
        return dilate(ink, w, h, p.thicken)
    }

    /** Adds pixels for which [faint] is true and that connect (8-way) to existing ink, in place. */
    private inline fun growInto(ink: BooleanArray, w: Int, h: Int, faint: (Int) -> Boolean) {
        val stack = IntArray(ink.size)
        var sp = 0
        for (i in ink.indices) if (ink[i]) stack[sp++] = i
        while (sp > 0) {
            val p = stack[--sp]
            val px = p % w
            val py = p / w
            for (dy in -1..1) {
                val yy = py + dy
                if (yy < 0 || yy >= h) continue
                for (dx in -1..1) {
                    val xx = px + dx
                    if (xx < 0 || xx >= w) continue
                    val q = yy * w + xx
                    if (!ink[q] && faint(q)) {
                        ink[q] = true
                        stack[sp++] = q
                    }
                }
            }
        }
    }

    /** Clears 8-connected ink blobs with fewer than [minSize] pixels, in place. */
    fun removeSmallBlobs(ink: BooleanArray, w: Int, h: Int, minSize: Int) {
        if (minSize <= 1) return
        val seen = BooleanArray(ink.size)
        val stack = IntArray(ink.size)
        val blob = IntArray(ink.size)
        for (start in ink.indices) {
            if (!ink[start] || seen[start]) continue
            var sp = 0
            var n = 0
            stack[sp++] = start
            seen[start] = true
            while (sp > 0) {
                val p = stack[--sp]
                blob[n++] = p
                val px = p % w
                val py = p / w
                for (dy in -1..1) {
                    val yy = py + dy
                    if (yy < 0 || yy >= h) continue
                    for (dx in -1..1) {
                        val xx = px + dx
                        if (xx < 0 || xx >= w) continue
                        val q = yy * w + xx
                        if (ink[q] && !seen[q]) {
                            seen[q] = true
                            stack[sp++] = q
                        }
                    }
                }
            }
            if (n < minSize) for (i in 0 until n) ink[blob[i]] = false
        }
    }

    /** Thickens ink by [radius] pixels using a round brush. */
    fun dilate(ink: BooleanArray, w: Int, h: Int, radius: Int): BooleanArray {
        if (radius <= 0) return ink.copyOf()
        val offsets = ArrayList<Pair<Int, Int>>()
        for (dy in -radius..radius) for (dx in -radius..radius) {
            if (dx * dx + dy * dy <= radius * radius + radius) offsets += dx to dy
        }
        val out = BooleanArray(ink.size)
        for (y in 0 until h) for (x in 0 until w) {
            if (!ink[y * w + x]) continue
            for ((dx, dy) in offsets) {
                val xx = x + dx
                val yy = y + dy
                if (xx in 0 until w && yy in 0 until h) out[yy * w + xx] = true
            }
        }
        return out
    }

    /**
     * Turns an ink mask into soft ink coverage (0 = paper, 1 = ink) with a light blur,
     * so lines have smooth, not jagged, edges.
     */
    fun coverage(ink: BooleanArray, w: Int, h: Int): FloatArray =
        gaussian(FloatArray(ink.size) { if (ink[it]) 1f else 0f }, w, h, 0.6f)

    /** Area-average resize of a square [src] image from [srcSize] to [dstSize] pixels. */
    fun resize(src: FloatArray, srcSize: Int, dstSize: Int): FloatArray {
        require(src.size == srcSize * srcSize)
        if (srcSize == dstSize) return src.copyOf()
        val scale = srcSize.toFloat() / dstSize
        // Separable: first along x, then along y.
        val tmp = FloatArray(dstSize * srcSize)
        for (y in 0 until srcSize) for (x in 0 until dstSize) {
            tmp[y * dstSize + x] = areaSample(scale, x, srcSize) { src[y * srcSize + it] }
        }
        val out = FloatArray(dstSize * dstSize)
        for (y in 0 until dstSize) for (x in 0 until dstSize) {
            out[y * dstSize + x] = areaSample(scale, y, srcSize) { tmp[it * dstSize + x] }
        }
        return out
    }

    private inline fun areaSample(scale: Float, o: Int, srcSize: Int, at: (Int) -> Float): Float {
        val start = o * scale
        val end = start + scale
        if (scale <= 1f) return at(min(srcSize - 1, ((start + end) / 2f).toInt()))
        var i = start.toInt()
        var sum = 0f
        var wsum = 0f
        while (i < end && i < srcSize) {
            val wt = min(end, i + 1f) - max(start, i.toFloat())
            if (wt > 0f) {
                sum += wt * at(i)
                wsum += wt
            }
            i++
        }
        return if (wsum > 0f) sum / wsum else 0f
    }

    /**
     * Paints ink [coverage] (a [coverageSize] square) as a round drawing with a ring at the edge.
     * The circle has [diameter] pixels and sits in the centre of a [canvasSize] square.
     * Outside the circle is [background], or transparent when it is null.
     * Returns ARGB pixels, row by row.
     */
    fun renderCircle(
        coverage: FloatArray,
        coverageSize: Int,
        canvasSize: Int,
        diameter: Int,
        ink: Int,
        paper: Int,
        background: Int?,
        ringFraction: Float = 0.025f,
    ): IntArray {
        val cov = resize(coverage, coverageSize, diameter)
        val radius = diameter / 2f
        val ringWidth = max(1f, ringFraction * diameter)
        val centre = canvasSize / 2f
        val offset = (canvasSize - diameter) / 2f
        val out = IntArray(canvasSize * canvasSize)
        for (y in 0 until canvasSize) {
            val py = y + 0.5f
            for (x in 0 until canvasSize) {
                val px = x + 0.5f
                val dx = px - centre
                val dy = py - centre
                val d = sqrt(dx * dx + dy * dy)
                val inside = (radius - d + 0.5f).coerceIn(0f, 1f)
                if (inside <= 0f) {
                    out[y * canvasSize + x] = background ?: 0
                    continue
                }
                val cx = (px - offset).toInt().coerceIn(0, diameter - 1)
                val cy = (py - offset).toInt().coerceIn(0, diameter - 1)
                val ring = (d - (radius - ringWidth) + 0.5f).coerceIn(0f, 1f)
                val amount = max(cov[cy * diameter + cx].coerceIn(0f, 1f), ring)
                val colour = mix(paper, ink, amount)
                out[y * canvasSize + x] = if (background == null) {
                    (((inside * 255f) + 0.5f).toInt() shl 24) or (colour and 0xFFFFFF)
                } else {
                    mix(background, colour, inside)
                }
            }
        }
        return out
    }

    /** Opaque blend from colour [a] to colour [b]; [t] = 0 gives a, 1 gives b. */
    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(shift: Int): Int {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return ((ca + (cb - ca) * t) + 0.5f).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    private fun clamp(v: Int, size: Int): Int = if (v < 0) 0 else if (v >= size) size - 1 else v

    private fun bin(v: Float, bins: Int): Int = (v.coerceIn(0f, 1f) * (bins - 1) + 0.5f).toInt()

    /** Index of the first bin where the running count reaches [fraction] of [total]. */
    private fun percentile(hist: IntArray, total: Int, fraction: Float): Int {
        val target = max(1, ceil(total * fraction.toDouble()).toInt())
        var count = 0
        for (i in hist.indices) {
            count += hist[i]
            if (count >= target) return i
        }
        return hist.size - 1
    }
}
