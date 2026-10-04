package com.codetiger.mymusicapp.ui.screens

import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.add.LinkPreview
import com.codetiger.mymusicapp.add.displayName
import com.codetiger.mymusicapp.ui.Format
import com.codetiger.mymusicapp.ui.LocalApp
import com.codetiger.mymusicapp.ui.LocalUi
import com.codetiger.mymusicapp.ui.TabScreen
import com.codetiger.mymusicapp.ui.components.ButtonKind
import com.codetiger.mymusicapp.ui.components.ConfirmDialog
import com.codetiger.mymusicapp.ui.components.FlatCard
import com.codetiger.mymusicapp.ui.components.HeroButton
import com.codetiger.mymusicapp.ui.components.NoticeCard
import com.codetiger.mymusicapp.ui.components.PageHeader
import com.codetiger.mymusicapp.ui.components.SongArt
import com.codetiger.mymusicapp.ui.components.Tab
import com.codetiger.mymusicapp.ui.components.TickBox
import com.codetiger.mymusicapp.ui.components.WideButton
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

private const val ONE_GB = 1024L * 1024 * 1024

/** Add a Song: Paste Link, Pick a File, and the preview cards (ADD-1 to ADD-9). */
@Composable
fun AddSongScreen() {
    val app = LocalApp.current
    val ui = LocalUi.current
    val context = LocalContext.current
    val vm: AddSongViewModel = viewModel { AddSongViewModel(app) }
    val state by vm.state.collectAsStateWithLifecycle()
    val incoming by ui.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(incoming) {
        incoming?.let {
            ui.incoming.value = null
            vm.handle(it)
        }
    }
    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.handleFiles(uris)
    }
    val lowSpace = remember(state.items.size) { app.files.audioDir.usableSpace < ONE_GB }

    TabScreen(Tab.AddSong) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Space.S4, vertical = Space.S2),
            verticalArrangement = Arrangement.spacedBy(Space.S4),
        ) {
            PageHeader("Add a Song")
            // Once there is something to save, Save Song is the one accent and Paste Link steps back.
            HeroButton("Paste Link", R.drawable.ic_content_paste, {
                val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
                vm.paste(clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString())
            }, subText = "Uses the link you copied", kind = if (state.items.isEmpty()) ButtonKind.Primary else ButtonKind.Secondary)
            HeroButton("Pick a File", R.drawable.ic_audio_file, {
                pickFiles.launch(arrayOf("audio/*", "video/mp4", "application/ogg"))
            }, subText = "Song from phone or WhatsApp", kind = ButtonKind.Secondary)
            if (lowSpace) {
                NoticeCard(R.drawable.ic_sd_card_alert, "Your phone is nearly full", detail = "Remove songs you don't need to make room for new ones.")
            }
            if (state.readingFiles) {
                FlatCard { Text("Reading the file…", style = MusicType.Heading) }
            }
            val many = state.items.size > 1
            state.items.forEach { item -> PreviewCard(item, many, vm) }
            if (state.items.isNotEmpty()) {
                val count = if (many) state.items.count { it.ticked && (it !is AddItem.Link || it.canSave) } else 1
                val anyReady = state.items.any { it !is AddItem.Link || it.preview != null }
                if (anyReady) {
                    WideButton(
                        if (count == 1) "Save Song" else "Save $count Songs",
                        R.drawable.ic_download,
                        { if (!state.saving) vm.save() },
                        kind = ButtonKind.Primary,
                    )
                }
                WideButton("Cancel", R.drawable.ic_close, vm::clear)
            }
        }
    }

    state.askLong?.let { question ->
        ConfirmDialog(question, "Save", onConfirm = { vm.save(confirmedLong = true) }, onDismiss = vm::cancelLong, confirmIcon = R.drawable.ic_download)
    }
}

/** SongPreviewCard: picture, title, "From YouTube · 4:05"; a tick box when there are several. */
@Composable
private fun PreviewCard(item: AddItem, many: Boolean, vm: AddSongViewModel) {
    val (title, meta, picture) = when (item) {
        is AddItem.Link -> when (val p = item.preview) {
            null -> Triple("Reading the link…", "From ${item.link.type.displayName}", null)
            is LinkPreview.Ready -> Triple(
                p.info.title,
                "From ${p.info.sourceType.displayName}" + (if (p.info.durationMs > 0) " · ${Format.time(p.info.durationMs)}" else ""),
                p.info.thumbnailUrl,
            )
            is LinkPreview.Later -> Triple(
                "Song from ${item.link.type.displayName}",
                if (p.offline) "No internet now. It will download later." else "The site isn't answering now. It will download later.",
                null,
            )
            else -> Triple("", "", null)
        }
        is AddItem.File -> Triple(
            item.preview.title,
            "From ${item.preview.sourceType.displayName}" + (if (item.preview.durationMs > 0) " · ${Format.time(item.preview.durationMs)}" else ""),
            null,
        )
    }
    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
            val canTick = item !is AddItem.Link || item.canSave
            if (many && canTick) TickBox(item.ticked, { vm.setTicked(item.key, it) }, "Save $title")
            if (picture != null) {
                coil3.compose.AsyncImage(
                    model = picture,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(Size.ArtCard).clip(Radius.Sm).background(MusicColors.Surface),
                )
            } else {
                SongArt(null, size = Size.ArtCard, onFill = true, noteSize = Size.IconLg)
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(title, style = MusicType.BodyStrong)
                Text(meta, style = MusicType.Body)
            }
        }
    }
}
