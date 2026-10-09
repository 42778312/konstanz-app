package com.example.konstanz.ui.map

import android.graphics.Bitmap
import com.example.konstanz.ui.theme.OnPrimary
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Primary
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.all
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.neq
import org.maplibre.android.style.expressions.Expression.not
import org.maplibre.android.style.expressions.Expression.step
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.expressions.Expression.toString as exprToString
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.circleTranslate
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconPadding
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textMaxWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Bus stops drawn by MapLibre itself, as layers of the offline map (like its streets and labels), so they
 * pan and zoom at the same speed as the map: a GeoJSON source with MapLibre's own clustering, red clusters
 * with the count, dots, then bus tiles and names when zoomed in. One per map; [StopMarkersLayer] feeds it.
 */
class NativeStopLayer {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var stops: List<Stop> = emptyList()
    private var byId: Map<String, Stop> = emptyMap()
    private var hiddenId: String? = null

    /** Who set the stops last: the map is shared by screens, and a closing screen must not clear the next one's. */
    private var shownBy: Any? = null

    /** Stops to show for the screen [by]; [hidden] = the selected stop, drawn separately on top. */
    fun show(by: Any, stops: List<Stop>, hidden: String?) {
        shownBy = by
        show(stops, hidden)
    }

    /** Takes away [by]'s stops, unless another screen has shown its own since. */
    fun clear(by: Any) {
        if (shownBy !== by) return
        shownBy = null
        show(emptyList(), null)
    }

    private fun show(stops: List<Stop>, hidden: String?) {
        val dataChanged = stops != this.stops
        this.stops = stops
        byId = stops.associateBy { it.id }
        hiddenId = hidden
        val s = style ?: return
        if (dataChanged) source(s)?.setGeoJson(collection())
        applyHidden(s)
    }

    /** What's under a finger at [x], [y] (px in the map view): a stop, or a cluster with its centre and size. */
    fun hit(x: Float, y: Float, radiusPx: Float): Hit? {
        val m = map ?: return null
        val box = RectF(x - radiusPx, y - radiusPx, x + radiusPx, y + radiusPx)
        val features = m.queryRenderedFeatures(box, CLUSTER, TILE, DOT)
        val f = features.minByOrNull { feature ->
            val p = feature.geometry() as? Point ?: return@minByOrNull Float.MAX_VALUE
            val px = m.projection.toScreenLocation(org.maplibre.android.geometry.LatLng(p.latitude(), p.longitude()))
            (px.x - x) * (px.x - x) + (px.y - y) * (px.y - y)
        } ?: return null
        val p = f.geometry() as? Point ?: return null
        if (f.hasProperty("point_count")) {
            return Hit.Cluster(Geo.mapPoint(p.latitude(), p.longitude()), f.getNumberProperty("point_count").toInt())
        }
        return byId[f.getStringProperty("id")]?.let { Hit.Single(it) }
    }

    sealed interface Hit {
        data class Single(val stop: Stop) : Hit
        data class Cluster(val center: MapPoint, val count: Int) : Hit
    }

    /**
     * Adds the source, the tile image and the layers to a freshly loaded style. [tileZoom] / [dotZoom] are the
     * MapLibre zooms where the app's design switches to tiles / dots (same steps as before).
     */
    internal fun attach(map: MapLibreMap, style: Style, density: Float, tileZoom: Float, dotZoom: Float) {
        this.map = map
        this.style = style
        style.addImage(TILE_IMAGE, tileBitmap(density))
        style.addSource(
            GeoJsonSource(
                SOURCE, collection(),
                GeoJsonOptions().withCluster(true).withClusterRadius(50).withClusterMaxZoom(kotlin.math.round(dotZoom).toInt()),
            )
        )
        val clustered = has("point_count")
        style.addLayer(CircleLayer(CLUSTER_SHADOW, SOURCE).withFilter(clustered).withProperties(
            circleColor(0x40000000), circleRadius(15f), circleTranslate(arrayOf(0f, 2f)),
        ))
        style.addLayer(CircleLayer(CLUSTER, SOURCE).withFilter(clustered).withProperties(
            // A calm disc in the sheet colour with a brand ring: groups of stops shouldn't shout over the route.
            circleColor(Surface.toArgb()), circleRadius(15f), circleStrokeColor(Primary.toArgb()), circleStrokeWidth(2.5f),
        ))
        style.addLayer(SymbolLayer(CLUSTER_COUNT, SOURCE).withFilter(clustered).withProperties(
            textField(exprToString(get("point_count"))), textFont(arrayOf("Noto Sans Medium")), textSize(13f),
            textColor(Primary.toArgb()), textAllowOverlap(true), textIgnorePlacement(true),
        ))
        style.addLayer(CircleLayer(DOT, SOURCE).withFilter(single()).withProperties(
            circleColor(android.graphics.Color.WHITE), circleRadius(4f), circleStrokeColor(Primary.toArgb()), circleStrokeWidth(2.5f),
        ).apply { maxZoom = tileZoom; minZoom = minOf(dotZoom, tileZoom) - 3f })
        // Tile and name are one symbol: as two layers, the names (placed first, being on top) blocked the tiles
        // under them, so at the closest zooms the stops vanished. The name is optional and only from tileZoom + 1.3.
        style.addLayer(SymbolLayer(TILE, SOURCE).withFilter(single()).withProperties(
            // Like other maps: tiles that would cover each other show once; the rest appear when zooming in.
            iconImage(TILE_IMAGE), iconAllowOverlap(false), iconIgnorePlacement(false), iconPadding(1f),
            textField(step(zoom(), literal(""), stop(tileZoom + 1.3f, get("name")))),
            textFont(arrayOf("Noto Sans Medium")), textSize(11.5f),
            textColor(Ink.toArgb()), textHaloColor(Surface.toArgb()), textHaloWidth(1.6f),
            textAnchor("top"), textOffset(arrayOf(0f, 1.35f)), textMaxWidth(8f), textOptional(true),
        ).apply { minZoom = tileZoom })
        applyHidden(style)
    }

    internal fun detach() {
        map = null
        style = null
    }

    private fun single(): Expression {
        val notCluster = not(has("point_count"))
        return hiddenId?.let { all(notCluster, neq(get("id"), literal(it))) } ?: notCluster
    }

    private fun applyHidden(style: Style) {
        for (id in listOf(DOT, TILE)) (style.getLayer(id) as? CircleLayer)?.setFilter(single())
            ?: (style.getLayer(id) as? SymbolLayer)?.setFilter(single())
    }

    private fun source(style: Style) = style.getSourceAs<GeoJsonSource>(SOURCE)

    private fun collection(): FeatureCollection = FeatureCollection.fromFeatures(stops.map { stop ->
        val (lat, lon) = Geo.latLon(stop.point)
        Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
            addStringProperty("id", stop.id)
            addStringProperty("name", stop.name)
        }
    })

    /** The design's stop tile (26 dp red rounded square, 2 dp white edge, white bus), with a soft shadow. */
    private fun tileBitmap(density: Float): Bitmap {
        val pad = 3 * density
        val size = 26 * density
        val bmp = Bitmap.createBitmap((size + 2 * pad).toInt(), (size + 2 * pad).toInt(), Bitmap.Config.ARGB_8888)
        bmp.density = (density * 160).toInt()
        val c = Canvas(bmp)
        val rect = RectF(pad, pad, pad + size, pad + size)
        val r = 8 * density
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Primary.toArgb()
            setShadowLayer(2.5f * density, 0f, 1f * density, 0x50000000)
        }
        c.drawRoundRect(rect, r, r, fill)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2 * density; color = android.graphics.Color.WHITE
        }
        val inset = RectF(rect).apply { inset(density, density) }
        c.drawRoundRect(inset, r - density, r - density, edge)
        val icon = 16 * density
        val k = icon / 256f // Phosphor's grid
        val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = OnPrimary.toArgb() }
        c.save()
        c.translate(rect.centerX() - icon / 2, rect.centerY() - icon / 2)
        c.scale(k, k)
        for (d in KtIcons.Bus.bold) PathParser.createPathFromPathData(d)?.let { c.drawPath(it, glyph) }
        c.restore()
        return bmp
    }

    companion object {
        private const val SOURCE = "kt-stops"
        private const val TILE_IMAGE = "kt-stop-tile"
        private const val CLUSTER_SHADOW = "kt-stop-cluster-shadow"
        private const val CLUSTER = "kt-stop-cluster"
        private const val CLUSTER_COUNT = "kt-stop-cluster-count"
        private const val DOT = "kt-stop-dot"
        private const val TILE = "kt-stop-tile"
    }
}
