package com.example.konstanz.ui.search

import com.example.konstanz.data.Texts
import com.example.konstanz.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.konstanz.data.PlaceKind as SavedKind
import com.example.konstanz.data.RecentSearch
import com.example.konstanz.data.SavedStore
import com.example.konstanz.data.SavedPlace
import com.example.konstanz.data.transit.Place
import com.example.konstanz.data.transit.PlaceKind
import com.example.konstanz.data.transit.SearchFilter
import com.example.konstanz.data.transit.SearchResults
import com.example.konstanz.data.transit.SearchCategory
import com.example.konstanz.data.transit.SearchHit
import com.example.konstanz.data.search.PlaceSearch
import com.example.konstanz.ui.components.placeGroupColor
import com.example.konstanz.ui.components.placeGroupIcon
import com.example.konstanz.ui.components.group
import com.example.konstanz.data.transit.Stop
import com.example.konstanz.data.transit.Transit
import com.example.konstanz.ui.components.BadgeSize
import com.example.konstanz.ui.components.KtChip
import com.example.konstanz.ui.components.LineBadge
import com.example.konstanz.ui.components.SectionLabel
import com.example.konstanz.ui.icons.KonstanzIcon
import com.example.konstanz.ui.icons.KtIcon
import com.example.konstanz.ui.icons.KtIcons
import com.example.konstanz.ui.theme.Background
import com.example.konstanz.ui.theme.Ink
import com.example.konstanz.ui.theme.Ink2
import com.example.konstanz.ui.theme.Ink3
import com.example.konstanz.ui.theme.Ink4
import com.example.konstanz.ui.theme.KonstanzType
import com.example.konstanz.ui.theme.Line
import com.example.konstanz.ui.theme.OnPrimaryTint
import com.example.konstanz.ui.theme.Primary
import com.example.konstanz.ui.theme.PrimaryTint
import com.example.konstanz.ui.theme.Radius
import com.example.konstanz.ui.theme.White
import com.example.konstanz.ui.theme.outlined
import kotlinx.coroutines.delay

/** What a search can lead to. */
data class SearchActions(
    val onBack: () -> Unit,
    val onOpenPlace: (Place) -> Unit,
    val onOpenStop: (Stop) -> Unit,
    val onQuickDestination: (SavedPlace) -> Unit,
    val onEditSaved: () -> Unit,
    val onSeeAllRecent: () -> Unit,
    val onChooseOnMap: () -> Unit,
    /** Choosing a start: "Your location" row at the top. */
    val onMyLocation: () -> Unit = {},
)

/** Filter chips of artboards 08 / 09. "Recent" narrows the empty state to recent searches. */
private enum class Filter(@param:androidx.annotation.StringRes val label: Int, val search: SearchFilter?) {
    All(R.string.filter_all, SearchFilter.All), Places(R.string.places, SearchFilter.Places), Stops(R.string.bus_stops, SearchFilter.Stops),
    Addresses(R.string.addresses, SearchFilter.Addresses), Recent(R.string.recent, null),
}

/** A recent search with what it points to, for the rows of artboard 08. */
private data class RecentEntry(val search: RecentSearch, val subtitle: String, val place: Place?, val stop: Stop?)

/** 08 Search / 09 Search results. Everything runs on the local (mock) data, instantly. */
@Composable
fun SearchRoute(initialQuery: String, actions: SearchActions, pickMode: Boolean = false, offerMyLocation: Boolean = false) {
    val repo = Transit.repository
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var filter by rememberSaveable { mutableStateOf(Filter.All) }
    // A category picked ("Cafés"): everything of that kind, nearest first. Typing leaves it.
    var categoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val shortcuts = remember { repo.searchShortcuts() }
    var knownCategories by remember { mutableStateOf(shortcuts) }
    val category = categoryKey?.let { k -> knownCategories.firstOrNull { it.key == k } }
    // Belt-and-braces: normally already warm from app start (Transit.init); coalesces for free if so.
    LaunchedEffect(Unit) { repo.warmSearch() }
    // Small debounce so typing stays smooth; results update as you type.
    val results by produceState<SearchResults?>(null, query, filter, category) {
        if (category != null) {
            val hits = repo.nearby(category)
            value = SearchResults(emptyList(), emptyList(), emptyList(), top = hits)
            return@produceState
        }
        if (query.isBlank()) { value = null; return@produceState }
        delay(90)
        value = repo.search(query, filter.search ?: SearchFilter.All).also { r -> knownCategories = (knownCategories + r.categories).distinct() }
    }
    val recent by produceState(emptyList<RecentEntry>(), SavedStore.recent.toList()) {
        value = SavedStore.recent.take(4).map { r ->
            if (r.isStop) {
                val stop = repo.stopByName(r.name)
                RecentEntry(r, stop?.let { Texts.get(R.string.bus_stop) + " · " + it.lines.joinToString(" · ") } ?: Texts.get(R.string.bus_stop), null, stop)
            } else {
                val place = repo.placeByName(r.name)
                // Drop postcode and town: "Universitätsstraße 10, 78464 Konstanz" → "Universitätsstraße 10".
                RecentEntry(r, place?.address?.replace(Regex(", \\d{5} .*$"), "") ?: r.whenLabel, place, null)
            }
        }
    }

    // Opening a result remembers the query (for "Back to search") and records a recent search.
    val now = stringResource(R.string.today_at, repo.now().format())
    val tracked = actions.copy(
        onOpenPlace = { place ->
            SearchSession.lastQuery = query
            SavedStore.addRecent(place.name, isStop = false, whenLabel = now)
            actions.onOpenPlace(place)
        },
        onOpenStop = { stop ->
            SearchSession.lastQuery = query
            SavedStore.addRecent(stop.name, isStop = true, whenLabel = now)
            actions.onOpenStop(stop)
        },
    )

    SearchScreen(
        query = query,
        onQueryChange = { query = it; categoryKey = null },
        category = category,
        onCategory = { c ->
            knownCategories = (knownCategories + c).distinct()
            categoryKey = c.key
            query = c.label
        },
        shortcuts = shortcuts,
        filter = filter,
        onFilterChange = { filter = it },
        results = results,
        saved = SavedStore.places,
        recent = recent,
        actions = tracked,
        pickMode = pickMode,
        offerMyLocation = offerMyLocation,
    )
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    category: SearchCategory?,
    onCategory: (SearchCategory) -> Unit,
    shortcuts: List<SearchCategory>,
    filter: Filter,
    onFilterChange: (Filter) -> Unit,
    results: SearchResults?,
    saved: List<SavedPlace>,
    recent: List<RecentEntry>,
    actions: SearchActions,
    /** Choosing a start / destination for the planner: no "Choose on map". */
    pickMode: Boolean = false,
    offerMyLocation: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(White).statusBarsPadding().imePadding()) {
        // Back + search field.
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier.size(48.dp).clip(Radius.Button).clickable(role = Role.Button, onClickLabel = stringResource(R.string.back_to_map), onClick = actions.onBack),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.ChevronLeft, contentDescription = stringResource(R.string.back_to_map), size = 22.dp, tint = Ink)
            }
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { keyboard?.hide() },
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Filter.entries.forEach { f -> KtChip(stringResource(f.label), { onFilterChange(f) }, selected = f == filter) }
                }
            }
            if (offerMyLocation && category == null) item {
                ResultRow(
                    icon = KtIcons.Locate,
                    tint = com.example.konstanz.ui.theme.Location,
                    title = AnnotatedString(stringResource(R.string.your_location)),
                    subtitle = { Text(stringResource(R.string.start_from_here), style = subtitleStyle(), color = Ink3) },
                    trailing = {},
                    onClick = actions.onMyLocation,
                )
            }
            if (results == null) {
                emptyState(filter, saved, recent, actions, pickMode)
            } else if (category != null) {
                nearbyList(category, results, actions)
            } else {
                resultList(query, results, actions, onCategory = { keyboard?.hide(); onCategory(it) }, onQueryChange = onQueryChange)
            }
        }

        // Offline note at the bottom.
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KonstanzIcon(if (results == null) KtIcons.Database else KtIcons.CloudOff, contentDescription = null, size = 16.dp, tint = Ink3)
            Text(
                stringResource(if (results == null) R.string.search_offline_note else R.string.search_offline_results),
                style = KonstanzType.Caption,
                color = Ink3,
            )
        }
    }
}

/** 52 dp field with a 2 dp red outline, red magnifier, clear button. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val searchLabel = stringResource(R.string.search)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .height(52.dp)
            .border(2.dp, Primary, shape)
            .clip(shape)
            .background(White)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KonstanzIcon(KtIcons.Search, contentDescription = null, size = 20.dp, tint = Primary, strokeWidth = 2.6f)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.search_hint), style = KonstanzType.Body, color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = KonstanzType.Body.copy(color = Ink),
                cursorBrush = SolidColor(Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabel },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Background)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.clear_search)) { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(KtIcons.Close, contentDescription = stringResource(R.string.clear_search), size = 20.dp, tint = Ink2)
            }
        }
    }
}

// ---------- 08: nothing typed yet ----------

private fun androidx.compose.foundation.lazy.LazyListScope.emptyState(
    filter: Filter,
    saved: List<SavedPlace>,
    recent: List<RecentEntry>,
    actions: SearchActions,
    pickMode: Boolean,
) {
    if (filter != Filter.Recent) {
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel(stringResource(R.string.tab_saved), trailing = { TextLink(stringResource(R.string.edit), actions.onEditSaved) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    saved.take(4).forEach { place ->
                        SavedTile(place, Modifier.weight(1f)) { actions.onQuickDestination(place) }
                    }
                }
            }
        }
    }
    // No heading without recent searches; the Recent filter says so instead of showing nothing.
    if (recent.isNotEmpty()) item {
        SectionLabel(
            stringResource(R.string.recent),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 4.dp),
            trailing = { TextLink(stringResource(R.string.see_all), actions.onSeeAllRecent) },
        )
    } else if (filter == Filter.Recent) item {
        Text(
            stringResource(R.string.no_recent),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp),
            style = KonstanzType.BodySmall,
            color = Ink3,
        )
    }
    items(recent, key = { it.search.id }) { entry ->
        ResultRow(
            icon = if (entry.search.isStop) KtIcons.Bus else KtIcons.Pin,
            stopStyle = entry.search.isStop,
            title = AnnotatedString(entry.search.name),
            subtitle = { Text(entry.subtitle, style = subtitleStyle(), color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailing = { KonstanzIcon(KtIcons.History, contentDescription = null, size = 18.dp, tint = Ink4) },
            onClick = {
                when {
                    entry.stop != null -> actions.onOpenStop(entry.stop)
                    entry.place != null -> actions.onOpenPlace(entry.place)
                }
            },
        )
    }
    if (filter != Filter.Recent && !pickMode) {
        item {
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 22.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .outlined(Radius.Button)
                    .clip(Radius.Button)
                    .clickable(role = Role.Button, onClick = actions.onChooseOnMap)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KonstanzIcon(KtIcons.Map, contentDescription = null, size = 20.dp, tint = Primary)
                Text(stringResource(R.string.choose_on_map), style = KonstanzType.RowTitle, color = Ink)
            }
        }
    }
}

@Composable
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp),
        style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.Bold),
        color = Primary,
    )
}

/** Grey tile with a white circle icon: Home / University / Work / Bahnhof. */
@Composable
private fun SavedTile(place: SavedPlace, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(44.dp).background(White, CircleShape), contentAlignment = Alignment.Center) {
            KonstanzIcon(savedIcon(place.kind), contentDescription = null, size = 22.dp, tint = Primary)
        }
        Text(place.displayName, style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun savedIcon(kind: SavedKind): KtIcon = when (kind) {
    SavedKind.Home -> KtIcons.Home
    SavedKind.Work -> KtIcons.Brief
    SavedKind.University -> KtIcons.Cap
    SavedKind.Stop, SavedKind.Other -> KtIcons.Star
}

// ---------- 09: results ----------

private fun androidx.compose.foundation.lazy.LazyListScope.resultList(
    query: String,
    results: SearchResults,
    actions: SearchActions,
    onCategory: (SearchCategory) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    // "Cafés nearby" for "caf", before the places.
    items(results.categories, key = { "c-" + it.key }) { c ->
        ResultRow(
            icon = placeGroupIcon(c.group),
            tint = placeGroupColor(c.group),
            title = AnnotatedString(c.label),
            subtitle = { Text(stringResource(R.string.nearby_nearest), style = subtitleStyle(), color = Ink3) },
            trailing = { KonstanzIcon(KtIcons.ChevronRight, contentDescription = null, size = 18.dp, tint = Ink4) },
            onClick = { onCategory(c) },
        )
    }
    results.didYouMean?.let { suggestion ->
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.did_you_mean) + " ", style = KonstanzType.BodySmall, color = Ink2)
                Text(
                    suggestion,
                    Modifier.clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button) { onQueryChange(suggestion) },
                    style = KonstanzType.BodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text("?", style = KonstanzType.BodySmall, color = Ink2)
            }
        }
    }
    if (results.isEmpty) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.no_results, query.trim()), style = KonstanzType.Body.copy(fontWeight = FontWeight.Bold), color = Ink)
                Text(
                    stringResource(R.string.no_results_hint),
                    style = KonstanzType.BodySmall, color = Ink3,
                )
            }
        }
        return
    }
    // The design sample answers in three lists; the real data in one ranked list.
    val hits = results.top.ifEmpty {
        results.places.map { SearchHit(it, null, it.distanceKm) } +
            results.stops.map { (stop, km) -> SearchHit(null, stop, km) } +
            results.addresses.map { SearchHit(it, null, it.distanceKm) }
    }
    items(hits, key = { h -> h.stop?.let { "s-" + it.id } ?: ("p-" + h.place!!.id) }) { hit ->
        HitRow(hit, query, actions)
    }
}

/** Everything of one kind, nearest first. */
private fun androidx.compose.foundation.lazy.LazyListScope.nearbyList(category: SearchCategory, results: SearchResults, actions: SearchActions) {
    item {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(28.dp).background(placeGroupColor(category.group), CircleShape), contentAlignment = Alignment.Center) {
                KonstanzIcon(placeGroupIcon(category.group), contentDescription = null, size = 16.dp, tint = White)
            }
            Text(stringResource(R.string.category_nearby, category.label), style = KonstanzType.Body.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
            Text("· ${results.top.size}", style = KonstanzType.Body, color = Ink3)
        }
    }
    if (results.top.isEmpty()) item {
        Text(stringResource(R.string.nothing_of_kind), Modifier.padding(16.dp), style = KonstanzType.BodySmall, color = Ink3)
    }
    items(results.top, key = { h -> h.stop?.let { "s-" + it.id } ?: ("p-" + h.place!!.id) }) { hit -> HitRow(hit, "", actions) }
}

@Composable
private fun HitRow(hit: SearchHit, query: String, actions: SearchActions) {
    val stop = hit.stop
    val place = hit.place
    if (stop != null) {
        ResultRow(
            icon = KtIcons.Bus,
            stopStyle = true,
            title = highlight(stop.name, query),
            subtitle = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.bus_stop) + " ·", style = subtitleStyle(), color = Ink3)
                    stop.lines.take(5).forEach { LineBadge(it, size = BadgeSize.S) }
                }
            },
            trailing = { Distance(hit.distanceKm) },
            onClick = { actions.onOpenStop(stop) },
        )
    } else if (place != null) {
        val group = place.kind.group()
        ResultRow(
            icon = placeGroupIcon(group),
            tint = placeGroupColor(group),
            title = highlight(place.name, query),
            subtitle = {
                val what = place.category ?: place.kind.label
                val where = place.address.removePrefix(stringResource(R.string.street) + " · ")
                Text(
                    listOf(what, where).filter { it.isNotEmpty() }.distinct().joinToString(" · "),
                    style = subtitleStyle(), color = Ink3, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            },
            trailing = { Distance(hit.distanceKm) },
            onClick = { actions.onOpenPlace(place) },
        )
    }
}

@Composable
private fun Section(title: String) {
    SectionLabel(title, Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 2.dp))
}

@Composable
private fun Distance(km: Double) {
    Text(
        if (km < 1.0) "${(km * 1000).toInt().coerceAtLeast(10) / 10 * 10} m" else String.format(java.util.Locale.ROOT, "%.1f km", km),
        style = KonstanzType.BodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        color = Ink3,
        maxLines = 1,
    )
}

/** The typed words in bold where they start a word of the name: "**Bahnhof**platz", "**Mark**tstätte". */
private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    val typed = PlaceSearch.tokens(query)
    append(text)
    if (typed.isEmpty()) return@buildAnnotatedString
    Regex("[\\p{L}\\p{N}]+").findAll(text).forEach { word ->
        val folded = PlaceSearch.tokens(word.value).firstOrNull() ?: return@forEach
        val t = typed.filter { folded.startsWith(it) }.maxByOrNull { it.length } ?: return@forEach
        // Folding can change the length (ß → ss, ä → a): mark about as many letters as typed.
        val len = t.length.coerceAtMost(word.value.length)
        addStyle(SpanStyle(color = OnPrimaryTint, fontWeight = FontWeight.ExtraBold), word.range.first, word.range.first + len)
    }
}

@Composable
private fun subtitleStyle() = KonstanzType.BodySmall.copy(fontSize = 14.sp)

/** 64 dp result row: 40 dp tile (pink for stops, grey otherwise), title, subtitle, trailing. */
@Composable
private fun ResultRow(
    icon: KtIcon,
    title: AnnotatedString,
    subtitle: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    stopStyle: Boolean = false,
    /** A coloured round icon (the kind of place); null = grey tile. */
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(
                    tint ?: if (stopStyle) PrimaryTint else Background,
                    if (tint != null) CircleShape else Radius.Small,
                ),
                contentAlignment = Alignment.Center,
            ) {
                KonstanzIcon(icon, contentDescription = null, size = 20.dp, tint = if (tint != null) White else if (stopStyle) Primary else Ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = KonstanzType.Body.copy(fontWeight = FontWeight.Bold), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle()
            }
            trailing()
        }
        HorizontalDivider(thickness = 1.dp, color = Line)
    }
}
