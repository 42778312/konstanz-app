package com.example.konstanz.data.search

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Search quality on the real Konstanz index (assets/search, built by tools/places-import/build_search.py). */
class PlaceSearchTest {
    private val file = File("src/main/assets/${PlaceSearch.ASSET}")
    private val search = PlaceSearch { file.inputStream() }
    // Konstanz Bahnhof.
    private val lat = 47.6588
    private val lon = 9.1771

    private fun top(q: String, n: Int = 5): List<String> = runBlocking {
        search.search(q, lat, lon, limit = n).map { "${it.entry.name} [${it.entry.group}/${it.entry.category}]" + if (it.fuzzy) " ~" else "" }
    }.also { println("$q → $it") }

    @Test
    fun findsWhatPeopleType() {
        assumeTrue("search index not built", file.exists())
        assertTrue(top("htwg").first().startsWith("HTWG"))
        assertTrue(top("uni").any { it.contains("Universität Konstanz") })
        assertTrue(top("marktst").first().startsWith("Marktstätte"))
        assertTrue(top("bahnhofstr").first().startsWith("Bahnhofstraße"))
        assertTrue(top("münster").any { it.contains("Münster") })
        assertTrue(top("muenster").any { it.contains("Münster") })
        assertTrue(top("apotheke").all { it.contains("Health") })
        assertTrue(top("pharmacy").all { it.contains("Health") })
        assertTrue(top("pizza").isNotEmpty())
        assertTrue(top("lago").any { it.contains("LAGO", ignoreCase = true) })
        assertTrue(top("mainau").first().contains("Mainau"))
    }

    @Test
    fun forgivesTypos() {
        assumeTrue("search index not built", file.exists())
        assertTrue(top("bahnhfo").first().contains("Bahnhof"))
        assertTrue(top("univrsitat").first().contains("Universität"))
        assertTrue(top("marktstate").any { it.contains("Marktstätte") })
    }

    @Test
    fun findsHouseNumbers() {
        assumeTrue("search index not built", file.exists())
        assertTrue(top("bahnhofplatz 1").first().startsWith("Bahnhofplatz 1"))  // 10, 12 … (no number 1 mapped)
        assertTrue(top("schottenstr 1").first().contains("Schottenstraße"))
        // No number 12 mapped on Schottenstraße: the street, not a typo guess (Schützenstraße 12).
        assertTrue(top("schottenstr 12").first().startsWith("Schottenstraße"))
    }

    @Test
    fun printsSamples() {
        assumeTrue("search index not built", file.exists())
        listOf(
            "cafe", "kaffee", "schule", "gymnasium", "sea life", "seerhein", "rewe", "edeka", "dm", "kino", "zahnarzt",
            "bodensee therme", "konzil", "petershausen", "kreuzlingen", "imperia", "pizzeria", "döner", "sushi", "post",
            "rathaus", "klinikum",
        ).forEach { top(it, 4) }
    }
}
