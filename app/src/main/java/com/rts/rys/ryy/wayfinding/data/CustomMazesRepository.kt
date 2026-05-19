package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class CustomMazesRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<CustomMaze> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val linesArr = o.getJSONArray("lines")
                    val lines = buildList(linesArr.length()) {
                        for (j in 0 until linesArr.length()) add(linesArr.getString(j))
                    }
                    add(
                        CustomMaze(
                            id = o.getInt("id"),
                            level = o.getInt("level"),
                            name = o.getString("name"),
                            lines = lines,
                            createdAt = o.getLong("createdAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(level: Int, lines: List<String>): CustomMaze {
        val existing = load()
        val nextId = (existing.maxOfOrNull { it.id } ?: BASE_ID).coerceAtLeast(BASE_ID) + 1
        val sameLevelCount = existing.count { it.level == level }
        val maze = CustomMaze(
            id = nextId,
            level = level,
            name = "내 미로 ${sameLevelCount + 1}",
            lines = lines,
            createdAt = System.currentTimeMillis()
        )
        save(existing + maze)
        return maze
    }

    fun delete(id: Int) {
        save(load().filter { it.id != id })
    }

    private fun save(mazes: List<CustomMaze>) {
        val arr = JSONArray()
        for (m in mazes) {
            val linesArr = JSONArray()
            for (l in m.lines) linesArr.put(l)
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("level", m.level)
                    .put("name", m.name)
                    .put("lines", linesArr)
                    .put("createdAt", m.createdAt)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "custom_mazes"
        private const val KEY = "mazes"
        const val BASE_ID = 1000
    }
}
