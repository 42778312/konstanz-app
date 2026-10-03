package com.example.konstanz.ui.map

import com.example.konstanz.R
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.transit.NearbyStop
import com.example.konstanz.data.transit.Place
import com.example.konstanz.ui.components.BadgeSize
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.DragSheetState
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.dragSheetContainer
import com.example.konstanz.ui.components.dragSheetHandle
import com.example.konstanz.ui.components.dragSheetPeek
import com.example.konstanz.ui.components.rememberDragSheetState
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.SkeletonBar
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.sheetShadow

data class LocationSheetActions(
    val onRouteHere: () -> Unit,
    val onFromHere: () -> Unit,
    val onToggleSave: () -> Unit,
    val onOpenStop: (NearbyStop) -> Unit,
)

/** 11 Location details sheet: kind, name, address · distance, actions, nearby stops. */
@Composable
fun LocationSheet(
    place: Place,
    nearby: List<NearbyStop>,
    saved: Boolean,
    actions: LocationSheetActions,
    modifier: Modifier = Modifier,
    /** Shared with the map screen so dragging the map also minimizes this sheet. */
    dragState: DragSheetState = rememberDragSheetState(),
) {
    Column(
        modifier
            .dragSheetContainer(dragState)
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(White)
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
            Column(Modifier.dragSheetHandle(dragState), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text((place.category ?: place.kind.label).uppercase(), style = KonstanzType.Label, color = Ink3)
                Text(
                    place.name,
                    Modifier.semantics { heading() },
                    style = KonstanzType.TitleL.copy(fontSize = 26.sp, letterSpacing = (-0.02).em),
                    color = Ink,
                )
                Text(
                    "${place.address} · ${formatKm(place.distanceKm)}",
                    style = KonstanzType.BodySmall,
                    color = Ink2,
                )
            }
        }
        Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(stringResource(R.string.route_here), actions.onRouteHere, Modifier.weight(1f), leadingIcon = KtIcons.Nav, compact = true)
            KtButton(stringResource(R.string.from_here), actions.onFromHere, Modifier.weight(1f), variant = ButtonVariant.Tonal, compact = true)
            Box(
                Modifier
                    .size(52.dp)
                    .clip(Radius.Button)
                    .background(PrimaryTint)
                    .clickable(role = Role.Button, onClickLabel = stringResource(if (saved) R.string.remove_saved else R.string.save_place), onClick = actions.onToggleSave),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.Star, contentDescription = stringResource(if (saved) R.string.remove_saved else R.string.save_place), size = 22.dp, tint = Primary, filled = saved)
            }
        }
        if (nearby.isNotEmpty()) {
            SectionLabel(stringResource(R.string.nearby_stops), Modifier.padding(top = 22.dp, bottom = 4.dp))
            nearby.forEach { NearbyStopRow(it) { actions.onOpenStop(it) } }
        }
    }
}

/** "🚌 Universität  [9][12]   🚶 3 min" */
@Composable
private fun NearbyStopRow(nearby: NearbyStop, onClick: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 20.dp, tint = Primary)
            Text(nearby.stop.name, Modifier.weight(1f), style = KonstanzType.RowTitle, color = Ink, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                nearby.stop.lines.forEach { LineBadge(it, size = BadgeSize.S) }
            }
            Row(
                Modifier.width(72.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KonstanzIcon(KtIcons.Walk, contentDescription = null, size = 16.dp, tint = Ink2)
                Text(
                    stringResource(R.string.minutes_short, nearby.walkMinutes),
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = Ink2,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

internal fun formatKm(km: Double) = String.format(java.util.Locale.ROOT, "%.1f km", km)
