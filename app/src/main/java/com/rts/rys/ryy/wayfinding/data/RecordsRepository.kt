package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RecordsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<GameRecord> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        GameRecord(
                            stageId = o.getInt("stageId"),
                            stageName = o.getString("stageName"),
                            elapsedMs = o.getLong("elapsedMs"),
                            timestamp = o.getLong("timestamp")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(record: GameRecord) {
        val all = (load() + record).sortedByDescending { it.timestamp }
        val arr = JSONArray()
        for (r in all) {
            arr.put(
                JSONObject()
                    .put("stageId", r.stageId)
                    .put("stageName", r.stageName)
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
        private const val PREFS = "maze_records"
        private const val KEY = "records"
    }
}
