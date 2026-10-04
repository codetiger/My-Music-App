package com.codetiger.mymusicapp.data

import android.content.Context
import java.io.File

/**
 * Where things live on the phone. Audio and pictures are excluded from Auto Backup
 * (res/xml/data_extraction_rules.xml); the drawing is small and is backed up.
 */
class AppFiles(context: Context) {
    val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
    val picturesDir = File(context.filesDir, "pictures").apply { mkdirs() }
    val drawingDir = File(context.filesDir, "drawing").apply { mkdirs() }
    val importDir = File(context.cacheDir, "import").apply { mkdirs() }
    val updatesDir = File(context.noBackupFilesDir, "updates").apply { mkdirs() }

    val drawing = File(drawingDir, "drawing.png")
    val drawingIcon = File(drawingDir, "icon.png")

    fun audioFile(songId: Long, ext: String) = File(audioDir, "$songId.$ext")
    fun partFile(songId: Long, ext: String) = File(audioDir, "$songId.$ext.part")
    fun picture(songId: Long) = File(picturesDir, "$songId.jpg")
}
