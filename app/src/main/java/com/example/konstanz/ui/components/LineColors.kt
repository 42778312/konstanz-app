package com.example.konstanz.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Each line's colour from the Stadtwerke Konstanz network map (Liniennetzplan). The NVBW timetable has no
 * line colours, so they are kept here. A line that is missing uses the brand mint.
 * Add a line as "number" to 0xFFRRGGBB, using the colour printed on the official network map.
 */
object LineColors {
    private val official: Map<String, Long> = mapOf(
        // e.g. "1" to 0xFF______,
    )

    /** The line's colour, or null when it isn't known yet. */
    fun of(line: String): Color? = official[line]?.let(::Color)

    /** Black or white text, whichever reads better on [background]. */
    fun contentOn(background: Color): Color =
        if (background.luminance() > 0.4f) Color(0xFF111111) else Color.White
}
