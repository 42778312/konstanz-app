package com.example.konstanz.ui.navigation

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
import androidx.core.net.toUri
import com.example.konstanz.ui.map.MapCommand
import com.example.konstanz.ui.search.SearchActions
import com.example.konstanz.ui.search.SearchRoute
import com.example.konstanz.ui.plan.PlannerActions
import com.example.konstanz.ui.offline.OfflineDataScreen
import com.example.konstanz.ui.offline.SimulateStatesScreen
import com.example.konstanz.ui.plan.PlannerRoute
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.konstanz.ui.stops.AllDeparturesRoute
import com.example.konstanz.ui.stops.DepartureDetailsActions
import com.example.konstanz.ui.stops.DepartureDetailsRoute
import com.example.konstanz.ui.stops.StopDetailsActions
import com.example.konstanz.ui.stops.StopDetailsRoute
import com.example.konstanz.data.SettingsRepository
import com.example.konstanz.ui.about.PrivacyScreen
import com.example.konstanz.data.AppInfo
import com.example.konstanz.data.SavedStore
import com.example.konstanz.ui.designsystem.DesignSystemScreen
import com.example.konstanz.ui.main.MainShell
import com.example.konstanz.ui.onboarding.OnboardingScreen
import com.example.konstanz.ui.recent.RecentRoute
import com.example.konstanz.ui.splash.SplashRoute
import kotlinx.coroutines.launch

/** Top-level destinations. The bottom-bar tabs live inside [Routes.MAIN] (see MainShell). */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val PRIVACY = "privacy"
    const val RECENT = "recent"
    const val SEARCH = "search?q={q}&pick={pick}"
    /** [pick] = "from" / "to" when choosing for the planner. */
    fun search(query: String, pick: String? = null) =
        "search?q=${Uri.encode(query)}" + (pick?.let { "&pick=$it" } ?: "")
    const val PLAN = "plan?from={from}&to={to}"
    fun plan(from: String, to: String) = "plan?from=${Uri.encode(from)}&to=${Uri.encode(to)}"
    const val OFFLINE_DATA = "offline-data"
    const val SIMULATE = "dev/simulate"
    /** Key in the planner's saved state for a place picked in Search. */
    const val PLAN_PICK = "planPick"
    const val STOP = "stop/{stopId}"
    const val STOP_DEPARTURES = "stop/{stopId}/departures?line={line}"
    /** [at] = departure time (minutes) at [stopId]: ring lines pass a stop twice. */
    const val TRIP = "trip/{tripId}?stop={stopId}&at={at}"

    fun stop(stopId: String) = "stop/${Uri.encode(stopId)}"
    fun stopDepartures(stopId: String, line: String?) =
        "stop/${Uri.encode(stopId)}/departures" + (line?.let { "?line=${Uri.encode(it)}" } ?: "")
    fun trip(tripId: String, stopId: String?, at: Int? = null) =
        "trip/${Uri.encode(tripId)}?stop=${Uri.encode(stopId.orEmpty())}&at=${at ?: -1}"
    /** Developer screen showing every token and component. */
    const val DESIGN_SYSTEM = "design-system"
}

@Composable
fun KonstanzNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    NavHost(
        navController,
        startDestination = Routes.SPLASH,
        // The new screen fades in *over* the old one. A cross-fade would let the red window
        // background show through while both screens are half transparent.
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { ExitTransition.KeepUntilTransitionsFinished },
    ) {
        composable(Routes.SPLASH) {
            SplashRoute(
                isOnboardingCompleted = settings::isOnboardingCompleted,
                seasonalThemes = settings::seasonalThemes,
                onFinished = { onboardingDone ->
                    navController.navigate(if (onboardingDone) Routes.MAIN else Routes.ONBOARDING) {
                        // Splash is never shown again on Back.
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                scope.launch { settings.setOnboardingCompleted() }
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.MAIN) { entry ->
            val command by entry.savedStateHandle.getStateFlow<String?>(MapCommand.KEY, null).collectAsState()
            MainShell(
                onSendFeedback = AppInfo.SUPPORT_EMAIL?.let { email -> { sendFeedback(context, email) } },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenStop = { navController.navigate(Routes.stop(it)) },
                onOpenDeparture = { trip, stop, at -> navController.navigate(Routes.trip(trip, stop, at)) },
                onOpenSearch = { navController.navigate(Routes.search(it)) },
                onOpenPlanner = { from, to -> navController.navigate(Routes.plan(from, to)) },
                // "start" or "start>Destination": the search hands the start to the planner.
                onChooseStart = { to -> navController.navigate(Routes.search("", pick = "start" + (to?.let { ">$it" } ?: ""))) },
                onChooseSavedPlace = { slot -> navController.navigate(Routes.search("", pick = "save-$slot")) },
                onOpenOfflineData = { navController.navigate(Routes.OFFLINE_DATA) },
                onOpenSimulate = { navController.navigate(Routes.SIMULATE) },
                mapCommand = MapCommand.decode(command),
                onMapCommandHandled = { entry.savedStateHandle[MapCommand.KEY] = null },
                onOpenDesignSystem = { navController.navigate(Routes.DESIGN_SYSTEM) },
            )
        }
        composable(Routes.PRIVACY) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RECENT) {
            RecentRoute(
                onBack = { navController.popBackStack() },
                onOpenStop = { navController.navigate(Routes.stop(it)) },
                onOpenPlace = { navController.backToMap(MapCommand.ShowPlace(it)) },
            )
        }
        composable(
            Routes.SEARCH,
            arguments = listOf(
                navArgument("q") { type = NavType.StringType; defaultValue = "" },
                navArgument("pick") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val pick = entry.arguments?.getString("pick")
            // "save-home" / "save-work" / "save-new": choosing a place for Saved.
            val saveSlot = pick?.takeIf { it.startsWith("save-") }?.removePrefix("save-")
            // Picking for the planner: hand the name back instead of showing it on the map;
            // picking for Saved: store it and go back.
            fun picked(name: String, address: String = "", id: String = "") {
                if (pick != null && pick.startsWith("start")) {
                    val to = pick.substringAfter(">", "")
                    navController.navigate(Routes.plan(name, to)) { popUpTo(Routes.MAIN) }
                    return
                }
                if (saveSlot != null) {
                    SavedStore.save(saveSlot, name, address, id)
                    val label = when (saveSlot) { "home" -> "Home"; "work" -> "Work"; else -> name }
                    android.widget.Toast.makeText(context, "$label saved", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    navController.previousBackStackEntry?.savedStateHandle?.set(Routes.PLAN_PICK, "$pick:$name")
                }
                navController.popBackStack()
            }
            SearchRoute(
                initialQuery = entry.arguments?.getString("q").orEmpty(),
                pickMode = pick != null,
                offerMyLocation = pick == "from" || pick?.startsWith("start") == true,
                actions = SearchActions(
                    onBack = { navController.popBackStack() },
                    onOpenPlace = { if (pick != null) picked(it.name, it.address, it.id) else navController.backToMap(MapCommand.ShowPlace(it.id)) },
                    onOpenStop = { if (pick != null) picked(it.name, "Bus stop", it.id) else navController.backToMap(MapCommand.ShowStop(it.id)) },
                    onQuickDestination = { place ->
                        when {
                            place.address == null ->
                                if (saveSlot == null) navController.navigate(Routes.search("", pick = "save-" + (if (place.kind == com.example.konstanz.data.PlaceKind.Work) "work" else "home")))
                            pick != null -> picked(place.name)
                            else -> navController.navigate(Routes.plan("My location", place.name)) { popUpTo(Routes.MAIN) }
                        }
                    },
                    onEditSaved = { navController.backToMap(MapCommand.OpenSaved) },
                    onSeeAllRecent = { navController.navigate(Routes.RECENT) },
                    onChooseOnMap = { navController.backToMap(MapCommand.PickOnMap) },
                    onMyLocation = { picked("My location") },
                ),
            )
        }
        composable(Routes.STOP, arguments = listOf(navArgument("stopId") { type = NavType.StringType })) { entry ->
            val stopId = entry.arguments?.getString("stopId").orEmpty()
            StopDetailsRoute(
                stopId,
                StopDetailsActions(
                    onBack = { navController.popBackStack() },
                    onViewAll = { line -> navController.navigate(Routes.stopDepartures(stopId, line)) },
                    onDepartureClick = { navController.navigate(Routes.trip(it.tripId, stopId, it.scheduled.value)) },
                    onRouteFromHere = { name -> navController.navigate(Routes.plan(name, "")) },
                ),
            )
        }
        composable(
            Routes.STOP_DEPARTURES,
            arguments = listOf(
                navArgument("stopId") { type = NavType.StringType },
                navArgument("line") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val stopId = entry.arguments?.getString("stopId").orEmpty()
            AllDeparturesRoute(
                stopId = stopId,
                initialLine = entry.arguments?.getString("line"),
                onBack = { navController.popBackStack() },
                onDepartureClick = { navController.navigate(Routes.trip(it.tripId, stopId, it.scheduled.value)) },
            )
        }
        composable(
            Routes.TRIP,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("stopId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("at") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { entry ->
            val tripId = entry.arguments?.getString("tripId").orEmpty()
            val fromStopId = entry.arguments?.getString("stopId")?.ifEmpty { null }
            val at = entry.arguments?.getInt("at")?.takeIf { it >= 0 }
            DepartureDetailsRoute(
                tripId = tripId,
                fromStopId = fromStopId,
                at = at,
                actions = DepartureDetailsActions(
                    onBack = { navController.popBackStack() },
                    onShowOnMap = { navController.backToMap(MapCommand.ShowTrip(tripId, fromStopId)) },
                ),
            )
        }
        composable(
            Routes.PLAN,
            arguments = listOf(
                navArgument("from") { type = NavType.StringType; defaultValue = "" },
                navArgument("to") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val pick by entry.savedStateHandle.getStateFlow<String?>(Routes.PLAN_PICK, null).collectAsState()
            PlannerRoute(
                initialFrom = entry.arguments?.getString("from").orEmpty(),
                initialTo = entry.arguments?.getString("to").orEmpty(),
                pick = pick,
                onPickHandled = { entry.savedStateHandle[Routes.PLAN_PICK] = null },
                actions = PlannerActions(
                    onClose = { navController.popBackStack() },
                    onPickFrom = { navController.navigate(Routes.search("", pick = "from")) },
                    onPickTo = { navController.navigate(Routes.search("", pick = "to")) },
                    onOpenRide = { ride -> ride.tripId?.let { navController.navigate(Routes.trip(it, ride.fromStopId, ride.start.value)) } },
                ),
            )
        }
        composable(Routes.OFFLINE_DATA) {
            OfflineDataScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SIMULATE) {
            SimulateStatesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DESIGN_SYSTEM) {
            DesignSystemScreen()
        }
    }
}

/** Return to the main screen (closing search / recent on top of it) and hand it a [command]. */
private fun NavHostController.backToMap(command: MapCommand) {
    getBackStackEntry(Routes.MAIN).savedStateHandle[MapCommand.KEY] = command.encode()
    popBackStack(Routes.MAIN, inclusive = false)
}

/** "Send feedback": the user's e-mail app, addressed to the support address, with the app version. */
private fun sendFeedback(context: android.content.Context, email: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(email))
        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Konstant ${AppInfo.versionName(context)}")
    runCatching { context.startActivity(intent) }
        .onFailure { android.widget.Toast.makeText(context, "No e-mail app found. Write to $email", android.widget.Toast.LENGTH_LONG).show() }
}
