package com.codetiger.mymusicapp.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The one button a Message may carry: a word and its icon, like every button. */
class MessageAction(val label: String, val icon: Int, val run: () -> Unit)

/** One plain sentence at the bottom, with an optional action (Message). */
data class UiMessage(
    val text: String,
    val icon: Int = R.drawable.ic_info,
    val action: MessageAction? = null,
    val id: Long = System.nanoTime(),
    val shownAt: Long = System.currentTimeMillis(),
)

/** Holds the message on screen; anything can post one. */
class MessageCenter {
    private val _current = MutableStateFlow<UiMessage?>(null)
    val current: StateFlow<UiMessage?> = _current.asStateFlow()

    fun show(message: UiMessage) {
        _current.value = message
    }

    fun show(text: String, icon: Int = R.drawable.ic_info, action: MessageAction? = null) =
        show(UiMessage(text, icon, action))

    /** The screen changed: a message with an action goes, unless it was posted just now for the new screen. */
    fun onScreenChanged() {
        val m = _current.value ?: return
        if (m.action != null && System.currentTimeMillis() - m.shownAt > 1_000) _current.value = null
    }

    fun dismiss(id: Long? = null) {
        if (id == null || _current.value?.id == id) _current.value = null
    }
}

/**
 * Stays 10 seconds or until tapped, longer if Android's accessibility timeout asks. With an
 * action it stays until used or the screen changes. No sound, no vibration.
 */
@Composable
fun MessageBar(center: MessageCenter, modifier: Modifier = Modifier) {
    val message by center.current.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message?.id) {
        val m = message ?: return@LaunchedEffect
        if (m.action != null) return@LaunchedEffect
        val a11y = context.getSystemService(AccessibilityManager::class.java)
        val timeout = a11y.getRecommendedTimeoutMillis(10_000, AccessibilityManager.FLAG_CONTENT_TEXT or AccessibilityManager.FLAG_CONTENT_ICONS)
        delay(timeout.toLong())
        center.dismiss(m.id)
    }
    AnimatedVisibility(message != null, modifier, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        val m = message ?: return@AnimatedVisibility
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.S4)
                .focusRing(Radius.Md)
                .clip(Radius.Md)
                .background(MusicColors.Fill)
                .clickable { center.dismiss(m.id) }
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(Space.S4),
            horizontalArrangement = Arrangement.spacedBy(Space.S4),
            verticalArrangement = Arrangement.spacedBy(Space.S3),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.S3), modifier = Modifier.weight(1f, fill = true)) {
                Icon(painterResource(m.icon), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.Icon))
                Text(m.text, style = MusicType.Body)
            }
            m.action?.let { a ->
                MusicButton(a.label, a.icon, { center.dismiss(m.id); a.run() }, onFill = true)
            }
        }
    }
}
