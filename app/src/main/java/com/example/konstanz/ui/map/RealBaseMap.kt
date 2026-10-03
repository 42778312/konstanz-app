package com.example.konstanz.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.konstanz.data.OfflineData
import com.example.konstanz.data.transit.DataSource
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Transit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** The real map goes with the real timetable; the design sample keeps the design's drawing. */
val realMapEnabled: Boolean get() = Transit.source == DataSource.Timetable

/**
 * The offline OpenStreetMap of Konstanz (assets/map: PMTiles + style in the design's colours),
 * drawn by MapLibre under the Compose overlays. It never handles touches: [InteractiveMap]'s camera
 * owns pan and zoom, and this view follows it every frame, so markers stay glued to the streets.
 */
@Composable
internal fun RealBaseMap(
    projection: () -> MapProjection,
    widthPx: Float,
    heightPx: Float,
    modifier: Modifier = Modifier,
    stops: NativeStopLayer? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember { drawOnlyMapView(context) { map = it } }
    // Reloaded on a light/dark switch; the stop layer is re-attached with the new palette.
    val dark = isSystemInDarkTheme()
    val styleJson by produceState<String?>(null, dark) {
        value = withContext(Dispatchers.IO) { runCatching { OfflineMapFiles.styleJson(context, dark) }.getOrNull() }
    }
    LaunchedEffect(map, styleJson) {
        val m = map ?: return@LaunchedEffect
        val json = styleJson ?: return@LaunchedEffect
        // The app's zoom steps (tiles from 0.8, dots from 0.5) as MapLibre zooms on this screen.
        fun mapZoom(appZoom: Float) = Geo.mapLibreZoom(widthPx / MapCameraState.DESIGN_WIDTH_UNITS * appZoom, density).toFloat()
        m.setStyle(Style.Builder().fromJson(json)) { style ->
            stops?.attach(m, style, density, tileZoom = mapZoom(0.8f), dotZoom = mapZoom(0.5f))
        }
    }

    DisposableEffect(stops) { onDispose { stops?.detach() } }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Same view as the Compose projection: the point in the middle of the box, at the same scale. Follows the
    // camera state directly (no recomposition per frame).
    LaunchedEffect(map, widthPx, heightPx) {
        val m = map ?: return@LaunchedEffect
        snapshotFlow { projection() }.collect { p ->
            val middle = p.toMap(Offset(widthPx / 2f, heightPx / 2f))
            val (lat, lon) = Geo.latLon(MapPoint(middle.x, middle.y))
            val zoom = Geo.mapLibreZoom(p.scale, density)
            m.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(lat, lon)).zoom(zoom).bearing(0.0).tilt(0.0).build()
            ))
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

@SuppressLint("ClickableViewAccessibility")
private fun drawOnlyMapView(context: Context, onReady: (MapLibreMap) -> Unit): MapView {
    MapLibre.getInstance(context)
    val options = MapLibreMapOptions.createFromAttributes(context)
        // TextureView: clips and animates like any Compose content (a SurfaceView would punch through sheets).
        .textureMode(true)
        .logoEnabled(false)
        .attributionEnabled(false)
        .compassEnabled(false)
    return object : MapView(context, options) {
        // Gestures belong to InteractiveMap; this view only draws.
        override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
    }.apply {
        onCreate(null)
        getMapAsync { map ->
            map.uiSettings.setAllGesturesEnabled(false)
            onReady(map)
        }
    }
}

/** The style from the assets, pointed at the map file ([OfflineData.mapFile]). */
private object OfflineMapFiles {
    private const val TILES = "map/konstanz.pmtiles"
    private const val STYLE = "map/style.json"

    fun styleJson(context: Context, dark: Boolean): String {
        val copy = OfflineData.mapFile(context)
        var style = context.assets.open(STYLE).bufferedReader().use { it.readText() }
            .replace("pmtiles://asset://$TILES", "pmtiles://file://${copy.absolutePath}")
        if (dark) {
            // White halos and POI-dot rings sit on land, unlike white roads.
            style = style.replace("\"text-halo-color\": \"#FFFFFF\"", "\"text-halo-color\": \"#1E2126\"")
                .replace("\"circle-stroke-color\": \"#FFFFFF\"", "\"circle-stroke-color\": \"#1E2126\"")
            DARK.forEach { (light, night) -> style = style.replace("\"$light\"", "\"$night\"", ignoreCase = true) }
        }
        return style
    }

    /** The style's light colours → dark ones (same roles as DarkColors' map tokens). POI colours stay. */
    private val DARK = mapOf(
        "#A7D3F2" to "#1B3347", // water
        "#93C4E8" to "#24425A", // shore
        "#F6F4EF" to "#1E2126", // land
        "#CFE8C0" to "#1F2E23", // landcover, parks
        "#BFDDAB" to "#1C2B20", // forest
        "#C2E3B2" to "#213325", // pitches
        "#ECE9E4" to "#23262B", // industrial
        "#F1E8D8" to "#26252A", // education
        "#F7E0DE" to "#2B2326", // hospital
        "#FBFAF7" to "#2A2D33", // pedestrian areas
        "#EDEAE5" to "#2A2D33", // buildings
        "#DEDAD3" to "#33363D", // building outlines
        "#C9C2B6" to "#4A4E56", // paths, steps
        "#D9D3C9" to "#26292E", // road casings
        "#FFFFFF" to "#363A41", // roads
        "#E5B64C" to "#6B5520", // highway casing
        "#FFD873" to "#8A7230", // highway
        "#B8B2A8" to "#5A5E66", // rail
        "#B9B2C6" to "#6A6478", // border
        "#5E6168" to "#B5B9C0", // street names
        "#3F7FB5" to "#7FA9C6", // water names
        "#D6EBF8" to "#1B3347", // water-name halo
        "#7D828B" to "#9DA2AB", // district names
    )
}
