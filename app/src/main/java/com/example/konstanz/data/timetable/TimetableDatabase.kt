package com.example.konstanz.data.timetable

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

// The offline timetable for the Konstanz area, built from the NVBW GTFS feed by tools/gtfs-import
// and shipped in assets/timetable.db. Read-only in the app; an update swaps the whole file
// (Architecture artboard: atomic swap). Ids are small integers given by the importer; the GTFS ids
// are kept for realtime matching later. Times are minutes after midnight of the service day
// (≥ 1440 = after midnight), dates are yyyyMMdd integers.

/** A station: all platforms of one stop area ("Konstanz Universität", Bstg 1–3). */
@Entity(tableName = "station")
data class StationRow(
    @PrimaryKey val id: Int,
    /** Full feed name, "Konstanz Universität". */
    val name: String,
    /** Without the town prefix for stops inside Konstanz, "Universität". */
    @ColumnInfo(name = "short_name") val shortName: String,
    val lat: Double,
    val lon: Double,
    /** Lowercase, accents folded (ä → a, ß → ss), for search. */
    @ColumnInfo(name = "search_name") val searchName: String,
)

/** A platform where buses stop; belongs to a [StationRow]. */
@Entity(tableName = "platform", indices = [Index("station_id")])
data class PlatformRow(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "station_id") val stationId: Int,
    @ColumnInfo(name = "gtfs_id") val gtfsId: String,
    /** "1", "2", … or null when the feed gives none. */
    val code: String?,
    val lat: Double,
    val lon: Double,
    /** Which way buses leave from here, "Towards city centre" (City of Konstanz stop register). */
    val direction: String?,
)

@Entity(tableName = "route")
data class RouteRow(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "gtfs_id") val gtfsId: String,
    /** "12", "S6", "RE 2". */
    @ColumnInfo(name = "short_name") val shortName: String,
    @ColumnInfo(name = "long_name") val longName: String,
    /** GTFS route_type: 2 rail, 3 bus, 4 ferry. */
    val type: Int,
    val agency: String,
    /** Konstanz city bus (Stadtwerke), shown first. */
    @ColumnInfo(name = "is_city") val isCity: Boolean,
)

@Entity(tableName = "trip", indices = [Index("route_id"), Index("service_id")])
data class TripRow(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "gtfs_id") val gtfsId: String,
    @ColumnInfo(name = "route_id") val routeId: Int,
    @ColumnInfo(name = "service_id") val serviceId: Int,
    val headsign: String,
    val direction: Int,
    /** The street path it drives ([ShapeRow]), null when the feed has none. */
    @ColumnInfo(name = "shape_id") val shapeId: Int?,
)

/** A trip's path on the streets, clipped to the Konstanz area and simplified to ~2 m. */
@Entity(tableName = "shape")
data class ShapeRow(
    @PrimaryKey val id: Int,
    /** Encoded polyline (Google format, 1e-5 degrees): lat/lon pairs. */
    val points: String,
)

/** One call of a trip at a platform. Only calls inside the Konstanz area are kept. */
@Entity(
    tableName = "stop_time",
    primaryKeys = ["trip_id", "seq"],
    indices = [Index(value = ["platform_id", "departure"])],
)
data class StopTimeRow(
    @ColumnInfo(name = "trip_id") val tripId: Int,
    val seq: Int,
    @ColumnInfo(name = "platform_id") val platformId: Int,
    val arrival: Int,
    val departure: Int,
)

/** The dates a service runs on (calendar + calendar_dates, expanded by the importer). */
@Entity(tableName = "service_date", primaryKeys = ["service_id", "date"], indices = [Index("date")])
data class ServiceDateRow(
    @ColumnInfo(name = "service_id") val serviceId: Int,
    val date: Int,
)

/**
 * A place to search for and travel to, from OpenStreetMap (tools/places-import): a named place
 * (university, museum, pier …), a street, a district or a town.
 */
@Entity(tableName = "place", indices = [Index(value = ["slug"], unique = true), Index("kind")])
data class PlaceRow(
    @PrimaryKey val id: Int,
    /** Stable id for links and saved places, "universitaet-konstanz". */
    val slug: String,
    val name: String,
    /** University, Library, Station, Harbour, Square, Venue, Street, District or Town. */
    val kind: String,
    /** What it is, "Museum", "Theatre"; for a Town its country, "Germany". */
    val detail: String,
    /** Where it is, "Altstadt, Konstanz". */
    val area: String,
    val lat: Double,
    val lon: Double,
    @ColumnInfo(name = "search_name") val searchName: String,
    /** Importance for ordering search results (100 = landmark). */
    val rank: Int,
)

/** Points along a street, every ~40 m: "what's here?" finds the nearest street with them. */
@Entity(tableName = "street_point", indices = [Index("lat"), Index("place_id")])
data class StreetPointRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "place_id") val placeId: Int,
    val lat: Double,
    val lon: Double,
)

/** Feed version, validity, build time, attribution. */
@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val key: String, val value: String)

/** A departure joined with its trip and route, for stop boards. */
data class DepartureRow(
    @ColumnInfo(name = "trip_id") val tripId: Int,
    val seq: Int,
    val departure: Int,
    @ColumnInfo(name = "platform_code") val platformCode: String?,
    @ColumnInfo(name = "platform_direction") val platformDirection: String?,
    val line: String,
    val headsign: String,
)

/** A call joined with its station, for trip views. */
data class TripCallRow(
    val seq: Int,
    @ColumnInfo(name = "station_id") val stationId: Int,
    val name: String,
    val arrival: Int,
    val departure: Int,
    @ColumnInfo(name = "platform_code") val platformCode: String?,
    @ColumnInfo(name = "platform_direction") val platformDirection: String?,
)

/** A call of a running trip at a station, for the router. */
data class CallRow(
    @ColumnInfo(name = "trip_id") val tripId: Int,
    @ColumnInfo(name = "station_id") val stationId: Int,
    val arrival: Int,
    val departure: Int,
)

/** A line calling at a station. */
data class StationLineRow(
    @ColumnInfo(name = "station_id") val stationId: Int,
    @ColumnInfo(name = "route_id") val routeId: Int,
)

@Dao
interface TimetableDao {
    @Query("SELECT * FROM station ORDER BY short_name")
    suspend fun stations(): List<StationRow>

    @Query("SELECT * FROM station WHERE id = :id")
    suspend fun station(id: Int): StationRow?

    @Query("SELECT * FROM platform WHERE station_id = :stationId ORDER BY code")
    suspend fun platforms(stationId: Int): List<PlatformRow>

    @Query("SELECT * FROM route ORDER BY is_city DESC, short_name")
    suspend fun routes(): List<RouteRow>

    /** Lines calling at a station on any day, city buses first. */
    @Query(
        """SELECT DISTINCT r.* FROM route r
           JOIN trip t ON t.route_id = r.id
           JOIN stop_time st ON st.trip_id = t.id
           JOIN platform p ON p.id = st.platform_id
           WHERE p.station_id = :stationId
           ORDER BY r.is_city DESC, r.short_name"""
    )
    suspend fun routesAt(stationId: Int): List<RouteRow>

    /**
     * Departures at a station on [date] from minute [from] on. The last call of a trip is left out
     * (nobody boards there).
     */
    @Query(
        """SELECT st.trip_id, st.seq, st.departure, p.code AS platform_code, p.direction AS platform_direction,
                  r.short_name AS line, t.headsign
           FROM stop_time st
           JOIN platform p ON p.id = st.platform_id
           JOIN trip t ON t.id = st.trip_id
           JOIN route r ON r.id = t.route_id
           JOIN service_date sd ON sd.service_id = t.service_id AND sd.date = :date
           WHERE p.station_id = :stationId AND st.departure >= :from
             AND EXISTS (SELECT 1 FROM stop_time n WHERE n.trip_id = st.trip_id AND n.seq > st.seq)
           ORDER BY st.departure
           LIMIT :limit"""
    )
    suspend fun departures(stationId: Int, date: Int, from: Int, limit: Int): List<DepartureRow>

    @Query("SELECT * FROM trip WHERE id = :id")
    suspend fun trip(id: Int): TripRow?

    @Query("SELECT * FROM route WHERE id = :id")
    suspend fun route(id: Int): RouteRow?

    @Query("SELECT * FROM shape WHERE id = :id")
    suspend fun shape(id: Int): ShapeRow?

    @Query(
        """SELECT st.seq, p.station_id, s.short_name AS name, st.arrival, st.departure, p.code AS platform_code,
                  p.direction AS platform_direction
           FROM stop_time st
           JOIN platform p ON p.id = st.platform_id
           JOIN station s ON s.id = p.station_id
           WHERE st.trip_id = :tripId ORDER BY st.seq"""
    )
    suspend fun tripCalls(tripId: Int): List<TripCallRow>

    /** Every call of every trip running on [date], trip by trip in order (connection scan input). */
    @Query(
        """SELECT st.trip_id, p.station_id, st.arrival, st.departure
           FROM stop_time st
           JOIN platform p ON p.id = st.platform_id
           JOIN trip t ON t.id = st.trip_id
           JOIN service_date sd ON sd.service_id = t.service_id AND sd.date = :date
           ORDER BY st.trip_id, st.seq"""
    )
    suspend fun callsOn(date: Int): List<CallRow>

    @Query(
        """SELECT DISTINCT p.station_id, t.route_id FROM stop_time st
           JOIN platform p ON p.id = st.platform_id
           JOIN trip t ON t.id = st.trip_id"""
    )
    suspend fun stationLines(): List<StationLineRow>

    @Query("SELECT * FROM platform")
    suspend fun allPlatforms(): List<PlatformRow>

    /** Places whose folded name contains [query]; names starting with it first, then by importance. */
    @Query(
        """SELECT * FROM place WHERE search_name LIKE '%' || :query || '%' AND kind NOT IN ('District', 'Town')
           ORDER BY (search_name LIKE :query || '%') DESC, rank DESC, length(name)
           LIMIT :limit"""
    )
    suspend fun searchPlaces(query: String, limit: Int): List<PlaceRow>

    @Query("SELECT * FROM place WHERE slug = :slug")
    suspend fun placeBySlug(slug: String): PlaceRow?

    @Query("SELECT * FROM place WHERE search_name = :folded AND kind NOT IN ('District', 'Town') ORDER BY rank DESC LIMIT 1")
    suspend fun placeByName(folded: String): PlaceRow?

    @Query("SELECT * FROM place WHERE kind IN ('District', 'Town')")
    suspend fun areas(): List<PlaceRow>

    @Query("SELECT * FROM place WHERE kind NOT IN ('Street', 'District', 'Town') ORDER BY rank DESC LIMIT :limit")
    suspend fun landmarks(limit: Int): List<PlaceRow>

    @Query("SELECT * FROM place WHERE id = :id")
    suspend fun placeById(id: Int): PlaceRow?

    @Query(
        """SELECT * FROM street_point
           WHERE lat BETWEEN :south AND :north AND lon BETWEEN :west AND :east"""
    )
    suspend fun streetPointsIn(south: Double, north: Double, west: Double, east: Double): List<StreetPointRow>

    @Query("SELECT value FROM meta WHERE `key` = :key")
    suspend fun meta(key: String): String?
}

@Database(
    entities = [StationRow::class, PlatformRow::class, RouteRow::class, TripRow::class,
        StopTimeRow::class, ServiceDateRow::class, MetaRow::class, ShapeRow::class, PlaceRow::class, StreetPointRow::class],
    version = 4,
    exportSchema = true,
)
abstract class TimetableDatabase : RoomDatabase() {
    abstract fun timetableDao(): TimetableDao

    companion object {
        const val ASSET = "timetable.db"

        @Volatile private var instance: TimetableDatabase? = null

        /** Closes the timetable and deletes the copy: the next [get] copies it fresh from the APK (repair). */
        fun reset(context: Context) = synchronized(this) {
            instance?.close()
            instance = null
            context.applicationContext.deleteDatabase(ASSET)
        }

        /** Opens the shipped timetable; the first call copies it out of the APK. */
        fun get(context: Context): TimetableDatabase =
            instance ?: synchronized(this) {
                instance ?: open(context.applicationContext).also { instance = it }
            }

        private fun open(app: Context): TimetableDatabase {
            // The copy is only a cache of the asset: after an install or update (which may ship a
            // newer timetable with the same schema) copy it again.
            val copy = app.getDatabasePath(ASSET)
            val installed = app.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime
            if (copy.exists() && copy.lastModified() < installed) app.deleteDatabase(ASSET)
            return Room.databaseBuilder(app, TimetableDatabase::class.java, ASSET)
                .createFromAsset(ASSET)
                // A newer schema: replace the copy, it holds no user data.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
