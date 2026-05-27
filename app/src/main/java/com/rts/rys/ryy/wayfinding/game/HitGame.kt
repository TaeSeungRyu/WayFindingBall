package com.rts.rys.ryy.wayfinding.game

import kotlin.random.Random

/**
 * "굴려서 맞히기" 모드 데이터.
 *
 * 테두리(+선택적 범퍼 벽)로 둘러싸인 광장에 표적을 흩뿌리고, 당구처럼
 * 벽에서 튕기는 공([BallPhysics]의 restitution)으로 표적을 모두 맞혀 없앤다.
 */
data class HitStage(
    val level: Int,
    val name: String,
    val description: String,
    val targetCount: Int,
    /** null이면 테두리만 있는 빈 광장. 아니면 이 ASCII 미로를 사용. */
    val arenaLines: List<String>? = null,
)

/** 표적 한 개. (c, r) 셀 중심에 위치. */
data class HitTarget(val c: Int, val r: Int) {
    val cx: Float get() = c + 0.5f
    val cy: Float get() = r + 0.5f
}

object HitGame {
    const val SIZE = 13

    // 3단계: 가운데에 범퍼 벽 추가 (당구처럼 튕겨 맞히는 재미)
    private val stage3Arena = listOf(
        "#############",
        "#           #",
        "#  ##   ##  #",
        "#           #",
        "#           #",
        "#           #",
        "#     S     #",
        "#           #",
        "#           #",
        "#           #",
        "#  ##   ##  #",
        "#           #",
        "#############",
    )

    val stages: List<HitStage> = listOf(
        HitStage(1, "1단계", "표적 5개", 5),
        HitStage(2, "2단계", "표적 8개", 8),
        HitStage(3, "3단계", "범퍼 + 표적 10개", 10, arenaLines = stage3Arena),
    )

    fun stageOf(level: Int): HitStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 가운데 출발점, 테두리 벽, 내부는 빈 광장(또는 지정 레이아웃). */
    fun buildArena(stage: HitStage): Maze {
        stage.arenaLines?.let { return Maze.fromAscii(it) }
        val n = SIZE
        val center = n / 2
        val lines = (0 until n).map { r ->
            buildString {
                for (c in 0 until n) {
                    append(
                        when {
                            r == 0 || r == n - 1 || c == 0 || c == n - 1 -> '#'
                            r == center && c == center -> 'S'
                            else -> ' '
                        }
                    )
                }
            }
        }
        return Maze.fromAscii(lines)
    }

    /** 시작점·벽을 피하고 서로 2칸 이상 떨어진 표적들을 무작위로 배치. */
    fun spawnTargets(stage: HitStage, maze: Maze, random: Random = Random.Default): List<HitTarget> {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (maze.grid[r][c] == Cell.WALL) continue
            if (c == maze.startCol && r == maze.startRow) continue
            // 시작점과 너무 가깝지 않게
            if (kotlin.math.abs(c - maze.startCol) + kotlin.math.abs(r - maze.startRow) < 2) continue
            candidates.add(c to r)
        }
        candidates.shuffle(random)
        val picked = mutableListOf<HitTarget>()
        for ((c, r) in candidates) {
            if (picked.size >= stage.targetCount) break
            if (picked.any { kotlin.math.abs(it.c - c) + kotlin.math.abs(it.r - r) < 2 }) continue
            picked.add(HitTarget(c, r))
        }
        return picked
    }
}
