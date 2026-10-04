package com.codetiger.mymusicapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Size
import com.codetiger.mymusicapp.ui.theme.Space

/** design-system/components/Button. Primary is the screen's one main action. */
enum class ButtonKind { Primary, Secondary }

private fun colorsFor(kind: ButtonKind, onFill: Boolean): Pair<Color, Color> = when (kind) {
    ButtonKind.Primary -> MusicColors.Accent to MusicColors.OnAccent
    ButtonKind.Secondary -> (if (onFill) MusicColors.Surface else MusicColors.Fill) to MusicColors.Ink
}

/** A flat shape with an icon and a word. Set [onFill] when it sits on a `fill` area. */
@Composable
fun MusicButton(
    text: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    onFill: Boolean = false,
) {
    val (container, content) = colorsFor(kind, onFill)
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = Size.Target, minHeight = Size.Target)
            .clip(Radius.Md)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.S5, vertical = Space.S2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(Size.Icon))
            Spacer(Modifier.width(Space.S3))
        }
        Text(text, style = MusicType.Button, color = content, textAlign = TextAlign.Center)
    }
}

/** Full-width button, for the main action and dialog buttons. */
@Composable
fun WideButton(
    text: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Secondary,
    onFill: Boolean = false,
) = MusicButton(text, icon, onClick, modifier.fillMaxWidth(), kind, onFill)

/** A secondary button whose label says its state; when on it takes the accent ("Shuffle: On"). */
@Composable
fun ToggleButton(
    text: String,
    @DrawableRes icon: Int,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (on) MusicColors.Accent else MusicColors.Fill
    val content = if (on) MusicColors.OnAccent else MusicColors.Ink
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = Size.Target, minHeight = Size.Target)
            .clip(Radius.Md)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.S4, vertical = Space.S2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(Size.Icon))
        Spacer(Modifier.width(Space.S3))
        Text(text, style = MusicType.Button, color = content, textAlign = TextAlign.Center)
    }
}

/** Icon above a short word, for tight spots: title bar Settings, row Play, bar Pause. */
@Composable
fun StackedButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFill: Boolean = false,
    description: String? = null,
) {
    val (container, content) = colorsFor(ButtonKind.Secondary, onFill)
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = Size.Target, minHeight = Size.Target)
            .clip(Radius.Md)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = Space.S3, vertical = Space.S1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(Size.Icon))
        Spacer(Modifier.size(2.dp))
        Text(text, style = MusicType.ControlLabel, color = content, textAlign = TextAlign.Center)
    }
}

/** The one 96dp main button on Home and Add Song: accent, a disc with the icon, big label. */
@Composable
fun HeroButton(
    text: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subText: String? = null,
    subFirst: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Size.Play)
            .clip(Radius.Lg)
            .background(MusicColors.Accent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = Space.S3, end = Space.S5, top = Space.S3, bottom = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        Box(
            Modifier.size(Size.Transport).clip(CircleShape).background(MusicColors.OnAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MusicColors.Accent, modifier = Modifier.size(Size.IconLg))
        }
        Column(Modifier.weight(1f)) {
            if (subText != null && subFirst) Text(subText, style = MusicType.Body, color = MusicColors.OnAccent)
            Text(text, style = MusicType.ButtonHero, color = MusicColors.OnAccent)
            if (subText != null && !subFirst) Text(subText, style = MusicType.Body, color = MusicColors.OnAccent)
        }
    }
}

/** Two buttons sharing a row at equal width (design-system "Pairs"). */
@Composable
fun ButtonPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Both buttons take the taller one's height.
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.S4)) {
        first(Modifier.weight(1f).fillMaxHeight())
        second(Modifier.weight(1f).fillMaxHeight())
    }
}

/** A round 36dp tick box in a 64dp target (SongPreviewCard). */
@Composable
fun TickBox(checked: Boolean, onChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(Size.Target)
            .clickable(role = Role.Checkbox) { onChange(!checked) }
            .semantics {
                contentDescription = label
                stateDescription = if (checked) "Ticked" else "Not ticked"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(Radius.Sm).background(if (checked) MusicColors.Accent else MusicColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painterResource(com.codetiger.mymusicapp.R.drawable.ic_check),
                    contentDescription = null,
                    tint = MusicColors.OnAccent,
                    modifier = Modifier.size(Size.Icon),
                )
            }
        }
    }
}
