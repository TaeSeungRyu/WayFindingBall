package com.rts.rys.ryy.wayfinding.data

import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.Stages

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val colorArgb: Long
)

object Badges {
    val ALL: List<Badge> = listOf(
        Badge("first_step",     "첫 발자국",    "처음 스테이지를 클리어해요",          "🚀", 0xFF55B0F2),
        Badge("star_10",        "별 새싹",      "별을 10개 모아요",                   "🌱", 0xFF8BC34A),
        Badge("star_20",        "별 지킴이",    "별을 20개 모아요",                   "⭐", 0xFFFFB300),
        Badge("star_30",        "별 명인",      "별을 30개 모아요",                   "✨", 0xFFFF7043),
        Badge("infinite_first", "무한 입문",    "무한모드 첫 단계를 통과해요",        "♾",  0xFF7E57C2),
        Badge("infinite_5",     "무한 전사",    "무한모드 5단계까지 가요",            "⚔",  0xFFE53935),
        Badge("infinite_10",    "무한 영웅",    "무한모드 10단계까지 가요",           "👑", 0xFFFFC107),
        Badge("perfect_score",  "완벽주의자",   "1~13 스테이지에서 모두 별 3개 받아요", "💎", 0xFF26C6DA),
        Badge("creator",        "꼬마 디자이너", "나만의 미로를 만들어요",             "🎨", 0xFFEC407A),
        Badge("veteran",        "단골 손님",    "50번 도전해요",                       "🏅", 0xFF8D6E63),
    )

    fun byId(id: String): Badge? = ALL.firstOrNull { it.id == id }

    /**
     * 현재 데이터 기준으로 달성한 배지 id 집합을 계산한다.
     * 평가는 화면 진입 시·게임 종료 시 1회씩만 호출하면 충분.
     */
    fun evaluate(records: List<GameRecord>, customStagesCount: Int): Set<String> {
        val earned = mutableSetOf<String>()

        val normalRecords = records.filter {
            runCatching { Stages.byId(it.stageId).level in 1..13 }.getOrDefault(false)
        }
        val infiniteRecords = records.filter {
            runCatching { Stages.byId(it.stageId).level in 14..20 }.getOrDefault(false)
        }

        if (normalRecords.isNotEmpty()) earned += "first_step"

        val bestByStage = normalRecords.groupBy { it.stageId }
            .mapValues { (_, rs) -> rs.minOf { it.elapsedMs } }
        val normalStages = Stages.all.filter { it.level in 1..13 }
        val totalStars = normalStages.sumOf { stage ->
            val best = bestByStage[stage.id] ?: return@sumOf 0
            MazePar.starsFor(stage, best)
        }
        if (totalStars >= 10) earned += "star_10"
        if (totalStars >= 20) earned += "star_20"
        if (totalStars >= 30) earned += "star_30"

        val maxInfinite = infiniteRecords.maxOfOrNull { it.cleared } ?: 0
        if (maxInfinite >= 1) earned += "infinite_first"
        if (maxInfinite >= 5) earned += "infinite_5"
        if (maxInfinite >= 10) earned += "infinite_10"

        if (normalStages.isNotEmpty() && normalStages.all { stage ->
                val best = bestByStage[stage.id] ?: return@all false
                MazePar.starsFor(stage, best) >= 3
            }
        ) earned += "perfect_score"

        if (customStagesCount > 0) earned += "creator"
        if (records.size >= 50) earned += "veteran"

        return earned
    }
}
