package com.example.konstanz.ui.plan

import com.example.konstanz.ui.components.placeDisplayName
import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.Maneuver
import com.example.konstanz.data.transit.WalkStep
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.map.markerShadow
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import com.example.konstanz.ui.theme.sheetShadow

// ---------- One walk on the map: directions like a maps app ----------

fun Maneuver.icon(): KtIcon = when (this) {
    Maneuver.Depart -> KtIcons.Walk
    Maneuver.Straight -> KtIcons.Straight
    Maneuver.SlightLeft -> KtIcons.SlightLeft
    Maneuver.SlightRight -> KtIcons.SlightRight
    Maneuver.Left -> KtIcons.TurnLeft
    Maneuver.Right -> KtIcons.TurnRight
    Maneuver.SharpLeft -> KtIcons.SharpLeft
    Maneuver.SharpRight -> KtIcons.SharpRight
    Maneuver.UTurn -> KtIcons.UTurn
    Maneuver.Stairs -> KtIcons.Stairs
    Maneuver.Cross -> KtIcons.Crossing
    Maneuver.Arrive -> KtIcons.Pin
}

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m" else String.format(java.util.Locale.ROOT, "%.1f km", meters / 1000.0)

/** How far to walk before step [i]: the previous step's length (null for the first). */
fun List<WalkStep>.distanceBefore(i: Int): Int? = if (i <= 0) null else this[i - 1].meters

/** Where the walk starts: the previous leg's end, or the journey's start. */
fun Journey.walkFrom(leg: Leg.Walk): String = when (val prev = legs.getOrNull(legs.indexOf(leg) - 1)) {
    is Leg.Ride -> prev.to
    is Leg.Walk -> prev.to
    null -> from
}

/**
 * Bottom sheet for one walk: time and distance, from → to, and the turn-by-turn list.
 * Tap a step to see it on the map ([selected] = its index, −1 = the whole walk).
 */
@Composable
fun WalkSheet(
    journey: Journey,
    leg: Leg.Walk,
    selected: Int,
    onSelect: (Int) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val steps = leg.steps
    val list = rememberLazyListState()
    LaunchedEffect(selected) { if (selected >= 0) list.animateScrollToItem(selected) }
    Column(modifier.fillMaxWidth().height(height).sheetShadow(shape).clip(shape).background(White)) {
        Box(Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp)
            .background(com.example.konstanz.ui.theme.SkeletonBar, com.example.konstanz.ui.theme.Radius.Pill))
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(48.dp).background(Location.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                KonstanzIcon(KtIcons.Walk, contentDescription = null, size = 26.dp, tint = Location)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.walk_x_min, leg.minutes),
                    Modifier.semantics { heading() },
                    style = KonstanzType.Title.copy(fontSize = 22.sp, letterSpacing = (-0.01).em),
                    color = Ink,
                )
                Text(
                    "${formatDistance(leg.meters)} · ${leg.start.format()} – ${leg.end.format()}",
                    style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = Ink2,
                )
                Text(
                    "${placeDisplayName(journey.walkFrom(leg))} → ${placeDisplayName(leg.to)}",
                    style = KonstanzType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        if (steps.isEmpty()) {
            Text(
                stringResource(R.string.no_steps),
                Modifier.padding(20.dp),
                style = KonstanzType.BodySmall, color = Ink2,
            )
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                state = list,
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(steps) { i, step ->
                    StepRow(step, steps.distanceBefore(i), selected = i == selected, last = i == steps.lastIndex) {
                        onSelect(if (selected == i) -1 else i)
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun StepRow(step: WalkStep, before: Int?, selected: Boolean, last: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Location.copy(alpha = 0.08f) else White)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.show_on_map), onClick = onClick)
            .padding(start = 20.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(if (selected) Location else Background, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            KonstanzIcon(step.maneuver.icon(), contentDescription = null, size = 22.dp, tint = if (selected) White else Ink)
        }
        Column(Modifier.weight(1f)) {
            Column(Modifier.padding(vertical = 14.dp)) {
                Text(
                    before?.let(::formatDistance) ?: stringResource(R.string.start),
                    style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                    color = Ink,
                )
                Text(step.instruction, style = KonstanzType.BodySmall.copy(fontSize = 15.sp), color = Ink2)
            }
            if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        }
    }
}

/** Dark banner at the top while one step is shown: the step large, with previous / next. */
@Composable
fun StepBanner(steps: List<WalkStep>, index: Int, onIndex: (Int) -> Unit, modifier: Modifier = Modifier) {
    val step = steps[index]
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxWidth().floatingShadow(shape).background(Ink, shape).padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KonstanzIcon(step.maneuver.icon(), contentDescription = null, size = 34.dp, tint = White)
        Column(Modifier.weight(1f)) {
            Text(
                steps.distanceBefore(index)?.let { stringResource(R.string.in_distance, formatDistance(it)) } ?: stringResource(R.string.start),
                style = KonstanzType.RowTitle.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                color = White,
            )
            Text(step.instruction, style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.SemiBold), color = White.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        BannerArrow(KtIcons.ChevronLeft, stringResource(R.string.prev_step), enabled = index > 0) { onIndex(index - 1) }
        BannerArrow(KtIcons.ChevronRight, stringResource(R.string.next_step), enabled = index < steps.lastIndex) { onIndex(index + 1) }
    }
}

@Composable
private fun BannerArrow(icon: KtIcon, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(White.copy(alpha = if (enabled) 0.14f else 0.05f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { KonstanzIcon(icon, contentDescription = null, size = 20.dp, tint = White.copy(alpha = if (enabled) 1f else 0.35f)) }
}

/** On the route line: a small ring where a step starts. */
@Composable
fun ManeuverDot(modifier: Modifier = Modifier) {
    Box(modifier.size(12.dp).background(White, CircleShape).border(3.dp, Location, CircleShape))
}

/** On the route line: the step being shown, as a blue disc with its arrow. */
@Composable
fun ManeuverMarker(maneuver: Maneuver, modifier: Modifier = Modifier) {
    Box(
        modifier.size(36.dp).markerShadow(CircleShape).background(Location, CircleShape).border(3.dp, White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { KonstanzIcon(maneuver.icon(), contentDescription = null, size = 20.dp, tint = White) }
}
