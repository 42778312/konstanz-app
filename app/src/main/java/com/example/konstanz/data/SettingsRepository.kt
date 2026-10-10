package com.example.konstanz.data

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// One DataStore per file per process — the delegate guarantees that.
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** A setting with a fixed list of values; [label] is what the Settings screen shows. */
interface SettingOption {
    val label: String
}

enum class RoutePreference(@param:androidx.annotation.StringRes private val text: Int) : SettingOption {
    Fastest(R.string.pref_fastest), LeastWalking(R.string.pref_least_walking), FewestTransfers(R.string.pref_fewest_transfers);

    override val label: String get() = Texts.get(text)
}

/** Seasonal effects: in autumn and winter the map shows falling leaves or snow (see [SeasonalCalendar]). */
enum class SeasonalThemes(@param:androidx.annotation.StringRes private val text: Int) : SettingOption {
    EventsAndSeasons(R.string.themes_events_seasons), EventsOnly(R.string.themes_events_only), Off(R.string.themes_off);

    override val label: String get() = Texts.get(text)
}

/**
 * What the user can change: the default route preference (Settings) and bus stops on the map (map
 * layers sheet). Only options that take effect are here.
 */
data class UserSettings(
    val showBusStops: Boolean = true,
    val routePreference: RoutePreference = RoutePreference.Fastest,
    /** Holiday / Konstanz-event / season look on the splash screen. */
    val seasonalThemes: SeasonalThemes = SeasonalThemes.EventsAndSeasons,
)

/** Small app preferences, stored on the device with DataStore. */
class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val onboardingCompleted: Flow<Boolean> =
        store.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun isOnboardingCompleted(): Boolean = onboardingCompleted.first()

    suspend fun setOnboardingCompleted() {
        store.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }

    val settings: Flow<UserSettings> = store.data.map { p ->
        val d = UserSettings()
        UserSettings(
            showBusStops = p[Keys.SHOW_BUS_STOPS] ?: d.showBusStops,
            routePreference = p.enum(Keys.ROUTE_PREFERENCE, d.routePreference),
            // The first version stored an on/off switch; "off" stays off.
            seasonalThemes = if (p[Keys.SEASONAL_THEMES_ON] == false) SeasonalThemes.Off else p.enum(Keys.SEASONAL_THEMES, d.seasonalThemes),
        )
    }

    suspend fun setShowBusStops(value: Boolean) = set(Keys.SHOW_BUS_STOPS, value)
    suspend fun setRoutePreference(value: RoutePreference) = set(Keys.ROUTE_PREFERENCE, value.name)
    suspend fun setSeasonalThemes(value: SeasonalThemes) {
        store.edit { it[Keys.SEASONAL_THEMES] = value.name; it.remove(Keys.SEASONAL_THEMES_ON) }
    }
    suspend fun seasonalThemes(): SeasonalThemes = settings.first().seasonalThemes

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    /** Enums are stored by name; an unknown name (e.g. after a rename) falls back to the default. */
    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_BUS_STOPS = booleanPreferencesKey("show_bus_stops")
        val ROUTE_PREFERENCE = stringPreferencesKey("route_preference")
        val SEASONAL_THEMES_ON = booleanPreferencesKey("seasonal_themes")
        val SEASONAL_THEMES = stringPreferencesKey("seasonal_themes_mode")
    }
}
