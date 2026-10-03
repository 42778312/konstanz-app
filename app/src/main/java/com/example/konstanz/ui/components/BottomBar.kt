package com.example.konstanz.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White

/** One destination in the bottom bar. */
data class BottomBarItem(val label: String, val icon: KtIcon)

/**
 * Main bottom bar from artboards 07 / 27 / 28: white, 1 dp top rule, equal-width tabs 56 dp tall,
 * 22 dp icon over a 12 sp bold label. The selected tab is brand red, the others ink-3.
 * Sits above the system navigation bar (the design's 14 px bottom padding is the home indicator).
 */
@Composable
fun KonstanzBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(Surface).navigationBarsPadding()) {
        HorizontalDivider(thickness = 1.dp, color = Line)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).selectableGroup()) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val color by animateColorAsState(if (selected) Primary else Ink3, label = "tab-color")
                Column(
                    Modifier
                        .weight(1f)
                        .height(56.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    KonstanzIcon(item.icon, contentDescription = null, size = 22.dp, tint = color)
                    Text(
                        item.label,
                        style = KonstanzType.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 390)
@Composable
private fun BottomBarPreview() {
    KonstanzTheme {
        KonstanzBottomBar(
            items = listOf(
                BottomBarItem("Map", KtIcons.Map),
                BottomBarItem("Saved", KtIcons.Star),
                BottomBarItem("Settings", KtIcons.Sliders),
            ),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
