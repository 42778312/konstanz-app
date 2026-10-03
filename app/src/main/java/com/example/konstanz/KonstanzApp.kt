package com.example.konstanz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.konstanz.data.ConnectivityWatcher
import com.example.konstanz.data.LocationWatcher
import com.example.konstanz.data.OfflineData
import com.example.konstanz.ui.map.ProvideMapHolder
import com.example.konstanz.ui.navigation.KonstanzNavHost

/** App root: splash → onboarding → main. */
@Composable
fun KonstanzApp() {
    val context = LocalContext.current
    // Checks the offline data once at start: status pill, repair sheet, expiry warning.
    LaunchedEffect(Unit) { OfflineData.refresh(context) }
    ConnectivityWatcher()
    LocationWatcher()
    // One offline map for every screen: navigating away and back no longer rebuilds it.
    ProvideMapHolder { KonstanzNavHost() }
}
