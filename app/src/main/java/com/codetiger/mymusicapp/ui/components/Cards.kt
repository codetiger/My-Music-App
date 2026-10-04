package com.codetiger.mymusicapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

/** The flat `fill` card: Home notices, song previews. Buttons inside are `surface` shapes. */
@Composable
fun FlatCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(Radius.Md).background(MusicColors.Fill).padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
        content = content,
    )
}

/** One Home notice (HOME-4): icon, headline, detail, and its buttons, the doing action first. */
@Composable
fun NoticeCard(
    @DrawableRes icon: Int,
    headline: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actions: @Composable () -> Unit = {},
) {
    FlatCard(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S3)) {
            Icon(painterResource(icon), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.IconMd))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(headline, style = MusicType.Heading)
                if (detail != null) Text(detail, style = MusicType.Body)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
            actions()
        }
    }
}

/** A list tile on Home and in Add to List: same `fill` for all, told apart by name, icon and picture. */
@Composable
fun ListTile(
    name: String,
    count: Int,
    @DrawableRes icon: Int,
    picture: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .heightIn(min = Size.Tile)
            .focusRing(Radius.Md)
            .clip(Radius.Md)
            .background(MusicColors.Fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        if (picture != null) {
            SongArt(picture, onFill = true)
        } else {
            Icon(painterResource(icon), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.IconLg))
        }
        Spacer(Modifier.weight(1f, fill = false))
        Column {
            Text(name, style = MusicType.Button)
            Text(com.codetiger.mymusicapp.ui.Format.songCount(count), style = MusicType.Body)
        }
    }
}

/** "New List": `surface`, with a plus in a `fill` circle. */
@Composable
fun NewListTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .heightIn(min = Size.Tile)
            .focusRing(Radius.Md)
            .clip(Radius.Md)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S3),
    ) {
        Box(Modifier.size(Size.IconXl).clip(CircleShape).background(MusicColors.Fill), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.IconLg))
        }
        Text("New List", style = MusicType.Button)
    }
}

/** A full-width row with a round mark, for Text Size and Default Playlist (ChoiceRow). */
@Composable
fun ChoiceRow(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Size.Row)
            .focusRing(Radius.Md)
            .clip(Radius.Md)
            .background(if (selected) MusicColors.Accent else MusicColors.Fill)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(MusicColors.Surface), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(14.dp).clip(CircleShape).background(MusicColors.Accent))
        }
        Text(text, style = MusicType.Button, color = if (selected) MusicColors.OnAccent else MusicColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** One item on Settings: name, current value in words, chevron. */
@Composable
fun SettingsRow(
    name: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    @DrawableRes valueIcon: Int? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Size.RowTall)
            .focusRing(Radius.Md)
            .clip(Radius.Md)
            .background(MusicColors.Fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Space.S4, vertical = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(name, style = MusicType.BodyStrong)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.S1)) {
                if (valueIcon != null) Icon(painterResource(valueIcon), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.IconSm))
                Text(value, style = MusicType.Body)
            }
        }
        Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MusicColors.Ink, modifier = Modifier.size(Size.Icon))
    }
}
