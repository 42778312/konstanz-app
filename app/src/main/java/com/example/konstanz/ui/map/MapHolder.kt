package com.example.konstanz.ui.map

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * The one offline map of the activity, shared by every screen that shows it (main map, planner).
 * Leaving a screen only takes the view off it; the GL map, its style, its tile cache and the stop layer
 * stay loaded, so coming back shows the map at once instead of building it again from the files.
 * Destroyed with the activity (see [ProvideMapHolder]).
 */
class MapHolder internal constructor(private val context: Context) {
    /** Bus stops as layers of the map; added to every style this map loads. */
    val stops = NativeStopLayer()

    /** Set once MapLibre is ready (state, so screens waiting for it start when it is). */
    var map by mutableStateOf<MapLibreMap?>(null)
        private set

    /** The screen currently showing the map; only it moves the camera. */
    var owner by mutableStateOf<Any?>(null)
        private set

    private var view: MapView? = null
    private var lifecycleState = Lifecycle.State.CREATED
    private var styleDark: Boolean? = null
    private var styleWidthPx = 0f

    /** Puts the map into [container] (taking it from the screen that had it), opened at [camera] the first time. */
    internal fun attach(container: FrameLayout, token: Any, camera: CameraPosition) {
        val v = view ?: create(camera).also { view = it }
        if (v.parent !== container) {
            (v.parent as? ViewGroup)?.removeView(v)
            container.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        owner = token
    }

    /** [container]'s screen is going away; if it still shows the map, the map leaves with it (and stays loaded). */
    internal fun detach(container: FrameLayout, token: Any) {
        view?.let { if (it.parent === container) container.removeView(it) }
        if (owner === token) owner = null
    }

    /**
     * Loads the style for [dark] once; later calls with the same theme and screen width do nothing.
     * [zooms] gives the MapLibre zooms where stops switch to tiles / dots for this width.
     */
    internal fun ensureStyle(json: String, dark: Boolean, widthPx: Float, density: Float, zooms: () -> Pair<Float, Float>) {
        val m = map ?: return
        if (styleDark == dark && styleWidthPx == widthPx) return
        styleDark = dark
        styleWidthPx = widthPx
        stops.detach()
        m.setStyle(Style.Builder().fromJson(json)) { style ->
            val (tileZoom, dotZoom) = zooms()
            stops.attach(m, style, density, tileZoom = tileZoom, dotZoom = dotZoom)
        }
    }

    internal fun onLifecycle(event: Lifecycle.Event) {
        lifecycleState = event.targetState
        val v = view ?: return
        when (event) {
            Lifecycle.Event.ON_START -> v.onStart()
            Lifecycle.Event.ON_RESUME -> v.onResume()
            Lifecycle.Event.ON_PAUSE -> v.onPause()
            Lifecycle.Event.ON_STOP -> v.onStop()
            else -> Unit
        }
    }

    /** MapLibre keeps tiles, glyphs and sprites in native memory; this is the only way to make it let go. */
    internal fun onLowMemory() {
        view?.onLowMemory()
    }

    internal fun destroy() {
        val v = view ?: return
        view = null
        map = null
        owner = null
        styleDark = null
        stops.detach()
        (v.parent as? ViewGroup)?.removeView(v)
        if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) v.onPause()
        if (lifecycleState.isAtLeast(Lifecycle.State.STARTED)) v.onStop()
        v.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(camera: CameraPosition): MapView {
        MapLibre.getInstance(context)
        val options = MapLibreMapOptions.createFromAttributes(context)
            // TextureView: clips and animates like any Compose content (a SurfaceView would punch through sheets).
            .textureMode(true)
            .logoEnabled(false)
            .attributionEnabled(false)
            .compassEnabled(false)
            // Open where the screen looks, not at MapLibre's world view.
            .camera(camera)
        return object : MapView(context, options) {
            // Gestures belong to InteractiveMap; this view only draws.
            override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
        }.apply {
            onCreate(null)
            // Created mid-lifecycle: catch up with the activity.
            if (lifecycleState.isAtLeast(Lifecycle.State.STARTED)) onStart()
            if (lifecycleState.isAtLeast(Lifecycle.State.RESUMED)) onResume()
            getMapAsync { m ->
                m.uiSettings.setAllGesturesEnabled(false)
                map = m
            }
        }
    }
}

/** The activity's shared map; null outside [ProvideMapHolder] (then each map makes its own). */
val LocalMapHolder = staticCompositionLocalOf<MapHolder?> { null }

/** Gives [content] one map for the activity's lifetime, following the activity's lifecycle and memory trims. */
@Composable
fun ProvideMapHolder(content: @Composable () -> Unit) {
    val holder = rememberOwnedMapHolder()
    CompositionLocalProvider(LocalMapHolder provides holder, content = content)
}

/** A [MapHolder] that lives as long as this composition does. */
@Composable
internal fun rememberOwnedMapHolder(): MapHolder {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val holder = remember { MapHolder(context) }
    DisposableEffect(lifecycle, holder) {
        val observer = LifecycleEventObserver { _, event -> holder.onLifecycle(event) }
        lifecycle.addObserver(observer)
        val trims = object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                @Suppress("DEPRECATION")
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && level != ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                    holder.onLowMemory()
                }
            }
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = holder.onLowMemory()
        }
        context.applicationContext.registerComponentCallbacks(trims)
        onDispose {
            context.applicationContext.unregisterComponentCallbacks(trims)
            lifecycle.removeObserver(observer)
            holder.destroy()
        }
    }
    return holder
}
