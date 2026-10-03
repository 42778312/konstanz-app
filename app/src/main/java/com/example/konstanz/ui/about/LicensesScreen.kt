package com.example.konstanz.ui.about

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.SettingsGroup
import com.example.konstanz.ui.components.SettingsRow
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.Spacing

/** Third-party components shipped in the app. Keep in sync with gradle/libs.versions.toml and res/font. */
private data class License(val name: String, val detail: String)

private val fonts = listOf(
    License("Figtree", "© The Figtree Project Authors · SIL Open Font License 1.1"),
    License("Noto Sans (map labels)", "© Google LLC · SIL Open Font License 1.1"),
    License("Bricolage Grotesque", "© The Bricolage Grotesque Project Authors · SIL Open Font License 1.1"),
)

private val data = listOf(
    License("OpenStreetMap", "© OpenStreetMap contributors · Open Database License (ODbL) · map tiles built by Protomaps"),
    License(
        "Timetable (Verkehrsverbund Hegau-Bodensee)",
        "Datensatz der NVBW GmbH · Datenlizenz Deutschland – Namensnennung – Version 2.0 (govdata.de/dl-de/by-2-0) · filtered to the Konstanz area",
    ),
    License("Bus stops (platform directions, positions)", "Stadt Konstanz · Bushaltestellen"),
)

private val libraries = listOf(
    License("AndroidX (Core, Activity, Lifecycle, Navigation, DataStore, Room, SplashScreen)", "© The Android Open Source Project · Apache License 2.0"),
    License("Jetpack Compose & Material 3", "© The Android Open Source Project · Apache License 2.0"),
    License("Kotlin & kotlinx.coroutines", "© JetBrains s.r.o. · Apache License 2.0"),
    License("MapLibre Native for Android", "© MapLibre contributors · BSD 2-Clause License"),
)

/** Open-source licenses, opened from About. Not a separate artboard; built from About's list style. */
@Composable
fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(Background)) {
        KtTopBar(stringResource(R.string.oss_licenses), onBack = onBack, background = Background)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.m, end = Spacing.m, top = Spacing.xs, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            LicenseGroup(stringResource(R.string.fonts), fonts)
            LicenseGroup(stringResource(R.string.map_data), data)
            LicenseGroup(stringResource(R.string.libraries), libraries)
        }
    }
}

@Composable
private fun LicenseGroup(title: String, items: List<License>) {
    SettingsGroup(title) {
        items.forEachIndexed { i, item ->
            SettingsRow(item.name, subtitle = item.detail, showDivider = i < items.lastIndex)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LicensesPreview() {
    KonstanzTheme { LicensesScreen(onBack = {}) }
}
