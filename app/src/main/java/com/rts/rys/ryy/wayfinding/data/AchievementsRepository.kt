package com.rts.rys.ryy.wayfinding.data

import android.content.Context

class AchievementsRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadUnlockedBadges(): Set<String> =
        prefs.getStringSet(KEY_BADGES, emptySet())?.toSet() ?: emptySet()

    fun saveUnlockedBadges(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_BADGES, ids).apply()
    }

    fun loadCurrentSkinId(): String =
        prefs.getString(KEY_SKIN, BallSkins.DEFAULT.id) ?: BallSkins.DEFAULT.id

    fun saveCurrentSkinId(id: String) {
        prefs.edit().putString(KEY_SKIN, id).apply()
    }

    fun loadPurchasedSkins(): Set<String> =
        prefs.getStringSet(KEY_PURCHASED, emptySet())?.toSet() ?: emptySet()

    fun markSkinPurchased(id: String) {
        val updated = loadPurchasedSkins() + id
        prefs.edit().putStringSet(KEY_PURCHASED, updated).apply()
    }

    companion object {
        private const val PREFS = "achievements"
        private const val KEY_BADGES = "unlocked_badges"
        private const val KEY_SKIN = "current_skin"
        private const val KEY_PURCHASED = "purchased_skins"
    }
}
