package com.codetiger.mymusicapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.PhotoDraft
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.components.ButtonKind
import com.codetiger.mymusicapp.ui.components.NameField
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.launch

/** A first-launch page: big title, words, then the buttons. */
@Composable
private fun WelcomePage(title: String, body: String?, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.padding(top = Space.S5).size(Size.Play))
        Text(title, style = MusicType.SongHero, modifier = Modifier.semantics { heading() })
        if (body != null) Text(body, style = MusicType.Body)
        content()
    }
}

/** FL-1: "What is your name?" with Continue and Skip. */
@Composable
fun WelcomeNameScreen() {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    var name by rememberSaveable { mutableStateOf("") }
    val next = {
        if (name.isBlank()) {
            ui.messages.show("Type your name, or tap Skip.")
        } else {
            app.scope.launch { app.settings.setUserName(name) }
            nav.navigate(Routes.WELCOME_PHOTO)
        }
    }
    WelcomePage("Welcome", "This app keeps your songs on your phone and plays them with one tap.") {
        NameField(name, { name = it }, "What is your name?", onDone = next)
        WideButton("Continue", R.drawable.ic_chevron_right, next, kind = ButtonKind.Primary)
        WideButton("Skip", null, {
            app.scope.launch { app.settings.setUserName(null) }
            nav.navigate(Routes.WELCOME_PHOTO)
        })
        com.codetiger.mymusicapp.ui.components.MessageBar(ui.messages)
    }
}

/** FL-3: "Add your photo?" with Take Photo, Choose Photo and Skip. */
@Composable
fun WelcomePhotoScreen() {
    val nav = LocalNav.current
    val pickers = rememberPhotoPickers(Routes.FROM_WELCOME)
    WelcomePage(
        "Add your photo?",
        "Your photo becomes a simple drawing that shows at the top of the app. The photo itself is not kept.",
    ) {
        WideButton("Take Photo", R.drawable.ic_photo_camera, pickers.takePhoto, kind = ButtonKind.Primary)
        WideButton("Choose Photo", R.drawable.ic_image, pickers.choosePhoto)
        WideButton("Skip", null, { nav.navigate(Routes.setup(Routes.FROM_WELCOME)) })
    }
}

/** FL-5: the drawing large, with Use This and Try Again. */
@Composable
fun PhotoPreviewScreen(from: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    val draft by ui.photoDraft.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Your drawing", style = MusicType.SongHero, modifier = Modifier.fillMaxWidth().padding(top = Space.S5).semantics { heading() })
        Box(Modifier.size(Size.ArtHero), contentAlignment = Alignment.Center) {
            when (val d = draft) {
                is PhotoDraft.Ready -> Image(
                    d.result.drawing.asImageBitmap(),
                    contentDescription = "Your photo drawing",
                    modifier = Modifier.size(Size.ArtHero).clip(CircleShape),
                )
                else -> Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = Modifier.size(Size.ArtHero))
            }
        }
        when (val d = draft) {
            PhotoDraft.Making, PhotoDraft.None -> Text("Making your drawing…", style = MusicType.Heading, textAlign = TextAlign.Center)
            PhotoDraft.Failed -> {
                Text("This photo couldn't be used. Try another one.", style = MusicType.Body, textAlign = TextAlign.Center)
                WideButton("Try Again", R.drawable.ic_photo_camera, { nav.popBackStack() }, kind = ButtonKind.Primary)
            }
            is PhotoDraft.Ready -> {
                WideButton("Use This", R.drawable.ic_check, {
                    app.scope.launch { app.photoStore.save(d.result) }
                    ui.photoDraft.value = PhotoDraft.None
                    if (from == Routes.FROM_WELCOME) {
                        nav.navigate(Routes.setup(Routes.FROM_WELCOME)) { popUpTo(Routes.WELCOME_PHOTO) }
                    } else {
                        nav.popBackStack()
                    }
                }, kind = ButtonKind.Primary)
                WideButton("Try Again", R.drawable.ic_photo_camera, {
                    ui.photoDraft.value = PhotoDraft.None
                    nav.popBackStack()
                })
            }
        }
    }
}
