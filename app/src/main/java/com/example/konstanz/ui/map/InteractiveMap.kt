package com.example.konstanz.ui.map

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.MapPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Where the map looks: [center] in map units and [zoom] relative to the design's framing
 * (1 = artboard 07, where the phone width shows 487.5 map units).
 */
@Stable
class MapCameraState(center: MapPoint, zoom: Float) {
    var centerX by mutableFloatStateOf(center.x)
        private set
    var centerY by mutableFloatStateOf(center.y)
        private set
    var zoom by mutableFloatStateOf(zoom)
        private set

    /** Pixels per map unit for a map [widthPx] wide (zoom 1 = the design's framing). */
    fun scale(widthPx: Float): Float = widthPx / DESIGN_WIDTH_UNITS * zoom

    /**
     * The camera centre is drawn at a fixed height from the top (the middle of a design-proportioned
     * phone), not at the middle of the map box. So the map doesn't jump when the box gets taller or
     * shorter, e.g. when the bottom bar hides.
     */
    fun anchorY(widthPx: Float): Float = widthPx * DESIGN_ASPECT / 2f

    @Suppress("UNUSED_PARAMETER")
    fun projection(widthPx: Float, heightPx: Float): MapProjection {
        val scale = scale(widthPx)
        return MapProjection(scale, widthPx / 2f - centerX * scale, anchorY(widthPx) - centerY * scale)
    }

    /** Camera centre that shows [point] at screen height [screenY] (px) at [zoom]. */
    fun centerShowing(point: MapPoint, screenY: Float, widthPx: Float, zoom: Float): MapPoint {
        val scale = widthPx / DESIGN_WIDTH_UNITS * zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        return MapPoint(point.x, point.y - (screenY - anchorY(widthPx)) / scale)
    }

    /**
     * Centre and zoom that show all [points] between screen heights [topPx] and [bottomPx]
     * (the map left visible by bars and sheets), [marginPx] in from every edge.
     */
    fun framing(
        points: List<MapPoint>,
        widthPx: Float,
        topPx: Float,
        bottomPx: Float,
        marginPx: Float,
        maxZoom: Float = 2.5f,
    ): Pair<MapPoint, Float> {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val pxPerUnit = minOf(
            (widthPx - 2 * marginPx) / (maxX - minX).coerceAtLeast(1f),
            (bottomPx - topPx - 2 * marginPx).coerceAtLeast(1f) / (maxY - minY).coerceAtLeast(1f),
        )
        val zoom = (pxPerUnit * DESIGN_WIDTH_UNITS / widthPx).coerceIn(MIN_ZOOM, maxZoom)
        val middle = MapPoint((minX + maxX) / 2f, (minY + maxY) / 2f)
        return centerShowing(middle, (topPx + bottomPx) / 2f, widthPx, zoom) to zoom
    }

    /** Pan by a pixel delta and zoom by [factor] around [focusPx] (pinch centre). */
    fun transform(panPx: Offset, factor: Float, focusPx: Offset, widthPx: Float, heightPx: Float) {
        val before = projection(widthPx, heightPx).toMap(focusPx)
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        // Keep the point under the fingers where it is, then apply the pan.
        val after = projection(widthPx, heightPx).toMap(focusPx)
        val scale = scale(widthPx)
        moveTo(centerX + (before.x - after.x) - panPx.x / scale, centerY + (before.y - after.y) - panPx.y / scale)
    }

    fun snapTo(center: MapPoint, zoom: Float = this.zoom) {
        this.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        moveTo(center.x, center.y)
    }

    /** Smooth camera move (my location, cluster tap, zoom buttons). */
    suspend fun animateTo(center: MapPoint, zoom: Float = this.zoom, durationMs: Int = 450) = coroutineScope {
        val from = Triple(centerX, centerY, this@MapCameraState.zoom)
        val target = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val t = Animatable(0f)
        t.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) {
            val f = value
            this@MapCameraState.zoom = from.third + (target - from.third) * f
            moveTo(from.first + (center.x - from.first) * f, from.second + (center.y - from.second) * f)
        }
    }

    /** Where the camera centre may go: the drawing, or the (larger) real map. */
    private var bounds = DRAWING_BOUNDS

    internal fun useRealMapBounds(real: Boolean) {
        bounds = if (real) REAL_MAP_BOUNDS else DRAWING_BOUNDS
        moveTo(centerX, centerY)
    }

    private fun moveTo(x: Float, y: Float) {
        centerX = x.coerceIn(bounds.left, bounds.right)
        centerY = y.coerceIn(bounds.top, bounds.bottom)
    }

    companion object {
        const val MIN_ZOOM = 0.35f
        const val MAX_ZOOM = 4f
        /** Map units visible across the phone at zoom 1 (artboard 07 view box width). */
        const val DESIGN_WIDTH_UNITS = 487.5f
        /** Artboard height / width (844 / 390). */
        const val DESIGN_ASPECT = 844f / 390f

        // Keep the camera over the drawn part of the city map…
        private val DRAWING_BOUNDS = Rect(150f, 100f, 1150f, 1300f)
        // …or over the offline tiles (9.05–9.26 E, 47.63–47.74 N), half a screen in from their edge.
        private val REAL_MAP_BOUNDS: Rect = run {
            val nw = Geo.mapPoint(47.74, 9.05)
            val se = Geo.mapPoint(47.63, 9.26)
            Rect(nw.x + 240f, nw.y + 400f, se.x - 240f, se.y - 400f)
        }

        /** Artboard 07's framing: view box 374.5, 477 → 862, 1532. */
        val DESIGN_CENTER = MapPoint(618.25f, 1004.5f)

        val Saver: Saver<MapCameraState, FloatArray> = Saver(
            save = { floatArrayOf(it.centerX, it.centerY, it.zoom) },
            restore = { MapCameraState(MapPoint(it[0], it[1]), it[2]) },
        )
    }
}

@Composable
fun rememberMapCameraState(
    center: MapPoint = MapCameraState.DESIGN_CENTER,
    zoom: Float = 1f,
): MapCameraState = rememberSaveable(saver = MapCameraState.Saver) { MapCameraState(center, zoom) }

/**
 * The pannable, zoomable city map. Draws the real offline map ([RealBaseMap]) with the real timetable,
 * otherwise the design's drawing [asset], under [camera]; [overlays] are placed with `Modifier.at(x, y)` and keep their
 * on-screen size at every zoom, like markers on a real map.
 */
@Composable
fun InteractiveMap(
    camera: MapCameraState,
    modifier: Modifier = Modifier,
    asset: String = "maps/city.svg",
    onTap: ((MapPoint) -> Unit)? = null,
    onLongPress: ((MapPoint) -> Unit)? = null,
    /** Dragging or pinching the map (not just tapping it): callers use this to minimize open sheets. */
    onGestureStart: () -> Unit = {},
    overlays: @Composable MapOverlayScope.() -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scene by produceState<MapScene?>(null, asset) {
        value = withContext(Dispatchers.IO) { runCatching { MapSceneCache.get(context, asset) }.getOrNull() }
    }
    val realMap = realMapEnabled && !LocalInspectionMode.current
    LaunchedEffect(camera, realMap) { camera.useRealMapBounds(realMap) }
    val mapLabel = stringResource(R.string.map_of_konstanz)
    BoxWithConstraints(modifier.clipToBounds().semantics { contentDescription = mapLabel }) {
        val density = LocalDensity.current
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        // Read in layout / draw only (see MapOverlayScope): panning doesn't recompose the map's content.
        val projection = remember(camera, w, h) { { camera.projection(w, h) } }
        val zoom = remember(camera) { { camera.zoom } }
        val stopLayer = remember { NativeStopLayer() }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(camera, w, h, onGestureStart) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        onGestureStart()
                        camera.transform(pan, zoom, centroid, w, h)
                    }
                }
                .pointerInput(camera, w, h, onTap, onLongPress, onGestureStart) {
                    detectTapGestures(
                        onDoubleTap = { at ->
                            onGestureStart()
                            val p = camera.projection(w, h).toMap(at)
                            scope.launch { camera.animateTo(MapPoint(p.x, p.y), camera.zoom * 2f, 300) }
                        },
                        onTap = { at ->
                            onGestureStart()
                            val p = camera.projection(w, h).toMap(at)
                            onTap?.invoke(MapPoint(p.x, p.y))
                        },
                        onLongPress = { at ->
                            val p = camera.projection(w, h).toMap(at)
                            onLongPress?.invoke(MapPoint(p.x, p.y))
                        },
                    )
                }
        ) {
            if (realMap) RealBaseMap(projection, w, h, Modifier.fillMaxSize(), stopLayer)
            else scene?.let { s -> Canvas(Modifier.fillMaxSize()) { drawScene(s, projection()) } }
            Box(Modifier.fillMaxSize()) { MapOverlayScope(projection, this, zoom, if (realMap) stopLayer else null).overlays() }
        }
    }
}

/** Centre of [points] — used to place a cluster. */
internal fun centroid(points: List<MapPoint>) =
    MapPoint(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
