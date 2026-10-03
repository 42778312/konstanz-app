package com.example.konstanz.ui.stops

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.Realtime
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.data.transit.Trip
import com.example.konstanz.data.transit.TripStop
import com.example.konstanz.ui.components.IconChip
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.KtTopBar
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.StatusChip
import com.example.konstanz.ui.components.TopBarIconButton
import com.example.konstanz.ui.components.TripStatus
import com.example.konstanz.ui.components.toTripStatus
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Figtree
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.White

data class DepartureDetailsActions(
    val onBack: () -> Unit,
    val onShowOnMap: () -> Unit,
)

/** 15 Departure details: one trip seen from the stop the user opened it at ([fromStopId]). */
@Composable
fun DepartureDetailsRoute(tripId: String, fromStopId: String?, actions: DepartureDetailsActions, at: Int? = null) {
    val repo = Transit.repository
    val trip by produceState<Trip?>(null, tripId) { value = repo.trip(tripId) }
    val from by produceState<Stop?>(null, fromStopId) { value = fromStopId?.let { repo.stop(it) } }
    val realtime by produceState<RealtimeInfo?>(null, tripId) { value = repo.realtimeInfo(fromStopId ?: "") }
    val current = trip ?: return
    DepartureDetailsScreen(current, hereStopId = from?.id ?: current.stops.first().stopId, realtime = realtime, actions = actions, hereAt = at)
}

@Composable
fun DepartureDetailsScreen(
    trip: Trip,
    hereStopId: String,
    realtime: RealtimeInfo?,
    actions: DepartureDetailsActions,
    modifier: Modifier = Modifier,
    /** Departure time at the stop: which pass, when a ring line calls there twice. */
    hereAt: Int? = null,
) {
    val context = LocalContext.current
    val hereIndex = trip.stops.indices.filter { trip.stops[it].stopId == hereStopId }
        .let { passes -> passes.firstOrNull { trip.stops[it].scheduled.value == hereAt } ?: passes.firstOrNull() } ?: 0
    val here = trip.stops[hereIndex]
    Column(modifier.fillMaxSize().background(Surface)) {
        KtTopBar(
            title = stringResource(R.string.bus_x, trip.line),
            subtitle = stringResource(R.string.direction_x, trip.destination),
            titleLeading = { LineBadge(trip.line) },
            onBack = actions.onBack,
            showDivider = true,
        ) {
            TopBarIconButton(KtIcons.Share, stringResource(R.string.share), onClick = { shareTrip(context, trip, here) })
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DepartureCard(trip, here, realtime)
            Column {
                SectionLabel(stringResource(R.string.stops))
                Column(Modifier.padding(top = 8.dp)) {
                    trip.stops.forEachIndexed { i, stop ->
                        StopLine(
                            stop = stop,
                            first = i == 0,
                            last = i == trip.stops.lastIndex,
                            isHere = i == hereIndex,
                            live = trip.realtime.isLive && realtime != null,
                        )
                    }
                }
            }
            KtButton(stringResource(R.string.show_on_map), actions.onShowOnMap, Modifier.fillMaxWidth(), leadingIcon = KtIcons.Map)
        }
    }
}

/** Grey card: big departure time (struck timetable time when late), platform, status chips. */
@Composable
private fun DepartureCard(trip: Trip, here: TripStop, realtime: RealtimeInfo?) {
    val live = trip.realtime.isLive && realtime != null
    val cancelled = trip.realtime == Realtime.Cancelled
    val late = here.expected != here.scheduled
    val big = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val caps = KonstanzType.Label.copy(letterSpacing = 0.06.em)
    Column(
        Modifier.fillMaxWidth().background(Background, RoundedCornerShape(20.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.departure).uppercase(), style = caps, color = Ink3)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        (if (live) here.expected else here.scheduled).format(),
                        style = big,
                        color = if (live) Live else Ink,
                        textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                    )
                    if (live && late) {
                        Text(
                            here.scheduled.format(),
                            Modifier.padding(bottom = 8.dp),
                            style = KonstanzType.Title.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                            color = Ink3,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
            }
            if (trip.platform != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.platform).uppercase(), style = caps, color = Ink3)
                    Text(trip.platform, style = big.copy(letterSpacing = 0.em), color = Ink)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val status = trip.realtime.toTripStatus()
            if (status is TripStatus.Delay) {
                IconChip(stringResource(R.string.delayed_x, status.minutes), status.icon, status.content, status.container)
            } else {
                StatusChip(status)
            }
            when {
                realtime == null -> Text(stringResource(R.string.checking_live_short), style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink3)
                trip.realtime.isLive -> {
                    IconChip(stringResource(R.string.status_live), KtIcons.Live, Live, LiveTint)
                    Text(stringResource(R.string.updated_s_ago, realtime.updatedSecondsAgo ?: 0), style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink3)
                }
            }
        }
    }
}

/**
 * One stop on the line rail: 16 dp ring (filled for the first stop), a 4 dp red line down to the
 * next stop, name, "You are here" and the time (struck timetable time when late).
 */
@Composable
private fun StopLine(stop: TripStop, first: Boolean, last: Boolean, isHere: Boolean, live: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(16.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = 8.dp.toPx()
                val w = 4.dp.toPx()
                if (!last) drawLine(Primary, Offset(cx, cy), Offset(cx, size.height), strokeWidth = w)
                if (!first) drawLine(Primary, Offset(cx, 0f), Offset(cx, cy), strokeWidth = w)
                drawCircle(if (first) Primary else White, radius = r, center = Offset(cx, cy))
                drawCircle(Primary, radius = r - w / 2, center = Offset(cx, cy), style = Stroke(w))
            }
        }
        Text(
            stop.name,
            Modifier.weight(1f),
            style = KonstanzType.RowTitle.copy(fontWeight = if (first || last) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = Ink,
            maxLines = 1,
        )
        if (isHere) {
            Text(
                stringResource(R.string.you_are_here),
                Modifier.background(PrimaryTint, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                style = KonstanzType.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold),
                color = OnPrimaryTint,
            )
        }
        val late = stop.expected != stop.scheduled
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (live) stop.expected else stop.scheduled).format(),
                style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                color = if (live) Live else Ink,
            )
            if (live && late) {
                Text(
                    stop.scheduled.format(),
                    style = KonstanzType.Caption.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = Ink3,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
        }
    }
}

/** Opens the system share sheet with the trip in plain words. */
private fun shareTrip(context: Context, trip: Trip, here: TripStop) {
    val text = buildString {
        append(context.getString(R.string.share_departure_text, trip.line, trip.destination, here.expected.format(), here.name))
        trip.platform?.let { append(", " + context.getString(R.string.platform_x, it)) }
        append(".")
    }
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_departure)))
}
