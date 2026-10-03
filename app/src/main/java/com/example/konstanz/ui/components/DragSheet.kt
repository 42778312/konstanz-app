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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Collapse state for a bottom sheet that can be dragged down to a "peek" height showing just its
 * header (Google Maps style), and back up to its full height. [totalHeightPx] / [peekHeightPx]
 * are measured live from the actual laid-out content, so the two heights track font scaling and
 * content changes instead of a guessed constant.
 */
@Stable
class DragSheetState internal constructor(internal val scope: CoroutineScope) {
    var collapsed by mutableStateOf(false)
    internal var totalHeightPx by mutableFloatStateOf(0f)
    internal var peekHeightPx by mutableFloatStateOf(0f)
    internal val offsetY = Animatable(0f)
    val maxOffsetPx: Float get() = (totalHeightPx - peekHeightPx).coerceAtLeast(0f)
}

@Composable
fun rememberDragSheetState(): DragSheetState {
    val scope = rememberCoroutineScope()
    val state = remember { DragSheetState(scope) }
    LaunchedEffect(state.collapsed, state.maxOffsetPx) {
        state.offsetY.animateTo(if (state.collapsed) state.maxOffsetPx else 0f, tween(250))
    }
    return state
}

/** Put on the sheet's outermost container: slides the whole sheet down by the collapse offset. */
fun Modifier.dragSheetContainer(state: DragSheetState): Modifier = this
    .offset { IntOffset(0, state.offsetY.value.roundToInt()) }
    .onSizeChanged { state.totalHeightPx = it.height.toFloat() }

/** Put on the always-visible header block: measures how tall the collapsed "peek" state is. */
fun Modifier.dragSheetPeek(state: DragSheetState): Modifier = this
    .onSizeChanged { state.peekHeightPx = it.height.toFloat() }

/** Put on the grabber and/or header title: dragging there collapses or expands the sheet. */
fun Modifier.dragSheetHandle(state: DragSheetState): Modifier = this.pointerInput(state.maxOffsetPx) {
    detectVerticalDragGestures(onDragEnd = { state.collapsed = state.offsetY.value > state.maxOffsetPx / 2 }) { change, dragAmount ->
        change.consume()
        state.scope.launch { state.offsetY.snapTo((state.offsetY.value + dragAmount).coerceIn(0f, state.maxOffsetPx)) }
    }
}
