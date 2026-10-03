package com.example.konstanz.ui.components

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

// From / To card used on the main map (07) and in the route planner (16).

/** From / To card with the start → destination rail and the swap button. */
@Composable
fun RouteCard(
    from: String?,
    to: String?,
    onFrom: () -> Unit,
    onTo: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Background, Radius.Card)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Blue ring = your location; ink ring = any other start (artboard 31).
        RouteRail(startIsMe = from == "My location")
        Column(Modifier.weight(1f)) {
            RouteField(stringResource(R.string.from_label), from?.let { placeDisplayName(it) } ?: stringResource(R.string.choose_start), filled = from != null, onClick = onFrom)
            HorizontalDivider(thickness = 1.dp, color = Line)
            RouteField(stringResource(R.string.to_label), to?.let { placeDisplayName(it) } ?: stringResource(R.string.where_to), filled = to != null, onClick = onTo)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Background)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.swap), onClick = onSwap),
            contentAlignment = Alignment.Center,
        ) {
            KonstanzIcon(KtIcons.Swap, contentDescription = stringResource(R.string.swap), size = 22.dp, tint = Ink)
        }
    }
}

/** Blue start ring, dotted line, ink destination diamond. */
@Composable
private fun RouteRail(startIsMe: Boolean) {
    Column(Modifier.width(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(14.dp)
                .background(White, CircleShape)
                .drawBehind { drawCircle(if (startIsMe) Location else Ink, radius = size.minDimension / 2f - 2.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx())) }
        )
        Box(
            Modifier
                .padding(vertical = 3.dp)
                .size(width = 2.dp, height = 40.dp)
                .drawBehind {
                    drawLine(
                        Ink3, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = size.width,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                    )
                }
        )
        Box(Modifier.size(12.dp).rotate(45f).background(Ink, RoundedCornerShape(2.dp)))
    }
}

@Composable
private fun RouteField(label: String, value: String, filled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClickLabel = "$label: $value", onClick = onClick),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = KonstanzType.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), color = Ink3)
        Text(value, style = KonstanzType.Body.copy(fontWeight = FontWeight.Bold), color = if (filled) Ink else Ink3, maxLines = 1)
    }
}

/** A start / destination as shown: the "My location" marker in the phone's language, other names as they are. */
@Composable
fun placeDisplayName(name: String): String = when {
    name == "My location" -> stringResource(R.string.my_location)
    // Home / Work travel under their stored names; show them in the phone's language.
    else -> com.example.konstanz.data.SavedStore.places
        .firstOrNull { it.name == name && it.kind != com.example.konstanz.data.PlaceKind.Other }?.displayName ?: name
}
