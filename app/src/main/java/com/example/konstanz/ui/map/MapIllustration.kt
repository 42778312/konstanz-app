package com.example.konstanz.ui.map

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Static map pictures exported from the design (SVG files in assets/maps). They stand in for the real
 * offline map (Part E) in onboarding. Only the SVG subset the design uses is supported:
 * <path> with M/L/C/Z, <rect rx>, <ellipse>, fill/stroke, width, dasharray, linecap, linejoin.
 */
@Immutable
class MapScene internal constructor(val viewBox: Rect, internal val shapes: List<MapShape>)

@Immutable
internal class MapShape(
    val path: Path,
    val fill: Color?,
    val stroke: Color?,
    val strokeWidth: Float,
    val dash: FloatArray?,
    val cap: StrokeCap,
    val join: StrokeJoin,
)

internal object MapSceneCache {
    private val scenes = mutableMapOf<String, MapScene>()

    @Synchronized
    fun get(context: Context, asset: String): MapScene =
        scenes.getOrPut(asset) {
            parseSvg(context.assets.open(asset).bufferedReader().use { it.readText() })
        }
}

private val tagRegex = Regex("""<(svg|path|rect|ellipse)\s([^>]*)>""")
private val attrRegex = Regex("""([a-zA-Z-]+)="([^"]*)"""")

internal fun parseSvg(svg: String): MapScene {
    var viewBox = Rect.Zero
    val shapes = mutableListOf<MapShape>()
    for (tag in tagRegex.findAll(svg)) {
        val a = attrRegex.findAll(tag.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] }
        fun f(name: String) = a[name]?.toFloatOrNull() ?: 0f
        val path = when (tag.groupValues[1]) {
            "svg" -> {
                val (x, y, w, h) = a.getValue("viewBox").trim().split(Regex("\\s+")).map { it.toFloat() }
                viewBox = Rect(x, y, x + w, y + h)
                null
            }
            "path" -> PathParser().parsePathString(a.getValue("d")).toPath()
            "rect" -> Path().apply {
                val r = f("rx")
                addRoundRect(RoundRect(f("x"), f("y"), f("x") + f("width"), f("y") + f("height"), CornerRadius(r, r)))
            }
            "ellipse" -> Path().apply {
                addOval(Rect(f("cx") - f("rx"), f("cy") - f("ry"), f("cx") + f("rx"), f("cy") + f("ry")))
            }
            else -> null
        } ?: continue
        shapes += MapShape(
            path = path,
            fill = a["fill"].toColorOrNull() ?: if ("fill" !in a) Color.Black else null,
            stroke = a["stroke"].toColorOrNull(),
            strokeWidth = f("stroke-width").takeIf { it > 0f } ?: 1f,
            dash = a["stroke-dasharray"]?.split(Regex("[ ,]+"))?.mapNotNull { it.toFloatOrNull() }?.toFloatArray(),
            cap = when (a["stroke-linecap"]) { "round" -> StrokeCap.Round; "square" -> StrokeCap.Square; else -> StrokeCap.Butt },
            join = when (a["stroke-linejoin"]) { "round" -> StrokeJoin.Round; "bevel" -> StrokeJoin.Bevel; else -> StrokeJoin.Miter },
        )
    }
    return MapScene(viewBox, shapes)
}

private fun String?.toColorOrNull(): Color? {
    if (this == null || this == "none" || !startsWith("#") || length != 7) return null
    return Color(0xFF000000 or substring(1).toLong(16))
}

/**
 * Converts map coordinates (the design's world units) to pixels: `px = origin + point * scale`.
 * Build it from a fixed view box ([fromViewBox], illustrations) or from a camera (interactive map).
 */
@Immutable
class MapProjection(val scale: Float, private val originX: Float, private val originY: Float) {
    fun toPx(x: Float, y: Float) = Offset(originX + x * scale, originY + y * scale)
    fun origin() = Offset(originX, originY)
    /** Pixels → map units, e.g. for a tap position. */
    fun toMap(px: Offset) = Offset((px.x - originX) / scale, (px.y - originY) / scale)

    companion object {
        /** The view box scaled to *cover* the box, so any phone keeps the design's framing without stretching. */
        fun fromViewBox(viewBox: Rect, width: Float, height: Float): MapProjection {
            val scale = max(width / viewBox.width, height / viewBox.height)
            val offsetX = (width - viewBox.width * scale) / 2f
            val offsetY = (height - viewBox.height * scale) / 2f
            return MapProjection(scale, offsetX - viewBox.left * scale, offsetY - viewBox.top * scale)
        }
    }
}

/**
 * Scope for overlays: [at] centres a child on a map coordinate.
 * [projection] and [zoom] read the camera when they are used: in a layout or draw block that means a moving
 * map only re-places and re-draws its overlays each frame instead of rebuilding them (smooth panning).
 */
class MapOverlayScope internal constructor(
    private val projectionOf: () -> MapProjection,
    private val boxScope: BoxScope,
    private val zoomOf: () -> Float = { 1f },
    /** On the real map: bus stops are layers of the map itself (see [StopMarkersLayer]). */
    internal val nativeStops: NativeStopLayer? = null,
) : BoxScope by boxScope {
    val projection: MapProjection get() = projectionOf()
    /** Camera zoom (1 = the design's framing). */
    val zoom: Float get() = zoomOf()

    /** Put the child to the left of (x, y), vertically centred, [gap] away (map callouts). */
    fun Modifier.leftOf(x: Float, y: Float, gap: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val p = projection.toPx(x, y)
        layout(placeable.width, placeable.height) {
            placeable.place(IntOffset((p.x - gap.toPx() - placeable.width).roundToInt(), (p.y - placeable.height / 2f).roundToInt()))
        }
    }

    /** Centre the child on (x, y); with [anchorBottom] its bottom edge sits on the point (pins). */
    fun Modifier.at(x: Float, y: Float, anchorBottom: Boolean = false): Modifier = layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val p = projection.toPx(x, y)
        val top = if (anchorBottom) p.y - placeable.height else p.y - placeable.height / 2f
        layout(placeable.width, placeable.height) {
            placeable.place(IntOffset((p.x - placeable.width / 2f).roundToInt(), top.roundToInt()))
        }
    }
}

/**
 * Draws [asset] (e.g. "maps/onboarding_welcome.svg") filling the box, plus [overlays]
 * (markers, labels) placed with `Modifier.at(x, y)` in map coordinates.
 * The SVG is parsed once off the main thread and cached.
 */
@Composable
fun MapIllustration(
    asset: String,
    modifier: Modifier = Modifier,
    /** Framing in map units; default = the SVG's own view box. */
    viewBox: Rect? = null,
    overlays: @Composable MapOverlayScope.() -> Unit = {},
) {
    val context = LocalContext.current
    // A missing or broken asset leaves just the card background: an illustration must never crash a screen.
    val scene by produceState<MapScene?>(null, asset) {
        value = withContext(Dispatchers.IO) {
            runCatching { MapSceneCache.get(context, asset) }
                .onFailure { Log.w("MapIllustration", "Cannot load $asset", it) }
                .getOrNull()
        }
    }
    // Clip: the drawing covers the whole city and would otherwise paint outside this box.
    BoxWithConstraints(modifier.clipToBounds()) {
        val current = scene ?: return@BoxWithConstraints
        val density: Density = LocalDensity.current
        val projection = with(density) { MapProjection.fromViewBox(viewBox ?: current.viewBox, maxWidth.toPx(), maxHeight.toPx()) }
        Canvas(Modifier.fillMaxSize()) { drawScene(current, projection) }
        Box(Modifier.fillMaxSize()) { MapOverlayScope({ projection }, this, { 1.875f }).overlays() }
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScene(scene: MapScene, projection: MapProjection) {
    val origin = projection.origin()
    withTransform({
        translate(origin.x, origin.y)
        scale(projection.scale, projection.scale, pivot = Offset.Zero)
    }) {
        for (shape in scene.shapes) {
            shape.fill?.let { drawPath(shape.path, it) }
            shape.stroke?.let {
                drawPath(
                    shape.path, it,
                    style = Stroke(
                        width = shape.strokeWidth,
                        cap = shape.cap,
                        join = shape.join,
                        pathEffect = shape.dash?.let { d -> PathEffect.dashPathEffect(d) },
                    ),
                )
            }
        }
    }
}
