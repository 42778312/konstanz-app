package com.example.konstanz.ui.plan

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.KonstanzType
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private val RowHeight = 36.dp
private const val VisibleRows = 5
/** Unselected wheel values (#9DA1A9 in the design). */
private val WheelGrey = Color(0xFF9DA1A9)

/**
 * One column of the day / hour / minute wheel (artboard 17): five 36 dp rows, the middle one
 * selected (20 sp / 800 ink), the others 17 sp grey. Scrolls and snaps; tapping a value selects it.
 */
@Composable
fun WheelPicker(
    values: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    width: Dp = 60.dp,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val select by rememberUpdatedState(onSelect)

    // When scrolling stops, the item in the middle is the value.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .collect { select(state.firstVisibleItemIndex.coerceIn(0, values.lastIndex)) }
    }
    // Outside changes (e.g. switching to "Now") move the wheel.
    LaunchedEffect(selectedIndex) {
        if (!state.isScrollInProgress && state.firstVisibleItemIndex != selectedIndex) state.animateScrollToItem(selectedIndex)
    }

    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = RowHeight * (VisibleRows / 2)),
        modifier = modifier
            .width(width)
            .height(RowHeight * VisibleRows)
            .semantics {
                contentDescription = label
                stateDescription = values.getOrElse(selectedIndex) { "" }
            },
    ) {
        itemsIndexed(values) { i, value ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RowHeight)
                    .clickable { scope.launch { state.animateScrollToItem(i) }; select(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    value,
                    style = KonstanzType.Body.copy(
                        fontSize = if (selected) 20.sp else 17.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
                    color = if (selected) Ink else WheelGrey,
                    maxLines = 1,
                )
            }
        }
    }
}
