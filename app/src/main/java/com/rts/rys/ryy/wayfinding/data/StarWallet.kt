package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.Stages

/**
 * Star currency wallet. Players earn stars on clear (1~3 depending on time)
 * and spend them to unlock skins.
 *
 * Existing records get a one-time migration grant equal to the sum of best
 * stars per cleared stage, so long-time players aren't stuck at zero.
 */
class StarWallet(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun balance(): Int = prefs.getInt(KEY_BALANCE, 0)

    fun add(amount: Int) {
        if (amount <= 0) return
        prefs.edit().putInt(KEY_BALANCE, balance() + amount).apply()
    }

    fun spend(amount: Int): Boolean {
        if (amount <= 0) return true
        val current = balance()
        if (current < amount) return false
        prefs.edit().putInt(KEY_BALANCE, current - amount).apply()
        return true
    }

    /** Runs once per install. Seeds wallet from each stage's historical best. */
    fun migrateOnce(records: List<GameRecord>) {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val bestStarsByStage = records
            .groupBy { it.stageId }
            .mapValues { (stageId, rs) ->
                val stage = runCatching { Stages.byId(stageId) }.getOrNull()
                    ?: return@mapValues 0
                if (stage.level in 14..20) 0
                else MazePar.starsFor(stage, rs.minOf { it.elapsedMs })
            }
        val seed = bestStarsByStage.values.sum()
        prefs.edit()
            .putInt(KEY_BALANCE, balance() + seed)
            .putBoolean(KEY_MIGRATED, true)
            .apply()
    }

    companion object {
        private const val PREFS = "star_wallet"
        private const val KEY_BALANCE = "balance"
        private const val KEY_MIGRATED = "migrated_v1"
    }
}
