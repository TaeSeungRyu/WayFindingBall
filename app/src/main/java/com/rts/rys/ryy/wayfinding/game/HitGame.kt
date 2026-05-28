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
    /** true면 표적에 번호가 붙고 작은 수부터 순서대로 맞혀야 한다. */
    val ordered: Boolean = false,
    /** true면 표적이 떠다니며 벽에서 튕긴다. */
    val moving: Boolean = false,
    /** true면 벽이 주기적으로 생겼다 사라진다 (공·표적은 보호). */
    val dynamicWalls: Boolean = false,
    /** true면 4모서리에 포켓이 있고, 큐볼과 목적공이 탄성 충돌해 포켓에 넣어야 사라진다(포켓볼). */
    val pockets: Boolean = false,
    /** true면 각 포켓이 색을 가지고, 같은 색(=같은 번호) 공만 그 포켓에 입수된다.
     *  포켓 인덱스 0~3 ↔ 표적 order 1~4가 1대1 대응 (POOL_BALL_COLORS와 같은 순서). */
    val colorMatch: Boolean = false,
)

/** 표적 한 개. (c, r) 셀 중심에 위치. [order]는 순서 모드의 번호(1부터). */
data class HitTarget(val c: Int, val r: Int, val order: Int = 0) {
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

    // 4단계: 기둥 4개 사이로 순서대로 맞히기 (벽 반사 활용)
    private val stage4Arena = listOf(
        "#############",
        "#           #",
        "#  #     #  #",
        "#  #     #  #",
        "#  #     #  #",
        "#           #",
        "#     S     #",
        "#           #",
        "#  #     #  #",
        "#  #     #  #",
        "#  #     #  #",
        "#           #",
        "#############",
    )

    // 5단계: 고정 기둥 사이를 움직이는 표적이 떠다님
    private val stage5Arena = listOf(
        "#############",
        "#S          #",
        "#   #   #   #",
        "#           #",
        "#   #   #   #",
        "#           #",
        "#     #     #",
        "#           #",
        "#   #   #   #",
        "#           #",
        "#   #   #   #",
        "#           #",
        "#############",
    )

    val stages: List<HitStage> = listOf(
        HitStage(1, "1단계", "표적 5개", 5),
        HitStage(2, "2단계", "표적 8개", 8),
        HitStage(3, "3단계", "범퍼 + 표적 10개", 10, arenaLines = stage3Arena),
        HitStage(4, "4단계", "순서대로 맞히기", 6, arenaLines = stage4Arena, ordered = true),
        HitStage(5, "5단계", "움직이는 표적", 6, arenaLines = stage5Arena, moving = true),
        HitStage(6, "6단계", "사라지는 벽", 10, dynamicWalls = true),
        HitStage(7, "7단계", "순서대로 움직이는 표적", 6, ordered = true, moving = true),
        HitStage(8, "8단계", "포켓에 공을 넣어요", 5, pockets = true),
        HitStage(9, "9단계", "순서대로 포켓", 5, ordered = true, pockets = true),
        HitStage(10, "10단계", "색깔 매칭 포켓", 10, pockets = true, colorMatch = true),
    )




























































    fun stageOf(level: Int): HitStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 포켓 좌표: 아레나 안쪽 4모서리. 포켓 모드가 아니면 빈 리스트. */
    fun pocketsFor(stage: HitStage): List<Pair<Int, Int>> {
        if (!stage.pockets) return emptyList()
        val edge = SIZE - 2
        return listOf(
            1 to 1,
            edge to 1,
            1 to edge,
            edge to edge,
        )
    }

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

    /** 시작점·벽·포켓을 피하고 서로 2칸 이상 떨어진 표적들을 무작위로 배치. */
    fun spawnTargets(stage: HitStage, maze: Maze, random: Random = Random.Default): List<HitTarget> {
        val pocketCells = pocketsFor(stage).toSet()
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (maze.grid[r][c] == Cell.WALL) continue
            if (c == maze.startCol && r == maze.startRow) continue
            if ((c to r) in pocketCells) continue
            // 시작점과 너무 가깝지 않게
            if (kotlin.math.abs(c - maze.startCol) + kotlin.math.abs(r - maze.startRow) < 2) continue
            candidates.add(c to r)
        }
        candidates.shuffle(random)
        val picked = mutableListOf<HitTarget>()
        for ((c, r) in candidates) {
            if (picked.size >= stage.targetCount) break
            if (picked.any { kotlin.math.abs(it.c - c) + kotlin.math.abs(it.r - r) < 2 }) continue
            picked.add(HitTarget(c, r, order = picked.size + 1))
        }
        return picked
    }
}
