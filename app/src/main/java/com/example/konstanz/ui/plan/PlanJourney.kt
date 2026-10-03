package com.example.konstanz.ui.plan

import com.example.konstanz.ui.components.placeDisplayName
import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.platformLabel
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.StatusChip
import com.example.konstanz.ui.components.toTripStatus
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import com.example.konstanz.ui.theme.outlined
import com.example.konstanz.ui.theme.sheetShadow

// ---------- 19 Route map ----------

/** "··· Walk   ━ Bus" key on the map. */
@Composable
fun RouteLegend(modifier: Modifier = Modifier) {
    Row(
        modifier.floatingShadow(RoundedCornerShape(12.dp)).background(White, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(22.dp, 6.dp)) {
                drawLine(Ink3, Offset(3.dp.toPx(), size.height / 2), Offset(size.width - 3.dp.toPx(), size.height / 2), 4.dp.toPx(), StrokeCap.Round,
                    PathEffect.dashPathEffect(floatArrayOf(0f, 7.dp.toPx())))
            }
            Text(stringResource(R.string.walk), style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(22.dp, 6.dp)) {
                drawLine(Primary, Offset(2.dp.toPx(), size.height / 2), Offset(size.width - 2.dp.toPx(), size.height / 2), 5.dp.toPx(), StrokeCap.Round)
            }
            Text(stringResource(R.string.bus), style = KonstanzType.Caption.copy(fontWeight = FontWeight.Bold), color = Ink)
        }
    }
}

/** Floating card at the bottom of the route map; swipe sideways to see the other options. */
@Composable
fun RouteMapCards(
    journeys: List<Journey>,
    pager: PagerState,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(pager, modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), pageSpacing = 8.dp) { page ->
        val j = journeys[page]
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().floatingShadow(shape).clip(shape).background(White)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.show_route_details), onClick = onDetails).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val big = KonstanzType.Time.copy(fontSize = 22.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${j.start.format()} – ${j.end.format()}", style = big, color = Ink)
                Text(stringResource(R.string.minutes_short, j.minutes), style = big, color = Ink)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                LegsStrip(j)
                j.firstRide?.let { StatusChip(it.realtime.toTripStatus()) }
            }
            KtButton(stringResource(R.string.route_details), onDetails, Modifier.fillMaxWidth(), variant = ButtonVariant.Neutral, leadingIcon = KtIcons.Document, compact = true)
        }
    }
}

// ---------- 20 Your journey ----------

/** A point of the timeline: start, a stop where the kind of travel changes, or the destination. */
private data class Node(val time: Minutes, val title: String, val subtitle: String, val kind: NodeKind, val below: Leg?)
private enum class NodeKind { Start, Stop, End }

private fun Journey.nodes(): List<Node> {
    val out = mutableListOf(Node(start, from, Texts.get(R.string.start), NodeKind.Start, legs.firstOrNull()))
    legs.zipWithNext().forEach { (a, b) ->
        val place = when (a) { is Leg.Walk -> a.to; is Leg.Ride -> a.to }
        val subtitle = when {
            b is Leg.Ride && a is Leg.Ride -> Texts.get(R.string.change_here)
            b is Leg.Ride -> platformLabel(b.platform, b.side) ?: Texts.get(R.string.bus_stop)
            else -> Texts.get(R.string.get_off_here)
        }
        out += Node(b.start, place, subtitle, NodeKind.Stop, b)
    }
    out += Node(end, to, Texts.get(R.string.destination), NodeKind.End, null)
    return out
}

@Composable
fun DetailsSheet(
    journey: Journey,
    realtimeAgo: Int?,
    onShare: () -> Unit,
    onOpenRide: (Leg.Ride) -> Unit,
    onShowWalk: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().sheetShadow(Radius.Sheet).clip(Radius.Sheet).background(White)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(com.example.konstanz.ui.theme.SkeletonBar, Radius.Pill))
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.your_journey), Modifier.semantics { heading() }, style = KonstanzType.Title.copy(fontSize = 24.sp, letterSpacing = (-0.01).em), color = Ink)
                    Text(
                        "${journey.start.format()} – ${journey.end.format()} · ${stringResource(R.string.minutes_short, journey.minutes)} · ${journey.summary().substringBefore(" ·")}",
                        style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = Ink2,
                    )
                }
                CircleButton(KtIcons.Share, stringResource(R.string.share_route), onShare, size = 44.dp)
            }
            Column(Modifier.padding(top = 20.dp)) {
                journey.nodes().forEach { node ->
                    TimelineRow(node) {
                        when (val leg = node.below) {
                            is Leg.Walk -> WalkBox(leg) { onShowWalk(journey.legs.indexOf(leg)) }
                            is Leg.Ride -> RideBlock(leg, realtimeAgo) { onOpenRide(leg) }
                            null -> Unit
                        }
                    }
                }
            }
            ArrivalSummary(journey)
        }
    }
}

/** Closing card: when you arrive, how long it takes, how much of it is walking. */
@Composable
private fun ArrivalSummary(journey: Journey) {
    val walkMin = journey.legs.filterIsInstance<Leg.Walk>().sumOf { it.minutes }
    val rides = journey.legs.count { it is Leg.Ride }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().clip(shape).background(Background).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SummaryStat(stringResource(R.string.stat_arrive), journey.end.format())
        SummaryStat(stringResource(R.string.stat_travel), stringResource(R.string.minutes_short, journey.minutes))
        SummaryStat(stringResource(R.string.stat_walking), stringResource(R.string.minutes_short, walkMin))
        if (rides > 0) SummaryStat(pluralStringResource(R.plurals.buses, rides), "$rides")
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"), color = Ink)
        Text(label, style = KonstanzType.Caption, color = Ink3)
    }
}

/** Time · rail (marker + segment to the next node) · content. */
@Composable
private fun TimelineRow(node: Node, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            node.time.format(),
            Modifier.width(50.dp).padding(top = 1.dp),
            style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
            color = Ink,
        )
        Box(Modifier.width(24.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val top = 10.dp.toPx()
                when (node.below) {
                    is Leg.Ride -> drawLine(Primary, Offset(cx, top), Offset(cx, size.height + 2.dp.toPx()), 6.dp.toPx(), StrokeCap.Round)
                    is Leg.Walk -> drawLine(
                        Ink3, Offset(cx, 24.dp.toPx()), Offset(cx, size.height - 4.dp.toPx()), 4.dp.toPx(), StrokeCap.Round,
                        PathEffect.dashPathEffect(floatArrayOf(0f, 9.dp.toPx())),
                    )
                    null -> Unit
                }
                val r = 10.dp.toPx()
                when (node.kind) {
                    NodeKind.Start -> {
                        drawCircle(White, r, Offset(cx, top))
                        drawCircle(Ink, r - 2.5.dp.toPx(), Offset(cx, top), style = Stroke(5.dp.toPx()))
                    }
                    NodeKind.Stop -> {
                        drawCircle(White, r, Offset(cx, top))
                        drawCircle(Primary, r - 2.dp.toPx(), Offset(cx, top), style = Stroke(4.dp.toPx()))
                    }
                    NodeKind.End -> Unit
                }
            }
            if (node.kind == NodeKind.End) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 2.dp).size(16.dp).rotate(45f).background(Ink, RoundedCornerShape(2.dp)))
            }
        }
        Column(Modifier.weight(1f).padding(bottom = 16.dp)) {
            Text(placeDisplayName(node.title), style = KonstanzType.Body.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(node.subtitle, style = KonstanzType.BodySmall.copy(fontSize = 14.sp), color = Ink3)
            content()
        }
    }
}

@Composable
private fun WalkBox(leg: Leg.Walk, onShowOnMap: () -> Unit) {
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Background)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.show_walk_map), onClick = onShowOnMap)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KonstanzIcon(KtIcons.Walk, contentDescription = null, size = 18.dp, tint = Ink)
        Text(stringResource(R.string.walk_min_m, leg.minutes, leg.meters), Modifier.weight(1f), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink)
        KonstanzIcon(KtIcons.Map, contentDescription = null, size = 16.dp, tint = OnPrimaryTint)
        Text(stringResource(R.string.tab_map), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = OnPrimaryTint)
    }
}

/** Bus card (line, direction, status) + the collapsible list of stops passed. */
@Composable
private fun RideBlock(ride: Leg.Ride, realtimeAgo: Int?, onOpen: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.padding(top = 12.dp).fillMaxWidth().outlined(shape).clip(shape).clickable(role = Role.Button, onClick = onOpen).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineBadge(ride.line)
            Text(stringResource(R.string.direction_x, ride.direction), style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusChip(ride.realtime.toTripStatus())
            if (ride.realtime.isLive && realtimeAgo != null) {
                Text(stringResource(R.string.updated_s_ago, realtimeAgo), style = KonstanzType.Caption, color = Ink3)
            }
        }
    }
    val passed = ride.stops.dropLast(1)
    if (passed.isNotEmpty()) {
        Row(
            Modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClickLabel = stringResource(if (expanded) R.string.hide_stops else R.string.show_stops)) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(pluralStringResource(R.plurals.n_stops, passed.size, passed.size) + " · " + stringResource(R.string.minutes_short, ride.minutes), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ink2)
            KonstanzIcon(KtIcons.ChevronDown, contentDescription = null, size = 18.dp, tint = Ink2, modifier = Modifier.rotate(if (expanded) 180f else 0f))
        }
        if (expanded) {
            passed.forEach { stop ->
                Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Small ring drawn on the rail, 26 dp to the left of this column.
                    Box(
                        Modifier
                            .layout { m, c ->
                                val p = m.measure(c)
                                layout(0, p.height) { p.place(-26.dp.roundToPx(), 0) }
                            }
                            .size(8.dp)
                            .background(White, androidx.compose.foundation.shape.CircleShape)
                    ) {
                        Canvas(Modifier.fillMaxSize()) { drawCircle(Primary, size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx())) }
                    }
                    Text(stop.name, Modifier.weight(1f), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.SemiBold), color = Ink2)
                    Text(stop.expected.format(), style = KonstanzType.BodySmall.copy(fontFeatureSettings = "tnum"), color = Ink3)
                }
            }
        }
    }
}
