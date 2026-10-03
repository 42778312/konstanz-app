package com.example.konstanz.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Collapse state for a bottom sheet that can be dragged down to a "peek" height showing just its
 * header (Google Maps style), and back up to its full height. [totalHeightPx] and where the header
 * ends are measured live from the actual laid-out content, so the two heights track font scaling and
 * content changes instead of a guessed constant.
 */
@Stable
class DragSheetState internal constructor(internal val scope: CoroutineScope) {
    var collapsed by mutableStateOf(false)
    internal var totalHeightPx by mutableFloatStateOf(0f)
    /** Bottom of the header, from the sheet's top edge (so the sheet's own top padding counts). */
    internal var peekBottomPx by mutableFloatStateOf(0f)
    /** Shown under the header when minimized: the system navigation bar the sheet runs behind, plus a margin. */
    internal var belowPeekPx by mutableFloatStateOf(0f)
    internal val offsetY = Animatable(0f)
    val maxOffsetPx: Float get() = (totalHeightPx - peekBottomPx - belowPeekPx).coerceAtLeast(0f)

    private var container: LayoutCoordinates? = null
    private var peek: LayoutCoordinates? = null

    internal fun containerPlaced(c: LayoutCoordinates) {
        container = c
        totalHeightPx = c.size.height.toFloat()
        measurePeek()
    }

    internal fun peekPlaced(c: LayoutCoordinates) {
        peek = c
        measurePeek()
    }

    private fun measurePeek() {
        val sheet = container?.takeIf { it.isAttached } ?: return
        val header = peek?.takeIf { it.isAttached } ?: return
        // At least the header's own height: in a sheet that scrolls, a scrolled-away header would end above the top.
        peekBottomPx = sheet.localPositionOf(header, Offset(0f, header.size.height.toFloat())).y
            .coerceAtLeast(header.size.height.toFloat())
    }
}

@Composable
fun rememberDragSheetState(): DragSheetState {
    val scope = rememberCoroutineScope()
    val state = remember { DragSheetState(scope) }
    val density = LocalDensity.current
    state.belowPeekPx = WindowInsets.navigationBars.getBottom(density) + with(density) { PEEK_MARGIN.toPx() }
    LaunchedEffect(state.collapsed, state.maxOffsetPx) {
        state.offsetY.animateTo(if (state.collapsed) state.maxOffsetPx else 0f, tween(250))
    }
    return state
}

/** Space between the header and the screen's bottom (or navigation bar) when minimized. */
private val PEEK_MARGIN = 16.dp

/** Put on the sheet's outermost container: slides the whole sheet down by the collapse offset. */
fun Modifier.dragSheetContainer(state: DragSheetState): Modifier = this
    .offset { IntOffset(0, state.offsetY.value.roundToInt()) }
    .onGloballyPositioned { state.containerPlaced(it) }

/** Put on the always-visible header block: measures how tall the collapsed "peek" state is. */
fun Modifier.dragSheetPeek(state: DragSheetState): Modifier = this
    .onGloballyPositioned { state.peekPlaced(it) }

/**
 * Put on everything below the header: it fades out as the sheet goes down, so the strip left under the
 * header when minimized shows no half-cut chips or rows.
 */
fun Modifier.dragSheetBody(state: DragSheetState): Modifier = this.graphicsLayer {
    val max = state.maxOffsetPx
    alpha = if (max <= 0f) 1f else (1f - state.offsetY.value / max).coerceIn(0f, 1f)
}

/** Put on the grabber and/or header title: dragging there collapses or expands the sheet. */
fun Modifier.dragSheetHandle(state: DragSheetState): Modifier = this.pointerInput(state.maxOffsetPx) {
    detectVerticalDragGestures(onDragEnd = { state.collapsed = state.offsetY.value > state.maxOffsetPx / 2 }) { change, dragAmount ->
        change.consume()
        state.scope.launch { state.offsetY.snapTo((state.offsetY.value + dragAmount).coerceIn(0f, state.maxOffsetPx)) }
    }
}
