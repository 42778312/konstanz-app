package com.example.konstanz.ui.saved

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.example.konstanz.data.transit.Transit
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.PlaceKind
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.SavedPlace
import com.example.konstanz.data.SavedStop
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.KtButton
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.components.TopBarTextAction
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Surface
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White

/** Actions of the Saved tab. */
data class SavedActions(
    val onSetPlace: (SavedPlace) -> Unit,
    val onGo: (SavedPlace) -> Unit,
    val onOpenStop: (SavedStop) -> Unit,
    val onAddPlace: () -> Unit,
)

/**
 * 28 Saved places. [onChoosePlace] opens Search to choose a place: slot "home" / "work" fills that
 * slot, "new" adds a place.
 */
@Composable
fun SavedRoute(onOpenStop: (stopId: String) -> Unit, onRouteTo: (name: String) -> Unit, onChoosePlace: (slot: String) -> Unit) {
    val repo = Transit.repository
    // Journey time from here to each saved place, from the route search (the Go buttons).
    val minutes by produceState(emptyMap<String, Int>(), SavedStore.places.toList(), repo) {
        value = SavedStore.places.filter { it.address != null }.mapNotNull { place ->
            repo.journeys("My location", place.name).minOfOrNull { it.minutes }?.let { place.id to it }
        }.toMap()
    }
    // Next departure at each saved stop, refreshed every minute.
    val next by produceState(emptyMap<String, String>(), SavedStore.stops.toList(), repo) {
        while (true) {
            value = SavedStore.stops.mapNotNull { stop ->
                repo.departures(stop.id, limit = 1).firstOrNull()?.let { stop.id to Texts.get(R.string.next_line, it.expected.format(), it.line) }
            }.toMap()
            delay(60_000)
        }
    }
    SavedScreen(
        places = SavedStore.places,
        stops = SavedStore.stops,
        actions = SavedActions(
            onSetPlace = { onChoosePlace(if (it.kind == PlaceKind.Work) "work" else "home") },
            onGo = { onRouteTo(it.name) },
            onOpenStop = { onOpenStop(it.id) },
            onAddPlace = { onChoosePlace("new") },
        ),
        onRemovePlace = { SavedStore.removePlace(it) },
        onRemoveStop = { SavedStore.removeStop(it) },
        travelMinutes = minutes,
        nextDepartures = next,
    )
}

@Composable
fun SavedScreen(
    places: List<SavedPlace>,
    stops: List<SavedStop>,
    actions: SavedActions,
    onRemovePlace: (SavedPlace) -> Unit,
    onRemoveStop: (SavedStop) -> Unit,
    modifier: Modifier = Modifier,
    /** Live journey time per saved place id; missing = still calculating or no route. */
    travelMinutes: Map<String, Int> = emptyMap(),
    /** Live "next 14:41 · line 5" per saved stop id. */
    nextDepartures: Map<String, String> = emptyMap(),
) {
    var editing by rememberSaveable { mutableStateOf(false) }

    Column(modifier.fillMaxSize().background(Surface).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.tab_saved), Modifier.semantics { heading() }, style = KonstanzType.TitleL.copy(fontSize = 30.sp), color = Ink)
            TopBarTextAction(stringResource(if (editing) R.string.done else R.string.edit), { editing = !editing }, color = OnPrimaryTint)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item { Label(stringResource(R.string.places), top = 0.dp) }
            items(places, key = { "place-" + it.id }) { place ->
                PlaceRow(
                    place = place,
                    editing = editing && SavedStore.canRemove(place),
                    onSet = { actions.onSetPlace(place) },
                    onGo = { actions.onGo(place) },
                    minutes = travelMinutes[place.id],
                    onRemove = { onRemovePlace(place) },
                )
            }
            if (stops.isNotEmpty()) {
                item { Label(stringResource(R.string.stops), top = 10.dp) }
                items(stops, key = { "stop-" + it.id }) { stop ->
                    StopRow(stop, nextDepartures[stop.id], editing, onOpen = { actions.onOpenStop(stop) }, onRemove = { onRemoveStop(stop) })
                }
            }
            item {
                KtButton(
                    stringResource(R.string.add_place), actions.onAddPlace,
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    variant = ButtonVariant.Outline, leadingIcon = KtIcons.Plus,
                )
            }
        }
    }
}

@Composable
private fun Label(text: String, top: androidx.compose.ui.unit.Dp) {
    SectionLabel(text, Modifier.padding(start = 16.dp, end = 16.dp, top = top, bottom = 10.dp))
}

private fun SavedPlace.icon(): KtIcon = when (kind) {
    PlaceKind.Home -> KtIcons.Home
    PlaceKind.Work -> KtIcons.Brief
    PlaceKind.University -> KtIcons.Cap
    PlaceKind.Stop, PlaceKind.Other -> KtIcons.Star
}

@Composable
private fun PlaceRow(place: SavedPlace, editing: Boolean, onSet: () -> Unit, onGo: () -> Unit, onRemove: () -> Unit, minutes: Int?) {
    SavedRow(
        icon = place.icon(),
        title = place.displayName,
        subtitle = place.address ?: place.displayPlaceholder.orEmpty(),
    ) {
        when {
            editing -> RemoveButton(place.displayName, onRemove)
            place.address == null -> SetButton(place.displayName, onSet)
            else -> GoButton(place.displayName, minutes, onGo)
        }
    }
}

@Composable
private fun StopRow(stop: SavedStop, next: String?, editing: Boolean, onOpen: () -> Unit, onRemove: () -> Unit) {
    SavedRow(
        icon = KtIcons.Bus,
        title = stop.name,
        subtitle = listOfNotNull(stringResource(R.string.bus_stop), next).joinToString(" · "),
        onClick = if (editing) null else onOpen,
    ) {
        if (editing) RemoveButton(stop.name, onRemove)
        else KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = Ink4, strokeWidth = 2.4f)
    }
}

/** Row layout shared by places and stops: 44 dp tinted icon tile, 17/800 title, 14/600 subtitle. */
@Composable
private fun SavedRow(
    icon: KtIcon,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).background(PrimaryTint, Radius.Button), contentAlignment = Alignment.Center) {
                KonstanzIcon(icon, contentDescription = null, size = 22.dp, tint = Primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = KonstanzType.Body.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
                Text(
                    subtitle,
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp),
                    color = Ink3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}

/** "Set" — tonal 40 dp button for Home / Work without an address. */
@Composable
private fun SetButton(placeName: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PrimaryTint)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.set_x, placeName), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.set), style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.ExtraBold), color = OnPrimaryTint)
    }
}

/** Red 64 × 52 go button: filled arrow + journey time ("Go" while it is calculated or without a route). */
@Composable
private fun GoButton(placeName: String, minutes: Int?, onClick: () -> Unit) {
    Column(
        Modifier
            .size(width = 64.dp, height = 52.dp)
            .clip(Radius.Button)
            .background(Primary)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.routes_to_x, placeName) + (minutes?.let { ", " + stringResource(R.string.minutes_short, it) } ?: ""), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        KonstanzIcon(KtIcons.Nav, contentDescription = null, size = 18.dp, tint = White, filled = true)
        Text(
            minutes?.let { stringResource(R.string.minutes_short, it) } ?: stringResource(R.string.go),
            style = KonstanzType.Caption.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
            color = White,
        )
    }
}

/** Edit mode: round remove button. */
@Composable
private fun RemoveButton(name: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Background)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.remove_x, name), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(KtIcons.Minus, contentDescription = stringResource(R.string.remove_x, name), size = 20.dp, tint = Ink2, strokeWidth = 2.6f)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SavedPreview() {
    KonstanzTheme {
        SavedScreen(
            places = SavedStore.places,
            stops = SavedStore.stops,
            actions = SavedActions({}, {}, {}, {}),
            onRemovePlace = {},
            onRemoveStop = {},
        )
    }
}
