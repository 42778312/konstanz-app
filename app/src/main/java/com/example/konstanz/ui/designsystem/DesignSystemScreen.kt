package com.example.konstanz.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.konstanz.ui.components.BadgeSize
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.FloatingIconButton
import com.example.konstanz.ui.components.FloatingRoundButton
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.SampleDepartures
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.SettingsGroup
import com.example.konstanz.ui.components.SettingsRow
import com.example.konstanz.ui.components.StatusChip
import com.example.konstanz.ui.components.SwitchRow
import com.example.konstanz.ui.components.TripStatus
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.Spacing
import com.example.konstanz.ui.theme.outlined

/**
 * Living copy of the "00 Design system" artboard, built only from real tokens and components.
 * Shown on launch until navigation lands (step B1); kept afterwards as a debug screen.
 */
@Composable
fun DesignSystemScreen(modifier: Modifier = Modifier) {
    val c = KonstanzTheme.colors
    val t = KonstanzTheme.type
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(c.background),
        contentPadding = PaddingValues(
            start = Spacing.m, end = Spacing.m,
            top = insets.calculateTopPadding() + Spacing.m,
            bottom = insets.calculateBottomPadding() + Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text("Konstanz Transit", style = t.Display, color = c.ink)
                Text("Design system · map-first, offline-first.", style = t.Body, color = c.ink2)
            }
        }

        item {
            Section("Colour") {
                val swatches = listOf(
                    "Primary" to c.primary, "Pressed" to c.primaryPressed, "Tint" to c.primaryTint,
                    "Ink" to c.ink, "Ink 2" to c.ink2, "Ink 3" to c.ink3, "Line" to c.line,
                    "Location" to c.location, "Live" to c.live, "Delayed" to c.delayed, "Detour" to c.detour,
                    "Land" to c.mapLand, "Water" to c.mapWater, "Park" to c.mapPark, "Building" to c.mapBuilding,
                )
                SwatchGrid(swatches)
            }
        }

        item {
            Section("Typography") {
                Text("Plan your journey", style = t.Title, color = c.ink)
                Text("Konstanz Bahnhof", style = t.TitleL, color = c.ink)
                Text("Walk 4 min to Konstanz Bahnhof, then take Bus 12.", style = t.Body, color = c.ink2)
                Text("14:32 – 14:48 · 16 min", style = t.Time, color = c.ink)
            }
        }

        item {
            Section("Buttons") {
                KtButton("Find routes", {}, Modifier.fillMaxWidth())
                KtButton("Route from here", {}, Modifier.fillMaxWidth(), ButtonVariant.Tonal)
                KtButton("All departures", {}, Modifier.fillMaxWidth(), ButtonVariant.Neutral)
                KtButton("Add a place", {}, Modifier.fillMaxWidth(), ButtonVariant.Outline, leadingIcon = KtIcons.Plus)
                KtButton("Clear all", {}, Modifier.fillMaxWidth(), ButtonVariant.Text)
                KtButton("Update data", {}, Modifier.fillMaxWidth(), enabled = false)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                    FloatingIconButton(KtIcons.Layers, "Map layers", {})
                    FloatingRoundButton(KtIcons.Locate, "My location", {}, tint = c.location)
                }
            }
        }

        item {
            Section("Transit badges & realtime") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LineBadge("12", size = BadgeSize.L)
                    LineBadge("5")
                    LineBadge("8")
                    LineBadge("9", size = BadgeSize.S)
                }
                ChipFlow(
                    listOf(
                        TripStatus.OnTime, TripStatus.Delay(4), TripStatus.Cancelled, TripStatus.Detour,
                        TripStatus.Live, TripStatus.LastKnown(), TripStatus.Stale(), TripStatus.Scheduled, TripStatus.NoRealtime,
                    )
                )
            }
        }

        item {
            Section("Departures") {
                Column(Modifier.fillMaxWidth().background(c.surface, Radius.Panel).padding(horizontal = Spacing.m)) {
                    SampleDepartures.forEachIndexed { i, d ->
                        DepartureRow(d, onClick = {}, showDivider = i < SampleDepartures.lastIndex)
                    }
                }
            }
        }

        item {
            var stops by remember { mutableStateOf(true) }
            var paths by remember { mutableStateOf(false) }
            Section("Lists") {
                SettingsGroup("Map") {
                    SwitchRow("Show bus stops", stops, { stops = it })
                    SwitchRow("Show walking paths", paths, { paths = it })
                    SettingsRow("Map style", value = "Standard", onClick = {}, showDivider = false)
                }
                SettingsGroup("Legal") {
                    SettingsRow("Open-source licenses", icon = KtIcons.Document, showChevron = true, onClick = {})
                    SettingsRow("Privacy policy", icon = KtIcons.Shield, showChevron = true, onClick = {}, showDivider = false)
                }
            }
        }

        item {
            // Artboard 34: every error says what still works and offers one clear next step.
            Section("Error states") {
                errorStates.forEach { e ->
                    Column(Modifier.fillMaxWidth().background(c.surface, Radius.Panel).padding(Spacing.m)) {
                        Text(e.where.uppercase(), Modifier.padding(bottom = Spacing.s), style = t.Label, color = c.ink3)
                        com.example.konstanz.ui.components.ErrorCard(
                            icon = e.icon, title = e.title, message = e.message,
                            primary = e.primary, onPrimary = {}, secondary = e.secondary,
                        )
                    }
                }
            }
        }

        item {
            Section("Icons · 24 px") { IconGrid() }
        }
    }
}

private data class ErrorState(
    val where: String,
    val icon: com.example.konstanz.ui.icons.KtIcon,
    val title: String,
    val message: String,
    val primary: String,
    val secondary: String,
)

// The eight states of artboard 34, word for word.
private val errorStates = listOf(
    ErrorState("Main map", KtIcons.Map, "Map couldn't load", "The saved map file couldn't be opened. Stops and timetables are still available as a list.", "Retry", "Show stops list"),
    ErrorState("Launch / any screen", KtIcons.Database, "Offline data is damaged", "Some timetable files are corrupted. Map, search and walking routes still work.", "Repair timetable", "Map only"),
    ErrorState("Offline data", KtIcons.Download, "Update failed", "We couldn't download the new timetable. You're still using yesterday's version.", "Try again", "Details"),
    ErrorState("Main map, journey", KtIcons.Locate, "Can't find your location", "GPS signal is weak. Step outside or choose a starting point on the map.", "Select on map", "Retry"),
    ErrorState("Route results", KtIcons.Nav, "Route calculation failed", "Something went wrong while planning. Try again or change the departure time.", "Try again", "Change time"),
    ErrorState("Search", KtIcons.Search, "Search isn't working", "The search index couldn't be read. Browse the map or pick a saved place instead.", "Rebuild index", "Browse map"),
    ErrorState("Route preferences, stop", KtIcons.Calendar, "No timetable for this date", "Saved timetable data ends before this date. Update data to see later departures.", "Update data", "Pick another date"),
    ErrorState("Stop, departures, journey", KtIcons.LiveOff, "Live times unavailable", "Showing scheduled departures. Last realtime update 47 minutes ago.", "Retry", "Dismiss"),
)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionLabel(title)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchGrid(swatches: List<Pair<String, Color>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        swatches.forEach { (name, color) ->
            Column(Modifier.width(80.dp)) {
                Box(Modifier.size(width = 80.dp, height = 44.dp).outlined(Radius.Small).background(color, Radius.Small))
                Text(name, style = KonstanzTheme.type.Caption, color = KonstanzTheme.colors.ink2)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipFlow(statuses: List<TripStatus>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        statuses.forEach { StatusChip(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGrid() {
    FlowRow(
        Modifier.fillMaxWidth().background(KonstanzTheme.colors.surface, Radius.Panel).padding(Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        KtIcons.all.forEach { icon ->
            Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                KonstanzIcon(icon, contentDescription = null, tint = KonstanzTheme.colors.ink, strokeWidth = 2f)
                Text(icon.name, style = KonstanzTheme.type.Caption, color = KonstanzTheme.colors.ink3, maxLines = 1)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 2600)
@Composable
private fun DesignSystemScreenPreview() {
    KonstanzTheme { DesignSystemScreen() }
}
