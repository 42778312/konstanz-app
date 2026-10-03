package com.example.konstanz.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A position fix from the phone. */
data class LocationFix(val latitude: Double, val longitude: Double, val accuracyM: Float)

/** The phone's position, kept fresh by [LocationWatcher] while the app is shown. */
object UserLocation {
    /** Last fix; null until the first one (or without permission). */
    var fix by mutableStateOf<LocationFix?>(null)
        internal set
}

/**
 * Follows the phone's position while the app is in the foreground, with the platform's
 * LocationManager (works offline, no Play services). Starts as soon as the permission is granted:
 * the permission dialog pauses the app, and resuming checks again.
 */
@Composable
fun LocationWatcher() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { UserLocation.fix = it.toFix() }
        var listening = false

        @SuppressLint("MissingPermission") // checked by hasPermission()
        fun start() {
            if (listening || !hasPermission(context)) return
            val providers = manager.getProviders(true)
            providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { UserLocation.fix = it.toFix() }
            val provider = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in providers -> LocationManager.FUSED_PROVIDER
                LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
                LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
                else -> return
            }
            // Every 5 s or 10 m: enough for "walk to the stop", easy on the battery.
            manager.requestLocationUpdates(provider, 5_000L, 10f, listener, Looper.getMainLooper())
            listening = true
        }

        fun stop() {
            if (listening) manager.removeUpdates(listener)
            listening = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            stop()
        }
    }
}

private fun hasPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Location.toFix() = LocationFix(latitude, longitude, accuracy)
