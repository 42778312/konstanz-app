package com.example.konstanz.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * System states the screens react to (Part J). The data's health comes from [OfflineData]; the app
 * never goes online, so there is no network state. The simulations are for the developer screen only
 * (Settings → Developer → Simulate states). In memory: a restart resets them.
 */
object AppStatus {
    // --- Simulation switches (Developer screen) ---
    var simulateLocationUnavailable by mutableStateOf(false)
    var simulateRealtimeUnavailable by mutableStateOf(false)
    /** Makes [OfflineData] report the timetable as damaged, to try the repair sheet. */
    var simulateDamagedTimetable by mutableStateOf(false)

    // --- Dismissed banners ---
    var damagedSheetDismissed by mutableStateOf(false)

    val realtimeAvailable: Boolean get() = !simulateRealtimeUnavailable
    val timetableDamaged: Boolean get() = simulateDamagedTimetable || OfflineData.info?.health == DataHealth.Damaged
}
