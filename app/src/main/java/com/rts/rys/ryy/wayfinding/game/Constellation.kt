package com.rts.rys.ryy.wayfinding.game

/**
 * "별자리 잇기" 모드 데이터.
 *
 * 밤하늘에 놓인 별을 손가락으로 1번부터 순서대로 이어 별자리(또는 그림)를 완성한다.
 * 좌표는 0..1 정규화 — 화면 비율에 맞춰 캔버스에서 곱해 쓴다.
 */

/** 정규 좌표 위의 별 한 개. [order]는 1부터의 순서 번호. */
data class ConstellationStar(val x: Float, val y: Float, val order: Int)

data class ConstellationStage(
    val level: Int,
    val name: String,
    val description: String,
    /** 완성 시 가운데에 띄울 그림 이모지. */
    val revealEmoji: String,
    /** 별 목록. order는 1..stars.size 순서로. */
    val stars: List<ConstellationStar>,
    /** true면 마지막 별과 첫 별을 자동으로 이어 닫힌 도형을 완성한다. */
    val closeOnComplete: Boolean = false,
    /** 도감에 표시할 짧은 한 줄 이야기. */
    val lore: String = "",
)

/** 3별 컷오프(이 시간 이하면 ★★★). */
fun ConstellationStage.threeStarMs(): Long = stars.size * 1500L

/** 2별 컷오프(이 시간 이하면 ★★, 초과 시 ★). */
fun ConstellationStage.twoStarMs(): Long = stars.size * 2500L

/** 완성 시간을 기준으로 획득한 별 개수. 미완성(null)은 0. */
fun ConstellationStage.starsEarned(elapsedMs: Long): Int = when {
    elapsedMs <= threeStarMs() -> 3
    elapsedMs <= twoStarMs() -> 2
    else -> 1
}

fun ConstellationStage.starsEarnedFor(bestMs: Long?): Int =
    bestMs?.let { starsEarned(it) } ?: 0

object Constellation {
    private fun mkStars(vararg pts: Pair<Float, Float>): List<ConstellationStar> =
        pts.mapIndexed { i, p -> ConstellationStar(p.first, p.second, i + 1) }

    val stages: List<ConstellationStage> = listOf(
        ConstellationStage(
            level = 1,
            name = "1단계",
            description = "작은 산",
            revealEmoji = "⛰️",
            stars = mkStars(
                0.10f to 0.70f,
                0.30f to 0.35f,
                0.50f to 0.55f,
                0.70f to 0.30f,
                0.90f to 0.65f,
            ),
            lore = "지그재그로 솟은 작은 산봉우리예요.",
        ),
        ConstellationStage(
            level = 2,
            name = "2단계",
            description = "카시오페아",
            revealEmoji = "👑",
            stars = mkStars(
                0.12f to 0.30f,
                0.32f to 0.65f,
                0.50f to 0.40f,
                0.68f to 0.65f,
                0.88f to 0.30f,
            ),
            lore = "왕비의 의자 모양인 W 별자리예요.",
        ),
        ConstellationStage(
            level = 3,
            name = "3단계",
            description = "북두칠성",
            revealEmoji = "🥄",
            stars = mkStars(
                0.10f to 0.30f,
                0.25f to 0.32f,
                0.40f to 0.36f,
                0.55f to 0.42f,
                0.55f to 0.62f,
                0.80f to 0.65f,
                0.80f to 0.40f,
            ),
            lore = "일곱 형제 별이 국자처럼 모였어요.",
        ),
        ConstellationStage(
            level = 4,
            name = "4단계",
            description = "물고기",
            revealEmoji = "🐟",
            stars = mkStars(
                0.08f to 0.50f,
                0.25f to 0.28f,
                0.55f to 0.30f,
                0.78f to 0.40f,
                0.92f to 0.55f,
                0.65f to 0.72f,
                0.25f to 0.72f,
            ),
            closeOnComplete = true,
            lore = "바닷속을 헤엄치는 물고기 모양이에요.",
        ),
        ConstellationStage(
            level = 5,
            name = "5단계",
            description = "반짝 별",
            revealEmoji = "⭐",
            stars = mkStars(
                0.50f to 0.08f,
                0.60f to 0.36f,
                0.92f to 0.36f,
                0.66f to 0.56f,
                0.76f to 0.88f,
                0.50f to 0.68f,
                0.24f to 0.88f,
                0.34f to 0.56f,
                0.08f to 0.36f,
                0.40f to 0.36f,
            ),
            closeOnComplete = true,
            lore = "다섯 갈래로 빛나는 큰 별 모양이에요.",
        ),
        ConstellationStage(
            level = 6,
            name = "6단계",
            description = "큰 고래",
            revealEmoji = "🐳",
            stars = mkStars(
                0.06f to 0.40f,
                0.20f to 0.55f,
                0.06f to 0.70f,
                0.24f to 0.68f,
                0.46f to 0.82f,
                0.80f to 0.72f,
                0.94f to 0.55f,
                0.80f to 0.42f,
                0.60f to 0.30f,
                0.45f to 0.20f,
                0.30f to 0.34f,
            ),
            closeOnComplete = true,
            lore = "푸른 바다의 거대한 고래예요.",
        ),
    )

    fun stageOf(level: Int): ConstellationStage =
        stages.firstOrNull { it.level == level } ?: stages.first()
}
