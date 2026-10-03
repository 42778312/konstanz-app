package com.example.konstanz

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.konstanz.data.PlaceKind
import com.example.konstanz.data.SavedPlace
import com.example.konstanz.data.RecentSearch
import com.example.konstanz.data.SavedStop
import com.example.konstanz.data.toEntity
import com.example.konstanz.data.toModel
import com.example.konstanz.data.user.UserDao
import com.example.konstanz.data.user.UserDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private val fixturePlaces = listOf(
        SavedPlace("home", "Home", PlaceKind.Home, address = null, placeholder = "Add your home address"),
        SavedPlace("uni", "University", PlaceKind.University, address = "Universitätsstraße 10", travelMinutes = 16, targetId = "uni"),
        SavedPlace("work", "Work", PlaceKind.Work, address = null, placeholder = "Add your work address"),
    )
    private val fixtureStops = listOf(SavedStop("sternenplatz", "Sternenplatz", "14:41", "12"), SavedStop("laube", "Laube", "14:37", "5"))
    private val fixtureRecent = listOf(RecentSearch("r1", "Universität Konstanz", false, "Today, 08:12"), RecentSearch("r2", "Konstanz Bahnhof", true, "Today, 07:55"))

    private lateinit var db: UserDatabase
    private lateinit var dao: UserDao

    @Before
    fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), UserDatabase::class.java).build()
        dao = db.userDao()
    }

    @After
    fun close() = db.close()

    @Test
    fun seedRoundTripsAllFieldsInOrder() = runBlocking {
        dao.replacePlaces(fixturePlaces.mapIndexed { i, p -> p.toEntity(i) })
        dao.replaceStops(fixtureStops.mapIndexed { i, s -> s.toEntity(i) })
        dao.replaceRecent(fixtureRecent.mapIndexed { i, r -> r.toEntity(i) })

        assertEquals(fixturePlaces, dao.places().map { it.toModel() })
        assertEquals(fixtureStops, dao.stops().map { it.toModel() })
        assertEquals(fixtureRecent, dao.recent().map { it.toModel() })
        assertEquals(fixturePlaces.size, dao.placeCount())
    }

    @Test
    fun replaceRemovesAndReorders() = runBlocking {
        dao.replacePlaces(fixturePlaces.mapIndexed { i, p -> p.toEntity(i) })
        val changed = listOf(fixturePlaces[2], fixturePlaces[0], SavedPlace("p-1", "Hafen", PlaceKind.Other, "Hafenstraße"))
        dao.replacePlaces(changed.mapIndexed { i, p -> p.toEntity(i) })

        assertEquals(changed, dao.places().map { it.toModel() })
    }

    @Test
    fun clearRecentEmptiesOnlyRecent() = runBlocking {
        dao.replaceStops(fixtureStops.mapIndexed { i, s -> s.toEntity(i) })
        dao.replaceRecent(fixtureRecent.mapIndexed { i, r -> r.toEntity(i) })
        dao.replaceRecent(emptyList())

        assertEquals(0, dao.recent().size)
        assertEquals(fixtureStops.size, dao.stops().size)
    }

    @Test
    fun unknownKindFallsBackToOther() = runBlocking {
        dao.insertPlaces(listOf(fixturePlaces[1].toEntity(0).copy(kind = "Gym")))
        assertEquals(PlaceKind.Other, dao.places().single().toModel().kind)
    }
}
