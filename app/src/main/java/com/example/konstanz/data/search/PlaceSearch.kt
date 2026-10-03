package com.example.konstanz.data.search

import android.content.Context
import com.example.konstanz.R
import com.example.konstanz.data.Texts
import com.example.konstanz.data.transit.Geo
import com.example.konstanz.data.transit.SearchCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.Normalizer
import java.util.zip.GZIPInputStream
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/** One thing to find: a place, street, address, district, town — or a bus stop the repository adds. */
class SearchEntry(
    val id: String,
    val name: String,
    /** Food, Cafe, Shop, Health, Education, University, Culture, Park, Leisure, Hotel, Transport, Service,
     *  Square, Street, Address, District, Town, Stop. */
    val group: String,
    /** "Pharmacy", "Café", "Street"… */
    val category: String,
    /** "Altstadt, Konstanz". */
    val area: String,
    val lat: Double,
    val lon: Double,
    /** Importance, 0–100. */
    val rank: Int,
    keywords: String,
    /** Street address of a place, "Marktstätte 1". */
    val address: String,
    /** [category] in German ("Apotheke"). */
    val categoryDe: String = category,
) {
    /** [category] in the phone's language (English or German). */
    val localCategory: String get() = if (java.util.Locale.getDefault().language == "de") categoryDe else category

    internal val tokens: Array<String> = PlaceSearch.tokens(name).toTypedArray()
    internal val compact: String = tokens.joinToString("")
    internal val keywords: Array<String> = PlaceSearch.tokens("$category $categoryDe $keywords").distinct().toTypedArray()
}

/** A search answer: the entry, and whether it only matched with a typo allowed. */
class SearchMatch(val entry: SearchEntry, val score: Double, val meters: Double, val fuzzy: Boolean, val byCategory: Boolean)


/**
 * Offline search over everything in Konstanz (assets/search/places.tsvz, built by
 * tools/places-import/build_search.py): matches word beginnings as you type, several words in any order,
 * small typos, German and English category words ("Apotheke", "pharmacy"), and house numbers;
 * ranks by how well it matches, how important the place is and how close it is.
 */
class PlaceSearch(private val open: () -> InputStream) {
    constructor(context: Context) : this({ context.assets.open(ASSET) })

    private var entries: List<SearchEntry>? = null
    private var byId: Map<String, SearchEntry> = emptyMap()
    private val mutex = Mutex()

    suspend fun entries(): List<SearchEntry> = entries ?: mutex.withLock {
        entries ?: withContext(Dispatchers.IO) {
            runCatching { open().use { read(it) } }.onFailure { android.util.Log.e("PlaceSearch", "index not loaded", it) }.getOrDefault(emptyList())
        }.also { list -> entries = list; byId = list.associateBy { it.id } }
    }

    suspend fun byId(id: String): SearchEntry? { entries(); return byId[id] }

    suspend fun byName(name: String): SearchEntry? {
        val f = fold(name.trim())
        return entries().filter { fold(it.name) == f && it.group != "District" && it.group != "Town" }.maxByOrNull { it.rank }
    }

    /**
     * Best matches for [query] around (lat, lon), [extra] = more entries to search with (bus stops).
     * [groups] limits the answer (null = everything).
     */
    suspend fun search(
        query: String, lat: Double, lon: Double, extra: List<SearchEntry> = emptyList(),
        groups: Set<String>? = null, limit: Int = 30,
    ): List<SearchMatch> = withContext(Dispatchers.Default) {
        val q = tokens(query)
        if (q.isEmpty()) return@withContext emptyList()
        val all = entries() + extra
        val withNumber = q.any { t -> t.any { it.isDigit() } }
        fun run(words: List<String>, fuzzy: Boolean, addresses: Boolean) = all.asSequence()
            .filter { groups == null || it.group in groups }
            // House addresses only when a number is typed ("Schottenstraße 12").
            .filter { it.group != "Address" || addresses || groups == setOf("Address") }
            .mapNotNull { e -> score(e, words, words.joinToString(""), fuzzy, lat, lon) }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
        var found = run(q, fuzzy = false, addresses = withNumber)
        // A house number that isn't mapped ("Schottenstraße 12"): the street itself, before guessing at typos.
        if (withNumber && found.size < 6) {
            val words = q.filterNot { t -> t.any { it.isDigit() } }
            if (words.isNotEmpty()) found = found + run(words, fuzzy = false, addresses = false).filter { f -> found.none { it.entry === f.entry } }
        }
        if (found.size >= 6) found.take(limit)
        else (found + run(q, fuzzy = true, addresses = withNumber).filter { f -> found.none { it.entry === f.entry } }).take(limit)
    }

    /** Everything of one category (its key word) by distance: "Cafés nearby". */
    suspend fun nearby(key: String, lat: Double, lon: Double, limit: Int = 40): List<SearchMatch> = withContext(Dispatchers.Default) {
        val k = tokens(key).firstOrNull() ?: return@withContext emptyList()
        entries().asSequence()
            .filter { e -> e.keywords.any { it == k } }
            .map { SearchMatch(it, 0.0, Geo.meters(lat, lon, it.lat, it.lon), fuzzy = false, byCategory = true) }
            .sortedBy { it.meters }
            .take(limit)
            .toList()
    }

    /** Categories whose words start like the query ("caf" → Cafés, "apo" → Pharmacies). */
    fun categories(query: String): List<SearchCategory> {
        val q = fold(query.trim())
        if (q.length < 3) return emptyList()
        return CATEGORIES.filter { c -> c.words.any { it.startsWith(q) } || fold(c.category.label).startsWith(q) }.map { it.category }.take(3)
    }

    private fun score(e: SearchEntry, q: List<String>, whole: String, fuzzy: Boolean, lat: Double, lon: Double): SearchMatch? {
        var total = 0.0
        var usedFuzzy = false
        var nameHits = 0
        for (t in q) {
            var best = 0.0
            var bestIsName = false
            var bestFuzzy = false
            for (n in e.tokens) {
                val s = when {
                    n == t -> 10.0
                    n.startsWith(t) -> 7.0 + 2.0 * t.length / n.length
                    t.length >= 3 && n.contains(t) -> 4.0
                    // "11" typed, "11a" there.
                    else -> 0.0
                }
                if (s > best) { best = s; bestIsName = true; bestFuzzy = false }
            }
            if (best == 0.0 && t.length >= 3 && e.compact.contains(t)) { best = 3.5; bestIsName = true }
            if (best < 6.0) {
                for (k in e.keywords) {
                    val s = when {
                        k == t -> 6.5
                        t.length >= 3 && k.startsWith(t) -> 5.0
                        else -> 0.0
                    }
                    if (s > best) { best = s; bestIsName = false; bestFuzzy = false }
                }
            }
            if (best == 0.0 && fuzzy && t.length >= 4 && !t.any { it.isDigit() }) {
                val allowed = if (t.length >= 7) 2 else 1
                for (n in e.tokens) {
                    val d = typoDistance(t, n, allowed)
                    if (d <= allowed) { val s = 6.5 - 1.5 * d; if (s > best) { best = s; bestIsName = true; bestFuzzy = true } }
                }
                if (best == 0.0) for (k in e.keywords) {
                    val d = typoDistance(t, k, 1)
                    if (d <= 1 && k.length >= 4) { val s = 1.5; if (s > best) { best = s; bestIsName = false; bestFuzzy = true } }
                }
            }
            if (best == 0.0) return null
            total += best
            if (bestIsName) nameHits++
            if (bestFuzzy) usedFuzzy = true
        }
        if (fuzzy && !usedFuzzy) return null // found by the strict pass already
        val meters = Geo.meters(lat, lon, e.lat, e.lon)
        // Whole name typed from its start ("bahnhofpl" → Bahnhofplatz) beats words found anywhere.
        if (e.compact.startsWith(whole)) total += 12.0
        if (e.compact == whole) total += 8.0
        if (nameHits == q.size && e.tokens.size == q.size) total += 3.0
        val byCategory = nameHits == 0 && !usedFuzzy
        total += e.rank / 10.0
        total += (if (byCategory) 14.0 else 7.0) * exp(-meters / (if (byCategory) 1200.0 else 2500.0))
        total += when (e.group) {
            "Stop" -> 2.5
            "Town" -> 3.0
            "District" -> 1.5
            "Address" -> 4.0
            else -> 0.0
        }
        return SearchMatch(e, total, meters, usedFuzzy, byCategory)
    }

    companion object {
        const val ASSET = "search/places.tsvz"

        /** Lowercase, ß → ss, accents removed: "Bäckerei" → "backerei"; also "ae"/"oe"/"ue" spelt out match. */
        fun fold(text: String): String {
            val lower = text.lowercase().replace("ß", "ss")
            return Normalizer.normalize(lower, Normalizer.Form.NFD).replace(MARKS, "")
        }

        private val MARKS = Regex("\\p{Mn}+")
        private val SPLIT = Regex("[^\\p{L}\\p{N}]+")

        /** Folded words; "str." → "strasse", umlauts written as "ae" match too ("Muenster" = "Münster"). */
        fun tokens(text: String): List<String> = fold(text)
            .replace("ae", "a").replace("oe", "o").replace("ue", "u")
            .split(SPLIT)
            .filter { it.isNotEmpty() }
            .map { if (it == "str") "strasse" else it }

        /**
         * Typos between what's typed and the start of a word (Damerau–Levenshtein on the same length,
         * ±1): "bahnhfo" ~ "bahnhof", "univrsitat" ~ "universitat". Stops early past [max].
         */
        fun typoDistance(typed: String, word: String, max: Int): Int {
            if (abs(word.length - typed.length) > max && word.length < typed.length) return max + 1
            var best = max + 1
            for (len in (typed.length - 1)..(typed.length + 1)) {
                if (len < 1 || len > word.length) continue
                best = min(best, damerau(typed, word.substring(0, len), max))
            }
            return best
        }

        private fun damerau(a: String, b: String, max: Int): Int {
            val n = a.length; val m = b.length
            if (abs(n - m) > max) return max + 1
            var prev2 = IntArray(m + 1)
            var prev = IntArray(m + 1) { it }
            var cur = IntArray(m + 1)
            for (i in 1..n) {
                cur[0] = i
                var rowMin = cur[0]
                for (j in 1..m) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = min(v, prev2[j - 2] + 1)
                    cur[j] = v
                    if (v < rowMin) rowMin = v
                }
                if (rowMin > max) return max + 1
                val t = prev2; prev2 = prev; prev = cur; cur = t
            }
            return prev[m]
        }

        internal fun read(input: InputStream): List<SearchEntry> {
            val out = ArrayList<SearchEntry>(60_000)
            GZIPInputStream(input, 1 shl 16).bufferedReader(Charsets.UTF_8).forEachLine { line ->
                val c = line.split('\t')
                if (c.size >= 10) out += SearchEntry(
                    id = c[0], name = c[1], group = c[2], category = c[3], area = c[4],
                    lat = c[5].toDouble(), lon = c[6].toDouble(), rank = c[7].toInt(), keywords = c[8], address = c[9],
                    categoryDe = c.getOrNull(10) ?: c[3],
                )
            }
            return out
        }

        private class Cat(val key: String, @param:androidx.annotation.StringRes val label: Int, val group: String, val words: List<String>) {
            /** Built when asked, so the label is in the phone's current language. */
            val category: SearchCategory get() = SearchCategory(key, Texts.get(label), group)
        }

        private fun cat(key: String, @androidx.annotation.StringRes label: Int, group: String, vararg words: String) =
            Cat(key, label, group, words.map(::fold) + fold(key))

        /** Shortcuts on the empty search screen and suggestions while typing. Keys are keywords in the index. */
        private val CATEGORIES = listOf(
            cat("restaurant", R.string.cat_restaurant, "Food", "essen", "food", "eat", "dinner", "lunch"),
            cat("cafe", R.string.cat_cafe, "Cafe", "kaffee", "coffee", "café"),
            cat("supermarket", R.string.cat_supermarket, "Shop", "supermarkt", "groceries", "lebensmittel", "einkaufen"),
            cat("bakery", R.string.cat_bakery, "Cafe", "bäckerei", "bäcker", "brot"),
            cat("pharmacy", R.string.cat_pharmacy, "Health", "apotheke", "medicine"),
            cat("doctor", R.string.cat_doctor, "Health", "arzt", "praxis", "hausarzt"),
            cat("hospital", R.string.cat_hospital, "Health", "krankenhaus", "klinikum", "notaufnahme"),
            cat("school", R.string.cat_school, "Education", "schule", "gymnasium", "grundschule"),
            cat("university", R.string.cat_university, "University", "universität", "hochschule", "uni"),
            cat("library", R.string.cat_library, "Education", "bibliothek", "bücherei"),
            cat("park", R.string.cat_park, "Park", "garten", "grün"),
            cat("playground", R.string.cat_playground, "Park", "spielplatz"),
            cat("hotel", R.string.cat_hotel, "Hotel", "übernachtung", "hostel"),
            cat("atm", R.string.cat_atm, "Service", "geldautomat", "cash", "bargeld"),
            cat("bank", R.string.cat_bank, "Service", "sparkasse"),
            cat("parking", R.string.cat_parking, "Transport", "parkplatz", "parkhaus"),
            cat("toilet", R.string.cat_toilet, "Service", "toilette", "wc"),
            cat("pizza", R.string.cat_pizza, "Food", "pizzeria"),
            cat("ice", R.string.cat_ice, "Cafe", "eis", "eisdiele", "gelato"),
            cat("bar", R.string.cat_bar, "Food", "kneipe", "pub", "drinks"),
            cat("museum", R.string.cat_museum, "Culture"),
            cat("church", R.string.cat_church, "Culture", "kirche"),
            cat("beach", R.string.cat_beach, "Leisure", "strand", "strandbad", "baden"),
            cat("gym", R.string.cat_gym, "Leisure", "fitness", "fitnessstudio"),
            cat("drugstore", R.string.cat_drugstore, "Shop", "drogerie", "dm", "rossmann"),
            cat("post", R.string.cat_post, "Service", "postamt", "dhl", "packstation"),
            cat("fuel", R.string.cat_fuel, "Transport", "tankstelle", "petrol"),
            cat("dentist", R.string.cat_dentist, "Health", "zahnarzt"),
            cat("kindergarten", R.string.cat_kindergarten, "Education", "kita"),
            cat("cinema", R.string.cat_cinema, "Culture", "kino"),
        )

        /** The shortcuts shown before typing. */
        val SHORTCUTS: List<SearchCategory> get() = listOf("restaurant", "cafe", "supermarket", "pharmacy", "bakery", "school", "park", "atm")
            .map { k -> CATEGORIES.first { it.key == k }.category }
    }
}
