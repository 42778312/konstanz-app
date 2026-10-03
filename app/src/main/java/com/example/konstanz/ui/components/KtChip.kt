package com.example.konstanz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
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
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.outlined

/**
 * 40 dp pill from the design ("Home", "University" on the map; "All", "Bus stops" in search).
 * Unselected: white with a 1.5 dp outline. Selected: ink with white text.
 */
@Composable
fun KtChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: KtIcon? = null,
    selected: Boolean = false,
) {
    val content = if (selected) White else Ink
    Row(
        modifier
            .height(40.dp)
            .then(if (selected) Modifier else Modifier.outlined(Radius.Pill))
            .clip(Radius.Pill)
            .background(if (selected) Ink else White)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) KonstanzIcon(icon, contentDescription = null, size = 18.dp, tint = content)
        Text(text, style = KonstanzType.BodySmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = content, maxLines = 1)
    }
}
