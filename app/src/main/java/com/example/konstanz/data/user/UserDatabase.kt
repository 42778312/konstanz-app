package com.example.konstanz.data.user

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

// The user's own data (Saved, Recent). Kept apart from the timetable database, so a timetable
// update — which replaces that file — can never touch it (Architecture artboard: atomic swap).

@Entity(tableName = "saved_place")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** data.PlaceKind name. */
    val kind: String,
    val address: String?,
    val travelMinutes: Int?,
    val placeholder: String?,
    val targetId: String?,
    /** Display order. */
    val position: Int,
)

@Entity(tableName = "saved_stop")
data class SavedStopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nextDeparture: String,
    val line: String,
    val position: Int,
)

@Entity(tableName = "recent_search")
data class RecentSearchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isStop: Boolean,
    val whenLabel: String,
    /** Newest first. */
    val position: Int,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM saved_place ORDER BY position")
    suspend fun places(): List<SavedPlaceEntity>

    @Query("SELECT * FROM saved_stop ORDER BY position")
    suspend fun stops(): List<SavedStopEntity>

    @Query("SELECT * FROM recent_search ORDER BY position")
    suspend fun recent(): List<RecentSearchEntity>

    @Query("SELECT COUNT(*) FROM saved_place")
    suspend fun placeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaces(rows: List<SavedPlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(rows: List<SavedStopEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(rows: List<RecentSearchEntity>)

    @Query("DELETE FROM saved_place") suspend fun clearPlaces()
    @Query("DELETE FROM saved_stop") suspend fun clearStops()
    @Query("DELETE FROM recent_search") suspend fun clearRecent()

    // The lists are short (a handful of rows), so each change rewrites its whole list in one
    // transaction: order and removals stay exactly as on screen.

    @Transaction
    suspend fun replacePlaces(rows: List<SavedPlaceEntity>) { clearPlaces(); insertPlaces(rows) }

    @Transaction
    suspend fun replaceStops(rows: List<SavedStopEntity>) { clearStops(); insertStops(rows) }

    @Transaction
    suspend fun replaceRecent(rows: List<RecentSearchEntity>) { clearRecent(); insertRecent(rows) }
}

@Database(
    entities = [SavedPlaceEntity::class, SavedStopEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var instance: UserDatabase? = null

        fun get(context: Context): UserDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, UserDatabase::class.java, "user.db")
                    .build()
                    .also { instance = it }
            }
    }
}
