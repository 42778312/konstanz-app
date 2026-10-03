package com.example.konstanz.data

import com.example.konstanz.R
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.konstanz.data.transit.StopDeparture
import com.example.konstanz.data.user.RecentSearchEntity
import com.example.konstanz.data.user.SavedPlaceEntity
import com.example.konstanz.data.user.SavedStopEntity
import com.example.konstanz.data.user.UserDao
import com.example.konstanz.data.user.UserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What a saved place stands for; decides its icon and whether it can be removed. */
enum class PlaceKind { Home, Work, University, Stop, Other }

/**
 * A saved place (artboard 28, "Places"). [address] null = Home/Work not set yet ("Set" button).
 * [travelMinutes] = journey time from here, shown on the red Go button.
 */
@Immutable
data class SavedPlace(
    val id: String,
    val name: String,
    val kind: PlaceKind,
    val address: String?,
    val travelMinutes: Int? = null,
    val placeholder: String? = null,
    /** The map place or stop this points at (TransitRepository ids), for markers and routes. */
    val targetId: String? = null,
) {
    /** As shown: Home / Work in the phone's language ([name] stays the stored key). */
    val displayName: String get() = when (kind) {
        PlaceKind.Home -> Texts.get(R.string.home)
        PlaceKind.Work -> Texts.get(R.string.work)
        else -> name
    }

    /** "Add your home address" in the phone's language. */
    val displayPlaceholder: String? get() = when {
        placeholder == null -> null
        kind == PlaceKind.Home -> Texts.get(R.string.add_home)
        kind == PlaceKind.Work -> Texts.get(R.string.add_work)
        else -> placeholder
    }
}

/** A favourite stop (artboard 28, "Stops"). */
@Immutable
data class SavedStop(val id: String, val name: String, val nextDeparture: String, val line: String)

/** A recent search (artboard 29). */
@Immutable
data class RecentSearch(val id: String, val name: String, val isStop: Boolean, val whenLabel: String)

/**
 * The user's Saved places/stops and Recent searches. Screens read the observable lists; every change
 * goes through a function here, which updates the list at once and writes it to [UserDatabase]
 * in the background. A new install starts with empty Home and Work slots.
 */
object SavedStore {
    val places: SnapshotStateList<SavedPlace> = mutableStateListOf()
    val stops: SnapshotStateList<SavedStop> = mutableStateListOf()
    val recent: SnapshotStateList<RecentSearch> = mutableStateListOf()

    // One write at a time, in the order the changes happened.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var dao: UserDao? = null

    /** Loads the lists from the database (seeding it on first run). Call once at app start. */
    fun init(context: Context) {
        if (dao != null) return
        val dao = UserDatabase.get(context).userDao().also { this.dao = it }
        io.launch {
            if (dao.placeCount() == 0) {
                dao.replacePlaces(SeedPlaces.mapIndexed { i, p -> p.toEntity(i) })
                dao.replaceStops(SeedStops.mapIndexed { i, s -> s.toEntity(i) })
                dao.replaceRecent(SeedRecent.mapIndexed { i, r -> r.toEntity(i) })
            }
            val p = dao.places().map { it.toModel() }
            val s = dao.stops().map { it.toModel() }
            val r = dao.recent().map { it.toModel() }
            withContext(Dispatchers.Main) {
                places.replaceWith(p); stops.replaceWith(s); recent.replaceWith(r)
            }
        }
    }

    /** Opening a search result puts it on top of Recent searches (no duplicates). */
    fun addRecent(name: String, isStop: Boolean, whenLabel: String) {
        recent.removeAll { it.name == name && it.isStop == isStop }
        recent.add(0, RecentSearch("r-${name.hashCode()}-$isStop", name, isStop, whenLabel))
        saveRecent()
    }

    fun removeRecent(item: RecentSearch) {
        recent.remove(item); saveRecent()
    }

    fun clearRecent() {
        recent.clear(); saveRecent()
    }

    fun isPlaceSaved(name: String) = places.any { it.name == name }

    /** Star on a place: save it (kind Other) or remove it. [targetId] = the place or stop it points at. */
    fun togglePlace(name: String, address: String, targetId: String? = null) {
        if (isPlaceSaved(name)) places.removeAll { it.name == name && canRemove(it) }
        else places += SavedPlace("p-${name.hashCode()}", name, PlaceKind.Other, address, targetId = targetId)
        savePlaces()
    }

    /**
     * Save a place chosen in Search: [slot] "home" / "work" fills that fixed slot (it keeps its name),
     * anything else adds a new place. The name shown for Home is still "Home"; the address says where.
     */
    fun save(slot: String, name: String, address: String, targetId: String) {
        val kind = when (slot) { "home" -> PlaceKind.Home; "work" -> PlaceKind.Work; else -> null }
        val index = kind?.let { k -> places.indexOfFirst { it.kind == k } } ?: -1
        if (index >= 0) {
            places[index] = places[index].copy(address = listOf(name, address).filter { it.isNotBlank() }.distinct().joinToString(" · "), targetId = targetId, travelMinutes = null)
        } else if (!isPlaceSaved(name)) {
            places += SavedPlace("p-${name.hashCode()}", name, PlaceKind.Other, address, targetId = targetId)
        }
        savePlaces()
    }

    fun removePlace(place: SavedPlace) {
        if (canRemove(place)) { places.remove(place); savePlaces() }
    }

    fun isStopSaved(stopId: String) = stops.any { it.id == stopId }

    /** Star on a stop: save it with its next departure, or remove it. */
    fun toggleStop(stopId: String, name: String, next: StopDeparture?) {
        if (isStopSaved(stopId)) stops.removeAll { it.id == stopId }
        else stops += SavedStop(stopId, name, next?.expected?.format() ?: "—", next?.line ?: "")
        saveStops()
    }

    fun removeStop(stop: SavedStop) {
        stops.remove(stop); saveStops()
    }

    /** Home and Work are fixed slots: they can be changed, never deleted. */
    fun canRemove(place: SavedPlace) = place.kind != PlaceKind.Home && place.kind != PlaceKind.Work

    // Snapshot the list on the calling (main) thread, write it off it.

    private fun savePlaces() {
        val rows = places.mapIndexed { i, p -> p.toEntity(i) }
        dao?.let { d -> io.launch { d.replacePlaces(rows) } }
    }

    private fun saveStops() {
        val rows = stops.mapIndexed { i, s -> s.toEntity(i) }
        dao?.let { d -> io.launch { d.replaceStops(rows) } }
    }

    private fun saveRecent() {
        val rows = recent.mapIndexed { i, r -> r.toEntity(i) }
        dao?.let { d -> io.launch { d.replaceRecent(rows) } }
    }

    private fun <T> SnapshotStateList<T>.replaceWith(items: List<T>) {
        clear(); addAll(items)
    }
}

// ---- First-run seed ----

/** A new install starts with the two fixed slots, empty ("Set"). Nothing else is invented. */
internal val SeedPlaces = listOf(
    SavedPlace("home", "Home", PlaceKind.Home, address = null, placeholder = "Add your home address"),
    SavedPlace("work", "Work", PlaceKind.Work, address = null, placeholder = "Add your work address"),
)

internal val SeedStops = emptyList<SavedStop>()

internal val SeedRecent = emptyList<RecentSearch>()

// ---- Model ↔ row mapping ----

internal fun SavedPlace.toEntity(position: Int) =
    SavedPlaceEntity(id, name, kind.name, address, travelMinutes, placeholder, targetId, position)

internal fun SavedPlaceEntity.toModel() = SavedPlace(
    id, name,
    kind = PlaceKind.entries.firstOrNull { it.name == kind } ?: PlaceKind.Other,
    address = address, travelMinutes = travelMinutes, placeholder = placeholder, targetId = targetId,
)

internal fun SavedStop.toEntity(position: Int) = SavedStopEntity(id, name, nextDeparture, line, position)
internal fun SavedStopEntity.toModel() = SavedStop(id, name, nextDeparture, line)

internal fun RecentSearch.toEntity(position: Int) = RecentSearchEntity(id, name, isStop, whenLabel, position)
internal fun RecentSearchEntity.toModel() = RecentSearch(id, name, isStop, whenLabel)
