package com.example.konstanz

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.konstanz.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The main journeys through the real app (real timetable, map and search index), past onboarding.
 * Labels come from the string resources, so the tests pass in English and German.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowsTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    private fun s(id: Int, vararg args: Any) = context.getString(id, *args)

    @Before
    fun openApp() {
        // "My location" is the start only with location allowed (otherwise: "Choose starting point").
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, android.Manifest.permission.ACCESS_FINE_LOCATION)
        runBlocking { SettingsRepository(context).setOnboardingCompleted() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // Splash, then the map's search bar.
        compose.waitUntilAtLeastOneExists(hasText(s(R.string.where_go)), timeoutMillis = 15_000)
    }

    @After
    fun close() = scenario.close()

    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntilAtLeastOneExists(hasText(text, substring = substring), timeoutMillis = 15_000)

    @Test
    fun tabsSwitchBetweenMapSavedAndSettings() {
        compose.onAllNodesWithText(s(R.string.tab_saved)).onFirst().performClick()
        waitFor(s(R.string.add_place))
        compose.onAllNodesWithText(s(R.string.tab_settings)).onFirst().performClick()
        waitFor(s(R.string.pref_least_walking))
        compose.onAllNodesWithText(s(R.string.tab_map)).onFirst().performClick()
        waitFor(s(R.string.where_go))
    }

    @Test
    fun searchFindsAPlaceAndRoutesThere() {
        compose.onNodeWithText(s(R.string.where_go)).performClick()
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), timeoutMillis = 5_000)
        compose.onNode(hasSetTextAction()).performTextInput("HTWG")
        waitFor("Hochschule für Technik", substring = true)
        compose.onAllNodesWithText("Hochschule für Technik", substring = true).onFirst().performClick()
        // Place sheet on the map → planner → routes.
        waitFor(s(R.string.route_here))
        compose.onNodeWithText(s(R.string.route_here)).performClick()
        waitFor(s(R.string.find_routes))
        compose.onNodeWithText(s(R.string.find_routes)).performClick()
        waitFor(s(R.string.routes))
    }

    @Test
    fun typoStillFindsThePlace() {
        compose.onNodeWithText(s(R.string.where_go)).performClick()
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), timeoutMillis = 5_000)
        compose.onNode(hasSetTextAction()).performTextInput("marktstate")
        waitFor("Marktstätte", substring = true)
        waitFor(s(R.string.did_you_mean).trim(), substring = true)
    }

    @Test
    fun categorySuggestionListsNearbyPlaces() {
        compose.onNodeWithText(s(R.string.where_go)).performClick()
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), timeoutMillis = 5_000)
        compose.onNode(hasSetTextAction()).performTextInput("apo")
        waitFor(s(R.string.cat_pharmacy))
        compose.onAllNodesWithText(s(R.string.cat_pharmacy)).onFirst().performClick()
        waitFor(s(R.string.category_nearby, s(R.string.cat_pharmacy)))
    }

    @Test
    fun emptySearchShowsNoResultsMessage() {
        compose.onNodeWithText(s(R.string.where_go)).performClick()
        compose.waitUntilAtLeastOneExists(hasSetTextAction(), timeoutMillis = 5_000)
        compose.onNode(hasSetTextAction()).performTextInput("qqqqxxxxzzzz")
        waitFor(s(R.string.no_results, "qqqqxxxxzzzz"))
    }

    @Test
    fun fromFieldOpensSearchWithYourLocation() {
        compose.onAllNodesWithText(s(R.string.my_location)).onFirst().performClick()
        waitFor(s(R.string.your_location))
        compose.onNodeWithText(s(R.string.your_location)).performClick()
        waitFor(s(R.string.plan_journey))
    }

    @Test
    fun mapLayersSheetOpensAndCloses() {
        compose.onNodeWithContentDescription(s(R.string.map_layers)).performClick()
        waitFor(s(R.string.bus_stops_layer_sub))
        compose.onNodeWithContentDescription(s(R.string.close_map_layers)).performClick()
        waitFor(s(R.string.where_go))
    }

    @Test
    fun settingsLinksOpenTheirScreens() {
        compose.onAllNodesWithText(s(R.string.tab_settings)).onFirst().performClick()
        waitFor(s(R.string.timetable_map))
        compose.onNodeWithText(s(R.string.timetable_map)).performClick()
        waitFor(s(R.string.storage_used))
        compose.onNodeWithContentDescription(s(R.string.back)).performClick()
        waitFor(s(R.string.about_sources))
        compose.onNode(hasText(s(R.string.privacy_policy)) and hasClickAction()).performClick()
        waitFor(s(R.string.pp_short))
    }
}
