package com.rts.rys.ryy.wayfinding.data

import androidx.compose.ui.graphics.Color

data class BallSkin(
    val id: String,
    val name: String,
    val emoji: String,
    val coreColor: Color,
    val deepColor: Color,
    val trailColor: Color,
    /** null이면 항상 해제. 아니면 그 배지 획득 시 해제 */
    val unlockBadgeId: String?
)

object BallSkins {
    val DEFAULT = BallSkin("default", "기본 공",   "🔴",
        coreColor = Color(0xFFFF5252), deepColor = Color(0xFFD32F2F), trailColor = Color(0xFFFF5252),
        unlockBadgeId = null)
    val SUNNY   = BallSkin("sunny",   "햇살 공",   "☀",
        coreColor = Color(0xFFFFD54F), deepColor = Color(0xFFF9A825), trailColor = Color(0xFFFFE082),
        unlockBadgeId = "first_step")
    val OCEAN   = BallSkin("ocean",   "바다 공",   "🌊",
        coreColor = Color(0xFF42A5F5), deepColor = Color(0xFF1565C0), trailColor = Color(0xFF90CAF9),
        unlockBadgeId = "star_10")
    val FOREST  = BallSkin("forest",  "숲 공",     "🌿",
        coreColor = Color(0xFF66BB6A), deepColor = Color(0xFF2E7D32), trailColor = Color(0xFFA5D6A7),
        unlockBadgeId = "star_20")
    val ROYAL   = BallSkin("royal",   "로열 공",   "💜",
        coreColor = Color(0xFFAB47BC), deepColor = Color(0xFF6A1B9A), trailColor = Color(0xFFCE93D8),
        unlockBadgeId = "star_30")
    val EMBER   = BallSkin("ember",   "불꽃 공",   "🔥",
        coreColor = Color(0xFFFF7043), deepColor = Color(0xFFBF360C), trailColor = Color(0xFFFFAB91),
        unlockBadgeId = "infinite_first")
    val SHADOW  = BallSkin("shadow",  "그림자 공", "🌑",
        coreColor = Color(0xFF424242), deepColor = Color(0xFF0F0F0F), trailColor = Color(0xFF757575),
        unlockBadgeId = "infinite_5")
    val RAINBOW = BallSkin("rainbow", "무지개 공", "🌈",
        coreColor = Color(0xFFFF6F61), deepColor = Color(0xFF7B1FA2), trailColor = Color(0xFFE91E63),
        unlockBadgeId = "perfect_score")

    val ALL: List<BallSkin> = listOf(DEFAULT, SUNNY, OCEAN, FOREST, ROYAL, EMBER, SHADOW, RAINBOW)

    fun byId(id: String): BallSkin = ALL.firstOrNull { it.id == id } ?: DEFAULT

    fun isUnlocked(skin: BallSkin, unlockedBadges: Set<String>): Boolean =
        skin.unlockBadgeId == null || skin.unlockBadgeId in unlockedBadges
}
