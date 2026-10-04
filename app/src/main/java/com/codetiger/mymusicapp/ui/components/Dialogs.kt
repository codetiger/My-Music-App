package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Space

/** The surface panel on the scrim, used by every dialog. Tapping outside or Back cancels. */
@Composable
fun DialogPanel(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(horizontal = Space.S4, vertical = Space.S6)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(Radius.Lg)
                .background(MusicColors.Surface)
                .verticalScroll(rememberScrollState())
                .padding(Space.S5),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
        ) { content() }
    }
}

/**
 * Asks once before anything is removed, deleted or very large. The confirm action repeats the
 * verb and is the primary button on top; Cancel below (ConfirmDialog).
 */
@Composable
fun ConfirmDialog(
    question: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    detail: String? = null,
    confirmIcon: Int? = null,
) {
    DialogPanel(onDismiss) {
        Text(question, style = MusicType.Heading, modifier = Modifier.semantics { heading() })
        if (detail != null) Text(detail, style = MusicType.Body)
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
            WideButton(confirmLabel, confirmIcon, { onDismiss(); onConfirm() }, kind = ButtonKind.Primary)
            WideButton("Cancel", null, onDismiss)
        }
    }
}

/** A name box in a dialog: New List, Rename List, Change Name. */
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val confirm = { if (text.isNotBlank()) { onDismiss(); onConfirm(text.trim()) } }
    DialogPanel(onDismiss) {
        Text(title, style = MusicType.Heading, modifier = Modifier.semantics { heading() })
        NameField(text, { text = it }, label, onDone = confirm, autoFocus = true)
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
            WideButton(confirmLabel, null, confirm, kind = ButtonKind.Primary)
            WideButton("Cancel", null, onDismiss)
        }
    }
}

/** Title and artist together (Edit Name, LIB-5). */
@Composable
fun EditSongDialog(
    title: String,
    artist: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var t by rememberSaveable { mutableStateOf(title) }
    var a by rememberSaveable { mutableStateOf(artist) }
    DialogPanel(onDismiss) {
        Text("Edit Name", style = MusicType.Heading, modifier = Modifier.semantics { heading() })
        NameField(t, { t = it }, "Song name", autoFocus = true)
        NameField(a, { a = it }, "Singer or artist")
        Column(Modifier.padding(top = Space.S2), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
            WideButton("Save", null, { if (t.isNotBlank()) { onDismiss(); onConfirm(t.trim(), a.trim()) } }, kind = ButtonKind.Primary)
            WideButton("Cancel", null, onDismiss)
        }
    }
}

/** A plain list of words in a dialog with OK (See Names). */
@Composable
fun ListDialog(title: String, lines: List<String>, onDismiss: () -> Unit) {
    DialogPanel(onDismiss) {
        Text(title, style = MusicType.Heading, modifier = Modifier.semantics { heading() })
        lines.forEach { Text(it, style = MusicType.Body) }
        WideButton("OK", null, onDismiss, kind = ButtonKind.Primary)
    }
}
