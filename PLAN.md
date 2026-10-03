# Konstanz Transit — build plan

Source design: https://claude.ai/artifact/MPAe8ghQWPWK29U7GE92EW (39 artboards, 390×844 phone frames).
Stack: Kotlin · Jetpack Compose · Material3 (re-themed) · Navigation Compose.
Rule for every step: it must **build and run** before we tick it.

---

## Part A — Foundation: design system in Compose  *(first milestone)*

- [x] A1. Clean `MainActivity`: remove the `Greeting` template and leave an empty `KonstanzApp()` root.
- [x] A2. Bump old deps in `libs.versions.toml` (core-ktx, activity-compose, lifecycle) and add `navigation-compose`.
- [x] A3. Bundle fonts offline in `res/font/`: **Bricolage Grotesque** (600/700/800) and **Figtree** (400–800).
- [x] A4. `Color.kt` → brand tokens: Primary `#D4003B`, Pressed `#A8002F`, Tint `#FCE8EE`, White.
- [x] A5. `Color.kt` → neutrals: Ink `#16181D`, Ink2 `#474C55`, Ink3 `#646973`, Line `#E7E8EB`, Bg `#F4F5F7`.
- [x] A6. `Color.kt` → status: Location `#1F6FEB`, Live `#0A7A43`/`#E3F4EA`, Delayed `#9A4A00`/`#FFF0DC`, Detour `#6B2FB3`.
- [x] A7. `Color.kt` → map palette: Land `#F1EEE9`, Water `#C9DFEC`, Park `#DDE9D3`, Building `#E5E0D8`, Road casing `#E2DDD4`.
- [x] A8. `Type.kt` → Display (Bricolage 800), TitleL 800/28, Title 800/22, Body 600/17, Time 800/20 **tabular numbers**, Label 800/13 caps.
- [x] A9. `Shapes/Spacing` tokens: radius 7/12/14/16/24/pill; spacing on a 4 px grid (4…48).
- [x] A10. `Theme.kt`: map the tokens to a light `MaterialTheme`, drop dynamic color, expose `KonstanzTheme.colors` for non-Material tokens.
- [x] A11. Icon set: port the ~35 stroke icons (24 px, 2 px stroke) as `ImageVector`s — start with the 10 needed for Parts B–C (bus, pin, map, calendar, nav, check, chevron, back, database, info).
- [x] A12. Component `PrimaryButton` (52 dp, 14 dp radius) + `SecondaryButton`.
- [x] A13. Component `LineBadge` (red tile with line number, 7 dp radius).
- [x] A14. Component `StatusChip` (On time / +N min / Scheduled / Cancelled / Live / Stale — icon + word + color).
- [x] A15. Component `DepartureRow` (time, struck-through old time, badge, destination, chip, chevron).
- [x] A16. Component `SectionLabel` + `SettingsRow` (grouped white card rows).
- [x] A17. A `@Preview` "design system" screen showing all of the above to compare with artboard 00.

## Part B — Launch & onboarding  *(artboards 01–06)*

- [x] B1. Set up `NavHost` with routes: `splash`, `onboarding`, `main` (placeholder).
- [x] B2. **Splash** (01): red background, decorative route curves, logo tile, title, subtitle, progress bar + "Loading local data".
- [x] B3. Android 12+ `SplashScreen` API so the system splash matches (red + logo).
- [x] B4. Reusable `OnboardingPage` scaffold: illustration card (top, 28 dp radius), page dots, headline, body, bottom button, Skip.
- [x] B5. **Onb1 Welcome** (02) — "Get Started".
- [x] B6. **Onb2 Offline maps** (03).
- [x] B7. **Onb3 Public transport** (04) — reuses `DepartureRow` + `StatusChip` in the card.
- [x] B8. **Onb4 Location** (05) — request location permission (allow / not now).
- [x] B9. **Onb5 Offline data ready** (06) — checklist Map / Schedules / Stops / Routing → "Finish Setup".
- [x] B10. `HorizontalPager` swipe between pages + animated dot indicator; Skip goes to `main`.
- [x] B11. Persist "onboarding done" in DataStore so it shows only once.

## Part C — Static screens that need no data

- [x] C1. Bottom navigation shell: Map · Saved · Settings.
- [x] C2. **Settings** (27).
- [x] C3. **About** (35).
- [x] C4. **Saved places** (28) and **Recent** (29) with fake data.

## Part D — Mock transit data (design first)

The real timetable database is postponed (see Part K). Screens read a `TransitRepository` interface;
today it is filled with the design's own sample data, later a database replaces it without touching the UI.

- [x] D1. Domain models: stops, lines, departures with realtime states, trips, places, search results, journeys & legs, data status (`data/transit/TransitModels.kt`).
- [x] D2. `TransitRepository` interface with plain suspend functions + one `Transit.repository` access point.
- [x] D3. Mock network from the artboards: 19 stops with map positions, lines 12 / 5 / 8 / 9 and their stop sequences, alerts, places, addresses (`MockTransitData.kt`).
- [x] D4. Konstanz Bahnhof departures exactly as artboards 12–14 (every realtime state); generated timetables for all other stops.
- [x] D5. Search ("Univ" → artboard 09), nearby stops, trip details (artboard 15), 3 journeys My location → Universität (artboards 18–23), "no route" case (30).
- [x] D6. Optional artificial delay to show loading states (33 / 33b).
- [x] D7. 12 unit tests proving the mock returns what the design shows.

## Part E — Main map (artboards 07, 10)

Uses the design's own Konstanz map (`assets/maps/city.svg`) as a pannable map; a real map engine comes in Part K.

- [x] E1. City map from artboard 07 with pan, pinch-zoom, double-tap zoom, clamped to the city; camera kept across tab switches.
- [x] E2. Stops from the mock data: bus tiles close up, dots further out, clusters far out (overlapping stops merge); 44 dp hit areas; district / water labels; my-location puck; "Bahnhof" label.
- [x] E3. Top: "Where do you want to go?" field, 56 dp menu button, "Offline · Using saved data" pill, layers button.
- [x] E4. Zoom +/− group and "center on my location" (animated, centred in the visible map above the sheet).
- [x] E5. Home sheet: grabber, From/To card with rail and swap, quick-destination chips from Saved.
- [x] E6. Taps wired: menu → Settings tab; search, stops, chips, pill, layers → "coming in Part …" until those screens exist.
- [x] E7. **Map location selection** (10): long-press (or tap "From") drops a pin; sheet shows place, coordinates, nearest stop; Set as start / destination fills the From/To card; bottom bar hides while picking.

## Part F — Stops & departures (artboards 12–15, 33b)

- [x] F1. Departure rows show platform; "Last known · 6 min ago" / "Stale · 18 min old" chips; loading shimmer; data → row mapping.
- [x] F2. **Stop sheet** (12): tap a stop → map flies in, selected 44 dp marker with pointer, sheet with 3 next departures (cancelled left out), All departures / Route from here; bottom bar hides; × or Back closes.
- [x] F3. **Loading** (33b): timetable shows at once, "Checking live times · showing timetable" + shimmer, live times follow.
- [x] F4. Save stop (star) → appears under Saved → Stops.
- [x] F5. **Full stop page** (13): mini map, 5 departures, "View all departures", lines serving the stop, service alert, last updated / refresh, Route from here.
- [x] F6. **All departures** (14): line filter chips, day/time selector, hour groups, legend.
- [x] F7. **Departure details** (15): big time + platform, status chips, stop list with line rail, Show on map / Ride this bus.
- [x] F8. Wire everything (sheet → full page → all departures → details; lines → filtered list).

## Part G — Search (artboards 08, 09, 11)

- [x] G1. **Search** (08): red-outlined field (keyboard opens), filter chips, Saved tiles + Edit, Recent + See all, Choose on map, offline note.
- [x] G2. **Results** (09) while typing: Places / Bus stops / Addresses, typed part highlighted, line badges, distances; "no results" message.
- [x] G3. **Location details** (11) on the map: pin, back + name bar, kind / name / address · km, Route here / From here / save, nearby stops.
- [x] G4. Wiring: map search field & "To" open Search; results return to the map (place → details, stop → stop sheet); Choose on map → pin mode; Edit → Saved tab; See all → Recent; opened results are added to Recent; Recent places open on the map.

## Part H — Route planning (artboards 16–20, 30, 33)

- [x] H1. Route shapes in the mock (design's exact paths for the fastest route), night rule for "no route", Insel Mainau.
- [x] H2. Route layer: bus red 7 dp on white casing, walking dots, grey alternatives — constant on-screen widths.
- [x] H3. **Plan your journey** (16): own map screen, From / To card with swap, Now / preference buttons, Find routes.
- [x] H4. **When & how** (17): Now / Depart at / Arrive by, scrolling day · hour · minute wheel, Prefer radios, step-free; defaults from Settings.
- [x] H5. **Finding routes** (33): three progress steps + shimmer cards.
- [x] H6. **Routes** (18): option cards (best outlined red), legs strip, first departure + status, transfers / walking.
- [x] H7. **Route map** (19): selected route drawn, legend, bottom card swipeable between options, Details / Start journey.
- [x] H8. **Your journey** (20): timeline with walk / bus segments, collapsible stops, Directions, share, Start journey.
- [x] H9. **No route found** (30): Change destination / Change departure time.
- [x] H10. Entry points: map chips, Saved "Go", search tiles, location Route here / From here, stop "Route from here", pin Set as start / destination; From / To pick through Search.

## Part I — Live journey (artboards 21–23b)

> **Removed (2026-10-02):** the app only shows times and routes, it doesn't guide a trip live. Start journey, Directions and "Ride this bus" are gone with the journey screen; route details end with an arrival summary instead. Tapping a walk in route details shows just that walk on the map (from / to, times), Back returns to the details.

- [x] I1. Journey screen: one step per leg (walk → bus → walk) then arrived; map framing per step; screen stays on; Back steps back.
- [x] I2. **Walking** (21): red turn-by-turn card with "Then …", dotted walk, callout at the stop, Recenter, 3-part progress, next-bus card, End / Journey overview.
- [x] I3. **Bus** (22): dark bus card with live status, bus marker + "Next:" callout, next stop, stops left / arrival / ride, upcoming stops with "Get off", "I got off the bus".
- [x] I4. **Final walk** (23): instructions to the entrance, "Arrival 14:48 · on schedule".
- [x] I5. **Arrived** (23b): check, summary, Save place / Done.
- [x] I6. Wired: planner Start journey & Directions → journey; End → map; Journey overview → planner.

## Part J — Offline, sync & error states (artboards 24–26, 31, 32, 34, 34b)

- [x] J1. App status: real internet connection + location permission; realtime / damaged data / failing updates simulated (Settings → Developer → Simulate states).
- [x] J2. **Offline mode** (24): "You're offline" banner; status pill Offline / Up to date / Updating · % / Update failed.
- [x] J3. **Offline data** (25): status card, data rows, storage bar, auto-update & mobile-data switches, Check now / Update data.
- [x] J4. **Update progress** (26): per-part progress, keeps running in the background, Cancel / Try again / Back to map; success bumps the data version.
- [x] J5. **Location unavailable** (31): card with Select on map / Enable location, "Choose starting point", no location dot.
- [x] J6. **Realtime unavailable** (32): stop sheet banner + scheduled departures.
- [x] J7. **Timetable needs repair** (34b): repair sheet → update; "Continue with map only".
- [x] J8. **Error states** (34): reusable `ErrorCard`, all 8 states in the design-system screen.
- [x] J9. Wiring: pill → Offline data; Settings rows (status, sync, storage, update) → pages; Simulate states screen.

## Part K — Real data & real map

The screens stay as they are: everything real plugs in behind `TransitRepository` and the saved-data store.

**K-a · Your data survives restarts (no downloads needed)**
- [x] K1. Add Room + KSP.
- [x] K2. User database: saved places, saved stops, recent searches (tables + DAOs).
- [x] K3. Saved / Recent read from and write to the database (the sample content is only the first-run seed).
- [x] K4. Tests for the user database.

**K-b · Real timetable (needs the NVBW VHB feed, ~37 MB download)**
- [x] K5. Timetable schema (`data/timetable/TimetableDatabase.kt`): station, platform, route, trip, stop_time, service_date (calendar expanded to dates), meta. Shapes left out for now (route lines are drawn stop to stop).
- [x] K6. `tools/gtfs-import/import_gtfs.py`: reads `vhb.zip`, keeps the Konstanz area, merges "Bahnhof" + "Bahnhof (Bus)", unifies line names, writes `assets/timetable.db` (2.4 MB) from Room's exported schema.
- [x] K7. `DatabaseTransitRepository`: real stops, lines, departures (incl. after-midnight trips), trips, stop search. Switch: Settings → Developer → Simulate states → "Real timetable (NVBW)" (off = design sample).
- [x] K8. On-device router (Connection Scan, `ConnectionScanRouter.kt`): walking to/from stops, transfers on foot ≤ 400 m, up to 4 distinct options.
- [x] K9. Attribution "Datensatz der NVBW GmbH" in About and Licenses.

Follow-ups for real data: realtime feed (departures show "Scheduled"), real places/addresses (still the design's), route-screen camera framing fitted to the real route, making the real timetable + map the default.

**K-c · Real map (needs offline map tiles for Konstanz)**
- [x] K10. MapLibre 13.6 with an offline Konstanz tile file (`assets/map/konstanz.pmtiles`, 7.3 MB, Protomaps/OpenStreetMap) styled in the design's map colours (`tools/map/make_style.py` → `assets/map/style.json`). Shown with the real timetable; the design sample keeps the drawing.
- [x] K11. Markers, routes and sheets on the real map: same composables, one shared camera (map units are now local Web Mercator, `Geo`). Rides follow the buses' real street paths (GTFS shapes, timetable schema v2); route screens show only the stops along the routes. "© OpenStreetMap" credit on the map.
- [x] K12. Real GPS for "My location" (`LocationWatcher`, platform LocationManager, works offline). Outside the map area it falls back to the design's spot at the Bahnhof.

## Part L · Real data everywhere

- [x] L1. Real timetable + real map are the default (Developer switch still shows the design sample). The planner fits the camera to the real route above each sheet; the map opens on the user.
- [x] L2. Real places and addresses: ~1,240 named places and ~1,560 streets of the area, read from the offline map tiles (OpenStreetMap) by `tools/places-import` into the timetable database (schema v3); offline search, place sheets and planning use them. The design's place ids ("uni") map to the real places.
- [x] L3. "What's here?": nearest street, district, town and country ("Fischmarkt, Altstadt · Konstanz, Germany"); a dropped pin keeps its exact point as start or destination.
- [x] L5. City of Konstanz stop register (`tools/gtfs-import/data/bushaltestellen_konstanz.geojson`, optional input of the timetable import): platform directions ("Towards city centre", "Out of town") in departures, planner and journey; surveyed platform positions; spelled-out stop names ("Sternenplatz/Spanierstraße"). 185 of 264 platforms matched (the rest: Swiss and regional stops).
- [ ] L4. Live departure times (needs a realtime feed — to be chosen; the city register links each stop to the EFA-BW live departure board of MobiData BW, a candidate).

## Part M · Production readiness

Audit (30 Sep 2026): 7 settings that do nothing, 3 duplicate rows, 6 "coming soon" actions, fake offline-data
numbers, a crash on Android 7, no release build setup.

**M-a · Crashes and build safety**
- [x] M1. Android 7 crash: java.time needs API 26 → core library desugaring (lint NewApi, 14 places).
- [x] M2. Release build: R8 code + resource shrinking; the release build was installed and used on the emulator (map, search, place sheet: no crash).
- [x] M3. Lint: 0 errors; warnings fixed. Left on purpose: Kotlin 2.4 and Gradle 9.8 updates (toolchain changes, not for a release pass).

**M-b · Duplicates and options that do nothing**
- [x] M4. Settings → About: "Data sources", "Licenses", "Privacy policy" all opened About → each opens its own page.
- [x] M5. Settings → Offline data: 4 rows for one page → one row with the real status.
- [x] M6. Remove settings that do nothing: Language, Units, Theme, Map style, Walking paths, Analytics.
- [x] M7. Map menu button only switched to the Settings tab (already in the bottom bar) → removed.
- [x] M8. Map layers button ("coming soon") → real layers sheet: bus stops on/off (moved from Settings, one place only).
- [x] M9. Recent searches: one place (Search), the developer-only duplicate page removed from navigation.

**M-c · Finish every feature**
- [x] M10. Offline data page with real facts: timetable date and validity, map date, real sizes; expiry warning.
  The simulated update is removed (data comes with app updates; no server yet).
- [x] M11. Saved: set Home / Work and "Add a place" through Search.
- [x] M12. Departure details: "Show on map" draws the trip on the map.
- [x] M13. Departure details: "Ride this bus" and the journey's bus details open the trip.
- [x] M14. Privacy policy page in the app (the app collects nothing, it has no internet permission). "Send feedback" appears once a support e-mail is set (`AppInfo.SUPPORT_EMAIL`) — *needs your address*.
- [x] M15. Walk-only option (≤ 20 min, or about as quick as the bus); "Arrive by" and other days now really work (they were ignored).
- [x] M15b. Removed "Step-free access": the NVBW feed has no usable wheelchair data for Konstanz (all city trips marked "not accessible", all stops "unknown").
- [x] M15c. Saved: first run starts with empty Home/Work only (the design's sample places, stops and recents were fake); Go buttons show live journey times, saved stops their real next departure.
- [x] M15e. Search: no empty "Recent" heading on a new install; stop name "Egg Egg/Universität" fixed.
- [x] M15d. Ring lines (e.g. 9) pass a stop twice: the trip page and "Ride this bus" use the pass that was tapped.

**M-d · Robustness**
- [x] M16. Damaged data is detected (a real query at start) and repaired for real: fresh copies from the app, saved places kept, works offline. Backup rules: only the user's own data.
- [x] M17. Location: denied → card with "Select on map"; outside the map area → falls back to the Bahnhof; live position otherwise.
- [x] M18. Process death restores the screen; rotation kept state but landscape hid controls → phones locked to portrait (large screens unaffected).
- [x] M19. Dark system theme: status-bar icons turned white on white → fixed (always dark icons). Font size 130 %: fine.
- [ ] M20. Accessibility: TalkBack labels exist on controls; touch targets follow the design's 44 dp (Android recommends 48 dp) — a design decision, left open.
- [x] M21. Performance: cold start 1.3 s on the emulator; route search well under a second (the fake 1.3 s "calculating" delay removed).

**M-e · Release**
- [ ] M22. App id and name (not com.example — Play Store rejects it) — *needs your choice*.
- [ ] M23. Signing: upload key read from keystore.properties (never in the repo) — *you create the key*.
- [x] M24. Version 1.0.0; App Bundle 32.5 MB with all four CPU types (Play sends each phone only its own).
- [x] M25. RELEASE.md: what is ready, what needs you, build steps, Play Console answers, data updates.
- [ ] M26. Final pass: all tests + a manual walk through every screen.

## Part N — Offline walking directions, map style, search (2026-10-02)

- [x] N1. Walking directions on the phone: OSM paths/streets of Konstanz (tools/walk-graph → assets/walk), A* + turn-by-turn steps; walks on the map follow the streets.
- [x] N2. Walk view: blue route with direction arrows, turn markers, step list, step banner with previous / next.
- [x] N3. Map style closer to Apple Maps (tools/map/make_style.py): living streets and pedestrian zones, coloured place dots, building outlines.
- [x] N4. Search everything offline (tools/places-import/fetch_search_osm.py + build_search.py → assets/search/places.tsvz): ~6,400 places, ~1,900 streets, ~22,800 house addresses; prefix, multi-word, typo-tolerant, German/English categories, distance-aware ranking; "Find nearby" shortcuts, category suggestions, "Did you mean".
- [x] N5. Stop sheet "All departures" opens the All departures screen directly.
- [x] N6. Map layers: kinds of places (restaurants, cafés, supermarkets, pharmacies, bakeries, schools, parks, ATMs) switchable on the map, saved in settings; markers thin out and names avoid each other; search no longer shows the "Find nearby" grid.
- [x] N7. Home card "From" opens search (with "Your location") instead of dropping a pin.
- [x] N8. App follows the phone's language: English (default) and German. All text in res/values(-de)/strings.xml; text made outside screens via data/Texts.kt; per-app language on Android 13+ (res/xml/locales_config.xml). Search index carries German category names.
- [x] N9. Map layers: only bus stops (on by default); the place layers were removed. Search still finds any place, street and address.
- [x] N10. Smooth map: bus stops drawn in one Canvas (one tap handler), clustering per zoom step on a grid, overlays read the camera in layout/draw only (no recomposition while panning), app baseline profile (src/main/baseline-prof.txt).
- [x] N11. Bus stops are MapLibre layers of the offline map (ui/map/NativeStopLayer.kt): GeoJSON source with MapLibre clustering, dots, bus tiles, names when zoomed in; taps via queryRenderedFeatures. Same speed as the empty map.
