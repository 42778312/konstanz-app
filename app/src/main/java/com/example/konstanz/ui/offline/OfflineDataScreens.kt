package com.example.konstanz.ui.offline

import androidx.compose.ui.res.pluralStringResource
import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.AppStatus
import com.example.konstanz.data.DataHealth
import com.example.konstanz.data.OfflineData
import com.example.konstanz.data.OfflineDataInfo
import com.example.konstanz.data.display
import com.example.konstanz.data.formatBytes
import com.example.konstanz.data.transit.DataSource
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.SmallSpinner
import com.example.konstanz.ui.components.SwitchRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Delayed
import com.example.konstanz.ui.theme.DelayedTint
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White
import kotlinx.coroutines.launch

/** Third storage colour (#9DA1A9). */
private val StorageGrey = Color(0xFF9DA1A9)

/** The health shown everywhere (map pill, Settings, this page): the developer simulation counts as damaged. */
val OfflineDataInfo?.shownHealth: DataHealth?
    get() = if (AppStatus.simulateDamagedTimetable) DataHealth.Damaged else this?.health

// ---------- 25 Offline data ----------

/**
 * What is stored for offline use, with real dates and sizes. Everything ships inside the app, so there
 * is no download here: new data comes with app updates. "Repair" makes fresh copies from the app.
 */
@Composable
fun OfflineDataScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var repairing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { OfflineData.refresh(context) }
    val info = OfflineData.info

    Column(Modifier.fillMaxSize().background(Surface)) {
        KtTopBar(stringResource(R.string.offline_data), onBack = onBack, showDivider = true)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (info == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { SmallSpinner(size = 28.dp) }
                return@Column
            }
            StatusCard(info)
            Column(Modifier.padding(top = 8.dp)) {
                DataRow(KtIcons.Map, stringResource(R.string.tab_map), "OpenStreetMap" + (info.mapDate?.let { " · ${it.display()}" } ?: ""))
                DataRow(
                    KtIcons.Calendar, stringResource(R.string.timetable),
                    listOfNotNull("NVBW", info.timetableDate?.display(), info.validUntil?.let { stringResource(R.string.valid_until, it.display()) }).joinToString(" · "),
                )
                DataRow(KtIcons.LiveOff, stringResource(R.string.live_times), stringResource(R.string.live_times_sub), iconTint = Ink3) {
                    IconChip(stringResource(R.string.timetable), KtIcons.Calendar, Ink3, null)
                }
                StorageBar(info)
            }
            Row(
                Modifier.padding(top = 10.dp).fillMaxWidth().background(Background, RoundedCornerShape(16.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KonstanzIcon(KtIcons.Info, contentDescription = null, size = 20.dp, tint = Ink2)
                Text(
                    stringResource(R.string.updates_note),
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp, lineHeight = 1.45.em),
                    color = Ink2,
                )
            }
        }
        Row(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 30.dp, top = 8.dp)) {
            val damaged = info.shownHealth == DataHealth.Damaged
            val repairedText = stringResource(R.string.data_repaired)
            val failedText = stringResource(R.string.repair_failed)
            KtButton(
                stringResource(if (repairing) R.string.repairing else R.string.repair_data),
                {
                    if (repairing) return@KtButton
                    repairing = true
                    scope.launch {
                        val ok = OfflineData.repair(context)
                        repairing = false
                        Toast.makeText(context, if (ok) repairedText else failedText, Toast.LENGTH_LONG).show()
                    }
                },
                Modifier.fillMaxWidth(),
                variant = if (damaged) ButtonVariant.Primary else ButtonVariant.Neutral,
                leadingIcon = KtIcons.Refresh,
                compact = true,
                enabled = info != null,
            )
        }
    }
}

/** Green "Ready for offline use", orange for damaged / expired, amber-ish for "ends soon". */
@Composable
private fun StatusCard(info: OfflineDataInfo) {
    val until = info.validUntil?.display() ?: "—"
    val (bg, tile, icon, title, text) = when (info.shownHealth) {
        DataHealth.Damaged -> Five(DelayedTint, Delayed, KtIcons.Database, stringResource(R.string.data_needs_repair),
            stringResource(R.string.damaged_body))
        DataHealth.Expired -> Five(DelayedTint, Delayed, KtIcons.Alert, stringResource(R.string.timetable_expired),
            stringResource(R.string.expired_body, until))
        DataHealth.ExpiresSoon -> Five(DelayedTint, Delayed, KtIcons.Calendar, stringResource(R.string.timetable_ends_soon),
            pluralStringResource(R.plurals.ends_soon_body, (info.daysLeft ?: 0L).toInt(), (info.daysLeft ?: 0L).toInt(), until))
        else -> Five(LiveTint, Live, KtIcons.Check, stringResource(R.string.ready_offline),
            stringResource(R.string.ready_offline_body))
    }
    Row(
        Modifier.fillMaxWidth().background(bg, RoundedCornerShape(18.dp)).padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).background(tile, CircleShape), contentAlignment = Alignment.Center) {
            KonstanzIcon(icon, contentDescription = null, size = 22.dp, tint = White, strokeWidth = 2.6f)
        }
        Column {
            Text(title, style = KonstanzType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(text, style = KonstanzType.BodySmall.copy(fontSize = 14.sp), color = Ink2)
        }
    }
}

private data class Five(val bg: Color, val tile: Color, val icon: KtIcon, val title: String, val text: String)

@Composable
private fun DataRow(icon: KtIcon, title: String, subtitle: String, iconTint: Color = Ink, trailing: @Composable () -> Unit = {}) {
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp).background(Background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                KonstanzIcon(icon, contentDescription = null, size = 20.dp, tint = iconTint)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
                Text(subtitle, style = KonstanzType.BodySmall.copy(fontSize = 14.sp), color = Ink3)
            }
            trailing()
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

/** "Storage used 11.2 MB" with the map / timetable / saved split. */
@Composable
private fun StorageBar(info: OfflineDataInfo) {
    val total = info.totalBytes.coerceAtLeast(1)
    val parts = listOf(
        Triple(stringResource(R.string.tab_map), info.mapBytes, Primary),
        Triple(stringResource(R.string.timetable_places), info.timetableBytes, Ink),
        Triple(stringResource(R.string.your_saved_places), info.savedBytes, StorageGrey),
    )
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.storage_used), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(formatBytes(info.totalBytes), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"), color = Ink)
        }
        Canvas(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Background)) {
            var x = 0f
            val gap = 2.dp.toPx()
            parts.forEach { (_, bytes, c) ->
                val w = size.width * bytes / total
                drawRect(c, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(w, size.height))
                x += w + gap
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            parts.forEach { (label, bytes, c) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).background(c, RoundedCornerShape(3.dp)))
                    Text("$label · ${formatBytes(bytes)}", style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink2)
                }
            }
        }
    }
}

// ---------- Developer: simulate the system states ----------

/** Settings → Developer → Simulate states: switch the error / offline screens on without breaking the phone. */
@Composable
fun SimulateStatesScreen(onBack: () -> Unit) {
    val s = AppStatus
    Column(Modifier.fillMaxSize().background(Background)) {
        KtTopBar("Simulate states", onBack = onBack, background = Background)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Show the offline and error screens without turning off Wi-Fi or GPS. States reset when the app restarts.",
                style = KonstanzType.BodySmall,
                color = Ink2,
            )
            com.example.konstanz.ui.components.SettingsGroup("States") {
                SwitchRow("Offline (24)", s.simulateOffline, { s.simulateOffline = it; s.offlineBannerDismissed = false }, subtitle = "No internet, banner on the map")
                SwitchRow("Location unavailable (31)", s.simulateLocationUnavailable, { s.simulateLocationUnavailable = it }, subtitle = "No GPS fix")
                SwitchRow("Realtime unavailable (32)", s.simulateRealtimeUnavailable, { s.simulateRealtimeUnavailable = it }, subtitle = "Stops show scheduled times")
                SwitchRow(
                    "Timetable damaged (34b)", s.simulateDamagedTimetable, { s.simulateDamagedTimetable = it; s.damagedSheetDismissed = false },
                    subtitle = "Repair sheet on the map; Repair clears it", showDivider = false,
                )
            }
            com.example.konstanz.ui.components.SettingsGroup("Data") {
                val real = Transit.source == DataSource.Timetable
                SwitchRow(
                    DataSource.Timetable.label,
                    real,
                    { Transit.select(if (it) DataSource.Timetable else DataSource.Design) },
                    subtitle = if (real) "Real stops, departures and routes · kept after restart" else "Off: the design's sample network",
                    showDivider = false,
                )
            }
        }
    }
}
