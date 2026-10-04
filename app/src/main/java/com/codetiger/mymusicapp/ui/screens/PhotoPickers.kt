package com.codetiger.mymusicapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.PhotoDraft
import com.codetiger.mymusicapp.ui.Routes
import kotlinx.coroutines.launch
import java.io.File

/** Take Photo opens the camera app; Choose Photo uses Android's photo picker. No permissions (FL-3). */
class PhotoPickers(val takePhoto: () -> Unit, val choosePhoto: () -> Unit)

@Composable
fun rememberPhotoPickers(from: String): PhotoPickers {
    val context = LocalContext.current
    val app = LocalApp.current
    val ui = LocalUi.current
    val nav = LocalNav.current
    val cameraFile = File(File(context.cacheDir, "camera").apply { mkdirs() }, "photo.jpg")
    val cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.files", cameraFile)

    fun make(uri: Uri, temporary: File?) {
        ui.photoDraft.value = PhotoDraft.Making
        nav.navigate(Routes.photoPreview(from))
        app.scope.launch {
            ui.photoDraft.value = try {
                PhotoDraft.Ready(app.drawingMaker.make(uri))
            } catch (e: Exception) {
                PhotoDraft.Failed
            } finally {
                // Only the drawing is kept; the photo is thrown away (FL-5).
                temporary?.delete()
            }
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) make(cameraUri, cameraFile)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) make(uri, null)
    }
    return PhotoPickers(
        takePhoto = { camera.launch(cameraUri) },
        choosePhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
}
