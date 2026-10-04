package com.codetiger.mymusicapp.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.setup.SetupStep
import com.codetiger.mymusicapp.ui.BackScreen
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalNav
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.components.ButtonKind
import com.codetiger.mymusicapp.ui.components.ScreenTitle
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.rememberSettings
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.launch

private data class StepText(val title: String, val sentence: String, val control: String, val tapHint: String)

private fun textFor(step: SetupStep) = when (step) {
    SetupStep.AllowUpdates -> StepText(
        "Allow updates",
        "Let this app install its own updates, so you always have the newest version.",
        "Allow from this source", "Turn this on on the next screen",
    )
    SetupStep.KeepPlaying -> StepText(
        "Keep music playing",
        "Stop the phone from pausing music and downloads when the screen is off.",
        "Allow", "Tap Allow on the next screen",
    )
    SetupStep.Autostart -> StepText(
        "Start on its own",
        "Let the app start by itself, so music and downloads keep going on this phone.",
        "My Music App", "Turn this on on the next screen",
    )
    SetupStep.AndroidAuto -> StepText(
        "Use in the car",
        "Android Auto shows this app only after one setting is changed. Follow these steps once.",
        "", "",
    )
}

/** Phone Setup (FL-7): one step per screen, with Open Settings and Skip. */
@Composable
fun SetupScreen(from: String) {
    val app = LocalApp.current
    val nav = LocalNav.current
    val ui = LocalUi.current
    val context = LocalContext.current
    val settings = rememberSettings()
    val steps = remember { app.phoneSetup.steps() }
    var index by rememberSaveable { mutableIntStateOf(steps.indexOfFirst { !app.phoneSetup.isDone(it, settings) }.coerceAtLeast(0)) }
    var openedAutostart by rememberSaveable { mutableStateOf(false) }
    // Android doesn't tell us when its page closes; check again whenever we come back.
    var checks by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        checks++
        if (openedAutostart) {
            openedAutostart = false
            app.scope.launch { app.settings.setAutostartDone() }
        }
    }

    val finish: () -> Unit = {
        if (from == Routes.FROM_WELCOME) {
            app.scope.launch { app.settings.setWelcomeDone() }
            nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
        } else {
            nav.popBackStack()
        }
    }
    val next: () -> Unit = { if (index < steps.lastIndex) index++ else finish() }
    val step = steps[index]
    val text = textFor(step)
    val done = remember(checks, settings, step) { app.phoneSetup.isDone(step, settings) }

    BackScreen("Phone Setup", onBack = { if (index > 0) index-- else nav.popBackStack() ; Unit }) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
        ) {
            Text("Step ${index + 1} of ${steps.size}", style = MusicType.BodyStrong)
            ScreenTitle(text.title)
            Text(text.sentence, style = MusicType.Body)
            if (step == SetupStep.AndroidAuto) AndroidAutoSteps() else ControlPicture(step, text)
            if (done) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
                    Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.IconMd))
                    Text("Done", style = MusicType.BodyStrong)
                }
                WideButton("Next", R.drawable.ic_chevron_right, next, kind = ButtonKind.Primary)
            } else if (step == SetupStep.AndroidAuto) {
                WideButton("I Have Done This", R.drawable.ic_check, {
                    app.scope.launch { app.settings.setAndroidAutoDone() }
                }, kind = ButtonKind.Primary)
                WideButton("Skip", R.drawable.ic_skip_next, next)
            } else {
                WideButton("Open Settings", R.drawable.ic_open_in_new, {
                    val intent = app.phoneSetup.intent(step)
                    try {
                        if (intent != null) context.startActivity(intent)
                        if (step == SetupStep.Autostart) openedAutostart = true
                    } catch (e: ActivityNotFoundException) {
                        runCatching { context.startActivity(app.phoneSetup.appDetails()) }
                        if (step == SetupStep.Autostart) openedAutostart = true
                    } catch (e: SecurityException) {
                        ui.messages.show("This phone doesn't allow opening that page. You can skip this step.")
                    }
                }, kind = ButtonKind.Primary)
                WideButton("Skip", R.drawable.ic_skip_next, next)
            }
        }
    }
}

/** A flat panel drawing the one Android control to tap (SetupStep picture). */
@Composable
private fun ControlPicture(step: SetupStep, text: StepText) {
    Column(
        Modifier.fillMaxWidth().clip(Radius.Md).background(MusicColors.Fill).padding(Space.S5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        if (step == SetupStep.KeepPlaying) {
            Box(
                Modifier.clip(Radius.Md).background(MusicColors.Accent).padding(horizontal = Space.S6, vertical = Space.S3),
            ) { Text(text.control, style = MusicType.Button, color = MusicColors.OnAccent) }
        } else {
            Row(
                Modifier.fillMaxWidth().clip(Radius.Sm).background(MusicColors.Surface).padding(horizontal = Space.S4, vertical = Space.S3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.S4),
            ) {
                Text(text.control, style = MusicType.BodyStrong, modifier = Modifier.weight(1f))
                // An "on" switch: accent track with a surface knob.
                Box(Modifier.size(width = 52.dp, height = 32.dp).clip(CircleShape).background(MusicColors.Accent), contentAlignment = Alignment.CenterEnd) {
                    Box(Modifier.padding(4.dp).size(24.dp).clip(CircleShape).background(MusicColors.OnAccent))
                }
            }
        }
        Text(text.tapHint, style = MusicType.Body, textAlign = TextAlign.Center)
    }
}

/** Android Auto has no direct link to this page, so the steps are written out. */
@Composable
private fun AndroidAutoSteps() {
    val steps = listOf(
        "Open the Android Auto app. If you can't find it, open Settings, tap Connected devices, then Android Auto.",
        "Scroll to the bottom and tap Version 10 times. Tap OK when asked.",
        "Tap the three dots at the top right, then Developer settings.",
        "Turn on Unknown sources.",
    )
    Column(
        Modifier.fillMaxWidth().clip(Radius.Md).background(MusicColors.Fill).padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        steps.forEachIndexed { i, s ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
                Box(Modifier.size(Size.Icon + 8.dp).clip(CircleShape).background(MusicColors.Surface), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", style = MusicType.BodyStrong)
                }
                Text(s, style = MusicType.Body, modifier = Modifier.weight(1f))
            }
        }
    }
}
