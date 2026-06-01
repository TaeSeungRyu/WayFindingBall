package com.rts.rys.ryy.wayfinding.game

/**
 * 황도 12궁(zodiac). 생일(월/일)에서 해당 별자리를 찾아 게임에 띄운다.
 *
 * 별자리 모양은 자녀가 한눈에 알아볼 수 있도록 5~8개 별로 단순화한 외형이며,
 * [ConstellationStage] 데이터를 그대로 재사용해 [ConstellationGameScreen]에서 플레이 가능.
 */
data class ZodiacEntry(
    /** 1..12 (양자리=1 … 물고기자리=12) */
    val index: Int,
    val korName: String,
    val symbol: String,
    /** 시작일(포함). (month, day) */
    val from: Pair<Int, Int>,
    /** 끝일(포함). (month, day) */
    val to: Pair<Int, Int>,
    /** 게임 화면으로 그대로 들어갈 별자리 데이터. */
    val stage: ConstellationStage,
)

object Zodiac {

    private fun mkStars(vararg pts: Pair<Float, Float>): List<ConstellationStar> =
        pts.mapIndexed { i, p -> ConstellationStar(p.first, p.second, i + 1) }

    private fun stageOf(
        id: Int,
        name: String,
        emoji: String,
        stars: List<ConstellationStar>,
        close: Boolean = false,
    ) = ConstellationStage(
        level = 100 + id,
        name = name,
        description = name,
        revealEmoji = emoji,
        stars = stars,
        closeOnComplete = close,
    )

    /** 1번부터 12번까지. 표시 순서는 양자리 시작(3/21~). */
    val entries: List<ZodiacEntry> = listOf(
        ZodiacEntry(
            index = 1,
            korName = "양자리",
            symbol = "♈",
            from = 3 to 21,
            to = 4 to 19,
            stage = stageOf(
                1, "양자리", "🐏",
                mkStars(
                    0.15f to 0.30f,
                    0.30f to 0.55f,
                    0.50f to 0.70f,
                    0.70f to 0.55f,
                    0.85f to 0.30f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 2,
            korName = "황소자리",
            symbol = "♉",
            from = 4 to 20,
            to = 5 to 20,
            stage = stageOf(
                2, "황소자리", "🐂",
                mkStars(
                    0.12f to 0.20f,
                    0.32f to 0.45f,
                    0.30f to 0.70f,
                    0.50f to 0.82f,
                    0.70f to 0.70f,
                    0.68f to 0.45f,
                    0.88f to 0.20f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 3,
            korName = "쌍둥이자리",
            symbol = "♊",
            from = 5 to 21,
            to = 6 to 21,
            stage = stageOf(
                3, "쌍둥이자리", "👫",
                mkStars(
                    0.25f to 0.18f,
                    0.25f to 0.82f,
                    0.50f to 0.50f,
                    0.75f to 0.82f,
                    0.75f to 0.18f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 4,
            korName = "게자리",
            symbol = "♋",
            from = 6 to 22,
            to = 7 to 22,
            stage = stageOf(
                4, "게자리", "🦀",
                mkStars(
                    0.18f to 0.55f,
                    0.30f to 0.30f,
                    0.50f to 0.20f,
                    0.70f to 0.30f,
                    0.82f to 0.55f,
                    0.50f to 0.80f,
                ),
                close = true,
            ),
        ),
        ZodiacEntry(
            index = 5,
            korName = "사자자리",
            symbol = "♌",
            from = 7 to 23,
            to = 8 to 22,
            stage = stageOf(
                5, "사자자리", "🦁",
                mkStars(
                    0.18f to 0.72f,
                    0.30f to 0.50f,
                    0.45f to 0.30f,
                    0.60f to 0.22f,
                    0.75f to 0.32f,
                    0.82f to 0.55f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 6,
            korName = "처녀자리",
            symbol = "♍",
            from = 8 to 23,
            to = 9 to 22,
            stage = stageOf(
                6, "처녀자리", "👩",
                mkStars(
                    0.15f to 0.20f,
                    0.32f to 0.38f,
                    0.48f to 0.28f,
                    0.55f to 0.55f,
                    0.68f to 0.45f,
                    0.82f to 0.68f,
                    0.50f to 0.85f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 7,
            korName = "천칭자리",
            symbol = "♎",
            from = 9 to 23,
            to = 10 to 22,
            stage = stageOf(
                7, "천칭자리", "⚖️",
                mkStars(
                    0.12f to 0.58f,
                    0.30f to 0.45f,
                    0.50f to 0.28f,
                    0.70f to 0.45f,
                    0.88f to 0.58f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 8,
            korName = "전갈자리",
            symbol = "♏",
            from = 10 to 23,
            to = 11 to 22,
            stage = stageOf(
                8, "전갈자리", "🦂",
                mkStars(
                    0.15f to 0.30f,
                    0.30f to 0.45f,
                    0.46f to 0.55f,
                    0.62f to 0.55f,
                    0.76f to 0.50f,
                    0.86f to 0.66f,
                    0.74f to 0.80f,
                    0.55f to 0.86f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 9,
            korName = "사수자리",
            symbol = "♐",
            from = 11 to 23,
            to = 12 to 21,
            stage = stageOf(
                9, "사수자리", "🏹",
                mkStars(
                    0.50f to 0.15f,
                    0.22f to 0.48f,
                    0.50f to 0.85f,
                    0.45f to 0.50f,
                    0.66f to 0.50f,
                    0.86f to 0.50f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 10,
            korName = "염소자리",
            symbol = "♑",
            from = 12 to 22,
            to = 1 to 19,
            stage = stageOf(
                10, "염소자리", "🐐",
                mkStars(
                    0.15f to 0.30f,
                    0.38f to 0.62f,
                    0.65f to 0.32f,
                    0.85f to 0.55f,
                    0.76f to 0.80f,
                    0.50f to 0.86f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 11,
            korName = "물병자리",
            symbol = "♒",
            from = 1 to 20,
            to = 2 to 18,
            stage = stageOf(
                11, "물병자리", "🌊",
                mkStars(
                    0.08f to 0.40f,
                    0.30f to 0.68f,
                    0.50f to 0.40f,
                    0.70f to 0.68f,
                    0.92f to 0.40f,
                ),
            ),
        ),
        ZodiacEntry(
            index = 12,
            korName = "물고기자리",
            symbol = "♓",
            from = 2 to 19,
            to = 3 to 20,
            stage = stageOf(
                12, "물고기자리", "🐠",
                mkStars(
                    0.10f to 0.22f,
                    0.32f to 0.50f,
                    0.50f to 0.80f,
                    0.68f to 0.50f,
                    0.90f to 0.22f,
                ),
            ),
        ),
    )

    /** 생일(월/일)로 zodiac 한 개. 잘못된 입력이면 null. */
    fun forBirthday(month: Int, day: Int): ZodiacEntry? {
        if (month !in 1..12 || day !in 1..31) return null
        val mmdd = month * 100 + day
        for (e in entries) {
            val fromKey = e.from.first * 100 + e.from.second
            val toKey = e.to.first * 100 + e.to.second
            val inside = if (fromKey <= toKey) {
                mmdd in fromKey..toKey
            } else {
                // 염소자리(12/22~1/19)처럼 연말을 넘어가는 구간.
                mmdd >= fromKey || mmdd <= toKey
            }
            if (inside) return e
        }
        return null
    }

    fun byIndex(index: Int): ZodiacEntry? = entries.firstOrNull { it.index == index }
}

/** "1/20 ~ 2/18" 같은 표시용 문자열. */
fun ZodiacEntry.dateRangeText(): String =
    "${from.first}/${from.second} ~ ${to.first}/${to.second}"
