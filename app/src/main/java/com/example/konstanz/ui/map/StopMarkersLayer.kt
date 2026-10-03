package com.example.konstanz.ui.map

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Figtree
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White
import kotlin.math.floor
import kotlin.math.pow
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Stop

/**
 * Design system → Map markers: "Stops show as bus tiles from zoom 15, dots from zoom 13,
 * clusters below. Hit area is at least 44 × 44 px even when the glyph is smaller."
 * Our zoom 1 is the design's close-up (tiles); markers that would overlap merge into a cluster.
 */
enum class StopStyle { Tile, Dot, Cluster }

fun stopStyleFor(zoom: Float): StopStyle = when {
    zoom >= 0.8f -> StopStyle.Tile
    zoom >= 0.5f -> StopStyle.Dot
    else -> StopStyle.Cluster
}

/** A group of stops drawn as one marker. */
private class MarkerGroup(val stops: List<Stop>) {
    val point: MapPoint = if (stops.size == 1) stops[0].point else centroid(stops.map { it.point })
}

/**
 * All bus stops in one layer that is drawn, not composed: one Canvas and one tap handler instead of a
 * composable per stop, so panning and zooming stay at the display's frame rate. Stops are grouped
 * into clusters per zoom step (not per frame) and only those on screen are drawn.
 */
@Composable
fun MapOverlayScope.StopMarkersLayer(
    stops: List<Stop>,
    onStopClick: (Stop) -> Unit,
    onClusterClick: (center: MapPoint, count: Int) -> Unit,
    /** Stop drawn separately as the selected marker. */
    hiddenStopId: String? = null,
) {
    val native = nativeStops
    if (native != null) {
        NativeStops(native, stops, hiddenStopId, onStopClick, onClusterClick)
        return
    }
    val density = LocalDensity.current
    val scope = this
    // Zoom in steps of ~19 % (2^¼): the grouping is redone only when a step is crossed.
    val step by remember { derivedStateOf { kotlin.math.round(kotlin.math.log2(scope.projection.scale) * 4f).toInt() } }
    val style by remember { derivedStateOf { stopStyleFor(scope.zoom) } }
    val minGapPx = with(density) {
        when (style) {
            StopStyle.Tile -> 30.dp
            StopStyle.Dot -> 16.dp
            StopStyle.Cluster -> 64.dp
        }.toPx()
    }
    val visible = remember(stops, hiddenStopId) { stops.filter { it.id != hiddenStopId } }
    val groups = remember(visible, step, minGapPx) {
        val stepScale = 2f.pow(step / 4f)
        groupByDistance(visible, minGapPx / stepScale)
    }

    val bus = rememberVectorPainter(remember { KtIcons.Bus.toImageVector(strokeWidth = 2.4f) })
    val textMeasurer = rememberTextMeasurer()
    val countStyle = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = White)
    val countLayouts = remember { HashMap<Int, TextLayoutResult>() }
    val stopLabel = stringResource(R.string.open_stop)

    Canvas(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = stopLabel }
            .pointerInput(groups) {
                // Taps on a marker open it; anything else (pan, pinch, taps on the map) goes on to the map.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hit = hitTest(groups, down.position, scope.projection, 24.dp.toPx()) ?: return@awaitEachGesture
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    up.consume()
                    if (hit.stops.size == 1) onStopClick(hit.stops[0]) else onClusterClick(hit.point, hit.stops.size)
                }
            },
    ) {
        val p = scope.projection
        val margin = 40.dp.toPx()
        val tileStyle = style
        for (g in groups) {
            val c = p.toPx(g.point.x, g.point.y)
            if (c.x < -margin || c.y < -margin || c.x > size.width + margin || c.y > size.height + margin) continue
            if (g.stops.size > 1) drawCluster(c, g.stops.size, countLayouts.getOrPut(g.stops.size) { textMeasurer.measure("${g.stops.size}", countStyle) })
            else if (tileStyle == StopStyle.Tile) drawTile(c, bus)
            else drawDot(c)
        }
    }
}

/** On the real map: hands the stops to MapLibre and turns taps on them into clicks. */
@Composable
private fun NativeStops(
    native: NativeStopLayer,
    stops: List<Stop>,
    hiddenStopId: String?,
    onStopClick: (Stop) -> Unit,
    onClusterClick: (center: MapPoint, count: Int) -> Unit,
) {
    val owner = remember { Any() }
    SideEffect { native.show(owner, stops, hiddenStopId) }
    DisposableEffect(native) { onDispose { native.clear(owner) } }
    val stopClick by rememberUpdatedState(onStopClick)
    val clusterClick by rememberUpdatedState(onClusterClick)
    val stopLabel = stringResource(R.string.open_stop)
    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = stopLabel }
            .pointerInput(native) {
                // Taps on a stop or cluster open it; everything else (pan, pinch, taps on the map) goes to the map.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hit = native.hit(down.position.x, down.position.y, 22.dp.toPx()) ?: return@awaitEachGesture
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    up.consume()
                    when (hit) {
                        is NativeStopLayer.Hit.Single -> stopClick(hit.stop)
                        is NativeStopLayer.Hit.Cluster -> clusterClick(hit.center, hit.count)
                    }
                }
            },
    )
}

private fun hitTest(groups: List<MarkerGroup>, at: Offset, p: MapProjection, radius: Float): MarkerGroup? =
    groups.minByOrNull { (p.toPx(it.point.x, it.point.y) - at).getDistanceSquared() }
        ?.takeIf { (p.toPx(it.point.x, it.point.y) - at).getDistance() <= radius }

private val MarkerShadow = Color(0x4016181D)

/** Same look as [StopTileMarker]: 26 dp red tile, 2 dp white edge, white bus. */
private fun DrawScope.drawTile(c: Offset, bus: androidx.compose.ui.graphics.vector.VectorPainter) {
    val s = 26.dp.toPx()
    val r = CornerRadius(8.dp.toPx())
    val tl = Offset(c.x - s / 2, c.y - s / 2)
    drawRoundRect(MarkerShadow, tl + Offset(0f, 1.dp.toPx()), Size(s, s), r)
    drawRoundRect(Primary, tl, Size(s, s), r)
    drawRoundRect(White, tl + Offset(1.dp.toPx(), 1.dp.toPx()), Size(s - 2.dp.toPx(), s - 2.dp.toPx()), CornerRadius(7.dp.toPx()), style = Stroke(2.dp.toPx()))
    val icon = 15.dp.toPx()
    translate(c.x - icon / 2, c.y - icon / 2) {
        with(bus) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(White)) }
    }
}

/** Same look as [StopDotMarker]: 12 dp white dot in a 3 dp red ring. */
private fun DrawScope.drawDot(c: Offset) {
    val r = 6.dp.toPx()
    drawCircle(White, r, c)
    drawCircle(Primary, r - 1.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
}

/** Same look as [ClusterMarker]: red disc with the count, white edge, soft red halo. */
private fun DrawScope.drawCluster(c: Offset, count: Int, text: TextLayoutResult) {
    val r = 18.dp.toPx()
    drawCircle(Primary.copy(alpha = 0.18f), r + 5.dp.toPx(), c)
    drawCircle(MarkerShadow, r, c + Offset(0f, 2.dp.toPx()))
    drawCircle(Primary, r, c)
    drawCircle(White, r - 1.5.dp.toPx(), c, style = Stroke(3.dp.toPx()))
    drawText(text, topLeft = Offset(c.x - text.size.width / 2f, c.y - text.size.height / 2f))
}

/**
 * Grouping on a grid of [minGapUnits] cells: each stop joins a group in its cell or a neighbouring one whose
 * first stop is closer than [minGapUnits]. Linear in the number of stops.
 */
private fun groupByDistance(stops: List<Stop>, minGapUnits: Float): List<MarkerGroup> {
    if (minGapUnits <= 0f) return stops.map { MarkerGroup(listOf(it)) }
    val cells = HashMap<Long, MutableList<MutableList<Stop>>>()
    val buckets = ArrayList<MutableList<Stop>>()
    val gap2 = minGapUnits * minGapUnits
    fun key(x: Int, y: Int) = x.toLong().shl(32) or (y.toLong() and 0xFFFFFFFFL)
    for (stop in stops) {
        val cx = floor(stop.point.x / minGapUnits).toInt()
        val cy = floor(stop.point.y / minGapUnits).toInt()
        var found: MutableList<Stop>? = null
        loop@ for (dx in -1..1) for (dy in -1..1) {
            for (b in cells[key(cx + dx, cy + dy)] ?: continue) {
                val a = b[0].point
                val ddx = a.x - stop.point.x
                val ddy = a.y - stop.point.y
                if (ddx * ddx + ddy * ddy < gap2) { found = b; break@loop }
            }
        }
        if (found != null) found += stop
        else {
            val b = mutableListOf(stop)
            buckets += b
            cells.getOrPut(key(cx, cy)) { ArrayList(2) } += b
        }
    }
    return buckets.map { MarkerGroup(it) }
}

/**
 * Stops worth showing behind routes. The real network has ~140 stops, which would bury the route:
 * there, only the stops the [journeys] ride through. The design sample keeps all of its few stops.
 */
fun List<Stop>.alongJourneys(journeys: List<Journey>): List<Stop> {
    if (!realMapEnabled) return this
    val rides = journeys.flatMap { it.rides }
    val ids = rides.flatMap { r -> r.stops.map { it.stopId } }.toSet()
    val boarding = rides.map { it.from }.toSet()
    return filter { it.id in ids || it.name in boarding }
}
