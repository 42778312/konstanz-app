package com.example.konstanz.ui.search

/** Remembers the last search query, so "Back to search" from location details returns to the results. */
object SearchSession {
    var lastQuery: String = ""
}
