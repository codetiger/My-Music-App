package com.codetiger.mymusicapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

/** The search box: flat `fill`, focus ring while typing, a labelled Clear when there is text. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Size.Target)
            .focusRing(Radius.Md)
            .clip(Radius.Md)
            .background(MusicColors.Fill)
            .padding(start = Space.S4, end = Space.S2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.Icon))
        Box(Modifier.weight(1f).padding(vertical = Space.S3)) {
            if (value.isEmpty()) Text("Type a song or singer", style = MusicType.Body.copy(fontStyle = FontStyle.Italic))
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MusicType.Body.copy(color = MusicColors.Ink),
                cursorBrush = SolidColor(MusicColors.Ink),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search songs" },
            )
        }
        if (value.isNotEmpty()) {
            StackedButton("Clear", R.drawable.ic_close, { onChange("") }, onFill = true, description = "Clear search")
        }
    }
}

/**
 * A plain text box for names (Welcome, New List, Edit Name). [hint] is a line under the box that
 * answers a tap on Save with nothing typed, since a Message would sit behind a dialog.
 */
@Composable
fun NameField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Words,
    autoFocus: Boolean = false,
    hint: String? = null,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Text(label, style = MusicType.BodyStrong)
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Size.Target)
                .focusRing(Radius.Md)
                .clip(Radius.Md)
                .background(MusicColors.Fill)
                .padding(horizontal = Space.S4, vertical = Space.S4),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MusicType.Input.copy(color = MusicColors.Ink),
                cursorBrush = SolidColor(MusicColors.Ink),
                keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = label },
            )
        }
        if (hint != null) {
            Row(
                Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(Space.S3),
            ) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.Icon))
                Text(hint, style = MusicType.Body)
            }
        }
    }
}

/** "Choose one" pills (SortButtons): selected is accent with a check in front. */
@Composable
fun <T> ChoicePills(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Text(label, style = MusicType.BodyStrong)
        FlowRow(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Space.S3),
            verticalArrangement = Arrangement.spacedBy(Space.S3),
        ) {
            options.forEach { (value, text) ->
                val isSelected = value == selected
                Row(
                    Modifier
                        .defaultMinSize(minHeight = Size.Target)
                        .focusRing(Radius.Full)
                        .clip(Radius.Full)
                        .background(if (isSelected) MusicColors.Accent else MusicColors.Fill)
                        .selectable(isSelected, role = Role.RadioButton) { onSelect(value) }
                        .padding(start = if (isSelected) Space.S4 else Space.S5, end = Space.S5),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.S2),
                ) {
                    if (isSelected) {
                        Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MusicColors.OnAccent, modifier = Modifier.size(Size.IconSm))
                    }
                    Text(text, style = MusicType.Button, color = if (isSelected) MusicColors.OnAccent else MusicColors.Ink)
                }
            }
        }
    }
}

