package com.example.konstanz.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * System states the screens react to (Part J). The network is real (see ConnectivityWatcher) and the
 * data's health comes from [OfflineData]; the simulations are for the developer screen only
 * (Settings → Developer → Simulate states). In memory: a restart resets them.
 */
object AppStatus {
    /** Set from the phone's connectivity. */
    var networkOnline by mutableStateOf(true)

    // --- Simulation switches (Developer screen) ---
    var simulateOffline by mutableStateOf(false)
    var simulateLocationUnavailable by mutableStateOf(false)
    var simulateRealtimeUnavailable by mutableStateOf(false)
    /** Makes [OfflineData] report the timetable as damaged, to try the repair sheet. */
    var simulateDamagedTimetable by mutableStateOf(false)

    // --- Dismissed banners ---
    var offlineBannerDismissed by mutableStateOf(false)
    var damagedSheetDismissed by mutableStateOf(false)

    val offline: Boolean get() = simulateOffline || !networkOnline
    val realtimeAvailable: Boolean get() = !offline && !simulateRealtimeUnavailable
    val timetableDamaged: Boolean get() = simulateDamagedTimetable || OfflineData.info?.health == DataHealth.Damaged
}
