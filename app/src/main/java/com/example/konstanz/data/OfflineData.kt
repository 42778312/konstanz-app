package com.example.konstanz.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.konstanz.data.timetable.TimetableDatabase
import com.example.konstanz.data.transit.Transit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** How usable the data on the phone is. */
enum class DataHealth { Ok, ExpiresSoon, Expired, Damaged }

/**
 * What is stored for offline use (Settings → Offline data). Everything ships inside the app: new
 * timetables and maps arrive with app updates, there is no separate download.
 */
data class OfflineDataInfo(
    val health: DataHealth,
    /** Date of the NVBW timetable feed. */
    val timetableDate: LocalDate?,
    /** Last day the timetable covers. */
    val validUntil: LocalDate?,
    /** Date of the OpenStreetMap data in the map. */
    val mapDate: LocalDate?,
    val timetableBytes: Long,
    val mapBytes: Long,
    /** Saved places, stops and recent searches. */
    val savedBytes: Long,
    val today: LocalDate,
) {
    val daysLeft: Long? get() = validUntil?.let { ChronoUnit.DAYS.between(today, it) }
    val totalBytes: Long get() = timetableBytes + mapBytes + savedBytes
}

/** Reads and repairs the offline data. [info] is observable; call [refresh] at start and after changes. */
object OfflineData {
    /** Days before the end of the timetable when the app starts warning. */
    private const val WARN_DAYS = 14
    private const val MAP_COPY = "map/konstanz.pmtiles"

    var info by mutableStateOf<OfflineDataInfo?>(null)
        private set

    suspend fun refresh(context: Context) {
        val app = context.applicationContext
        info = withContext(Dispatchers.IO) { read(app) }
    }

    /**
     * Throws away the phone's copies of timetable and map and makes them again from the app: fixes a
     * damaged file without any download. Saved places are not touched. True when the data works again.
     */
    suspend fun repair(context: Context): Boolean {
        val app = context.applicationContext
        withContext(Dispatchers.IO) {
            TimetableDatabase.reset(app)
            mapFile(app, fresh = true)
        }
        Transit.reload(app)
        AppStatus.simulateDamagedTimetable = false
        refresh(app)
        return info?.health != DataHealth.Damaged
    }

    /**
     * The map tiles as a file (MapLibre reads them by byte ranges, which it can't do inside the APK).
     * Copied from the app on first use, after an app update, or when [fresh]. The copy is written
     * next to the old one and renamed over it, so a map that is open keeps working.
     */
    fun mapFile(context: Context, fresh: Boolean = false): File {
        val app = context.applicationContext
        val copy = File(app.filesDir, MAP_COPY)
        val installed = app.packageManager.getPackageInfo(app.packageName, 0).lastUpdateTime
        if (fresh || !copy.exists() || copy.lastModified() < installed) {
            copy.parentFile?.mkdirs()
            val tmp = File(copy.path + ".tmp")
            app.assets.open(MAP_COPY).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(copy)) { "cannot replace $copy" }
        }
        return copy
    }

    private suspend fun read(app: Context): OfflineDataInfo {
        val today = LocalDate.now()
        val meta = runCatching {
            val dao = TimetableDatabase.get(app).timetableDao()
            // A real query: a damaged copy fails here, not later on a stop sheet.
            check(dao.stations().isNotEmpty()) { "empty timetable" }
            Triple(dao.meta("feed_version"), dao.meta("feed_end"), dao.meta("map_date"))
        }.getOrNull()
        val validUntil = meta?.second?.let(::parseDate)
        val health = when {
            meta == null || AppStatus.simulateDamagedTimetable -> DataHealth.Damaged
            validUntil != null && today.isAfter(validUntil) -> DataHealth.Expired
            validUntil != null && ChronoUnit.DAYS.between(today, validUntil) <= WARN_DAYS -> DataHealth.ExpiresSoon
            else -> DataHealth.Ok
        }
        fun size(file: File) = if (file.exists()) file.length() else 0L
        fun dbSize(name: String) = listOf("", "-wal", "-shm").sumOf { size(app.getDatabasePath(name + it)) }
        return OfflineDataInfo(
            health = health,
            timetableDate = meta?.first?.let(::parseDate),
            validUntil = validUntil,
            mapDate = meta?.third?.let(::parseDate),
            timetableBytes = dbSize(TimetableDatabase.ASSET),
            mapBytes = size(File(app.filesDir, MAP_COPY)),
            savedBytes = dbSize("user.db"),
            today = today,
        )
    }

    private fun parseDate(yyyymmdd: String): LocalDate? =
        runCatching { LocalDate.parse(yyyymmdd, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
}

/** "12 Dec 2026". */
fun LocalDate.display(): String = format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

/** "3.6 MB", "240 KB". */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1e6)
    else -> "${(bytes / 1_000).coerceAtLeast(1)} KB"
}
