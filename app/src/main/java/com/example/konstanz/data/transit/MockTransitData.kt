package com.example.konstanz.data.transit

import com.example.konstanz.data.transit.Minutes.Companion.of

/**
 * Sample network taken from the design artboards, so every screen shows exactly what was designed.
 * Positions are the design's map units (the same the onboarding maps use). Times are around the
 * design's "now" of 14:30. Replace with the real timetable later — only this file and
 * MockTransitRepository know it is fake.
 */
internal object MockTransitData {

    val NOW = of(14, 30)

    val lines = listOf(
        Line("12", "Bus 12", "Bahnhof", "Wollmatingen"),
        Line("5", "Bus 5", "Bahnhof", "Allmannsdorf"),
        Line("8", "Bus 8", "Bahnhof", "Universität"),
        Line("9", "Bus 9", "Bahnhof", "Egg"),
    )

    // Positions from artboard 16 (full-city map); lines from 08/09/11/12/28.
    val stops = listOf(
        Stop("bahnhof", "Konstanz Bahnhof", listOf("12", "5", "8", "9"), MapPoint(660f, 868f), platforms = "A–D"),
        Stop("marktstaette", "Marktstätte", listOf("12", "5"), MapPoint(638f, 826f)),
        Stop("laube", "Laube", listOf("12"), MapPoint(565f, 772f)),
        Stop("sternenplatz", "Sternenplatz", listOf("12", "5"), MapPoint(612f, 756f)),
        Stop("benediktinerplatz", "Benediktinerplatz", listOf("12", "8"), MapPoint(600f, 642f)),
        Stop("zaehringerplatz", "Zähringerplatz", listOf("12"), MapPoint(588f, 560f)),
        Stop("universitaet", "Universität", listOf("9", "12"), MapPoint(716f, 284f)),
        Stop("universitaet-sued", "Universität Süd", listOf("8"), MapPoint(700f, 330f)),
        Stop("egg", "Egg", listOf("9"), MapPoint(820f, 212f)),
        Stop("wollmatingen", "Wollmatingen", listOf("12"), MapPoint(180f, 432f)),
        Stop("allmannsdorf", "Allmannsdorf", listOf("5"), MapPoint(790f, 402f)),
        Stop("paradies", "Paradies", listOf("9"), MapPoint(430f, 832f)),
        Stop("doebele", "Döbele", listOf("9"), MapPoint(522f, 862f)),
        Stop("fuerstenberg", "Fürstenberg", listOf("12"), MapPoint(382f, 522f)),
        Stop("petershausen", "Petershausen", listOf("12", "8"), MapPoint(622f, 604f)),
        Stop("emmishofer-tor", "Emmishofer Tor", listOf("9"), MapPoint(626f, 992f)),
        Stop("seestrasse", "Seestraße", listOf("5"), MapPoint(722f, 560f)),
        Stop("schaenzle", "Schänzle", listOf("8"), MapPoint(392f, 690f)),
        Stop("jahnstrasse", "Jahnstraße", listOf("9"), MapPoint(600f, 470f)),
        Stop("klinikum", "Klinikum", listOf("8"), MapPoint(640f, 390f)),
    )

    /** Stop sequences with minutes from the first stop (line 12 matches artboard 15). */
    val patterns: Map<String, List<Pair<String, Int>>> = mapOf(
        "12" to listOf("bahnhof" to 0, "marktstaette" to 1, "laube" to 3, "benediktinerplatz" to 5,
            "zaehringerplatz" to 8, "fuerstenberg" to 13, "wollmatingen" to 20),
        "5" to listOf("bahnhof" to 0, "marktstaette" to 1, "sternenplatz" to 3, "petershausen" to 6,
            "seestrasse" to 9, "allmannsdorf" to 14),
        "8" to listOf("bahnhof" to 0, "benediktinerplatz" to 4, "petershausen" to 6, "klinikum" to 10,
            "universitaet-sued" to 13, "universitaet" to 15),
        "9" to listOf("bahnhof" to 0, "doebele" to 3, "paradies" to 5, "jahnstrasse" to 11,
            "universitaet" to 15, "egg" to 19),
    )

    /** Departures at Konstanz Bahnhof, exactly as on artboards 12–14. */
    val bahnhofDepartures = listOf(
        StopDeparture("t12-1440", "12", "Wollmatingen", of(14, 40), Realtime.OnTime, "A"),
        StopDeparture("t5-1444", "5", "Allmannsdorf", of(14, 44), Realtime.Delayed(3), "B"),
        StopDeparture("t9-1450", "9", "Egg", of(14, 50), Realtime.Cancelled, "C"),
        StopDeparture("t8-1455", "8", "Universität", of(14, 55), Realtime.OnTime, "A"),
        StopDeparture("t9-1500", "9", "Egg", of(15, 0), Realtime.Scheduled, "C"),
        StopDeparture("t12-1502", "12", "Wollmatingen", of(15, 2), Realtime.Detour, "A"),
        StopDeparture("t5-1505", "5", "Allmannsdorf", of(15, 5), Realtime.LastKnown(6)),
        StopDeparture("t8-1510", "8", "Universität", of(15, 10), Realtime.Stale(18)),
        StopDeparture("t9-1515", "9", "Egg", of(15, 15), Realtime.Scheduled, "C"),
        StopDeparture("t12-1520", "12", "Wollmatingen", of(15, 20), Realtime.Scheduled, "A"),
        StopDeparture("t5-1524", "5", "Allmannsdorf", of(15, 24), Realtime.Scheduled, "B"),
        StopDeparture("t8-1525", "8", "Universität", of(15, 25), Realtime.Scheduled, "A"),
    )

    val alerts = listOf(
        ServiceAlert(
            lines = listOf("12"),
            title = "Line 12: detour from 15:00",
            message = "Construction work. Stop Laube is not served; use Sternenplatz instead.",
        ),
    )

    // From artboards 08, 09, 11, 29.
    val places = listOf(
        Place("uni", "Universität Konstanz", PlaceKind.University, "Universitätsstraße 10, 78464 Konstanz", MapPoint(748f, 258f), 3.1),
        Place("uni-library", "Universitätsbibliothek", PlaceKind.Library, "Universität Konstanz", MapPoint(722f, 292f), 3.2),
        Place("bahnhof-place", "Konstanz Bahnhof", PlaceKind.Station, "Bahnhofplatz, Konstanz", MapPoint(666f, 874f), 0.3),
        Place("hafen", "Hafen Konstanz", PlaceKind.Harbour, "Hafenstraße, Konstanz", MapPoint(700f, 860f), 0.6),
        Place("marktstaette-place", "Marktstätte", PlaceKind.Square, "Marktstätte, Konstanz", MapPoint(640f, 820f), 0.4),
        Place("bodenseeforum", "Bodenseeforum", PlaceKind.Venue, "Reichenaustraße 21, Konstanz", MapPoint(470f, 610f), 2.4),
        // No bus reaches it in the mock → artboard 30 "No route found".
        Place("mainau", "Insel Mainau", PlaceKind.Venue, "Mainau, 78465 Konstanz", MapPoint(900f, 150f), 6.8),
    )

    val addresses = listOf(
        Place("addr-uni-str", "Universitätsstraße", PlaceKind.Street, "Street · 78464 Konstanz", MapPoint(716f, 300f), 3.0),
        Place("addr-bahnhofplatz", "Bahnhofplatz", PlaceKind.Street, "Street · 78462 Konstanz", MapPoint(662f, 872f), 0.3),
        Place("addr-hafenstr", "Hafenstraße", PlaceKind.Street, "Street · 78462 Konstanz", MapPoint(698f, 858f), 0.6),
    )

    /** Distance from "my location" to a stop, used in search results. */
    val stopDistanceKm = mapOf("universitaet" to 3.0, "universitaet-sued" to 3.3, "bahnhof" to 0.3, "marktstaette" to 0.4)

    /** District centres, used to name a picked point ("Paradies, Konstanz"). Same places as the map labels. */
    val districts = listOf(
        "Altstadt" to MapPoint(600f, 925f), "Petershausen" to MapPoint(560f, 540f), "Allmannsdorf" to MapPoint(760f, 445f),
        "Paradies" to MapPoint(460f, 880f), "Fürstenberg" to MapPoint(360f, 470f), "Wollmatingen" to MapPoint(250f, 400f),
        "Egg" to MapPoint(850f, 240f), "Kreuzlingen" to MapPoint(560f, 1080f),
    )

    /** Map units → WGS84 (see [Geo]). */
    fun latLon(point: MapPoint): Pair<Double, Double> = Geo.latLon(point)

    /** Where the user is (artboard 05 / 16). */
    val myLocation = MapPoint(612f, 902f)

    // ---------- Journeys: My location → Universität Konstanz (artboards 18–23) ----------

    private val walkToBahnhof = listOf(
        WalkStep("Turn left onto Bahnhofplatz", 80, street = "Bahnhofplatz"),
        WalkStep("Continue straight", 120),
        WalkStep("Konstanz Bahnhof, Platform A", 120),
    )

    private val walkToUniversity = listOf(
        WalkStep("Continue straight on Universitätsstraße", 150, street = "Universitätsstraße"),
        WalkStep("Main entrance on your right", 60),
    )

    // Route shapes from artboards 18–20 (map units).
    private const val WALK_TO_BAHNHOF = "M612 902 C616 902.2 629.3 905 636 903 C642.7 901 648 895.8 652 890 C656 884.2 658.7 871.7 660 868"
    private const val BUS_12_TO_UNI = "M660 868 C656.3 861 646 836.3 638 826 C630 815.7 624.2 815 612 806 C599.8 797 570 782.3 565 772 " +
        "C560 761.7 576.5 754 582 744 C587.5 734 595.3 721 598 712 C600.7 703 597.7 701.7 598 690 C598.3 678.3 601 657 600 642 " +
        "C599 627 594.5 613.7 592 600 C589.5 586.3 583.7 580 585 560 C586.3 540 592.5 503.3 600 480 C607.5 456.7 616.7 443.3 630 420 " +
        "C643.3 396.7 665.7 362.7 680 340 C694.3 317.3 710 293.3 716 284"
    private const val WALK_TO_UNI = "M716 284 C718.7 282 726.7 276.3 732 272 C737.3 267.7 745.3 260.3 748 258"
    // The other options (drawn grey when not selected), simplified.
    private const val WALK_TO_MARKTSTAETTE = "M612 902 L630 870 L638 826"
    private const val BUS_5_TO_PETERSHAUSEN = "M638 826 L612 756 L604 690 L622 604"
    private const val BUS_9_TO_UNI = "M622 604 L610 530 L600 470 L660 380 L716 284"
    private const val WALK_UNI_SHORT = "M716 284 L748 258"
    private const val WALK_TO_STERNENPLATZ = "M612 902 L606 850 L605 800 L612 756"
    private const val BUS_8_TO_UNI_SUED = "M612 756 L600 690 L622 604 L640 540 L640 390 L700 330"
    private const val WALK_FROM_UNI_SUED = "M700 330 L730 300 L748 258"

    fun journeysToUniversity(to: String): List<Journey> = listOf(
        Journey(
            id = "j-fastest",
            from = "My location",
            to = to,
            badge = "Fastest",
            legs = listOf(
                Leg.Walk(of(14, 32), of(14, 36), meters = 320, to = "Konstanz Bahnhof", steps = walkToBahnhof, path = WALK_TO_BAHNHOF),
                Leg.Ride(
                    of(14, 36), of(14, 45), line = "12", direction = "Wollmatingen",
                    from = "Konstanz Bahnhof", platform = "A", to = "Universität", realtime = Realtime.OnTime,
                    stops = listOf(
                        TripStop("marktstaette", "Marktstätte", of(14, 37), of(14, 37)),
                        TripStop("laube", "Laube", of(14, 39), of(14, 39)),
                        TripStop("benediktinerplatz", "Benediktinerplatz", of(14, 41), of(14, 41)),
                        TripStop("universitaet", "Universität", of(14, 45), of(14, 45)),
                    ),
                    path = BUS_12_TO_UNI,
                ),
                Leg.Walk(of(14, 45), of(14, 48), meters = 210, to = to, steps = walkToUniversity, path = WALK_TO_UNI),
            ),
        ),
        Journey(
            id = "j-transfer",
            from = "My location",
            to = to,
            legs = listOf(
                Leg.Walk(of(14, 35), of(14, 37), meters = 150, to = "Marktstätte", path = WALK_TO_MARKTSTAETTE),
                Leg.Ride(
                    of(14, 37), of(14, 43), line = "5", direction = "Allmannsdorf",
                    from = "Marktstätte", platform = null, to = "Petershausen", realtime = Realtime.Delayed(3),
                    stops = listOf(
                        TripStop("sternenplatz", "Sternenplatz", of(14, 39), of(14, 42)),
                        TripStop("petershausen", "Petershausen", of(14, 43), of(14, 46)),
                    ),
                    path = BUS_5_TO_PETERSHAUSEN,
                ),
                Leg.Ride(
                    of(14, 47), of(14, 54), line = "9", direction = "Egg",
                    from = "Petershausen", platform = null, to = "Universität", realtime = Realtime.Scheduled,
                    stops = listOf(
                        TripStop("jahnstrasse", "Jahnstraße", of(14, 50), of(14, 50)),
                        TripStop("universitaet", "Universität", of(14, 54), of(14, 54)),
                    ),
                    path = BUS_9_TO_UNI,
                ),
                Leg.Walk(of(14, 54), of(14, 58), meters = 300, to = to, path = WALK_UNI_SHORT),
            ),
        ),
        Journey(
            id = "j-walk",
            from = "My location",
            to = to,
            legs = listOf(
                Leg.Walk(of(14, 40), of(14, 49), meters = 700, to = "Sternenplatz", path = WALK_TO_STERNENPLATZ),
                Leg.Ride(
                    of(14, 49), of(14, 57), line = "8", direction = "Universität",
                    from = "Sternenplatz", platform = null, to = "Universität Süd", realtime = Realtime.Scheduled,
                    stops = listOf(
                        TripStop("petershausen", "Petershausen", of(14, 51), of(14, 51)),
                        TripStop("klinikum", "Klinikum", of(14, 54), of(14, 54)),
                        TripStop("universitaet-sued", "Universität Süd", of(14, 57), of(14, 57)),
                    ),
                    path = BUS_8_TO_UNI_SUED,
                ),
                Leg.Walk(of(14, 57), of(15, 2), meters = 380, to = to, path = WALK_FROM_UNI_SUED),
            ),
        ),
    )

    /** Destinations the mock can't reach, to show artboard 30 (No route found). */
    val unreachable = setOf("Kreuzlingen", "Mainau")

    /** No buses between 00:30 and 05:00 (artboard 30: "… at 00:40"). */
    fun isNight(time: Minutes): Boolean = (time.value % (24 * 60)) in 30 until 5 * 60

    val dataStatus = DataStatus(
        upToDate = true,
        version = 105,
        lastSync = "Yesterday, 22:14",
        savedAt = "Saved 30 Sep, 09:12",
        storage = "428 MB",
    )
}
