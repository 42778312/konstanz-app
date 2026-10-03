package com.example.konstanz.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.Spacing
import com.example.konstanz.ui.theme.SwitchTrackOff
import com.example.konstanz.ui.theme.White

/** Tracked-caps section header ("NEXT DEPARTURES") with an optional trailing action. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = KonstanzType.Label, color = Ink3)
        trailing?.invoke()
    }
}

/** A labelled white card holding [SettingsRow]s, as on Settings and About. */
@Composable
fun SettingsGroup(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (title != null) SectionLabel(title)
        Column(
            Modifier.fillMaxWidth().clip(Radius.Group).background(White),
            content = content,
        )
    }
}

/**
 * 56 dp list row: optional icon, title + subtitle, then a value, a chevron or a custom [trailing].
 * Rows with [onClick] are buttons; set [showDivider] = false on the last row of a group.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: KtIcon? = null,
    iconColor: Color? = null,
    value: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .heightIn(min = 56.dp)
                .padding(horizontal = Spacing.m, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null && iconColor != null) {
                Box(Modifier.size(32.dp).background(iconColor, CircleShape), contentAlignment = Alignment.Center) {
                    KonstanzIcon(icon, contentDescription = null, size = 18.dp, tint = White)
                }
            } else if (icon != null) KonstanzIcon(icon, contentDescription = null, size = 22.dp, tint = Ink2)
            Column(Modifier.weight(1f)) {
                Text(title, style = KonstanzType.RowTitle, color = Ink)
                if (subtitle != null) Text(subtitle, style = KonstanzType.Caption, color = Ink3)
            }
            if (value != null) {
                Text(value, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink3)
            }
            trailing?.invoke()
            if (showChevron) {
                KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 16.dp, tint = Ink4, strokeWidth = 2.4f)
            }
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

/** Settings row with the design's 50×30 switch; the whole row toggles. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: KtIcon? = null,
    showDivider: Boolean = true,
    /** A white icon on a coloured disc (kinds of places) instead of a grey icon. */
    iconColor: Color? = null,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconColor = iconColor,
        showDivider = showDivider,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        trailing = { KtSwitchVisual(checked) },
    )
}

/** Visual only — interaction lives on the row so the 56 dp row is the touch target. */
@Composable
private fun KtSwitchVisual(checked: Boolean) {
    val track by animateColorAsState(if (checked) Primary else SwitchTrackOff, label = "track")
    val thumbX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "thumb")
    Box(Modifier.size(width = 50.dp, height = 30.dp).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(thumbX.roundToPx(), 2.dp.roundToPx()) }
                .size(26.dp)
                .dropShadow(CircleShape, Shadow(radius = 3.dp, offset = DpOffset(0.dp, 1.dp), color = Color.Black, alpha = 0.25f))
                .background(White, CircleShape)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F5F7, widthDp = 390)
@Composable
private fun ListsPreview() {
    KonstanzTheme {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(26.dp)) {
            SettingsGroup("General") {
                SettingsRow("Language", value = "English", onClick = {})
                SettingsRow("Units", value = "Metric", onClick = {})
                SettingsRow("Theme", value = "System", onClick = {}, showDivider = false)
            }
            SettingsGroup("Map") {
                SwitchRow("Show bus stops", checked = true, onCheckedChange = {})
                SwitchRow("Show walking paths", checked = false, onCheckedChange = {}, showDivider = false)
            }
            SettingsGroup("Data sources") {
                SettingsRow("Map data", subtitle = "© OpenStreetMap contributors · ODbL", icon = KtIcons.Map)
                SettingsRow("Privacy policy", icon = KtIcons.Shield, showChevron = true, onClick = {}, showDivider = false)
            }
        }
    }
}
