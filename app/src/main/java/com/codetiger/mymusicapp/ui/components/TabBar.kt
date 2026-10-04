package com.codetiger.mymusicapp.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.codetiger.mymusicapp.R
import com.codetiger.mymusicapp.ui.Routes
import com.codetiger.mymusicapp.ui.theme.MusicColors
import com.codetiger.mymusicapp.ui.theme.MusicType
import com.codetiger.mymusicapp.ui.theme.Radius
import com.codetiger.mymusicapp.ui.theme.Space

enum class Tab(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
) {
    Home(Routes.HOME, R.string.tab_home, R.drawable.ic_home, R.drawable.ic_home_filled),
    AddSong(Routes.ADD, R.string.tab_add_song, R.drawable.ic_add_circle, R.drawable.ic_add_circle_filled),
}

/** Two tabs: Home holds the lists and every song. Selected: `fill` shape, bold word, filled icon. */
@Composable
fun TabBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.S3, vertical = Space.S2)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Space.S2),
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp)
                    .clip(Radius.Md)
                    .background(if (isSelected) MusicColors.Fill else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(vertical = Space.S2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                    contentDescription = null,
                    tint = MusicColors.Ink,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    stringResource(tab.label),
                    style = MusicType.ControlLabel.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
