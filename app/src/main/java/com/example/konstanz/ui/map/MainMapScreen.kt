package com.example.konstanz.ui.map

import com.example.konstanz.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.em
import com.example.konstanz.data.transit.LocationInfo
import com.example.konstanz.data.transit.NearbyStop
import com.example.konstanz.data.transit.Place
import com.example.konstanz.ui.search.SearchSession
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import com.example.konstanz.data.transit.RealtimeInfo
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.ui.stops.StopSheet
import com.example.konstanz.ui.stops.StopSheetActions
import com.example.konstanz.ui.components.ButtonVariant
import com.example.konstanz.ui.components.KtButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.konstanz.ui.plan.RouteLayer
import com.example.konstanz.data.transit.pathPoints
import com.example.konstanz.data.transit.Journey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.PlaceKind
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.SavedPlace
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.UserLocation
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.KtChip
import com.example.konstanz.data.AppStatus
import com.example.konstanz.ui.components.DataStatusPill
import com.example.konstanz.ui.components.ErrorCard
import com.example.konstanz.ui.components.ErrorFootnote
import com.example.konstanz.ui.components.OfflineBanner
import com.example.konstanz.ui.components.RouteCard
import com.example.konstanz.ui.components.StatusPill
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.KonstanzTheme
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.Location
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.SkeletonBar
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import com.example.konstanz.ui.theme.sheetShadow
import kotlinx.coroutines.launch

/** Everything the main map can open. Screens that don't exist yet are wired by the caller. */
data class MainMapActions(
    /** Opens Search (08/09), pre-filled with [query]. */
    val onOpenSearch: (query: String) -> Unit,
    val onQuickDestination: (SavedPlace) -> Unit,
    val onOpenOfflineStatus: () -> Unit,
    /** "Repair data" (34b): fresh copies of timetable and map from the app. */
    val onRepairTimetable: () -> Unit = {},
    /** Full stop page (artboard 13). */
    val onOpenStopDetails: (Stop) -> Unit,
    /** All departures of a stop (artboard 14). */
    val onOpenAllDepartures: (Stop) -> Unit = {},
    /** Departure details (artboard 15), seen from the stop it was tapped at. */
    val onOpenDeparture: (StopDeparture, Stop) -> Unit,
    val onRouteFrom: (Stop) -> Unit,
    val onRouteToPlace: (Place) -> Unit = {},
    val onRouteFromPlace: (Place) -> Unit = {},
    /** Route planner (16) with start and destination names ("" = still to choose). */
    val onOpenPlanner: (from: String, to: String) -> Unit = { _, _ -> },
    /** "From" tapped: search for a start ([to] = the destination already chosen, if any). */
    val onChooseStart: (to: String?) -> Unit = {},
    /** True while a sheet replaces the home sheet (pin or stop, artboards 10 / 12): the shell hides the bottom bar. */
    val onSelectionModeChange: (Boolean) -> Unit = {},
)

/** Where the user is (the mock keeps it fixed, artboards 05 / 07). */
private val MY_LOCATION: MapPoint get() = Transit.repository.myLocation()

/** The map centres on the user by itself only once per app start. */
private object StartFraming {
    var done = false
}

// Map labels from artboard 07 (map units).
private val districts = listOf(
    "Altstadt" to MapPoint(600f, 925f), "Petershausen" to MapPoint(560f, 540f), "Allmannsdorf" to MapPoint(760f, 445f),
    "Paradies" to MapPoint(460f, 880f), "Fürstenberg" to MapPoint(360f, 470f), "Kreuzlingen (CH)" to MapPoint(560f, 1080f),
    "Wollmatingen" to MapPoint(250f, 400f), "Egg" to MapPoint(850f, 240f),
)
private val waters = listOf("Konstanzer Trichter" to MapPoint(800f, 870f), "Bodensee" to MapPoint(1000f, 700f), "Seerhein" to MapPoint(470f, 760f))

/** 07 Main map, backed by the mock transit data. */
@Composable
fun MainMapRoute(actions: MainMapActions, command: MapCommand? = null, onCommandHandled: () -> Unit = {}) {
    val repo = Transit.repository
    val stops by produceState(emptyList<Stop>()) { value = repo.stops() }
    // The point picked on the map (artboard 10), null when not picking.
    var selection by rememberSaveable(stateSaver = MapPointSaver) { mutableStateOf<MapPoint?>(null) }
    val selectedInfo by produceState<LocationInfo?>(null, selection) {
        value = selection?.let { Transit.repository.locationAt(it) }
    }
    // The stop whose sheet is open (artboard 12), null when none.
    var selectedStopId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedStop = stops.firstOrNull { it.id == selectedStopId }

    // The place whose details are open (artboard 11), null when none.
    var selectedPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPlace by produceState<Place?>(null, selectedPlaceId) { value = selectedPlaceId?.let { repo.place(it) } }
    // "Choose on map" from Search: drop a pin in the middle once the map is laid out.
    var pickRequests by rememberSaveable { mutableIntStateOf(0) }

    // A trip shown from Departure details ("Show on map"): "<tripId>:<stopId>", null when none.
    var shownTripKey by rememberSaveable { mutableStateOf<String?>(null) }
    val shownTrip by produceState<Journey?>(null, shownTripKey) {
        value = shownTripKey?.substringBefore(":")?.let { repo.rideJourney(it, null) }
    }

    fun closeAll() { selection = null; selectedStopId = null; selectedPlaceId = null; shownTripKey = null }

    // Requests from other screens (search results).
    LaunchedEffect(command) {
        when (command) {
            is MapCommand.ShowPlace -> { closeAll(); selectedPlaceId = command.placeId }
            is MapCommand.ShowStop -> { closeAll(); selectedStopId = command.stopId }
            MapCommand.PickOnMap -> { closeAll(); pickRequests++ }
            is MapCommand.ShowTrip -> { closeAll(); shownTripKey = "${command.tripId}:${command.stopId.orEmpty()}" }
            MapCommand.OpenSaved, null -> return@LaunchedEffect
        }
        onCommandHandled()
    }

    // Location (artboard 31): unavailable without permission, or when simulated.
    val context = androidx.compose.ui.platform.LocalContext.current
    var permitted by remember { mutableStateOf(hasLocationPermission(context)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        permitted = hasLocationPermission(context)
        onPauseOrDispose { }
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permitted = hasLocationPermission(context) }
    val locationUnavailable = AppStatus.simulateLocationUnavailable || !permitted

    val settingsRepository = remember { com.example.konstanz.data.SettingsRepository(context) }
    val settings by settingsRepository.settings.collectAsState(initial = com.example.konstanz.data.UserSettings())
    val scope = rememberCoroutineScope()
    val sheetOpen = selection != null || selectedStopId != null || selectedPlaceId != null || shownTripKey != null
    LaunchedEffect(sheetOpen) { actions.onSelectionModeChange(sheetOpen) }
    BackHandler(enabled = sheetOpen) { closeAll() }

    MainMapScreen(
        stops = stops,
        quickDestinations = SavedStore.places,
        actions = actions,
        selection = selection,
        selectedInfo = selectedInfo,
        onSelect = { closeAll(); selection = it },
        selectedStop = selectedStop,
        onStopSelected = { closeAll(); selectedStopId = it?.id },
        stopSheet = { stop, sheetModifier ->
            StopSheetContent(stop, actions, onClose = { selectedStopId = null }, modifier = sheetModifier)
        },
        selectedPlace = selectedPlace,
        onClosePlace = {
            selectedPlaceId = null
            actions.onOpenSearch(SearchSession.lastQuery)
        },
        placeSheet = { place, sheetModifier ->
            PlaceSheetContent(place, actions, onOpenStop = { closeAll(); selectedStopId = it }, modifier = sheetModifier)
        },
        pickRequests = pickRequests,
        locationUnavailable = locationUnavailable,
        onEnableLocation = {
            if (AppStatus.simulateLocationUnavailable) AppStatus.simulateLocationUnavailable = false
            else permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        },
        showBusStops = settings.showBusStops,
        onShowBusStopsChange = { v -> scope.launch { settingsRepository.setShowBusStops(v) } },
        shownTrip = shownTrip,
        onCloseTrip = { shownTripKey = null },
    )
}

private fun hasLocationPermission(context: android.content.Context): Boolean =
    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

/** Location details sheet with nearby stops and save state. */
@Composable
private fun PlaceSheetContent(place: Place, actions: MainMapActions, onOpenStop: (String) -> Unit, modifier: Modifier) {
    val nearby by produceState(emptyList<NearbyStop>(), place.id) { value = Transit.repository.nearbyStops(place.point, limit = 2) }
    LocationSheet(
        place = place,
        nearby = nearby,
        saved = SavedStore.isPlaceSaved(place.name),
        actions = LocationSheetActions(
            onRouteHere = { actions.onRouteToPlace(place) },
            onFromHere = { actions.onRouteFromPlace(place) },
            onToggleSave = { SavedStore.togglePlace(place.name, place.address, place.id) },
            onOpenStop = { onOpenStop(it.stop.id) },
        ),
        modifier = modifier,
    )
}

/** Loads a stop's next departures (instantly) and its live status (takes a moment, artboard 33b). */
@Composable
private fun StopSheetContent(stop: Stop, actions: MainMapActions, onClose: () -> Unit, modifier: Modifier) {
    val repo = Transit.repository
    val departures by produceState(emptyList<StopDeparture>(), stop.id) { value = repo.departures(stop.id, limit = 3, includeCancelled = false) }
    // Realtime is an overlay: when it's unavailable the timetable is shown as scheduled (artboard 32).
    val realtimeOn = AppStatus.realtimeAvailable
    val realtime by produceState<RealtimeInfo?>(null, stop.id, realtimeOn) {
        value = null
        value = if (realtimeOn) repo.realtimeInfo(stop.id) else RealtimeInfo(available = false, updatedSecondsAgo = 47 * 60)
    }
    val saved = SavedStore.isStopSaved(stop.id)
    StopSheet(
        stop = stop,
        walkMinutes = remember(stop.id) { repo.walkMinutes(repo.myLocation(), stop.point) },
        departures = if (realtimeOn) departures else departures.map { it.copy(realtime = com.example.konstanz.data.transit.Realtime.Scheduled) },
        realtime = realtime,
        saved = saved,
        actions = StopSheetActions(
            onClose = onClose,
            onToggleSave = { SavedStore.toggleStop(stop.id, stop.name, departures.firstOrNull()) },
            onDepartureClick = { actions.onOpenDeparture(it, stop) },
            onAllDepartures = { actions.onOpenAllDepartures(stop) },
            onRouteFromHere = { actions.onRouteFrom(stop) },
        ),
        modifier = modifier,
    )
}

private val MapPointSaver = androidx.compose.runtime.saveable.Saver<MapPoint?, FloatArray>(
    save = { p -> p?.let { floatArrayOf(it.x, it.y) } ?: floatArrayOf() },
    restore = { a -> if (a.size == 2) MapPoint(a[0], a[1]) else null },
)

@Composable
fun MainMapScreen(
    stops: List<Stop>,
    quickDestinations: List<SavedPlace>,
    actions: MainMapActions,
    modifier: Modifier = Modifier,
    selection: MapPoint? = null,
    selectedInfo: LocationInfo? = null,
    onSelect: (MapPoint?) -> Unit = {},
    selectedStop: Stop? = null,
    onStopSelected: (Stop?) -> Unit = {},
    stopSheet: @Composable (Stop, Modifier) -> Unit = { _, _ -> },
    selectedPlace: Place? = null,
    onClosePlace: () -> Unit = {},
    placeSheet: @Composable (Place, Modifier) -> Unit = { _, _ -> },
    pickRequests: Int = 0,
    locationUnavailable: Boolean = false,
    onEnableLocation: () -> Unit = {},
    /** Map layers sheet: bus stops on the map (stored in settings). */
    showBusStops: Boolean = true,
    onShowBusStopsChange: (Boolean) -> Unit = {},
    /** A bus trip drawn on the map with its own sheet ("Show on map"). */
    shownTrip: Journey? = null,
    onCloseTrip: () -> Unit = {},
) {
    val camera = rememberMapCameraState()
    var layersOpen by rememberSaveable { mutableStateOf(false) }
    val picking = selection != null
    val stopOpen = selectedStop != null
    val placeOpen = selectedPlace != null
    // Stop, place or pin: the map is showing one thing, with its own sheet (bar hidden, blue locate).
    val focused = picking || stopOpen || placeOpen || shownTrip != null
    // The route being prepared in the From / To card.
    var from by rememberSaveable { mutableStateOf<String?>("My location") }
    var to by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // Height of everything at the bottom (controls + sheet), so "my location" centres in the visible map.
    var bottomPx by remember { mutableIntStateOf(0) }
    var mapWidthPx by remember { mutableIntStateOf(1) }
    var mapHeightPx by remember { mutableIntStateOf(1) }
    // Middle of the part of the map that the sheet doesn't cover.
    val visibleMiddleY = (mapHeightPx - bottomPx) / 2f

    fun visibleCenter(): MapPoint {
        val p = camera.projection(mapWidthPx.toFloat(), mapHeightPx.toFloat())
            .toMap(androidx.compose.ui.geometry.Offset(mapWidthPx / 2f, visibleMiddleY))
        return MapPoint(p.x, p.y)
    }

    fun centerOnMe() = scope.launch {
        val zoom = 1.4f
        // Sizes read now, not from the last composition: this also runs from effects.
        val middleY = (mapHeightPx - bottomPx) / 2f
        camera.animateTo(camera.centerShowing(MY_LOCATION, middleY, mapWidthPx.toFloat(), zoom), zoom)
    }

    // A trip from Departure details: fit the whole line into the map above its sheet.
    LaunchedEffect(shownTrip?.id) {
        val trip = shownTrip ?: return@LaunchedEffect
        snapshotFlow { mapWidthPx > 1 && mapHeightPx > 1 && bottomPx > 0 }.first { it }
        val points = trip.legs.mapNotNull { it.path }.flatMap(::pathPoints)
        if (points.isEmpty()) return@LaunchedEffect
        val (center, zoom) = camera.framing(points, mapWidthPx.toFloat(), mapHeightPx * 0.12f, (mapHeightPx - bottomPx).toFloat(), marginPx = mapWidthPx * 0.07f)
        camera.animateTo(center, zoom)
    }

    // Real map: open on the user once per app start, as soon as there is a fix (then leave the camera to them).
    LaunchedEffect(realMapEnabled) {
        if (!realMapEnabled || StartFraming.done) return@LaunchedEffect
        snapshotFlow { mapWidthPx > 1 && mapHeightPx > 1 && bottomPx > 0 && UserLocation.fix != null }.first { it }
        StartFraming.done = true
        centerOnMe()
    }

    // "Choose on map": drop a pin in the visible middle once the map knows its size.
    LaunchedEffect(pickRequests) {
        if (pickRequests == 0) return@LaunchedEffect
        snapshotFlow { mapWidthPx > 1 && bottomPx > 0 }.first { it }
        onSelect(visibleCenter())
    }

    // A place from search: zoom to it, pin in the upper third (artboard 11).
    LaunchedEffect(selectedPlace?.id) {
        val place = selectedPlace ?: return@LaunchedEffect
        snapshotFlow { mapWidthPx > 1 }.first { it }
        val zoom = 1.5f
        camera.animateTo(camera.centerShowing(place.point, mapWidthPx * 250f / 390f, mapWidthPx.toFloat(), zoom), zoom)
    }

    // Opening a stop zooms in and puts it in the upper part of the map, above the sheet (artboard 12).
    LaunchedEffect(selectedStop?.id) {
        val stop = selectedStop ?: return@LaunchedEffect
        val zoom = 1.875f
        camera.animateTo(camera.centerShowing(stop.point, mapWidthPx * 220f / 390f, mapWidthPx.toFloat(), zoom), zoom)
    }

    Box(modifier.fillMaxSize().background(com.example.konstanz.ui.theme.MapLand).onSizeChanged { mapWidthPx = it.width; mapHeightPx = it.height }) {
        InteractiveMap(camera, Modifier.fillMaxSize(), onLongPress = { onSelect(it) }) {
            // Read through derivedStateOf: the overlays rebuild when a threshold is crossed, not on every frame of a zoom.
            val labelsVisible by remember { derivedStateOf { camera.zoom >= 0.6f } }
            val stationNameVisible by remember { derivedStateOf { camera.zoom >= 0.8f } }
            if (labelsVisible) {
                // The real map writes its own place names.
                if (!realMapEnabled) {
                    districts.forEach { (name, p) -> DistrictLabel(name, Modifier.at(p.x, p.y)) }
                    waters.forEach { (name, p) -> WaterLabel(name, Modifier.at(p.x, p.y)) }
                }
            }
            if (!locationUnavailable) LocationPuck(
                Modifier.at(MY_LOCATION.x, MY_LOCATION.y),
                accuracy = if (stopOpen) 60.dp else 80.dp,
                showHeading = !stopOpen,
            )
            if (shownTrip != null) {
                // Line first, the stops on top of it.
                RouteLayer(listOf(shownTrip), shownTrip)
                StopMarkersLayer(stops.alongJourneys(listOf(shownTrip)), onStopClick = { onStopSelected(it) }, onClusterClick = { _, _ -> })
            } else if (showBusStops) {
                StopMarkersLayer(
                    stops = stops,
                    hiddenStopId = selectedStop?.id,
                    onStopClick = { onStopSelected(it) },
                    onClusterClick = { center, _ -> scope.launch { camera.animateTo(center, camera.zoom * 2f) } },
                )
                // The main station's name, under its marker (artboard 07); the real map names all stops itself.
                if (!realMapEnabled) stops.firstOrNull { it.id == "bahnhof" }?.point?.let { p ->
                    if (stationNameVisible) StopNameLabel("Bahnhof", Modifier.at(p.x, p.y).offset(y = 24.dp))
                }
            }
            if (selection != null) DroppedPin(Modifier.at(selection.x, selection.y))
            if (selectedPlace != null) DroppedPin(Modifier.at(selectedPlace.point.x, selectedPlace.point.y), pulse = false)
            if (selectedStop != null) {
                SelectedStopMarker(Modifier.at(selectedStop.point.x, selectedStop.point.y, anchorBottom = true))
            }
        }

        // Top: search field + menu, then the offline pill and layers.
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selectedPlace != null) {
                // Artboard 11: back to results + the place's name in the field.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SquareFloatingButton(KtIcons.ChevronLeft, stringResource(R.string.back_to_search), onClosePlace)
                    SearchField(
                        onClick = { actions.onOpenSearch(SearchSession.lastQuery.ifBlank { selectedPlace.name }) },
                        modifier = Modifier.weight(1f),
                        text = selectedPlace.name,
                    )
                }
            } else {
                SearchField(onClick = { actions.onOpenSearch("") }, modifier = Modifier.fillMaxWidth())
            }
            if (picking) {
                HintPill(stringResource(R.string.hint_long_press))
            } else if (!focused) {
                if (AppStatus.offline && !AppStatus.offlineBannerDismissed) {
                    OfflineBanner(onDismiss = { AppStatus.offlineBannerDismissed = true })
                }
                Row(verticalAlignment = Alignment.Top) {
                    DataStatusPill(onClick = actions.onOpenOfflineStatus)
                    Spacer(Modifier.weight(1f))
                    com.example.konstanz.ui.components.FloatingIconButton(KtIcons.Layers, stringResource(R.string.map_layers), { layersOpen = true })
                }
                if (locationUnavailable) {
                    LocationUnavailableCard(onSelectOnMap = { onSelect(visibleCenter()) }, onEnable = onEnableLocation)
                }
            }
        }

        // Bottom: zoom + locate above the sheet (and the map credit the real map needs).
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.Bottom) {
                if (realMapEnabled) {
                    Text("© OpenStreetMap", style = KonstanzType.Caption.copy(fontSize = 11.sp), color = Ink3)
                }
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ZoomControls(
                        onZoomIn = { scope.launch { camera.animateTo(MapPoint(camera.centerX, camera.centerY), camera.zoom * 1.6f, 250) } },
                        onZoomOut = { scope.launch { camera.animateTo(MapPoint(camera.centerX, camera.centerY), camera.zoom / 1.6f, 250) } },
                    )
                    com.example.konstanz.ui.components.FloatingRoundButton(
                        KtIcons.Nav, stringResource(R.string.center_on_me),
                        onClick = { if (locationUnavailable) onEnableLocation() else centerOnMe() },
                        tint = if (focused && !locationUnavailable) Location else Ink,
                    )
                }
            }
            val sheetModifier = Modifier.onSizeChanged { bottomPx = it.height }
            if (shownTrip != null) {
                TripSheet(shownTrip, onCloseTrip, sheetModifier)
            } else if (selectedStop != null) {
                stopSheet(selectedStop, sheetModifier)
            } else if (selectedPlace != null) {
                placeSheet(selectedPlace, sheetModifier)
            } else if (picking) {
                SelectedLocationSheet(
                    info = selectedInfo,
                    onClear = { onSelect(null) },
                    onSetStart = {
                        val name = selectedInfo?.name
                        from = name; onSelect(null)
                        if (name != null) actions.onOpenPlanner(name, to.orEmpty())
                    },
                    onSetDestination = {
                        val name = selectedInfo?.name
                        to = name; onSelect(null)
                        if (name != null) actions.onOpenPlanner(from ?: "My location", name)
                    },
                    modifier = sheetModifier,
                )
            } else if (layersOpen) {
                LayersSheet(
                    showBusStops, onShowBusStopsChange,
                    onClose = { layersOpen = false }, modifier = sheetModifier,
                )
            } else if (AppStatus.timetableDamaged && !AppStatus.damagedSheetDismissed) {
                RepairSheet(
                    onRepair = actions.onRepairTimetable,
                    onMapOnly = { AppStatus.damagedSheetDismissed = true },
                    modifier = sheetModifier,
                )
            } else {
                HomeSheet(
                    from = if (locationUnavailable && from == "My location") null else from,
                    to = to,
                    quickDestinations = quickDestinations,
                    actions = actions,
                    onFrom = { actions.onChooseStart(to) },
                    onSwap = { val f = from; from = to; to = f },
                    modifier = sheetModifier,
                )
            }
        }
    }
}

/** "Where do you want to go?" — 56 dp floating field that opens Search. */
@Composable
private fun SearchField(onClick: () -> Unit, modifier: Modifier = Modifier, text: String? = null) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .height(56.dp)
            .floatingShadow(shape)
            .clip(shape)
            .background(White)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.search), onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KonstanzIcon(KtIcons.Search, contentDescription = null, size = 22.dp, tint = com.example.konstanz.ui.theme.Primary, strokeWidth = 2.6f)
        Text(
            text ?: stringResource(R.string.where_go),
            style = if (text != null) KonstanzType.Body.copy(fontWeight = FontWeight.Bold) else KonstanzType.Body,
            color = if (text != null) Ink else Ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 56 dp square floating icon button (artboard 11 "Back to search"). */
@Composable
private fun SquareFloatingButton(icon: KtIcon, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(56.dp)
            .floatingShadow(shape)
            .clip(shape)
            .background(White)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(icon, contentDescription = label, size = 22.dp, tint = Ink)
    }
}

/** 56 dp square menu button (opens Settings). */
/** Joined +/− buttons, 48 dp each with a hairline between. */
@Composable
private fun ZoomControls(onZoomIn: () -> Unit, onZoomOut: () -> Unit) {
    Column(
        Modifier
            .width(48.dp)
            .floatingShadow(Radius.Button)
            .clip(Radius.Button)
            .background(White),
    ) {
        ZoomButton(KtIcons.Plus, stringResource(R.string.zoom_in), onZoomIn)
        HorizontalDivider(Modifier.padding(horizontal = 10.dp), thickness = 1.dp, color = Line)
        ZoomButton(KtIcons.Minus, stringResource(R.string.zoom_out), onZoomOut)
    }
}

@Composable
private fun ZoomButton(icon: KtIcon, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        KonstanzIcon(icon, contentDescription = label, size = 22.dp, tint = Ink)
    }
}

/** Collapsed home sheet: grabber, From/To card, quick destinations. The bottom bar sits right below. */
@Composable
private fun HomeSheet(
    from: String?,
    to: String?,
    quickDestinations: List<SavedPlace>,
    actions: MainMapActions,
    onFrom: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(White)
            .padding(top = 8.dp, bottom = 10.dp),
    ) {
        Grabber()
        Spacer(Modifier.height(15.dp))
        RouteCard(
            from = from,
            to = to,
            // Both open search; a start can also be dropped on the map with a long-press.
            onFrom = onFrom,
            onTo = { actions.onOpenSearch("") },
            onSwap = onSwap,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickDestinations.forEach { place ->
                KtChip(place.displayName, onClick = { actions.onQuickDestination(place) }, icon = place.chipIcon())
            }
        }
    }
}

@Composable
private fun ColumnScope.Grabber() {
    Box(Modifier.align(Alignment.CenterHorizontally).size(width = 36.dp, height = 5.dp).background(SkeletonBar, Radius.Pill))
}

/** Artboard 31: floating card when there is no location. */
@Composable
private fun LocationUnavailableCard(onSelectOnMap: () -> Unit, onEnable: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.padding(top = 40.dp).fillMaxWidth().floatingShadow(shape).background(White, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(44.dp).background(Background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                KonstanzIcon(KtIcons.LiveOff, contentDescription = null, size = 22.dp, tint = Ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.location_unavailable), style = KonstanzType.Body.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold), color = Ink)
                Text(
                    stringResource(R.string.location_unavailable_body),
                    style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Normal),
                    color = com.example.konstanz.ui.theme.Ink2,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(stringResource(R.string.select_on_map), onSelectOnMap, Modifier.weight(1f).height(48.dp), leadingIcon = KtIcons.Pin, compact = true)
            KtButton(stringResource(R.string.enable_location), onEnable, Modifier.weight(1f).height(48.dp), variant = ButtonVariant.Neutral, compact = true)
        }
    }
}

/** Artboard 34b: the timetable is damaged. */
/** "Bus 12 → Wollmatingen" over a trip shown on the map, with where it runs and a close button. */
@Composable
private fun TripSheet(trip: Journey, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val ride = trip.rides.firstOrNull() ?: return
    Column(
        modifier.fillMaxWidth().sheetShadow(Radius.Sheet).clip(Radius.Sheet).background(White).navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Grabber()
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            com.example.konstanz.ui.components.LineBadge(ride.line)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.direction_x, ride.direction), Modifier.semantics { heading() }, style = KonstanzType.Title, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${ride.from} ${ride.start.format()} → ${ride.to} ${ride.end.format()} · " + pluralStringResource(R.plurals.n_stops, ride.stops.size, ride.stops.size),
                    style = KonstanzType.BodySmall, color = Ink3, maxLines = 2,
                )
            }
            com.example.konstanz.ui.components.FloatingIconButton(KtIcons.Close, stringResource(R.string.close_trip), onClose)
        }
    }
}

/** Map layers: what the map shows besides streets (bus stops and their names). */
@Composable
private fun LayersSheet(
    showBusStops: Boolean,
    onShowBusStopsChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().sheetShadow(Radius.Sheet).clip(Radius.Sheet).background(White).navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Grabber()
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.map_layers), Modifier.weight(1f).semantics { heading() }, style = KonstanzType.Title, color = Ink)
            com.example.konstanz.ui.components.FloatingIconButton(KtIcons.Close, stringResource(R.string.close_map_layers), onClose)
        }
        com.example.konstanz.ui.components.SwitchRow(
            stringResource(R.string.bus_stops), showBusStops, onShowBusStopsChange,
            subtitle = stringResource(R.string.bus_stops_layer_sub), icon = KtIcons.Bus,
            iconColor = com.example.konstanz.ui.theme.Primary, showDivider = false,
        )
    }
}

@Composable
private fun RepairSheet(onRepair: () -> Unit, onMapOnly: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().sheetShadow(Radius.Sheet).clip(Radius.Sheet).background(White)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Grabber()
        Spacer(Modifier.height(15.dp))
        ErrorCard(
            icon = KtIcons.Database,
            title = stringResource(R.string.data_needs_repair),
            message = stringResource(R.string.repair_body),
            primary = stringResource(R.string.repair_now),
            onPrimary = onRepair,
            primaryIcon = KtIcons.Refresh,
            secondary = stringResource(R.string.map_only),
            onSecondary = onMapOnly,
        )
        ErrorFootnote(stringResource(R.string.repair_footnote))
    }
}

/** Dark hint pill shown while picking a location. */
@Composable
private fun HintPill(text: String) {
    Row(
        Modifier.height(40.dp).background(Ink, Radius.Pill).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KonstanzIcon(KtIcons.Pin, contentDescription = null, size = 18.dp, tint = White)
        Text(text, style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = White)
    }
}

/** Artboard 10 sheet: what was picked, its nearest stop, set as start / destination. */
@Composable
private fun SelectedLocationSheet(
    info: LocationInfo?,
    onClear: () -> Unit,
    onSetStart: () -> Unit,
    onSetDestination: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .sheetShadow(Radius.Sheet)
            .clip(Radius.Sheet)
            .background(White)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
    ) {
        Grabber()
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.selected_location).uppercase(), style = KonstanzType.Label, color = Ink3)
                Text(info?.name ?: " ", style = KonstanzType.Title.copy(letterSpacing = (-0.01).em), color = Ink)
                Text(
                    info?.let { "${formatCoordinates(it.latitude, it.longitude)} · ${it.region}" } ?: " ",
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontFeatureSettings = "tnum"),
                    color = Ink3,
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.clear_selection), onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.Close, contentDescription = stringResource(R.string.clear_selection), size = 20.dp, tint = Ink)
            }
        }
        info?.nearestStop?.let { nearest ->
            Row(
                Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .background(Background, Radius.Button)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KonstanzIcon(KtIcons.Bus, contentDescription = null, size = 20.dp, tint = com.example.konstanz.ui.theme.Primary)
                Text(
                    stringResource(R.string.nearest_stop, nearest.stop.name),
                    Modifier.weight(1f),
                    style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink,
                )
                KonstanzIcon(KtIcons.Walk, contentDescription = null, size = 16.dp, tint = com.example.konstanz.ui.theme.Ink2)
                Text(
                    stringResource(R.string.minutes_short, nearest.walkMinutes),
                    style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = com.example.konstanz.ui.theme.Ink2,
                )
            }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KtButton(stringResource(R.string.set_as_start), onSetStart, Modifier.weight(1f), variant = ButtonVariant.Tonal, enabled = info != null, compact = true)
            KtButton(stringResource(R.string.set_as_destination), onSetDestination, Modifier.weight(1f), enabled = info != null, compact = true)
        }
    }
}

/** "47.6619° N, 9.1661° E". */
private fun formatCoordinates(lat: Double, lon: Double): String =
    String.format(java.util.Locale.ROOT, "%.4f° %s, %.4f° %s", kotlin.math.abs(lat), if (lat >= 0) "N" else "S", kotlin.math.abs(lon), if (lon >= 0) "E" else "W")

private fun SavedPlace.chipIcon(): KtIcon = when (kind) {
    PlaceKind.Home -> KtIcons.Home
    PlaceKind.Work -> KtIcons.Brief
    PlaceKind.University -> KtIcons.Cap
    PlaceKind.Stop, PlaceKind.Other -> KtIcons.Star
}

@Preview(widthDp = 390, heightDp = 788)
@Composable
private fun MainMapPreview() {
    KonstanzTheme {
        MainMapScreen(
            stops = emptyList(),
            quickDestinations = SavedStore.places,
            actions = MainMapActions(
                onOpenSearch = {}, onQuickDestination = {}, onOpenOfflineStatus = {},
                onOpenStopDetails = {}, onOpenDeparture = { _, _ -> }, onRouteFrom = {},
            ),
        )
    }
}
