package com.example.konstanz.ui.onboarding

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.ui.components.Departure
import com.example.konstanz.ui.components.DepartureRow
import com.example.konstanz.ui.components.StatusPill
import com.example.konstanz.ui.components.TripStatus
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.map.DestinationMarker
import com.example.konstanz.ui.map.DistrictLabel
import com.example.konstanz.ui.map.LocationPuck
import com.example.konstanz.ui.map.LocationPulse
import com.example.konstanz.ui.map.MapIllustration
import com.example.konstanz.ui.map.StartMarker
import com.example.konstanz.ui.map.StopDotMarker
import com.example.konstanz.ui.map.StopTileMarker
import com.example.konstanz.ui.map.WaterLabel
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Live
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow

// Illustrations for onboarding artboards 02–06. They are decorative: screen readers get the
// headline and body text instead, so each one clears its semantics.
// Marker and label positions are map coordinates taken from the design.

/** 02 Welcome: Altstadt → Allmannsdorf route with its stops. */
@Composable
fun WelcomeIllustration(modifier: Modifier = Modifier) {
    MapIllustration("maps/onboarding_welcome.svg", modifier.fillMaxSize().clearAndSetSemantics { }) {
        DistrictLabel("Altstadt", Modifier.at(600f, 925f))
        DistrictLabel("Petershausen", Modifier.at(560f, 540f))
        DistrictLabel("Allmannsdorf", Modifier.at(760f, 445f))
        DistrictLabel("Paradies", Modifier.at(460f, 880f))
        DistrictLabel("Egg", Modifier.at(850f, 240f))
        DistrictLabel("Fürstenberg", Modifier.at(360f, 470f))
        WaterLabel("Bodensee", Modifier.at(1000f, 700f))
        WaterLabel("Konstanzer Trichter", Modifier.at(800f, 870f))
        listOf(
            660f to 868f,   // Bahnhof
            638f to 826f,   // Marktstätte
            565f to 772f,   // Laube
            600f to 642f,   // Benediktinerplatz
            588f to 560f,   // Zähringerplatz
            716f to 284f,   // Universität
            622f to 604f,   // Petershausen
            790f to 402f,   // Allmannsdorf
            722f to 560f,   // Seestraße
        ).forEach { (x, y) -> StopTileMarker(Modifier.at(x, y)) }
        StartMarker(Modifier.at(612f, 902f))
        DestinationMarker(Modifier.at(748f, 258f))
    }
}

/** 03 Offline maps: zoomed-in Altstadt, offline badge in the middle. */
@Composable
fun OfflineMapsIllustration(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clearAndSetSemantics { }) {
        MapIllustration("maps/onboarding_offline.svg", Modifier.fillMaxSize()) {
            DistrictLabel("Altstadt", Modifier.at(600f, 925f))
            DistrictLabel("Petershausen", Modifier.at(560f, 540f))
            DistrictLabel("Paradies", Modifier.at(460f, 880f))
            WaterLabel("Konstanzer Trichter", Modifier.at(800f, 870f))
            listOf(660f to 868f, 638f to 826f, 565f to 772f, 612f to 756f, 600f to 642f, 522f to 862f, 622f to 604f)
                .forEach { (x, y) -> StopDotMarker(Modifier.at(x, y)) }
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val tile = RoundedCornerShape(28.dp)
            Box(
                Modifier.size(88.dp).floatingShadow(tile).background(White, tile),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.WifiOff, contentDescription = null, size = 44.dp, tint = Primary, strokeWidth = 2.2f)
            }
            StatusPill(stringResource(R.string.offline_saved_data))
        }
    }
}

/** 04 Public transport: tilted bus tile and a departures card. */
@Composable
fun TransportIllustration(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        val card = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .offset(y = 24.dp)
                .fillMaxWidth()
                .floatingShadow(card)
                .background(White, card)
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 20.dp, tint = Primary, strokeWidth = 2.4f)
                Text("Konstanz Bahnhof", style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            }
            val departures = listOf(
                Departure("14:40", line = "12", destination = "Wollmatingen", status = TripStatus.OnTime),
                Departure("14:47", scheduledTime = "14:44", line = "5", destination = "Allmannsdorf", status = TripStatus.Delay(3)),
                Departure("14:55", line = "8", destination = "Universität", status = TripStatus.Scheduled),
            )
            departures.forEachIndexed { i, d -> DepartureRow(d, onClick = {}, showDivider = i < departures.lastIndex) }
        }
        val tile = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 34.dp, top = 40.dp)
                .rotate(-8f)
                .size(72.dp)
                .dropShadow(tile, Shadow(radius = 24.dp, offset = DpOffset(0.dp, 8.dp), color = Primary, alpha = 0.35f))
                .background(Primary, tile),
            contentAlignment = Alignment.Center,
        ) {
            KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 38.dp, tint = White, strokeWidth = 2.2f)
        }
    }
}

/** 05 Location: puck with a pulsing ring over the Altstadt. */
@Composable
fun LocationIllustration(modifier: Modifier = Modifier) {
    MapIllustration("maps/onboarding_location.svg", modifier.fillMaxSize().clearAndSetSemantics { }) {
        listOf(660f to 868f, 638f to 826f, 522f to 862f, 626f to 992f)
            .forEach { (x, y) -> StopTileMarker(Modifier.at(x, y)) }
        DistrictLabel("Altstadt", Modifier.at(600f, 935f))
        LocationPulse(Modifier.at(612f, 902f))
        LocationPuck(Modifier.at(612f, 902f))
    }
}

/** 06 Offline data ready: what is stored on the device. */
@Composable
fun OfflineDataIllustration(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .padding(start = 20.dp, end = 20.dp, top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KonstanzIcon(KtIcons.Database, contentDescription = null, size = 18.dp, tint = OnPrimaryTint)
            Text(stringResource(R.string.stored_on_device).uppercase(), style = KonstanzType.Label, color = OnPrimaryTint)
        }
        StoredItem(KtIcons.Map, stringResource(R.string.tab_map), stringResource(R.string.stored_map_sub))
        StoredItem(KtIcons.Calendar, stringResource(R.string.bus_schedules), stringResource(R.string.bus_schedules_sub))
        StoredItem(KtIcons.Bus, stringResource(R.string.bus_stops), stringResource(R.string.bus_stops_sub))
        StoredItem(KtIcons.Nav, stringResource(R.string.route_planning), stringResource(R.string.route_planning_sub))
        Row(
            Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KonstanzIcon(KtIcons.Info, contentDescription = null, size = 16.dp, tint = Ink2)
            Text(stringResource(R.string.updates_background), style = KonstanzType.Caption, color = Ink2)
        }
    }
}

@Composable
private fun StoredItem(icon: KtIcon, title: String, subtitle: String) {
    val card = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .dropShadow(card, Shadow(radius = 2.dp, offset = DpOffset(0.dp, 1.dp), color = Ink, alpha = 0.06f))
            .background(White, card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).background(PrimaryTint, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            KonstanzIcon(icon, contentDescription = null, size = 22.dp, tint = Primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = KonstanzType.RowTitle.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text(subtitle, style = KonstanzType.Caption, color = Ink3)
        }
        Box(Modifier.size(28.dp).background(Live, CircleShape), contentAlignment = Alignment.Center) {
            KonstanzIcon(KtIcons.Check, contentDescription = null, size = 18.dp, tint = White, strokeWidth = 3f)
        }
    }
}

@Composable
private fun IllustrationPreviewFrame(content: @Composable BoxScope.() -> Unit) {
    KonstanzTheme {
        Box(
            Modifier.size(358.dp, 400.dp).background(PrimaryTint, Radius.Hero),
            content = content,
        )
    }
}

@Preview @Composable private fun WelcomePreview() = IllustrationPreviewFrame { WelcomeIllustration() }
@Preview @Composable private fun OfflineMapsPreview() = IllustrationPreviewFrame { OfflineMapsIllustration() }
@Preview @Composable private fun TransportPreview() = IllustrationPreviewFrame { TransportIllustration() }
@Preview @Composable private fun LocationPreview() = IllustrationPreviewFrame { LocationIllustration() }
@Preview @Composable private fun OfflineDataPreview() = IllustrationPreviewFrame { OfflineDataIllustration() }
