package com.rts.rys.ryy.wayfinding.game

/**
 * "숫자·한글 배우기" 모드.
 *
 * 빈 광장에 숫자/글자 타일이 흩어져 있고, **순서대로**(1→2→3, ㄱ→ㄴ→ㄷ) 공을 굴려
 * 밟으면 다음으로 진행. 밟을 때마다 TTS로 읽어줘 숫자·글자·순서를 배운다.
 * 기존 [BallPhysics]/기울기·키패드 조작을 그대로 재사용한다.
 */
/** [col],[row]은 타일(2x2 구역)의 좌상단 칸. */
data class LearnItem(val label: String, val col: Int, val row: Int) {
    fun contains(c: Int, r: Int): Boolean =
        c in col until col + LearnGame.TILE && r in row until row + LearnGame.TILE
}

data class LearnStage(
    val level: Int,
    val name: String,
    val description: String,
    /** 밟아야 할 순서대로의 타일 목록. */
    val items: List<LearnItem>,
)

object LearnGame {
    const val SIZE = 11
    /** 타일 한 변 칸 수(2x2 = 크게). */
    const val TILE = 2

    // 3x3 격자의 각 셀 좌상단(열/행 2·5·8)을 라벨 순서와 다르게 흩뿌린 배치 — 각 타일은 2x2.
    private val POSITIONS = listOf(
        5 to 5, 2 to 2, 8 to 8, 8 to 2, 2 to 8, 5 to 2, 8 to 5, 2 to 5, 5 to 8,
    )

    private fun make(level: Int, name: String, desc: String, labels: List<String>): LearnStage {
        val items = labels.mapIndexed { i, lab ->
            val (c, r) = POSITIONS[i % POSITIONS.size]
            LearnItem(lab, c, r)
        }
        return LearnStage(level, name, desc, items)
    }

    val stages: List<LearnStage> = listOf(
        make(1, "1단계", "숫자 1~5", (1..5).map { it.toString() }),
        make(2, "2단계", "숫자 1~9", (1..9).map { it.toString() }),
        make(3, "3단계", "한글 ㄱ~ㅁ", listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ")),
        make(4, "4단계", "한글 ㄱ~ㅈ", listOf("ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ")),
        make(5, "5단계", "알파벳 A~E", listOf("A", "B", "C", "D", "E")),
    )

    fun stageOf(level: Int): LearnStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 빈 광장(테두리만 벽). 시작점 S는 라벨과 겹치지 않는 구석(1,1)에 둔다. */
    fun buildArena(): Maze {
        val n = SIZE
        val lines = (0 until n).map { r ->
            buildString {
                for (c in 0 until n) {
                    append(
                        when {
                            r == 0 || r == n - 1 || c == 0 || c == n - 1 -> '#'
                            r == 1 && c == 1 -> 'S'
                            else -> ' '
                        }
                    )
                }
            }
        }
        return Maze.fromAscii(lines)
    }

    /** 별점: 항목 수 기준으로 par를 정해 크기가 달라도 공정하게. 항목당 2.5초 이내면 ★★★. */
    fun starsFor(itemCount: Int, elapsedMs: Long): Int {
        val perItem = elapsedMs.toFloat() / itemCount.coerceAtLeast(1)
        return when {
            perItem <= 2500f -> 3
            perItem <= 4500f -> 2
            else -> 1
        }
    }
}
