package com.example.konstanz.ui.plan

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.konstanz.data.RoutePreference
import com.example.konstanz.data.SettingsRepository
import com.example.konstanz.data.transit.Journey
import com.example.konstanz.data.transit.Leg
import com.example.konstanz.data.transit.Maneuver
import com.example.konstanz.ui.theme.windowHeight
import com.example.konstanz.data.transit.MapPoint
import com.example.konstanz.data.transit.pathPoints
import com.example.konstanz.data.transit.Minutes
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.StatusPill
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.map.DestinationMarker
import com.example.konstanz.ui.map.InteractiveMap
import com.example.konstanz.ui.map.LocationPuck
import com.example.konstanz.ui.map.StartMarker
import com.example.konstanz.ui.map.StopMarkersLayer
import com.example.konstanz.ui.map.alongJourneys
import com.example.konstanz.ui.map.realMapEnabled
import com.example.konstanz.ui.map.rememberMapCameraState
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.MapLand
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.floatingShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The planner's screens (artboards 16, 33, 18, 19, 20, 30). */
enum class PlanStep { Input, Loading, Results, RouteMap, Details, Walk, NoRoute }

data class PlannerActions(
    val onClose: () -> Unit,
    /** Open Search to choose the start / destination; the result comes back as [PlannerRoute]'s `pick`. */
    val onPickFrom: () -> Unit,
    val onPickTo: () -> Unit,
    val onOpenRide: (Leg.Ride) -> Unit,
)

private const val MY_LOCATION = "My location"

/**
 * Route planning. [pick] is a choice coming back from Search, "from:<name>" or "to:<name>".
 */
@Composable
fun PlannerRoute(
    initialFrom: String,
    initialTo: String,
    pick: String?,
    onPickHandled: () -> Unit,
    actions: PlannerActions,
) {
    val context = LocalContext.current
    val repo = Transit.repository
    val scope = rememberCoroutineScope()

    var from by rememberSaveable { mutableStateOf<String?>(initialFrom.ifBlank { MY_LOCATION }) }
    var to by rememberSaveable { mutableStateOf<String?>(initialTo.ifBlank { null }) }
    var step by rememberSaveable { mutableStateOf(PlanStep.Input) }
    var prefsOpen by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable(stateSaver = OptionsSaver) { mutableStateOf(PlanOptions(time = repo.now())) }
    var journeys by remember { mutableStateOf(emptyList<Journey>()) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    /** Index of the walk leg shown on the map ([PlanStep.Walk]). */
    var walkLeg by rememberSaveable { mutableIntStateOf(-1) }
    var loadingStep by remember { mutableIntStateOf(0) }

    // Defaults from Settings → Routing.
    LaunchedEffect(Unit) {
        val s = SettingsRepository(context).settings.first()
        if (options.preference == RoutePreference.Fastest) options = options.copy(preference = s.routePreference)
    }

    fun search() {
        val f = from ?: return
        val t = to ?: return
        step = PlanStep.Loading
        loadingStep = 0
        scope.launch {
            val started = System.currentTimeMillis()
            val result = repo.journeys(
                f, t, options.preference,
                departAt = if (options.timeMode == TimeMode.Now) null else options.time,
                arriveBy = options.timeMode == TimeMode.ArriveBy,
                dayOffset = if (options.timeMode == TimeMode.Now) 0 else options.dayOffset,
            )
            // The search takes well under a second: keep the loading state (artboard 33) just long enough not to flicker.
            loadingStep = 3
            delay((350 - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            journeys = result
            selected = 0
            step = if (result.isEmpty()) PlanStep.NoRoute else PlanStep.Results
        }
    }

    // After a restore the calculated routes are gone: calculate again.
    LaunchedEffect(Unit) {
        if (step in listOf(PlanStep.Results, PlanStep.RouteMap, PlanStep.Details, PlanStep.Walk, PlanStep.Loading) && journeys.isEmpty()) search()
    }

    // A place chosen in Search.
    LaunchedEffect(pick) {
        val p = pick ?: return@LaunchedEffect
        when {
            p.startsWith("from:") -> from = p.removePrefix("from:")
            p.startsWith("to:") -> to = p.removePrefix("to:")
        }
        step = PlanStep.Input
        onPickHandled()
    }

    BackHandler {
        when {
            prefsOpen -> prefsOpen = false
            step == PlanStep.Walk -> step = PlanStep.Details
            step == PlanStep.Details -> step = PlanStep.RouteMap
            step == PlanStep.RouteMap -> step = PlanStep.Results
            step == PlanStep.Input -> actions.onClose()
            else -> step = PlanStep.Input
        }
    }

    val fromPoint by produceState<MapPoint?>(null, from) { value = from?.let { pointOf(it) } }
    val toPoint by produceState<MapPoint?>(null, to) { value = to?.let { pointOf(it) } }
    val stops by produceState(emptyList<Stop>()) { value = repo.stops() }

    PlannerScreen(
        from = from, to = to, step = step, prefsOpen = prefsOpen, options = options,
        journeys = journeys, selected = selected, loadingStep = loadingStep, walkLeg = walkLeg,
        fromPoint = fromPoint, toPoint = toPoint, stops = stops, now = repo.now(),
        onSelect = { selected = it; step = PlanStep.RouteMap },
        onSelectedChange = { selected = it },
        onShowWalk = { walkLeg = it; step = PlanStep.Walk },
        onStep = { step = it },
        onPrefs = { prefsOpen = it },
        onOptions = { options = it },
        onSwap = { val f = from; from = to; to = f },
        onSearch = { prefsOpen = false; search() },
        onShare = { shareJourney(context, it) },
        actions = actions,
    )
}

/** Where a named place is on the map (for markers). */
private suspend fun pointOf(name: String): MapPoint? {
    val repo = Transit.repository
    if (name == MY_LOCATION) return repo.myLocation()
    // Saved places ("University") point at a map place or stop.
    val target = com.example.konstanz.data.SavedStore.places.firstOrNull { it.name == name }?.targetId
    if (target != null) (repo.place(target)?.point ?: repo.stop(target)?.point)?.let { return it }
    return repo.placeByName(name)?.point ?: repo.stopByName(name)?.point
}

@Composable
private fun PlannerScreen(
    from: String?,
    to: String?,
    step: PlanStep,
    prefsOpen: Boolean,
    options: PlanOptions,
    journeys: List<Journey>,
    selected: Int,
    loadingStep: Int,
    walkLeg: Int,
    fromPoint: MapPoint?,
    toPoint: MapPoint?,
    stops: List<Stop>,
    now: Minutes,
    onSelect: (Int) -> Unit,
    onSelectedChange: (Int) -> Unit,
    onShowWalk: (Int) -> Unit,
    onStep: (PlanStep) -> Unit,
    onPrefs: (Boolean) -> Unit,
    onOptions: (PlanOptions) -> Unit,
    onSwap: () -> Unit,
    onSearch: () -> Unit,
    onShare: (Journey) -> Unit,
    actions: PlannerActions,
) {
    val camera = rememberMapCameraState(center = MapPoint(680f, 1013f), zoom = 0.525f)
    var mapWidth by remember { mutableIntStateOf(1) }
    var mapHeight by remember { mutableIntStateOf(1) }
    val journey = journeys.getOrNull(selected)
    val walk = journey?.legs?.getOrNull(walkLeg) as? Leg.Walk
    // The walking step shown on the map (−1 = the whole walk).
    var walkStep by rememberSaveable(walkLeg, selected) { mutableIntStateOf(-1) }
    BackHandler(enabled = step == PlanStep.Walk && walkStep >= 0) { walkStep = -1 }
    val walkSheetHeight = windowHeight() * 0.46f
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.getTop(density)

    // Framing per step. The real map fits the real places and routes into the map left visible above
    // each step's sheet (design px on a 390-wide phone); the design sample uses the artboards' view boxes.
    LaunchedEffect(step, journeys, selected, walkLeg, walkStep, fromPoint, toPoint) {
        snapshotFlow { mapWidth > 1 && mapHeight > 1 }.first { it }
        // One walk: the whole path above its sheet, or close in on the step being shown (below its banner).
        val walkPoints = walk?.path?.let(::pathPoints).orEmpty()
        if (step == PlanStep.Walk && walkPoints.isNotEmpty()) {
            val px = mapWidth / 390f
            val top = statusTop + with(density) { 12.dp.toPx() }
            val bottom = mapHeight - with(density) { walkSheetHeight.toPx() }
            val at = walk?.steps?.getOrNull(walkStep)?.at
            if (at != null) {
                val zoom = 3.6f
                val bannerBottom = top + 110f * px
                camera.animateTo(camera.centerShowing(at, (bannerBottom + bottom) / 2f, mapWidth.toFloat(), zoom), zoom)
            } else {
                val (center, zoom) = camera.framing(walkPoints, mapWidth.toFloat(), top + 60f * px, bottom - 16f * px, marginPx = 36f * px, maxZoom = 3.6f)
                camera.animateTo(center, zoom)
            }
            return@LaunchedEffect
        }
        if (realMapEnabled) {
            val ends = listOfNotNull(fromPoint, toPoint)
            val shown = when (step) {
                PlanStep.Input -> emptyList()
                PlanStep.Loading, PlanStep.Results, PlanStep.NoRoute -> journeys
                PlanStep.RouteMap, PlanStep.Details, PlanStep.Walk -> listOfNotNull(journey)
            }
            val points = ends + shown.flatMap { j -> j.legs.mapNotNull { it.path }.flatMap(::pathPoints) }
            if (points.isEmpty()) return@LaunchedEffect
            val (top, bottom) = when (step) {
                PlanStep.Input -> 60f to 460f
                PlanStep.Loading, PlanStep.Results, PlanStep.NoRoute -> 125f to 365f
                PlanStep.RouteMap -> 125f to 560f
                PlanStep.Details -> 110f to 300f
                PlanStep.Walk -> 110f to 520f
            }
            val px = mapWidth / 390f
            val (center, zoom) = camera.framing(points, mapWidth.toFloat(), top * px, bottom * px, marginPx = 28f * px)
            camera.animateTo(center, zoom)
            return@LaunchedEffect
        }
        when (step) {
            PlanStep.Input -> camera.animateTo(MapPoint(680f, 1013f), 0.525f)
            PlanStep.Loading, PlanStep.Results, PlanStep.NoRoute -> camera.animateTo(MapPoint(656f, 910f), 0.575f)
            PlanStep.RouteMap -> camera.animateTo(MapPoint(656f, 652f), 0.9f)
            PlanStep.Details -> camera.animateTo(camera.centerShowing(MapPoint(680f, 580f), mapWidth * 90f / 390f, mapWidth.toFloat(), 0.3f), 0.3f)
            PlanStep.Walk -> camera.animateTo(MapPoint(656f, 652f), 0.9f)
        }
    }

    Box(Modifier.fillMaxSize().background(MapLand).onSizeChanged { mapWidth = it.width; mapHeight = it.height }) {
        InteractiveMap(camera, Modifier.fillMaxSize()) {
            when (step) {
                PlanStep.Results, PlanStep.RouteMap, PlanStep.Details -> RouteLayer(journeys, journey)
                PlanStep.Walk -> if (journey != null) RouteLayer(listOf(journey), journey, focus = walk)
                else -> Unit
            }
            if (step == PlanStep.Walk && walk != null) {
                walk.steps.forEachIndexed { i, st ->
                    val at = st.at ?: return@forEachIndexed
                    if (i != walkStep && st.maneuver != Maneuver.Depart && st.maneuver != Maneuver.Arrive) {
                        ManeuverDot(Modifier.at(at.x, at.y))
                    }
                }
            }
            StopMarkersLayer(stops.alongJourneys(journeys), onStopClick = {}, onClusterClick = { _, _ -> })
            if (step == PlanStep.Input && from == MY_LOCATION && fromPoint != null) {
                LocationPuck(Modifier.at(fromPoint.x, fromPoint.y), accuracy = 44.dp, showHeading = false)
            } else if (fromPoint != null) {
                StartMarker(Modifier.at(fromPoint.x, fromPoint.y))
            }
            if (toPoint != null) DestinationMarker(Modifier.at(toPoint.x, toPoint.y))
            if (step == PlanStep.Walk) {
                walk?.steps?.getOrNull(walkStep)?.let { st -> st.at?.let { ManeuverMarker(st.maneuver, Modifier.at(it.x, it.y)) } }
            }
        }

        // ---- Top ----
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            when (step) {
                PlanStep.Input -> StatusPill(stringResource(R.string.offline))
                PlanStep.Details -> BackCircle(stringResource(R.string.back_route_map)) { onStep(PlanStep.RouteMap) }
                PlanStep.Walk -> Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackCircle(stringResource(R.string.back_route_details)) { onStep(PlanStep.Details) }
                    val steps = walk?.steps.orEmpty()
                    if (walkStep in steps.indices) StepBanner(steps, walkStep, onIndex = { walkStep = it }, Modifier.weight(1f))
                }
                else -> PlanBar(
                    from = from.orEmpty(),
                    to = to.orEmpty(),
                    subtitle = if (step == PlanStep.RouteMap && journey != null) {
                        stringResource(R.string.route_x_of_y, selected + 1, journeys.size, journey.summary().substringBefore(" ·"))
                    } else options.summary(),
                    onBack = {
                        onStep(if (step == PlanStep.RouteMap) PlanStep.Results else PlanStep.Input)
                    },
                    onEdit = { onStep(PlanStep.Input) },
                    onOptions = { onPrefs(true) },
                )
            }
        }

        // ---- Bottom ----
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            when (step) {
                PlanStep.Input -> InputSheet(
                    from = from, to = to, options = options,
                    onFrom = actions.onPickFrom, onTo = actions.onPickTo, onSwap = onSwap,
                    onOptions = { onPrefs(true) }, onFind = onSearch, onClose = actions.onClose,
                )
                PlanStep.Loading -> LoadingSheet(loadingStep)
                PlanStep.Results -> ResultsSheet(journeys, selected, options.preference, onSelect = onSelect, onOptions = { onPrefs(true) })
                PlanStep.NoRoute -> NoRouteSheet(
                    time = if (options.timeMode == TimeMode.Now) now else options.time,
                    onChangeDestination = actions.onPickTo,
                    onChangeTime = { onPrefs(true) },
                )
                PlanStep.RouteMap -> {
                    val pager = rememberPagerState(initialPage = selected) { journeys.size }
                    LaunchedEffect(pager.currentPage) { onSelectedChange(pager.currentPage) }
                    Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
                        RouteLegend(Modifier.padding(start = 16.dp, bottom = 12.dp))
                        RouteMapCards(
                            journeys, pager,
                            onDetails = { onStep(PlanStep.Details) },
                        )
                    }
                }
                PlanStep.Details -> if (journey != null) {
                    DetailsSheet(
                        journey = journey,
                        realtimeAgo = 30,
                        onShare = { onShare(journey) },
                        onOpenRide = actions.onOpenRide,
                        onShowWalk = onShowWalk,
                        modifier = Modifier.statusBarsPadding().padding(top = 140.dp),
                    )
                }
                PlanStep.Walk -> if (journey != null && walk != null) {
                    WalkSheet(journey, walk, walkStep, onSelect = { walkStep = it }, height = walkSheetHeight)
                }
            }
        }

        // ---- When & how (17), over a dimmed map ----
        if (prefsOpen) {
            Box(
                Modifier.fillMaxSize().background(Color(0x6616181D))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPrefs(false) }
            )
            Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 84.dp), contentAlignment = Alignment.BottomCenter) {
                PrefsSheet(options, now, onChange = onOptions, onClose = { onPrefs(false) }, onFind = onSearch)
            }
        }
    }
}

@Composable
private fun BackCircle(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).floatingShadow(CircleShape).clip(CircleShape).background(White)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { KonstanzIcon(KtIcons.ChevronLeft, contentDescription = label, size = 22.dp, tint = Ink) }
}

private val OptionsSaver = Saver<PlanOptions, List<Any>>(
    save = { listOf(it.timeMode.name, it.dayOffset, it.time.value, it.preference.name) },
    restore = {
        PlanOptions(
            timeMode = TimeMode.valueOf(it[0] as String),
            dayOffset = it[1] as Int,
            time = Minutes(it[2] as Int),
            preference = RoutePreference.valueOf(it[3] as String),
        )
    },
)

/** Share the journey as plain text. */
private fun shareJourney(context: Context, j: Journey) {
    val rides = j.rides.joinToString(", ") { context.getString(R.string.share_bus, it.line, it.from, it.start.format()) }
    val text = "${j.from} → ${j.to}: ${j.start.format()} – ${j.end.format()} (${context.getString(R.string.minutes_short, j.minutes)}). $rides."
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), context.getString(R.string.share_route)))
}
