package com.example.konstanz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow

/**
 * Small floating system-status pill ("Offline · Using saved data"). Status lives here, never in
 * a blocking dialog; tapping it opens Offline data (Part J).
 */
@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: KtIcon = KtIcons.CloudOff,
    onClick: (() -> Unit)? = null,
    iconTint: androidx.compose.ui.graphics.Color = Ink3,
    /** Replaces the icon, e.g. a spinner while updating. */
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .height(34.dp)
            .floatingShadow(Radius.Pill)
            .clip(Radius.Pill)
            .background(White)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = 10.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) leading() else KonstanzIcon(icon, contentDescription = null, size = 16.dp, tint = iconTint, strokeWidth = 2.4f)
        Text(text, style = KonstanzType.Caption.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink2, maxLines = 1)
    }
}
