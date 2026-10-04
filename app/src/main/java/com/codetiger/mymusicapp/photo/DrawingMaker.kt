package com.codetiger.mymusicapp.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.media.FaceDetector
import android.net.Uri
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Turns a photo into the user's round ink drawing (spec section 7, "Photo to line drawing").
 * Steps 1-2 (read, find face, crop) and 6-7 (colour, circle, icon) live here; the line
 * filter itself is in [Xdog]. Nothing from the photo is kept once [make] returns.
 */
class DrawingMaker(private val context: Context) {

    data class Result(
        /** 512 x 512 ARGB, transparent outside the circle. */
        val drawing: Bitmap,
        /** 432 x 432 opaque adaptive-icon picture; the drawing sits in the centre 66 %. */
        val icon: Bitmap,
    )

    /** Runs on Dispatchers.Default. Throws IOException if the image can't be read. */
    suspend fun make(photo: Uri): Result = withContext(Dispatchers.Default) {
        val argb = facePixels(photo)
        ensureActive()
        val grey = Xdog.smoothGrey(argb, WORK, WORK)
        ensureActive()

        val drawingInk = Xdog.coverage(Xdog.lines(grey, WORK, WORK, Xdog.DRAWING), WORK, WORK)
        val drawing = toBitmap(
            Xdog.renderCircle(drawingInk, WORK, DRAWING_SIZE, DRAWING_SIZE, INK, PAPER, null),
            DRAWING_SIZE,
        )

        ensureActive()
        val iconInk = Xdog.coverage(Xdog.lines(grey, WORK, WORK, Xdog.ICON), WORK, WORK)
        val iconDiameter = (ICON_SIZE * SAFE_ZONE).toInt()
        val icon = toBitmap(
            Xdog.renderCircle(iconInk, WORK, ICON_SIZE, iconDiameter, INK, PAPER, PAPER),
            ICON_SIZE,
        )
        Result(drawing, icon)
    }

    /** Steps 1-2: read the photo upright, find the face and return a 512 x 512 crop as ARGB. */
    private fun facePixels(photo: Uri): IntArray {
        val source = ImageDecoder.createSource(context.contentResolver, photo)
        val full = try {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val longest = max(w, h)
                if (longest > MAX_SIDE) {
                    val s = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        max(1, (w * s).roundToInt()),
                        max(1, (h * s).roundToInt()),
                    )
                }
            }
        } catch (e: RuntimeException) {
            // Some providers report unreadable files as runtime errors.
            throw IOException("Can't read image", e)
        }
        try {
            val square = faceSquare(full) ?: centreSquare(full.width, full.height)
            val crop = Bitmap.createBitmap(WORK, WORK, Bitmap.Config.ARGB_8888)
            try {
                Canvas(crop).drawBitmap(
                    full,
                    square,
                    Rect(0, 0, WORK, WORK),
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
                )
                val pixels = IntArray(WORK * WORK)
                crop.getPixels(pixels, 0, WORK, 0, 0, WORK, WORK)
                return pixels
            } finally {
                crop.recycle()
            }
        } finally {
            full.recycle()
        }
    }

    /**
     * Finds the largest face and returns a square about 5.5 x the eye distance wide, centred
     * just above the eye midpoint and kept inside the photo. Null when no face is found.
     */
    private fun faceSquare(photo: Bitmap): Rect? {
        // FaceDetector needs RGB_565 and an even width.
        val w = photo.width and 1.inv()
        val h = photo.height
        if (w < 2 || h < 2) return null
        val rgb565 = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        val faces = arrayOfNulls<FaceDetector.Face>(MAX_FACES)
        val found = try {
            Canvas(rgb565).drawBitmap(photo, 0f, 0f, null)
            FaceDetector(w, h, MAX_FACES).findFaces(rgb565, faces)
        } catch (e: RuntimeException) {
            0
        } finally {
            rgb565.recycle()
        }
        val face = faces.take(found).filterNotNull().maxByOrNull { it.eyesDistance() } ?: return null
        val eyes = PointF()
        face.getMidPoint(eyes)
        val eyeDistance = face.eyesDistance()
        if (eyeDistance <= 0f) return null

        val size = (CROP_EYE_DISTANCES * eyeDistance).roundToInt()
            .coerceAtMost(minOf(photo.width, photo.height))
            .coerceAtLeast(1)
        val centreY = eyes.y - CENTRE_ABOVE_EYES * eyeDistance
        val left = (eyes.x - size / 2f).roundToInt().coerceIn(0, photo.width - size)
        val top = (centreY - size / 2f).roundToInt().coerceIn(0, photo.height - size)
        return Rect(left, top, left + size, top + size)
    }

    private fun centreSquare(w: Int, h: Int): Rect {
        val size = minOf(w, h)
        val left = (w - size) / 2
        val top = (h - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    private fun toBitmap(pixels: IntArray, size: Int): Bitmap =
        Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)

    private companion object {
        /** Step 1: longest side of the decoded photo. */
        const val MAX_SIDE = 1024

        /** Working size of the face crop for steps 3-5. */
        const val WORK = 512
        const val DRAWING_SIZE = 512
        const val ICON_SIZE = 432

        /** Adaptive icons are masked by the launcher; only the centre 66 % is always shown. */
        const val SAFE_ZONE = 0.66f

        const val MAX_FACES = 5
        // Android's FaceDetector reports a smaller eye distance than measured by hand; 5.5 fits hair and chin.
        const val CROP_EYE_DISTANCES = 5.5f
        const val CENTRE_ABOVE_EYES = 0.2f

        const val INK = 0xFF2B1D14.toInt()
        const val PAPER = 0xFFF7F0E4.toInt()
    }
}
