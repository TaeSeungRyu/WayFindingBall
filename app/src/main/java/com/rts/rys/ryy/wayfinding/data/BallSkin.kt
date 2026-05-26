package com.rts.rys.ryy.wayfinding.data

import androidx.compose.ui.graphics.Color

enum class BallShape { CIRCLE, POOP }

enum class BallDecoration {
    NONE,
    SUN_RAYS,
    WAVE_BELOW,
    LEAF_TOP,
    CROWN,
    FLAME_TOP,
    SHADOW_TWIN,
    RAINBOW_RING,
    POOP_STEAM,
}

data class BallSkin(
    val id: String,
    val name: String,
    val emoji: String,
    val coreColor: Color,
    val deepColor: Color,
    val trailColor: Color,
    /** null이면 배지 조건 없음. 아니면 그 배지 획득 시 해제 */
    val unlockBadgeId: String?,
    val shape: BallShape = BallShape.CIRCLE,
    val decoration: BallDecoration = BallDecoration.NONE,
    /** null이면 구매 불가(배지/기본). 아니면 별 N개로 구매해 해제 */
    val priceStars: Int? = null,
)

object BallSkins {
    val DEFAULT = BallSkin("default", "기본 공",   "🔴",
        coreColor = Color(0xFFFF5252), deepColor = Color(0xFFD32F2F), trailColor = Color(0xFFFF5252),
        unlockBadgeId = null)
    val SUNNY   = BallSkin("sunny",   "햇살 공",   "☀",
        coreColor = Color(0xFFFFD54F), deepColor = Color(0xFFF9A825), trailColor = Color(0xFFFFE082),
        unlockBadgeId = "first_step", decoration = BallDecoration.SUN_RAYS)
    val OCEAN   = BallSkin("ocean",   "바다 공",   "🌊",
        coreColor = Color(0xFF42A5F5), deepColor = Color(0xFF1565C0), trailColor = Color(0xFF90CAF9),
        unlockBadgeId = "star_10", decoration = BallDecoration.WAVE_BELOW)
    val FOREST  = BallSkin("forest",  "숲 공",     "🌿",
        coreColor = Color(0xFF66BB6A), deepColor = Color(0xFF2E7D32), trailColor = Color(0xFFA5D6A7),
        unlockBadgeId = "star_20", decoration = BallDecoration.LEAF_TOP)
    val ROYAL   = BallSkin("royal",   "로열 공",   "💜",
        coreColor = Color(0xFFAB47BC), deepColor = Color(0xFF6A1B9A), trailColor = Color(0xFFCE93D8),
        unlockBadgeId = "star_30", decoration = BallDecoration.CROWN)
    val EMBER   = BallSkin("ember",   "불꽃 공",   "🔥",
        coreColor = Color(0xFFFF7043), deepColor = Color(0xFFBF360C), trailColor = Color(0xFFFFAB91),
        unlockBadgeId = "infinite_first", decoration = BallDecoration.FLAME_TOP)
    val SHADOW  = BallSkin("shadow",  "그림자 공", "🌑",
        coreColor = Color(0xFF424242), deepColor = Color(0xFF0F0F0F), trailColor = Color(0xFF757575),
        unlockBadgeId = "infinite_5", decoration = BallDecoration.SHADOW_TWIN)
    val RAINBOW = BallSkin("rainbow", "무지개 공", "🌈",
        coreColor = Color(0xFFFF6F61), deepColor = Color(0xFF7B1FA2), trailColor = Color(0xFFE91E63),
        unlockBadgeId = "perfect_score", decoration = BallDecoration.RAINBOW_RING)
    val POOP    = BallSkin("poop",    "응가 공",   "💩",
        coreColor = Color(0xFF8D6E63), deepColor = Color(0xFF4E342E), trailColor = Color(0xFFA1887F),
        unlockBadgeId = "veteran", shape = BallShape.POOP, decoration = BallDecoration.POOP_STEAM)

    // 별로 구매하는 프리미엄 스킨
    val STARLIGHT = BallSkin("starlight", "별빛 공", "✨",
        coreColor = Color(0xFF3949AB), deepColor = Color(0xFF1A237E), trailColor = Color(0xFF9FA8DA),
        unlockBadgeId = null, decoration = BallDecoration.SHADOW_TWIN, priceStars = 50)
    val CANDY     = BallSkin("candy",     "사탕 공", "🍬",
        coreColor = Color(0xFFFF80AB), deepColor = Color(0xFFC2185B), trailColor = Color(0xFFFFC1E3),
        unlockBadgeId = null, priceStars = 80)
    val ICE       = BallSkin("ice",       "얼음 공", "🧊",
        coreColor = Color(0xFF80DEEA), deepColor = Color(0xFF00838F), trailColor = Color(0xFFB2EBF2),
        unlockBadgeId = null, decoration = BallDecoration.LEAF_TOP, priceStars = 120)
    val GOLD      = BallSkin("gold",      "황금 공", "🏆",
        coreColor = Color(0xFFFFD700), deepColor = Color(0xFFB8860B), trailColor = Color(0xFFFFF59D),
        unlockBadgeId = null, decoration = BallDecoration.CROWN, priceStars = 200)
    val PRISM     = BallSkin("prism",     "무지개왕", "👑",
        coreColor = Color(0xFFEA80FC), deepColor = Color(0xFFAA00FF), trailColor = Color(0xFFE1BEE7),
        unlockBadgeId = null, decoration = BallDecoration.RAINBOW_RING, priceStars = 350)

    val ALL: List<BallSkin> = listOf(
        DEFAULT, SUNNY, OCEAN, FOREST, ROYAL, EMBER, SHADOW, RAINBOW, POOP,
        STARLIGHT, CANDY, ICE, GOLD, PRISM,
    )

    fun byId(id: String): BallSkin = ALL.firstOrNull { it.id == id } ?: DEFAULT

    fun isUnlocked(
        skin: BallSkin,
        unlockedBadges: Set<String>,
        purchasedSkinIds: Set<String> = emptySet(),
    ): Boolean {
        if (skin.unlockBadgeId == null && skin.priceStars == null) return true
        if (skin.unlockBadgeId != null && skin.unlockBadgeId in unlockedBadges) return true
        if (skin.priceStars != null && skin.id in purchasedSkinIds) return true
        return false
    }
}
