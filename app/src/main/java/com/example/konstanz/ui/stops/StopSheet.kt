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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.konstanz.data.transit.MockTransitRepository
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.RealtimeUnavailableBanner
import com.example.konstanz.ui.components.SmallSpinner
import com.example.konstanz.ui.components.toRow
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
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
    val onAllDepartures: () -> Unit,
    val onRouteFromHere: () -> Unit,
)

/**
 * 12 Bus stop sheet (and 33b while live times load) on top of the map.
 * The timetable shows immediately; [realtime] null = still checking live data.
 */
@Composable
fun StopSheet(
    stop: Stop,
    walkMinutes: Int,
    departures: List<StopDeparture>,
    realtime: RealtimeInfo?,
    saved: Boolean,
    actions: StopSheetActions,
    modifier: Modifier = Modifier,
) {
    val ready = realtime != null
    Column(
        modifier
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(White)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(SkeletonBar, Radius.Pill))
        Spacer(Modifier.height(15.dp))

        StopHeader(stop, walkMinutes, saved, actions)

        if (realtime?.available == false) {
            // Artboard 32: say so, then show the timetable.
            RealtimeUnavailableBanner(realtime.updatedSecondsAgo?.let { it / 60 }, Modifier.padding(top = 14.dp))
            Text(stringResource(R.string.scheduled_departures).uppercase(), Modifier.padding(top = 14.dp), style = KonstanzType.Label, color = Ink3)
        } else Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.next_departures).uppercase(), style = KonstanzType.Label, color = Ink3, maxLines = 1)
            // The status gives way (ellipsis) before the label wraps.
            RealtimeStatus(realtime, Modifier.weight(1f, fill = false).padding(start = 12.dp))
        }

        departures.forEach { d ->
            DepartureRow(
                d.toRow(),
                onClick = { actions.onDepartureClick(d) },
                verticalPadding = 12.dp,
                realtimeReady = ready,
            )
        }

        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(stringResource(R.string.all_departures), actions.onAllDepartures, Modifier.weight(1f), variant = ButtonVariant.Neutral, compact = true)
            KtButton(stringResource(R.string.route_from_here), actions.onRouteFromHere, Modifier.weight(1f), leadingIcon = KtIcons.Nav, compact = true)
        }
    }
}

/** Red stop tile, name, "Bus stop · Platforms A–D · 4 min walk", save and close. */
@Composable
private fun StopHeader(stop: Stop, walkMinutes: Int, saved: Boolean, actions: StopSheetActions) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).background(Primary, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 24.dp, tint = White)
        }
        Column(Modifier.weight(1f)) {
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

@Preview(widthDp = 390)
@Composable
private fun StopSheetPreview() {
    val deps = kotlinx.coroutines.runBlocking { MockTransitRepository().departures("bahnhof", limit = 3) }
    KonstanzTheme {
        StopSheet(previewStop, 4, deps, RealtimeInfo(true, 30), saved = false, actions = StopSheetActions({}, {}, {}, {}, {}))
    }
}

@Preview(widthDp = 390, name = "Loading live times (33b)")
@Composable
private fun StopSheetLoadingPreview() {
    val deps = kotlinx.coroutines.runBlocking { MockTransitRepository().departures("bahnhof", limit = 3) }
    KonstanzTheme {
        StopSheet(previewStop, 4, deps, realtime = null, saved = true, actions = StopSheetActions({}, {}, {}, {}, {}))
    }
}
