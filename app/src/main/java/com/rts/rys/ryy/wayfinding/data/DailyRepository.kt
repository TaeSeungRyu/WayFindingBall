package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Tracks completion of the daily challenge per date.
 *
 * Stored as a JSON array of { date: "yyyy-MM-dd", bestMs: Long }.
 * Streak counts consecutive days back from today (or back from yesterday
 * if today isn't cleared yet — so the streak doesn't reset mid-day).
 */
class DailyRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Entry(val date: LocalDate, val bestMs: Long)

    fun load(): List<Entry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Entry(
                            date = LocalDate.parse(o.getString("date")),
                            bestMs = o.getLong("bestMs"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun bestFor(date: LocalDate): Long? = load().firstOrNull { it.date == date }?.bestMs

    fun recordClear(date: LocalDate, elapsedMs: Long) {
        val existing = load().associateBy { it.date }.toMutableMap()
        val prior = existing[date]?.bestMs
        if (prior == null || elapsedMs < prior) {
            existing[date] = Entry(date, elapsedMs)
            save(existing.values.sortedByDescending { it.date })
        }
    }

    fun streak(today: LocalDate = LocalDate.now()): Int {
        val cleared = load().map { it.date }.toHashSet()
        var cursor = if (cleared.contains(today)) today else today.minusDays(1)
        var count = 0
        while (cleared.contains(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun save(entries: List<Entry>) {
        val arr = JSONArray()
        for (e in entries.take(MAX_ENTRIES)) {
            arr.put(
                JSONObject()
                    .put("date", e.date.toString())
                    .put("bestMs", e.bestMs)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "daily_challenge"
        private const val KEY = "entries"
        private const val MAX_ENTRIES = 365
    }
}
