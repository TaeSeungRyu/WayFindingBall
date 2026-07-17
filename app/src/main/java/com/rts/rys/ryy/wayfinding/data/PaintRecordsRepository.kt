package com.rts.rys.ryy.wayfinding.data

import android.content.Context

/**
 * "바닥 색칠하기" 단계별 최고(최단) 클리어 시간을 저장한다.
 */
class PaintRecordsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bestFor(level: Int): Long? {
        val v = prefs.getLong(key(level), -1L)
        return if (v < 0L) null else v
    }

    /** 기존 기록보다 빠르면 갱신하고 true 반환. */
    fun record(level: Int, elapsedMs: Long): Boolean {
        val prev = bestFor(level)
        if (prev == null || elapsedMs < prev) {
            prefs.edit().putLong(key(level), elapsedMs).apply()
            return true
        }
        return false
    }

    private fun key(level: Int) = "best_$level"

    companion object {
        private const val PREFS = "paint_records"
    }
}
