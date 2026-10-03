package com.example.konstanz.ui.components

import com.example.konstanz.R
import com.example.konstanz.data.Texts

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.Realtime
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Cancelled as CancelledColor
import com.example.konstanz.ui.theme.Delayed
import com.example.konstanz.ui.theme.DelayedTint
import com.example.konstanz.ui.theme.Detour as DetourColor
import com.example.konstanz.ui.theme.DetourTint
import com.example.konstanz.ui.theme.Figtree
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Live as LiveColor
import com.example.konstanz.ui.theme.LiveTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.Spacing
import com.example.konstanz.ui.theme.White

// ---------- Line badge ----------

/** Line badge sizes L / M / S from the design system. */
enum class BadgeSize(val minWidth: Dp, val height: Dp, val radius: Dp, val fontSize: TextUnit) {
    L(44.dp, 34.dp, 9.dp, 19.sp),
    M(34.dp, 26.dp, 7.dp, 15.sp),
    S(28.dp, 22.dp, 6.dp, 13.sp),
}

/** Red tile with the line number, e.g. "12". */
@Composable
fun LineBadge(line: String, modifier: Modifier = Modifier, size: BadgeSize = BadgeSize.M) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = size.minWidth)
            .height(size.height)
            .background(Primary, RoundedCornerShape(size.radius))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            line,
            color = White,
            style = TextStyle(
                fontFamily = Figtree, fontWeight = FontWeight.ExtraBold, fontSize = size.fontSize,
                letterSpacing = (-0.01).em, fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
        )
    }
}

// ---------- Realtime status ----------

/**
 * Realtime state of one trip. Every state pairs an icon and a word with its colour.
 * Only [OnTime]/[Live] may colour times green.
 */
@Immutable
sealed class TripStatus(
    private val text: () -> String,
    val icon: KtIcon,
    val content: Color,
    /** null = plain inline text without a pill. */
    val container: Color?,
) {
    /** The word shown, in the phone's language. */
    val label: String get() = text()

    data object OnTime : TripStatus({ Texts.get(R.string.status_on_time) }, KtIcons.Live, LiveColor, LiveTint)
    data object Live : TripStatus({ Texts.get(R.string.status_live) }, KtIcons.Live, LiveColor, LiveTint)
    data class Delay(val minutes: Int) : TripStatus({ Texts.get(R.string.status_delay, minutes) }, KtIcons.Clock, Delayed, DelayedTint)
    data object Cancelled : TripStatus({ Texts.get(R.string.status_cancelled) }, KtIcons.Cancel, White, CancelledColor)
    data object Detour : TripStatus({ Texts.get(R.string.status_detour) }, KtIcons.Detour, DetourColor, DetourTint)
    /** Realtime 2–15 min old. With [minutesAgo]: "Last known · 6 min ago". */
    data class LastKnown(val minutesAgo: Int? = null) : TripStatus(
        { if (minutesAgo == null) Texts.get(R.string.status_last_known) else Texts.get(R.string.status_last_known_ago, minutesAgo) },
        KtIcons.History, Ink2, Background,
    )
    /** Realtime over 15 min old — treated like scheduled. With [minutesOld]: "Stale · 18 min old". */
    data class Stale(val minutesOld: Int? = null) : TripStatus(
        { if (minutesOld == null) Texts.get(R.string.status_stale) else Texts.get(R.string.status_stale_old, minutesOld) },
        KtIcons.Alert, Delayed, null,
    )
    data object Scheduled : TripStatus({ Texts.get(R.string.status_scheduled) }, KtIcons.Calendar, Ink3, null)
    data object NoRealtime : TripStatus({ Texts.get(R.string.status_no_realtime) }, KtIcons.LiveOff, Ink3, null)

    /** True when the realtime time may be shown in green. */
    val isLive: Boolean get() = this == OnTime || this == Live || this is Delay
}

@Composable
fun StatusChip(status: TripStatus, modifier: Modifier = Modifier) {
    IconChip(status.label, status.icon, status.content, status.container, modifier)
}

/**
 * Small icon + word chip (13 sp bold). With a [container] it is a pill, without it plain inline text.
 * Used for trip states ([StatusChip]) and system states such as "Up to date".
 */
@Composable
fun IconChip(
    label: String,
    icon: KtIcon,
    content: Color,
    container: Color?,
    modifier: Modifier = Modifier,
) {
    val base = if (container != null) {
        modifier
            .background(container, Radius.Pill)
            .padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
    } else modifier
    Row(base, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        KonstanzIcon(icon, contentDescription = null, size = 14.dp, tint = content, strokeWidth = 2.4f)
        Text(
            label,
            color = content,
            style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

// ---------- Departure row ----------

@Immutable
data class Departure(
    /** Expected time, "14:47". */
    val time: String,
    /** Timetable time when it differs from [time], shown struck through. */
    val scheduledTime: String? = null,
    val line: String,
    val destination: String,
    val status: TripStatus = TripStatus.Scheduled,
    /** "A" → shown as "Platform A" next to the status. */
    val platform: String? = null,
    /** Which way buses leave from the platform, "Towards city centre". */
    val side: String? = null,
    /** Trip to open in Departure details. */
    val tripId: String? = null,
)

/** Realtime state from the data layer → the chip the design shows for it. */
fun Realtime.toTripStatus(): TripStatus = when (this) {
    Realtime.OnTime -> TripStatus.OnTime
    is Realtime.Delayed -> TripStatus.Delay(minutes)
    Realtime.Cancelled -> TripStatus.Cancelled
    Realtime.Detour -> TripStatus.Detour
    is Realtime.LastKnown -> TripStatus.LastKnown(minutesAgo)
    is Realtime.Stale -> TripStatus.Stale(minutesOld)
    Realtime.Scheduled -> TripStatus.Scheduled
    Realtime.NoRealtime -> TripStatus.NoRealtime
}

/** A departure from the data layer as a row: expected time, struck-through timetable time when late. */
fun StopDeparture.toRow(): Departure = Departure(
    time = if (realtime is Realtime.Delayed) expected.format() else scheduled.format(),
    scheduledTime = if (realtime is Realtime.Delayed) scheduled.format() else null,
    line = line,
    destination = destination,
    status = realtime.toTripStatus(),
    platform = platform,
    side = side,
    tripId = tripId,
)

/** One departure: time column, line badge, destination, status chip, chevron. */
@Composable
fun DepartureRow(
    departure: Departure,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    verticalPadding: Dp = 10.dp,
    /** False while live times are still being checked (artboard 33b): plain times, shimmer instead of the chip. */
    realtimeReady: Boolean = true,
) {
    val cancelled = realtimeReady && departure.status == TripStatus.Cancelled
    val timeColor = if (realtimeReady && departure.status.isLive) LiveColor else Ink
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(58.dp)) {
                Text(
                    // Before live data arrives only the timetable time is known.
                    if (!realtimeReady && departure.scheduledTime != null) departure.scheduledTime else departure.time,
                    style = KonstanzType.Time,
                    color = timeColor,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null,
                )
                if (realtimeReady && departure.scheduledTime != null) {
                    Text(
                        departure.scheduledTime,
                        style = KonstanzType.Caption.copy(fontFeatureSettings = "tnum"),
                        color = Ink3,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }
            LineBadge(departure.line)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    departure.destination,
                    style = KonstanzType.Body.copy(fontWeight = FontWeight.Bold, lineHeight = 1.25.em),
                    color = if (cancelled) Ink3 else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (realtimeReady) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(departure.status)
                        // Short rows: the direction says which side of the street to wait on; the number otherwise.
                        (departure.side ?: departure.platform?.let { stringResource(R.string.platform_x, it) })?.let {
                            Text(it, style = KonstanzType.Caption, color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Shimmer(Modifier.size(width = 84.dp, height = 12.dp))
                }
            }
            KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = Ink4, strokeWidth = 2.4f)
        }
        if (showDivider) HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

internal val SampleDepartures = listOf(
    Departure("14:40", line = "12", destination = "Wollmatingen", status = TripStatus.OnTime),
    Departure("14:47", scheduledTime = "14:44", line = "5", destination = "Allmannsdorf", status = TripStatus.Delay(3)),
    Departure("14:55", line = "8", destination = "Universität", status = TripStatus.Scheduled),
    Departure("15:02", line = "9", destination = "Egg", status = TripStatus.Cancelled, platform = "C"),
    Departure("15:06", line = "4", destination = "Petershausen Bahnhof", status = TripStatus.Detour),
)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TransitPreview() {
    KonstanzTheme {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LineBadge("12", size = BadgeSize.L)
                LineBadge("5")
                LineBadge("8")
                LineBadge("9", size = BadgeSize.S)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TripStatus.OnTime, TripStatus.Delay(4), TripStatus.Cancelled, TripStatus.Detour, TripStatus.Live,
                    TripStatus.LastKnown(), TripStatus.Stale(), TripStatus.Scheduled, TripStatus.NoRealtime,
                ).forEach { StatusChip(it) }
            }
            Column {
                SampleDepartures.forEachIndexed { i, d ->
                    DepartureRow(d, onClick = {}, showDivider = i < SampleDepartures.lastIndex)
                }
            }
        }
    }
}
