package com.example.konstanz.ui.components

import com.example.konstanz.data.transit.PlaceKind
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons

// Kinds of places (search results, map layers): one icon and colour each, like the map's place dots.

fun PlaceKind.group(): String = when (this) {
    PlaceKind.University -> "University"
    PlaceKind.Library, PlaceKind.Education -> "Education"
    PlaceKind.Station, PlaceKind.Harbour, PlaceKind.Transport -> "Transport"
    PlaceKind.Square -> "Square"
    PlaceKind.Street -> "Street"
    PlaceKind.Address -> "Address"
    PlaceKind.Food -> "Food"
    PlaceKind.Cafe -> "Cafe"
    PlaceKind.Shop -> "Shop"
    PlaceKind.Health -> "Health"
    PlaceKind.Culture -> "Culture"
    PlaceKind.Park -> "Park"
    PlaceKind.Leisure -> "Leisure"
    PlaceKind.Hotel -> "Hotel"
    PlaceKind.District, PlaceKind.Town -> "District"
    PlaceKind.Venue, PlaceKind.Service -> "Service"
}

fun placeGroupIcon(group: String, key: String? = null): KtIcon = when {
    key == "atm" || key == "bank" -> KtIcons.Brief
    key == "pharmacy" -> KtIcons.Health
    else -> when (group) {
        "Food" -> KtIcons.Food
        "Cafe" -> KtIcons.Coffee
        "Shop" -> KtIcons.Bag
        "Health" -> KtIcons.Health
        "Education", "University" -> KtIcons.Cap
        "Culture" -> KtIcons.Landmark
        "Park" -> KtIcons.Tree
        "Leisure" -> KtIcons.Star
        "Hotel" -> KtIcons.Bed
        "Transport" -> KtIcons.Bus
        "Street" -> KtIcons.Road
        "Address" -> KtIcons.Home
        "Square", "District", "Town" -> KtIcons.Map
        else -> KtIcons.Pin
    }
}

/** Colours like the map's place dots. */
fun placeGroupColor(group: String): androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(
    when (group) {
        "Food" -> 0xFFF08A24
        "Cafe" -> 0xFFC2762B
        "Shop" -> 0xFFD99A00
        "Health" -> 0xFFE5484D
        "Education", "University" -> 0xFF9A7553
        "Culture" -> 0xFF9B6AD6
        "Park" -> 0xFF3E9A4E
        "Leisure" -> 0xFF1E9E95
        "Hotel" -> 0xFF5B6BD6
        "Transport" -> 0xFF2F7BE5
        else -> 0xFF7A8699
    }
)

