package com.example.konstanz

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.Texts
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.theme.KonstanzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps Theme.Konstanz.Starting for the app theme.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Texts.init(applicationContext)
        SavedStore.init(applicationContext)
        Transit.init(applicationContext)
        // Fade the system splash into the Compose splash (same red, same logo tile).
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction { provider.remove() }
                .start()
        }
        // The app is always light (the design has no dark theme): keep dark bar icons even when the
        // phone is in dark mode, or the clock and battery turn white on white screens.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            KonstanzTheme {
                KonstanzApp()
            }
        }
    }
}
