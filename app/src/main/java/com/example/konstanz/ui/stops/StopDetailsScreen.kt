package com.example.konstanz.ui.stops

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.transit.DataStatus
import com.example.konstanz.data.transit.Line
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.ServiceAlert
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.TopBarIconButton
import com.example.konstanz.ui.components.toRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.map.MapIllustration
import com.example.konstanz.ui.map.SelectedStopMarker
import com.example.konstanz.ui.map.StopMarkersLayer
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Detour
import com.example.konstanz.ui.theme.DetourTint
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line as LineColor
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.White

data class StopDetailsActions(
    val onBack: () -> Unit,
    val onViewAll: (line: String?) -> Unit,
    val onDepartureClick: (StopDeparture) -> Unit,
    val onRouteFromHere: (stopName: String) -> Unit,
)

/** 13 Bus stop details (full page). Loads from the mock; live data follows the timetable. */
@Composable
fun StopDetailsRoute(stopId: String, actions: StopDetailsActions) {
    val repo = Transit.repository
    val stop by produceState<Stop?>(null, stopId) { value = repo.stop(stopId) }
    val departures by produceState(emptyList<StopDeparture>(), stopId) {
        value = repo.departures(stopId, limit = 5, includeCancelled = false)
    }
    val lines by produceState(emptyList<Line>(), stopId) { value = repo.linesAt(stopId) }
    val alerts by produceState(emptyList<ServiceAlert>(), stopId) { value = repo.alertsAt(stopId) }
    val dataStatus by produceState<DataStatus?>(null) { value = repo.dataStatus() }
    // "Refresh" asks for live data again (shows the checking state meanwhile).
    var refreshes by remember { mutableIntStateOf(0) }
    var realtime by remember { mutableStateOf<RealtimeInfo?>(null) }
    LaunchedEffect(stopId, refreshes) {
        realtime = null
        realtime = repo.realtimeInfo(stopId)
    }

    val current = stop ?: return
    StopDetailsScreen(
        stop = current,
        departures = departures,
        realtime = realtime,
        lines = lines,
        alerts = alerts,
        savedAt = dataStatus?.savedAt,
        saved = SavedStore.isStopSaved(stopId),
        onToggleSave = { SavedStore.toggleStop(stopId, current.name, departures.firstOrNull()) },
        onRefresh = { refreshes++ },
        actions = actions,
    )
}

@Composable
fun StopDetailsScreen(
    stop: Stop,
    departures: List<StopDeparture>,
    realtime: RealtimeInfo?,
    lines: List<Line>,
    alerts: List<ServiceAlert>,
    savedAt: String?,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onRefresh: () -> Unit,
    actions: StopDetailsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(White)) {
        KtTopBar(
            title = stop.name,
            subtitle = stopSubtitle(stop, walkMinutes = null),
            onBack = actions.onBack,
            showDivider = true,
        ) {
            TopBarIconButton(
                KtIcons.Star, stringResource(if (saved) R.string.remove_saved else R.string.save_stop), onToggleSave,
                tint = if (saved) Primary else Ink, filled = saved,
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            MiniMap(stop)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.next_departures).uppercase(), style = KonstanzType.Label, color = Ink3, maxLines = 1)
                        RealtimeStatus(realtime, Modifier.weight(1f, fill = false).padding(start = 12.dp))
                    }
                    departures.forEach { d ->
                        DepartureRow(d.toRow(), onClick = { actions.onDepartureClick(d) }, verticalPadding = 12.dp, realtimeReady = realtime != null)
                    }
                    ViewAllLink { actions.onViewAll(null) }
                }

                if (lines.isNotEmpty()) {
                    Column {
                        SectionLabel(stringResource(R.string.routes_serving), Modifier.padding(bottom = 4.dp))
                        lines.forEach { line -> ServingLineRow(line) { actions.onViewAll(line.id) } }
                    }
                }

                if (alerts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.service_alerts))
                        alerts.forEach { AlertCard(it, savedAt) }
                    }
                }

                LastUpdatedCard(realtime, onRefresh)

                KtButton(stringResource(R.string.route_from_here), { actions.onRouteFromHere(stop.name) }, Modifier.fillMaxWidth(), leadingIcon = KtIcons.Nav)
            }
        }
    }
}

/** 200 dp map strip centred on the stop (not interactive). */
@Composable
private fun MiniMap(stop: Stop) {
    // Same zoom as the stop sheet: 260 map units across.
    val width = 260f
    val height = width * 200f / 390f
    val box = Rect(stop.point.x - width / 2, stop.point.y - height / 2, stop.point.x + width / 2, stop.point.y + height / 2)
    val stops by produceState(emptyList<Stop>()) { value = Transit.repository.stops() }
    MapIllustration("maps/city.svg", Modifier.fillMaxWidth().height(200.dp), viewBox = box) {
        StopMarkersLayer(stops, onStopClick = {}, onClusterClick = { _, _ -> }, hiddenStopId = stop.id)
        SelectedStopMarker(Modifier.at(stop.point.x, stop.point.y, anchorBottom = true))
    }
}

@Composable
private fun ViewAllLink(onClick: () -> Unit) {
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.view_all_departures), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Primary)
        KonstanzIcon(KtIcons.ArrowRight, contentDescription = null, size = 18.dp, tint = Primary)
    }
}

/** "12  Bahnhof ↔ Wollmatingen  ›" */
@Composable
private fun ServingLineRow(line: Line, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(56.dp).clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LineBadge(line.id)
            Text(
                buildAnnotatedString {
                    append(line.terminusA)
                    withStyle(SpanStyle(color = Ink3)) { append("  ↔  ") }
                    append(line.terminusB)
                },
                Modifier.weight(1f),
                style = KonstanzType.RowTitle,
                color = Ink,
            )
            KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = Ink4, strokeWidth = 2.4f)
        }
        HorizontalDivider(thickness = 1.dp, color = LineColor)
    }
}

@Composable
private fun AlertCard(alert: ServiceAlert, savedAt: String?) {
    Row(
        Modifier.fillMaxWidth().background(DetourTint, RoundedCornerShape(16.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KonstanzIcon(KtIcons.Detour, contentDescription = null, size = 22.dp, tint = Detour)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(alert.title, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(alert.message, style = KonstanzType.BodySmall.copy(fontSize = 14.sp, lineHeight = 1.4.em), color = Ink2)
            if (savedAt != null) {
                Text(savedAt, Modifier.padding(top = 4.dp), style = KonstanzType.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = Ink3)
            }
        }
    }
}

/** "LAST UPDATED · 2 minutes ago · [Refresh]" */
@Composable
private fun LastUpdatedCard(realtime: RealtimeInfo?, onRefresh: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Background, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KonstanzIcon(KtIcons.Refresh, contentDescription = null, size = 22.dp, tint = Ink2)
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.last_updated).uppercase(), style = KonstanzType.Label.copy(letterSpacing = 0.06.em), color = Ink3)
            Text(
                if (realtime == null) stringResource(R.string.checking) else lastUpdatedLabel(realtime.updatedSecondsAgo),
                style = KonstanzType.RowTitle,
                color = Ink,
            )
        }
        Box(
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(White)
                .clickable(role = Role.Button, enabled = realtime != null, onClick = onRefresh)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.refresh), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink)
        }
    }
}

private fun lastUpdatedLabel(secondsAgo: Int?): String = when {
    secondsAgo == null -> Texts.get(R.string.timetable_only)
    secondsAgo < 60 -> Texts.plural(R.plurals.seconds_ago, secondsAgo, secondsAgo)
    else -> Texts.plural(R.plurals.minutes_ago, secondsAgo / 60, secondsAgo / 60)
}
