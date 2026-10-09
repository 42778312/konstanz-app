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
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.plan.WheelPicker
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
import com.example.konstanz.ui.theme.Surface
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
    var fromDay by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    val stop by produceState<Stop?>(null, stopId) { value = repo.stop(stopId) }
    val lines by produceState(emptyList<Line>(), stopId) { value = repo.linesAt(stopId) }
    val departures by produceState<List<StopDeparture>?>(null, stopId, line, fromMinutes, fromDay) {
        value = repo.departures(stopId, from = Minutes(fromMinutes), limit = 40, line = line, dayOffset = fromDay)
    }
    AllDeparturesScreen(
        stopName = stop?.name.orEmpty(),
        lines = lines,
        selectedLine = line,
        onSelectLine = { line = it },
        from = Minutes(fromMinutes),
        now = repo.now(),
        fromDay = fromDay,
        onChangeFrom = { day, time -> fromDay = day; fromMinutes = time.value },
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
    fromDay: Int,
    onChangeFrom: (day: Int, from: Minutes) -> Unit,
    departures: List<StopDeparture>?,
    onBack: () -> Unit,
    onDepartureClick: (StopDeparture) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    // Group by the hour of the timetable time: "14:00", "15:00" …
    val groups = departures.orEmpty().groupBy { it.scheduled.value / 60 }

    Column(modifier.fillMaxSize().background(Surface)) {
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
                    TimeButton(label = departuresFromLabel(fromDay, from), onClick = { pickingTime = true })
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
            selectedDay = fromDay,
            onDismiss = { pickingTime = false },
            onSelect = { day, time -> onChangeFrom(day, time); pickingTime = false },
        )
    }
}

/** "Today, from 14:30" / "Thu 15, from 08:00". */
internal fun departuresFromLabel(day: Int, from: Minutes) =
    if (day == 0) Texts.get(R.string.today_from, from.format())
    else Texts.get(R.string.day_from, com.example.konstanz.ui.plan.dayLabel(day), from.format())

/** "📅 Today, from 14:30 ⌄" */
@Composable
internal fun TimeButton(label: String, onClick: () -> Unit) {
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

/**
 * Start of the departures list: "Now", or any day of the coming week and time on the planner's
 * day · hour · minute wheel (5-minute steps).
 */
@Composable
internal fun TimeDialog(now: Minutes, selected: Minutes, selectedDay: Int, onDismiss: () -> Unit, onSelect: (day: Int, from: Minutes) -> Unit) {
    val days = (0..6).toList()
    val hours = (0..23).toList()
    val minutes = (0..55 step 5).toList()
    // The wheel starts at the chosen time, rounded up to its 5-minute step.
    val start = remember { Minutes(((selected.value + 4) / 5 * 5).coerceAtMost(23 * 60 + 55)) }
    var day by remember { mutableIntStateOf(selectedDay.coerceIn(0, days.last())) }
    var time by remember { mutableStateOf(start) }
    val isNow = selectedDay == 0 && selected == now
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = Radius.Panel,
        title = { Text(stringResource(R.string.departures_from), style = KonstanzType.Title, color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = isNow, role = Role.RadioButton, onClick = { onSelect(0, now) })
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.now_at, now.format()),
                        Modifier.weight(1f),
                        style = KonstanzType.RowTitle.copy(fontFeatureSettings = "tnum"),
                        color = Ink,
                    )
                    if (isNow) KonstanzIcon(KtIcons.Check, contentDescription = null, size = 22.dp, tint = Primary, strokeWidth = 2.6f)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxWidth().height(36.dp).background(Background, RoundedCornerShape(10.dp)))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WheelPicker(days.map { com.example.konstanz.ui.plan.dayLabel(it) }, day, { day = days[it] }, stringResource(R.string.day), width = 104.dp)
                        WheelPicker(hours.map { "%02d".format(it) }, time.value / 60, { time = Minutes.of(hours[it], time.value % 60) }, stringResource(R.string.hour), width = 56.dp)
                        WheelPicker(minutes.map { "%02d".format(it) }, (time.value % 60) / 5, { time = Minutes.of(time.value / 60, minutes[it]) }, stringResource(R.string.minute), width = 56.dp)
                    }
                }
            }
        },
        confirmButton = {
            KtButton(stringResource(R.string.show_departures), { onSelect(day, time) }, Modifier.fillMaxWidth())
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
