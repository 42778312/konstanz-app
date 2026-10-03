package com.example.konstanz.data.transit

import android.content.Context
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.konstanz.data.timetable.DatabaseTransitRepository

/** Where the network data comes from (Settings → Developer → Simulate states). */
enum class DataSource(val label: String) {
    /** The design's sample network: every screen exactly as designed. */
    Design("Design sample"),
    /** The shipped NVBW timetable. */
    Timetable("Real timetable (NVBW)"),
}

/** Single place to get the repository until dependency injection is added. */
// Holds the application context only (never an activity), which lives as long as the process.
@android.annotation.SuppressLint("StaticFieldLeak")
object Transit {
    private const val PREFS = "transit"
    private const val KEY_SOURCE = "source"

    private val mock by lazy { MockTransitRepository() }
    /** Observable, so screens switch to a new repository after a repair. */
    private var timetable by mutableStateOf<DatabaseTransitRepository?>(null)
    private var context: Context? = null

    /** Observable: screens that read [repository] while composing switch over at once. Real data by default. */
    var source by mutableStateOf(DataSource.Timetable)
        private set

    val repository: TransitRepository
        get() = if (source == DataSource.Timetable) timetable ?: mock else mock

    /** Call once at app start. */
    fun init(context: Context) {
        val app = context.applicationContext
        this.context = app
        timetable = DatabaseTransitRepository(app)
        val saved = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SOURCE, null)
        source = DataSource.entries.firstOrNull { it.name == saved } ?: DataSource.Timetable
    }

    /** A fresh real-data repository, after the timetable was repaired (drops its in-memory caches). */
    fun reload(context: Context) {
        timetable = DatabaseTransitRepository(context.applicationContext)
    }

    fun select(source: DataSource) {
        this.source = source
        context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit { putString(KEY_SOURCE, source.name) }
    }
}
