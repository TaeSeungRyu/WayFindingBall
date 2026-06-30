package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class VersusRecordsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<VersusRecord> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VersusRecord(
                            opponentName = o.getString("opponentName"),
                            game = o.getString("game").firstOrNull() ?: 'A',
                            result = runCatching { VersusResult.valueOf(o.getString("result")) }
                                .getOrDefault(VersusResult.DRAW),
                            elapsedMs = o.getLong("elapsedMs"),
                            timestamp = o.getLong("timestamp"),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(record: VersusRecord) {
        val combined = (load() + record)
            .sortedByDescending { it.timestamp }
            .take(MAX_RECORDS)
        val arr = JSONArray()
        for (r in combined) {
            arr.put(
                JSONObject()
                    .put("opponentName", r.opponentName)
                    .put("game", r.game.toString())
                    .put("result", r.result.name)
                    .put("elapsedMs", r.elapsedMs)
                    .put("timestamp", r.timestamp)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val PREFS = "versus_records"
        private const val KEY = "records"
        private const val MAX_RECORDS = 200
    }
}
