package com.example.konstanz.ui.map

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.example.konstanz.data.OfflineData
import com.example.konstanz.data.transit.DataSource
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Transit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng

/** The real map goes with the real timetable; the design sample keeps the design's drawing. */
val realMapEnabled: Boolean get() = Transit.source == DataSource.Timetable

/**
 * The offline OpenStreetMap of Konstanz (assets/map: PMTiles + style in the design's colours),
 * drawn by MapLibre under the Compose overlays. It never handles touches: [InteractiveMap]'s camera
 * owns pan and zoom, and this view follows it every frame, so markers stay glued to the streets.
 * The map itself is the activity's shared one ([MapHolder]): leaving this screen keeps it loaded.
 */
@Composable
internal fun RealBaseMap(
    projection: () -> MapProjection,
    widthPx: Float,
    heightPx: Float,
    holder: MapHolder,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val token = remember { Any() }
    val map = holder.map
    val owned = holder.owner === token
    // Reloaded on a light/dark switch; the stop layer is re-attached with the new palette.
    val dark = isSystemInDarkTheme()
    val styleJson by produceState(OfflineMapFiles.cachedStyle(dark), dark) {
        if (value == null) value = withContext(Dispatchers.IO) { runCatching { OfflineMapFiles.styleJson(context, dark) }.getOrNull() }
    }
    LaunchedEffect(map, styleJson, widthPx, owned) {
        val json = styleJson ?: return@LaunchedEffect
        if (!owned) return@LaunchedEffect
        // The app's zoom steps (tiles from 0.8, dots from 0.5) as MapLibre zooms on this screen.
        fun mapZoom(appZoom: Float) = Geo.mapLibreZoom(widthPx / MapCameraState.DESIGN_WIDTH_UNITS * appZoom, density).toFloat()
        holder.ensureStyle(json, dark, widthPx, density) { mapZoom(0.8f) to mapZoom(0.5f) }
    }

    // Same view as the Compose projection: the point in the middle of the box, at the same scale. Follows the
    // camera state directly (no recomposition per frame). Only the screen showing the map moves it.
    LaunchedEffect(map, owned, widthPx, heightPx) {
        val m = map ?: return@LaunchedEffect
        if (!owned) return@LaunchedEffect
        snapshotFlow { projection() }.collect { p -> m.moveCamera(CameraUpdateFactory.newCameraPosition(cameraOf(p, widthPx, heightPx, density))) }
    }
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).also { holder.attach(it, token, cameraOf(projection(), widthPx, heightPx, density)) }
        },
        modifier = modifier,
        onRelease = { holder.detach(it, token) },
    )
}

private fun cameraOf(p: MapProjection, widthPx: Float, heightPx: Float, density: Float): CameraPosition {
    val middle = p.toMap(Offset(widthPx / 2f, heightPx / 2f))
    val (lat, lon) = Geo.latLon(MapPoint(middle.x, middle.y))
    return CameraPosition.Builder().target(LatLng(lat, lon)).zoom(Geo.mapLibreZoom(p.scale, density)).bearing(0.0).tilt(0.0).build()
}

/** The style from the assets, pointed at the map file ([OfflineData.mapFile]); built once per theme. */
private object OfflineMapFiles {
    private const val TILES = "map/konstanz.pmtiles"
    private const val STYLE = "map/style.json"

    private val styles = java.util.concurrent.ConcurrentHashMap<Boolean, String>()

    fun cachedStyle(dark: Boolean): String? = styles[dark]

    fun styleJson(context: Context, dark: Boolean): String = styles.getOrPut(dark) { build(context, dark) }

    private fun build(context: Context, dark: Boolean): String {
        val copy = OfflineData.mapFile(context)
        var style = context.assets.open(STYLE).bufferedReader().use { it.readText() }
            .replace("pmtiles://asset://$TILES", "pmtiles://file://${copy.absolutePath}")
        if (dark) {
            // White halos and POI-dot rings sit on land, unlike white roads.
            style = style.replace("\"text-halo-color\": \"#FFFFFF\"", "\"text-halo-color\": \"#000000\"")
                .replace("\"circle-stroke-color\": \"#FFFFFF\"", "\"circle-stroke-color\": \"#000000\"")
            DARK.forEach { (light, night) -> style = style.replace("\"$light\"", "\"$night\"", ignoreCase = true) }
        }
        return style
    }

    /** The style's light colours → OLED-dark ones (same roles as DarkColors' map tokens). POI colours stay. */
    private val DARK = mapOf(
        "#A7D3F2" to "#0A1724", // water
        "#93C4E8" to "#10263A", // shore
        "#F6F4EF" to "#050505", // land
        "#CFE8C0" to "#0B160F", // landcover, parks
        "#BFDDAB" to "#0A140D", // forest
        "#C2E3B2" to "#0D1A11", // pitches
        "#ECE9E4" to "#0E0F11", // industrial
        "#F1E8D8" to "#100F12", // education
        "#F7E0DE" to "#140E10", // hospital
        "#FBFAF7" to "#141518", // pedestrian areas
        "#EDEAE5" to "#141518", // buildings
        "#DEDAD3" to "#1E2024", // building outlines
        "#C9C2B6" to "#3A3D44", // paths, steps
        "#D9D3C9" to "#141518", // road casings
        "#FFFFFF" to "#2A2C31", // roads
        "#E5B64C" to "#6B5520", // highway casing
        "#FFD873" to "#8A7230", // highway
        "#B8B2A8" to "#3A3D44", // rail
        "#B9B2C6" to "#6A6478", // border
        "#5E6168" to "#B5B9C0", // street names
        "#3F7FB5" to "#7FA9C6", // water names
        "#D6EBF8" to "#0A1724", // water-name halo
        "#7D828B" to "#9DA2AB", // district names
    )
}
