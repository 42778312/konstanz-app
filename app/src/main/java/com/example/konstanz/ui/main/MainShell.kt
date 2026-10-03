package com.example.konstanz.ui.main

import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.konstanz.ui.components.BottomBarItem
import com.example.konstanz.ui.components.KonstanzBottomBar
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.map.MainMapActions
import com.example.konstanz.ui.map.MainMapRoute
import com.example.konstanz.ui.map.MapCommand
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.example.konstanz.data.OfflineData
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.example.konstanz.ui.saved.SavedRoute
import com.example.konstanz.ui.settings.SettingsActions
import com.example.konstanz.ui.settings.SettingsRoute
import com.example.konstanz.ui.theme.KonstanzTheme

/** The three bottom-bar tabs, in bar order. */
enum class MainTab(val route: String, @param:androidx.annotation.StringRes val label: Int, val icon: com.example.konstanz.ui.icons.KtIcon) {
    Map("tab/map", R.string.tab_map, KtIcons.Map),
    Saved("tab/saved", R.string.tab_saved, KtIcons.Star),
    Settings("tab/settings", R.string.tab_settings, KtIcons.Sliders),
}

/**
 * Everything after onboarding: a tab host with the bottom bar.
 * Each tab keeps its own state (scroll position, later the map camera) when you switch away;
 * Back from Saved or Settings returns to Map, Back on Map leaves the app.
 */
@Composable
fun MainShell(
    onOpenAbout: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenStop: (stopId: String) -> Unit,
    onOpenAllDepartures: (stopId: String) -> Unit,
    onOpenDeparture: (tripId: String, stopId: String, at: Int) -> Unit,
    onOpenSearch: (query: String) -> Unit,
    onOpenPlanner: (from: String, to: String) -> Unit,
    onChooseStart: (to: String?) -> Unit = {},
    /** Search to choose a place to save: "home", "work" or "new". */
    onChooseSavedPlace: (slot: String) -> Unit,
    onOpenOfflineData: () -> Unit,
    onOpenSimulate: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    modifier: Modifier = Modifier,
    /** Request from another screen (search result); the shell switches to the right tab. */
    mapCommand: MapCommand? = null,
    onMapCommandHandled: () -> Unit = {},
    tabNavController: NavHostController = rememberNavController(),
) {
    val backStack by tabNavController.currentBackStackEntryAsState()
    val selected = MainTab.entries.indexOfFirst { tab ->
        backStack?.destination?.route == tab.route
    }.coerceAtLeast(0)

    // Hidden while the map is in "pick a location" mode (artboard 10 has no bottom bar).
    var barVisible by remember { mutableStateOf(true) }

    // Commands arrive when coming back from Search: show the map (or Saved) tab first.
    LaunchedEffect(mapCommand) {
        when (mapCommand) {
            null -> Unit
            MapCommand.OpenSaved -> { tabNavController.selectTab(MainTab.Saved); onMapCommandHandled() }
            else -> tabNavController.selectTab(MainTab.Map)
        }
    }

    Column(modifier.fillMaxSize()) {
        NavHost(
            tabNavController,
            startDestination = MainTab.Map.route,
            modifier = Modifier.weight(1f),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { ExitTransition.KeepUntilTransitionsFinished },
        ) {
            composable(MainTab.Map.route) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val repairedText = stringResource(R.string.data_repaired)
                val failedText = stringResource(R.string.repair_failed)
                MainMapRoute(
                    MainMapActions(
                        onOpenSearch = onOpenSearch,
                        onQuickDestination = { place ->
                            if (place.address == null) onChooseSavedPlace(if (place.kind == com.example.konstanz.data.PlaceKind.Work) "work" else "home")
                            else onOpenPlanner("My location", place.name)
                        },
                        onOpenOfflineStatus = onOpenOfflineData,
                        onRepairTimetable = {
                            scope.launch {
                                val ok = OfflineData.repair(context)
                                android.widget.Toast.makeText(
                                    context, if (ok) repairedText else failedText,
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        onOpenStopDetails = { onOpenStop(it.id) },
                        onOpenAllDepartures = { onOpenAllDepartures(it.id) },
                        onOpenDeparture = { departure, stop -> onOpenDeparture(departure.tripId, stop.id, departure.scheduled.value) },
                        onRouteToPlace = { onOpenPlanner("My location", it.name) },
                        onRouteFromPlace = { onOpenPlanner(it.name, "") },
                        onOpenPlanner = onOpenPlanner,
                        onChooseStart = onChooseStart,
                        onRouteFrom = { onOpenPlanner(it.name, "") },
                        onSelectionModeChange = { picking -> barVisible = !picking },
                    ),
                    command = mapCommand.takeIf { it != MapCommand.OpenSaved },
                    onCommandHandled = onMapCommandHandled,
                )
            }
            composable(MainTab.Saved.route) { SavedRoute(onOpenStop = onOpenStop, onRouteTo = { onOpenPlanner("My location", it) }, onChoosePlace = onChooseSavedPlace) }
            composable(MainTab.Settings.route) {
                SettingsRoute(
                    SettingsActions(
                        onClose = { tabNavController.selectTab(MainTab.Map) },
                        onOpenOfflineData = onOpenOfflineData,
                        onOpenAbout = onOpenAbout,
                        onOpenLicenses = onOpenLicenses,
                        onOpenPrivacy = onOpenPrivacy,
                        onOpenSimulate = onOpenSimulate,
                        onOpenDesignSystem = onOpenDesignSystem,
                    )
                )
            }
        }
        AnimatedVisibility(barVisible, enter = expandVertically(), exit = shrinkVertically()) {
            KonstanzBottomBar(
                items = MainTab.entries.map { BottomBarItem(stringResource(it.label), it.icon) },
                selectedIndex = selected,
                onSelect = { index -> tabNavController.selectTab(MainTab.entries[index]) },
            )
        }
    }
}

/** Standard bottom-navigation switch: one copy of each tab, state saved and restored. */
private fun NavHostController.selectTab(tab: MainTab) {
    if (currentDestination?.route == tab.route) return
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MainShellPreview() {
    KonstanzTheme { MainShell(onOpenAbout = {}, onOpenLicenses = {}, onOpenPrivacy = {}, onOpenStop = {}, onOpenAllDepartures = {}, onOpenDeparture = { _, _, _ -> }, onOpenSearch = {}, onOpenPlanner = { _, _ -> }, onChooseSavedPlace = {}, onOpenOfflineData = {}, onOpenSimulate = {}, onOpenDesignSystem = {}) }
}
