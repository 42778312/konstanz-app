package com.example.konstanz.ui.map

/**
 * Something another screen asks the main map to do when it comes back (e.g. a search result).
 * Travels as a short string through the navigation back stack.
 */
sealed interface MapCommand {
    /** Location details (artboard 11). */
    data class ShowPlace(val placeId: String) : MapCommand
    /** Stop sheet (artboard 12). */
    data class ShowStop(val stopId: String) : MapCommand
    /** Drop-a-pin mode (artboard 10). */
    data object PickOnMap : MapCommand
    /** Switch to the Saved tab (Search → "Edit"). Handled by the shell, not the map. */
    data object OpenSaved : MapCommand
    /** A bus trip drawn on the map (Departure details → "Show on map"), [stopId] = where it was opened. */
    data class ShowTrip(val tripId: String, val stopId: String?) : MapCommand

    fun encode(): String = when (this) {
        is ShowPlace -> "place:$placeId"
        is ShowStop -> "stop:$stopId"
        PickOnMap -> "pick"
        OpenSaved -> "saved"
        is ShowTrip -> "trip:$tripId:${stopId.orEmpty()}"
    }

    companion object {
        /** Key in the main screen's saved state. */
        const val KEY = "mapCommand"

        fun decode(value: String?): MapCommand? = when {
            value == null -> null
            value == "pick" -> PickOnMap
            value == "saved" -> OpenSaved
            value.startsWith("place:") -> ShowPlace(value.removePrefix("place:"))
            value.startsWith("stop:") -> ShowStop(value.removePrefix("stop:"))
            value.startsWith("trip:") -> value.removePrefix("trip:").split(":", limit = 2)
                .let { ShowTrip(it[0], it.getOrNull(1)?.ifEmpty { null }) }
            else -> null
        }
    }
}
