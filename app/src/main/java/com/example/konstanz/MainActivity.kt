package com.example.konstanz

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.Texts
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.theme.KonstanzTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate: swaps Theme.Konstanz.Starting for the app theme.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Texts.init(applicationContext)
        SavedStore.init(applicationContext)
        Transit.init(applicationContext)
        // Parses the place search index in the background so it's ready before Search is opened,
        // instead of stalling the first keystroke there.
        lifecycleScope.launch(Dispatchers.Default) { Transit.repository.warmSearch() }
        // Fade the system splash into the Compose splash (same red, same logo tile).
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction { provider.remove() }
                .start()
        }
        // Bar icons follow the phone's light/dark mode, like the theme: dark on light screens, light on dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            KonstanzTheme {
                KonstanzApp()
            }
        }
    }
}
