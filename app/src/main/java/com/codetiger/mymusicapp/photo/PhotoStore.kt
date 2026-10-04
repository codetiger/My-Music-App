package com.codetiger.mymusicapp.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.codetiger.mymusicapp.data.AppFiles
import com.codetiger.mymusicapp.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Keeps only the drawing (and its icon); the photo itself is never stored (FL-5). */
class PhotoStore(
    private val files: AppFiles,
    private val settings: SettingsRepository,
    private val shortcut: HomeShortcut,
) {
    suspend fun save(result: DrawingMaker.Result) = withContext(Dispatchers.IO) {
        write(result.drawing, files.drawing)
        write(result.icon, files.drawingIcon)
        settings.setDrawingVersion(System.currentTimeMillis())
        shortcut.update(settings.current().userName)
    }

    suspend fun remove() = withContext(Dispatchers.IO) {
        files.drawing.delete()
        files.drawingIcon.delete()
        settings.setDrawingVersion(0)
        shortcut.update(settings.current().userName)
    }

    suspend fun load(): Bitmap? = withContext(Dispatchers.IO) {
        files.drawing.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }

    private fun write(bitmap: Bitmap, target: File) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        tmp.renameTo(target)
    }
}
