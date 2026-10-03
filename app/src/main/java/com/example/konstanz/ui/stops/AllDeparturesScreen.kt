package com.example.konstanz.ui.stops

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.konstanz.data.transit.Line
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.KtChip
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.StatusChip
import com.example.konstanz.ui.components.TripStatus
import com.example.konstanz.ui.components.toRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

/** 14 All departures of a stop, optionally starting filtered to one [initialLine]. */
@Composable
fun AllDeparturesRoute(
    stopId: String,
    initialLine: String?,
    onBack: () -> Unit,
    onDepartureClick: (StopDeparture) -> Unit,
) {
    val repo = Transit.repository
    var line by rememberSaveable { mutableStateOf(initialLine) }
    var fromMinutes by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(repo.now().value) }
    val stop by produceState<Stop?>(null, stopId) { value = repo.stop(stopId) }
    val lines by produceState(emptyList<Line>(), stopId) { value = repo.linesAt(stopId) }
    val departures by produceState<List<StopDeparture>?>(null, stopId, line, fromMinutes) {
        value = repo.departures(stopId, from = Minutes(fromMinutes), limit = 40, line = line)
    }
    AllDeparturesScreen(
        stopName = stop?.name.orEmpty(),
        lines = lines,
        selectedLine = line,
        onSelectLine = { line = it },
        from = Minutes(fromMinutes),
        now = repo.now(),
        onChangeFrom = { fromMinutes = it.value },
        departures = departures,
        onBack = onBack,
        onDepartureClick = onDepartureClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllDeparturesScreen(
    stopName: String,
    lines: List<Line>,
    selectedLine: String?,
    onSelectLine: (String?) -> Unit,
    from: Minutes,
    now: Minutes,
    onChangeFrom: (Minutes) -> Unit,
    departures: List<StopDeparture>?,
    onBack: () -> Unit,
    onDepartureClick: (StopDeparture) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    // Group by the hour of the timetable time: "14:00", "15:00" …
    val groups = departures.orEmpty().groupBy { it.scheduled.value / 60 }

    Column(modifier.fillMaxSize().background(White)) {
        KtTopBar(stringResource(R.string.all_departures), onBack = onBack, subtitle = stopName, showDivider = true)
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            item {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KtChip(stringResource(R.string.all_lines), { onSelectLine(null) }, selected = selectedLine == null)
                    lines.forEach { l -> KtChip(stringResource(R.string.line_x, l.id), { onSelectLine(l.id) }, selected = selectedLine == l.id) }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeButton(label = dayLabel(from, now), onClick = { pickingTime = true })
                    Spacer(Modifier.weight(1f))
                    IconChip(stringResource(R.string.status_live), KtIcons.Live, Live, LiveTint)
                }
            }
            if (departures != null && departures.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_more_departures, from.format()),
                        Modifier.padding(16.dp),
                        style = KonstanzType.BodySmall,
                        color = Ink3,
                    )
                }
            }
            groups.forEach { (hour, list) ->
                item(key = "h$hour") {
                    Text(
                        Minutes.of(hour, 0).format(),
                        Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp).semantics { heading() },
                        style = KonstanzType.Label,
                        color = Ink3,
                    )
                }
                items(list, key = { it.tripId }) { d ->
                    DepartureRow(
                        d.toRow(),
                        onClick = { onDepartureClick(d) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalPadding = 12.dp,
                    )
                }
            }
            item { Legend(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp)) }
        }
    }

    if (pickingTime) {
        TimeDialog(
            now = now,
            selected = from,
            onDismiss = { pickingTime = false },
            onSelect = { onChangeFrom(it); pickingTime = false },
        )
    }
}

private fun dayLabel(from: Minutes, now: Minutes) =
    Texts.get(R.string.today_from, from.format())

/** "📅 Today, from 14:30 ⌄" */
@Composable
private fun TimeButton(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Background)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.change_time_short), onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KonstanzIcon(KtIcons.Calendar, contentDescription = null, size = 18.dp, tint = Ink)
        Text(label, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink)
        KonstanzIcon(KtIcons.ChevronDown, contentDescription = null, size = 18.dp, tint = Ink)
    }
}

/** Start time for the list: now or the next full hours of today. */
@Composable
private fun TimeDialog(now: Minutes, selected: Minutes, onDismiss: () -> Unit, onSelect: (Minutes) -> Unit) {
    val nextHour = (now.value / 60 + 1) * 60
    val options = listOf(now) + (0 until 4).map { Minutes(nextHour + it * 60) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = White,
        shape = Radius.Panel,
        title = { Text(stringResource(R.string.departures_from), style = KonstanzType.Title, color = Ink) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (option == now) stringResource(R.string.now_at, option.format()) else option.format(),
                            Modifier.weight(1f),
                            style = KonstanzType.RowTitle.copy(fontFeatureSettings = "tnum"),
                            color = Ink,
                        )
                        if (option == selected) KonstanzIcon(KtIcons.Check, contentDescription = null, size = 22.dp, tint = Primary, strokeWidth = 2.6f)
                    }
                }
            }
        },
    )
}

/** Key to every realtime state, two columns on a grey card (bottom of artboard 14). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val states = listOf(
        TripStatus.Live, TripStatus.LastKnown(), TripStatus.Scheduled, TripStatus.Stale(),
        TripStatus.NoRealtime, TripStatus.Delay(0), TripStatus.Cancelled, TripStatus.Detour,
    )
    Column(modifier.fillMaxWidth().background(Background, RoundedCornerShape(16.dp)).padding(14.dp)) {
        states.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                pair.forEach { status ->
                    Row(Modifier.weight(1f)) {
                        if (status is TripStatus.Delay) {
                            IconChip(stringResource(R.string.delayed), status.icon, status.content, status.container)
                        } else {
                            StatusChip(status)
                        }
                    }
                }
            }
        }
    }
}
