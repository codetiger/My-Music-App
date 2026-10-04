package com.codetiger.mymusicapp.ui

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.codetiger.mymusicapp.AppContainer
import com.codetiger.mymusicapp.photo.DrawingMaker
import com.codetiger.mymusicapp.ui.components.MessageCenter
import kotlinx.coroutines.flow.MutableStateFlow

/** Something shared into the app from another app (ADD-2, ADD-8). */
sealed interface IncomingShare {
    data class Text(val text: String) : IncomingShare
    data class Files(val uris: List<Uri>) : IncomingShare
}

/** The drawing being made from a new photo, between the photo screen and its preview. */
sealed interface PhotoDraft {
    data object None : PhotoDraft
    data object Making : PhotoDraft
    data class Ready(val result: DrawingMaker.Result) : PhotoDraft
    data object Failed : PhotoDraft
}

/** State that lives as long as the app's screens, shared between them. */
class UiState {
    val messages = MessageCenter()
    val incoming = MutableStateFlow<IncomingShare?>(null)
    val openPlayer = MutableStateFlow(false)
    val photoDraft = MutableStateFlow<PhotoDraft>(PhotoDraft.None)
}

val LocalApp = staticCompositionLocalOf<AppContainer> { error("No app") }
val LocalNav = staticCompositionLocalOf<NavHostController> { error("No navigation") }
val LocalUi = staticCompositionLocalOf<UiState> { error("No UI state") }
