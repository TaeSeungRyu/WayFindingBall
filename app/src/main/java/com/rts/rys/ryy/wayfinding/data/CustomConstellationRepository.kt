package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import com.rts.rys.ryy.wayfinding.game.ConstellationStar
import com.rts.rys.ryy.wayfinding.game.CustomConstellation
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 자녀가 만든 별자리 목록을 SharedPreferences에 JSON으로 저장한다.
 * 저장 순서 = 목록 표시 순서(먼저 만든 것이 앞).
 */
class CustomConstellationRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(): List<CustomConstellation> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching { parse(raw) }.getOrDefault(emptyList())
    }

    fun get(id: String): CustomConstellation? = all().firstOrNull { it.id == id }

    fun add(name: String, emoji: String, stars: List<ConstellationStar>): CustomConstellation {
        val item = CustomConstellation(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            emoji = emoji,
            stars = stars,
        )
        save(all() + item)
        return item
    }

    fun delete(id: String) {
        save(all().filterNot { it.id == id })
    }

    private fun save(list: List<CustomConstellation>) {
        val arr = JSONArray()
        for (c in list) {
            val stars = JSONArray()
            for (s in c.stars) {
                stars.put(
                    JSONObject()
                        .put("x", s.x.toDouble())
                        .put("y", s.y.toDouble())
                )
            }
            arr.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("emoji", c.emoji)
                    .put("stars", stars)
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    private fun parse(raw: String): List<CustomConstellation> {
        val arr = JSONArray(raw)
        val out = ArrayList<CustomConstellation>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val starsArr = o.getJSONArray("stars")
            val stars = ArrayList<ConstellationStar>(starsArr.length())
            for (j in 0 until starsArr.length()) {
                val so = starsArr.getJSONObject(j)
                stars.add(
                    ConstellationStar(
                        so.getDouble("x").toFloat(),
                        so.getDouble("y").toFloat(),
                        j + 1,
                    )
                )
            }
            out.add(
                CustomConstellation(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    emoji = o.optString("emoji", "🌟"),
                    stars = stars,
                )
            )
        }
        return out
    }

    companion object {
        private const val PREFS = "custom_constellations"
        private const val KEY_LIST = "list"
    }
}
