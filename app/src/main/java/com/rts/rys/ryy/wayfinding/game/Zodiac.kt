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
        lore: String = "",
        myth: String = "",
    ) = ConstellationStage(
        level = 100 + id,
        name = name,
        description = name,
        revealEmoji = emoji,
        stars = stars,
        closeOnComplete = close,
        lore = lore,
        myth = myth,
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
                lore = "용감한 양이 뿔을 흔들어요.",
                myth = "황금 양털을 가진 멋진 양이 두 형제를 위험에서 구해 주었어요. 신은 그 용감함을 칭찬해 양을 별자리로 만들어 주었답니다.",
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
                lore = "튼튼한 황소가 V자 뿔을 가졌어요.",
                myth = "힘세고 다정한 황소가 공주를 등에 태우고 바다를 건너 멀리 갔어요. 그 멋진 모습이 별이 되어 V자 뿔로 하늘에 남았답니다.",
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
                lore = "사이좋은 쌍둥이 형제 별이에요.",
                myth = "쌍둥이 형제는 무엇이든 함께 했어요. 한 명만 하늘로 갈 수는 없다며 둘이 함께 별이 되었답니다. 지금도 나란히 빛나고 있어요.",
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
                lore = "옆으로 걷는 게가 별이 됐어요.",
                myth = "작고 용감한 게가 큰 영웅의 발을 콕 꼬집었어요. 그 용기에 감동한 여신이 게를 하늘로 올려 둥근 별자리로 만들어 주었답니다.",
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
                lore = "용맹한 사자가 갈기를 휘날려요.",
                myth = "헤라클레스라는 영웅이 사납고 큰 사자와 멋지게 겨뤘어요. 신은 그 사자가 너무 멋져 별자리로 만들고 갈기를 환하게 빛나게 했답니다.",
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
                lore = "곡식을 든 부드러운 처녀예요.",
                myth = "곡식을 든 마음씨 고운 처녀가 마을 사람들에게 음식을 나누어 주었어요. 그 따뜻한 마음이 별이 되어 밤하늘을 환히 비추고 있답니다.",
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
                lore = "공평한 저울 모양 별자리예요.",
                myth = "옛날 마을에선 저울로 곡식을 나누어 먹었어요. 모두에게 공평했던 그 고마운 저울이 하늘로 올라가 별자리가 되었답니다.",
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
                lore = "꼬리가 굽은 전갈이 있어요.",
                myth = "사냥꾼 오리온이 너무 자랑을 많이 하자, 신이 큰 전갈을 보내 살짝 놀라게 했어요. 전갈도 그 공으로 별자리가 되어 굽은 꼬리를 자랑하고 있어요.",
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
                lore = "활을 쏘는 멋진 사수예요.",
                myth = "반은 사람, 반은 말인 켄타우로스가 활을 쏘아 위험한 별들을 막아 주었어요. 그 활 쏘는 멋진 모습이 그대로 별자리가 되었답니다.",
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
                lore = "산을 오르는 단단한 염소예요.",
                myth = "가파른 절벽도 거뜬히 오르는 산양이 신을 도와주었어요. 보답으로 신은 산양을 별로 만들어 하늘 가장 높은 곳에 두었답니다.",
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
                lore = "맑은 물을 따르는 물병이에요.",
                myth = "물을 좋아하던 소년이 매일 신의 잔에 정성껏 물을 따라 주었어요. 신은 그 친절을 잊지 않으려고 소년을 별자리로 만들어 주었답니다.",
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
                lore = "강물 따라 헤엄치는 두 물고기예요.",
                myth = "엄마 물고기와 아기 물고기가 끈으로 묶여 함께 헤엄쳤어요. 절대 헤어지지 않으려는 그 모습이 그대로 하늘에 박혀 별자리가 되었답니다.",
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
