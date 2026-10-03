package com.example.konstanz.ui.components

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

/**
 * Pushed-screen bar (About, Recent searches…): 48 dp back button, 20 sp / 800 title, optional
 * trailing actions. Design: 112 px including the status bar → 60 dp below it.
 */
@Composable
fun KtTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Second line, 13 sp ink-3 ("Bus stop · Platforms A–D"). */
    subtitle: String? = null,
    /** Shown before the title on the same line, e.g. a line badge. */
    titleLeading: (@Composable () -> Unit)? = null,
    background: Color = White,
    showDivider: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().background(background).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(Radius.Button)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.back), onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.ChevronLeft, contentDescription = stringResource(R.string.back), size = 22.dp, tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    titleLeading?.invoke()
                    Text(
                        title,
                        modifier = Modifier.semantics { heading() },
                        style = KonstanzType.BarTitle,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (subtitle != null) {
                    Text(subtitle, style = KonstanzType.Caption, color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            actions()
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

/** Text action in a top bar ("Clear all", "Edit"): 44 dp tall, 16 sp bold. */
@Composable
fun TopBarTextAction(text: String, onClick: () -> Unit, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(44.dp)
            .clip(Radius.Button)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = KonstanzType.RowTitle, color = color)
    }
}

/** Transparent 44 dp icon button for top bars (save, share). */
@Composable
fun TopBarIconButton(icon: com.example.konstanz.ui.icons.KtIcon, label: String, onClick: () -> Unit, tint: Color = Ink, filled: Boolean = false) {
    Box(
        Modifier
            .size(44.dp)
            .clip(Radius.Button)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(icon, contentDescription = label, size = 22.dp, tint = tint, filled = filled)
    }
}
