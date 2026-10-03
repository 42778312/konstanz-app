package com.example.konstanz.ui.stops

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.Line
import com.example.konstanz.data.transit.MockTransitRepository
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.DragSheetState
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.KtChip
import com.example.konstanz.ui.components.SmallSpinner
import com.example.konstanz.ui.components.dragSheetBody
import com.example.konstanz.ui.components.dragSheetContainer
import com.example.konstanz.ui.components.dragSheetHandle
import com.example.konstanz.ui.components.dragSheetPeek
import com.example.konstanz.ui.components.rememberDragSheetState
import com.example.konstanz.ui.components.toRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.SkeletonBar
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.sheetShadow

/** What the stop sheet can do. */
data class StopSheetActions(
    val onClose: () -> Unit,
    val onToggleSave: () -> Unit,
    val onDepartureClick: (StopDeparture) -> Unit,
    val onRouteFromHere: () -> Unit,
)

/**
 * 12 Bus stop sheet (and 33b while live times load) on top of the map.
 * The timetable shows immediately; [realtime] null = still checking live data.
 * Line filter + start time mirror the former "All departures" page, folded into the sheet itself.
 */
@Composable
fun StopSheet(
    stop: Stop,
    walkMinutes: Int,
    lines: List<Line>,
    selectedLine: String?,
    onSelectLine: (String?) -> Unit,
    from: Minutes,
    now: Minutes,
    onChangeFrom: (Minutes) -> Unit,
    departures: List<StopDeparture>,
    realtime: RealtimeInfo?,
    saved: Boolean,
    actions: StopSheetActions,
    modifier: Modifier = Modifier,
    /** Shared with the map screen so dragging the map also minimizes this sheet. */
    dragState: DragSheetState = rememberDragSheetState(),
) {
    val ready = realtime != null
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .dragSheetContainer(dragState)
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(Surface)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Column(Modifier.dragSheetPeek(dragState)) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 5.dp)
                    .background(SkeletonBar, Radius.Pill)
                    .dragSheetHandle(dragState),
            )
            Spacer(Modifier.height(15.dp))

            StopHeader(stop, walkMinutes, saved, actions, Modifier.dragSheetHandle(dragState))
        }

        Column(Modifier.dragSheetBody(dragState)) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 16.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KtChip(stringResource(R.string.all_lines), { onSelectLine(null) }, selected = selectedLine == null)
            lines.forEach { l -> KtChip(stringResource(R.string.line_x, l.id), { onSelectLine(l.id) }, selected = selectedLine == l.id) }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TimeButton(label = dayLabel(from, now), onClick = { pickingTime = true })
            // The status gives way (ellipsis) before it pushes the time button off-screen.
            RealtimeStatus(realtime, Modifier.weight(1f, fill = false).padding(start = 12.dp))
        }

        Column(
            Modifier
                .padding(top = 10.dp)
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (departures.isEmpty()) {
                Text(
                    stringResource(R.string.no_more_departures, from.format()),
                    Modifier.padding(vertical = 12.dp),
                    style = KonstanzType.BodySmall,
                    color = Ink3,
                )
            }
            departures.forEach { d ->
                DepartureRow(
                    d.toRow(),
                    onClick = { actions.onDepartureClick(d) },
                    verticalPadding = 12.dp,
                    realtimeReady = ready,
                )
            }
        }

        KtButton(
            stringResource(R.string.route_from_here), actions.onRouteFromHere,
            Modifier.padding(top = 16.dp).fillMaxWidth(), leadingIcon = KtIcons.Nav, compact = true,
        )
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

/** Red stop tile, name, "Bus stop · Platforms A–D · 4 min walk", save and close. */
@Composable
private fun StopHeader(stop: Stop, walkMinutes: Int, saved: Boolean, actions: StopSheetActions, dragModifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).background(Primary, RoundedCornerShape(13.dp)).then(dragModifier), contentAlignment = Alignment.Center) {
            KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 24.dp, tint = White)
        }
        Column(Modifier.weight(1f).then(dragModifier)) {
            Text(
                stop.name,
                Modifier.semantics { heading() },
                style = KonstanzType.Title.copy(letterSpacing = (-0.01).em),
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stopSubtitle(stop, walkMinutes),
                style = KonstanzType.BodySmall.copy(fontSize = 14.sp),
                color = Ink3,
            )
        }
        RoundIconButton(
            stringResource(if (saved) R.string.remove_saved else R.string.save_stop),
            onClick = actions.onToggleSave,
        ) {
            KonstanzIcon(KtIcons.Star, contentDescription = null, size = 20.dp, tint = if (saved) Primary else Ink, filled = saved)
        }
        RoundIconButton(stringResource(R.string.close), onClick = actions.onClose) {
            KonstanzIcon(KtIcons.Close, contentDescription = null, size = 20.dp, tint = Ink)
        }
    }
}

internal fun stopSubtitle(stop: Stop, walkMinutes: Int?): String = buildString {
    append(Texts.get(R.string.bus_stop))
    stop.platforms?.let { append(" · " + Texts.get(R.string.platforms_x, it)) }
    walkMinutes?.let { append(" · " + Texts.get(R.string.min_walk, it)) }
}

@Composable
internal fun RoundIconButton(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Background)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** "Live · updated 30 s ago", or a spinner while live times are being checked (33b). */
@Composable
internal fun RealtimeStatus(realtime: RealtimeInfo?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val caption = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold)
        when {
            realtime == null -> {
                SmallSpinner()
                Text(stringResource(R.string.checking_live), style = caption, color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            realtime.available -> {
                IconChip(stringResource(R.string.status_live), KtIcons.Live, Live, LiveTint)
                Text(stringResource(R.string.updated_s_ago, realtime.updatedSecondsAgo ?: 0), style = caption, color = Ink3, maxLines = 1)
            }
            else -> IconChip(stringResource(R.string.status_no_realtime), KtIcons.LiveOff, Ink3, null)
        }
    }
}

private val previewStop = Stop("bahnhof", "Konstanz Bahnhof", listOf("12", "5", "8", "9"), com.example.konstanz.data.transit.MapPoint(660f, 868f), "A–D")

private val previewRepo = MockTransitRepository()

@Preview(widthDp = 390)
@Composable
private fun StopSheetPreview() {
    val deps = kotlinx.coroutines.runBlocking { previewRepo.departures("bahnhof", limit = 8) }
    val lines = kotlinx.coroutines.runBlocking { previewRepo.linesAt("bahnhof") }
    KonstanzTheme {
        StopSheet(
            previewStop, 4, lines, null, {}, previewRepo.now(), previewRepo.now(), {},
            deps, RealtimeInfo(true, 30), saved = false, actions = StopSheetActions({}, {}, {}, {}),
        )
    }
}

@Preview(widthDp = 390, name = "Loading live times (33b)")
@Composable
private fun StopSheetLoadingPreview() {
    val deps = kotlinx.coroutines.runBlocking { previewRepo.departures("bahnhof", limit = 8) }
    val lines = kotlinx.coroutines.runBlocking { previewRepo.linesAt("bahnhof") }
    KonstanzTheme {
        StopSheet(
            previewStop, 4, lines, null, {}, previewRepo.now(), previewRepo.now(), {},
            deps, realtime = null, saved = true, actions = StopSheetActions({}, {}, {}, {}),
        )
    }
}
