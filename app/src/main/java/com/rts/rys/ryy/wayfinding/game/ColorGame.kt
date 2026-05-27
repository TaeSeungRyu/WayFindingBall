package com.rts.rys.ryy.wayfinding.game

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * "색깔 찾기" 모드 데이터.
 *
 * 테두리만 벽인 빈 광장에 색칸을 두고, 지시된 색칸으로 공을 굴려
 * 도착시키는 게임. 기존 [BallPhysics]/기울기·키패드 조작을 재사용한다.
 *
 * 단계가 올라갈수록 색 종류가 늘어 더 헷갈린다.
 */
data class ColorZone(
    val name: String,
    val color: Color,
    val cMin: Int, val cMax: Int,
    val rMin: Int, val rMax: Int,
) {
    fun contains(c: Int, r: Int): Boolean = c in cMin..cMax && r in rMin..rMax
    val centerC: Float get() = (cMin + cMax + 1) / 2f
    val centerR: Float get() = (rMin + rMax + 1) / 2f
}

data class ColorStage(
    val level: Int,
    val name: String,
    val description: String,
    val zones: List<ColorZone>,
    val targetCount: Int,
    /** null이면 테두리만 있는 빈 광장. 아니면 이 ASCII 미로를 사용. */
    val arenaLines: List<String>? = null,
    /** true면 벽이 주기적으로 생겼다 사라진다 (색칸은 보호). */
    val dynamicWalls: Boolean = false,
    /** true면 공 주변만 보이고 나머지는 어둡다. */
    val dark: Boolean = false,
    /** true면 매판 색칸의 색을 위치에 무작위로 배치한다 (기억 도전). */
    val shuffleColors: Boolean = false,
    /** true면 술래(적)가 공을 쫓아온다. 잡히면 게임 오버. */
    val chaser: Boolean = false,
    /** true면 시작 시 색 순서를 보여준 뒤, 안내 없이 그 순서대로 찾아가야 한다. */
    val memorizeOrder: Boolean = false,
    /** true면 12색 헌트 모드: 12칸 중 지정된 색을 찾아 모은다. */
    val huntMode: Boolean = false,
    /** 0보다 크면 그 초마다 색칸 색을 무작위로 재배치한다. */
    val reshuffleEveryS: Float = 0f,
    /** 0보다 크면 시작 시 내부 벽을 이만큼 무작위로 깐다. */
    val initialWalls: Int = 0,
    /** true면 색 바닥 모드: 모든 바닥 셀에 색이 있고 지정 색을 찾아 모은다. */
    val floorMode: Boolean = false,
    /** 0보다 크면 이 초마다 전체가 카드 뒤집기 효과와 함께 보였다/숨겨진다(기억 도전). */
    val flipCycleS: Float = 0f,
)

object ColorGame {
    const val SIZE = 13

    private val RED = Color(0xFFE53935)
    private val YELLOW = Color(0xFFFDD835)
    private val BLUE = Color(0xFF1E88E5)
    private val GREEN = Color(0xFF43A047)
    private val ORANGE = Color(0xFFFB8C00)
    private val PURPLE = Color(0xFF8E24AA)

    private const val LO = 1
    private val HI = SIZE - 2          // 11
    private val MID0 = SIZE / 2 - 1    // 5
    private val MID1 = SIZE / 2 + 1    // 7

    // 1단계: 네 모서리 4색
    private val zones4 = listOf(
        ColorZone("빨강", RED, LO, LO + 2, LO, LO + 2),
        ColorZone("노랑", YELLOW, HI - 2, HI, LO, LO + 2),
        ColorZone("파랑", BLUE, LO, LO + 2, HI - 2, HI),
        ColorZone("초록", GREEN, HI - 2, HI, HI - 2, HI),
    )

    // 2단계: 모서리 4색 + 위/아래 가운데 2색 = 6색
    private val zones6 = zones4 + listOf(
        ColorZone("주황", ORANGE, MID0, MID1, LO, LO + 2),
        ColorZone("보라", PURPLE, MID0, MID1, HI - 2, HI),
    )

    // 4단계: 6색을 작은 1x1 칸으로 (기존 3x3 칸의 중심 위치)
    private val mid = SIZE / 2  // 6
    private val zonesSmall = listOf(
        ColorZone("빨강", RED, LO + 1, LO + 1, LO + 1, LO + 1),       // (2,2)
        ColorZone("노랑", YELLOW, HI - 1, HI - 1, LO + 1, LO + 1),    // (10,2)
        ColorZone("파랑", BLUE, LO + 1, LO + 1, HI - 1, HI - 1),      // (2,10)
        ColorZone("초록", GREEN, HI - 1, HI - 1, HI - 1, HI - 1),     // (10,10)
        ColorZone("주황", ORANGE, mid, mid, LO + 1, LO + 1),          // (6,2)
        ColorZone("보라", PURPLE, mid, mid, HI - 1, HI - 1),          // (6,10)
    )

    // 8단계 헌트 모드용 12색 팔레트
    val palette12: List<Pair<Color, String>> = listOf(
        Color(0xFFE53935) to "빨강",
        Color(0xFFFB8C00) to "주황",
        Color(0xFFFDD835) to "노랑",
        Color(0xFF9CCC65) to "연두",
        Color(0xFF43A047) to "초록",
        Color(0xFF26A69A) to "청록",
        Color(0xFF4FC3F7) to "하늘",
        Color(0xFF1E88E5) to "파랑",
        Color(0xFF3949AB) to "남색",
        Color(0xFF8E24AA) to "보라",
        Color(0xFFEC407A) to "분홍",
        Color(0xFF8D6E63) to "갈색",
    )

    // 9단계 색 바닥용 팔레트 (또렷이 구분되는 6색)
    val floorPalette: List<Pair<Color, String>> = listOf(
        Color(0xFFE53935) to "빨강",
        Color(0xFFFB8C00) to "주황",
        Color(0xFFFDD835) to "노랑",
        Color(0xFF43A047) to "초록",
        Color(0xFF1E88E5) to "파랑",
        Color(0xFF8E24AA) to "보라",
    )

    // 8단계: 12개 작은 색칸 위치 (4열 x 3행). 색은 매번 무작위로 재배치된다.
    private val huntCols = listOf(2, 5, 8, 10)
    private val huntRows = listOf(2, 6, 10)
    private val zones12: List<ColorZone> = huntRows.flatMap { r ->
        huntCols.map { c -> ColorZone("", Color.Transparent, c, c, r, r) }
    }

    // 3단계 미로: 6색칸은 그대로 열어두고 가운데에 벽을 둬 길을 찾게 한다.
    // 색칸(모서리·위아래 가운데 3x3)과 시작점(가운데)은 막지 않는다.
    private val stage3Arena = listOf(
        "#############",
        "#   #   #   #",
        "#   #   #   #",
        "#   #   #   #",
        "#           #",
        "# #   #   # #",
        "#     S     #",
        "# #   #   # #",
        "#           #",
        "#   #   #   #",
        "#   #   #   #",
        "#   #   #   #",
        "#############",
    )

    val stages: List<ColorStage> = listOf(
        ColorStage(1, "1단계", "색깔 4개", zones4, 5),
        ColorStage(2, "2단계", "색깔 6개", zones6, 5),
        ColorStage(3, "3단계", "벽이 있어요", zones6, 5, arenaLines = stage3Arena),
        ColorStage(4, "4단계", "작은 칸 + 움직이는 벽", zonesSmall, 5, dynamicWalls = true),
        ColorStage(5, "5단계", "깜깜해요", zones6, 5, dark = true, shuffleColors = true),
        ColorStage(6, "6단계", "술래 + 움직이는 벽", zones6, 5, chaser = true, dynamicWalls = true),
        ColorStage(7, "7단계", "순서대로 기억해요", zones6, 4, memorizeOrder = true),
        ColorStage(
            8, "8단계", "12색 찾기", zones12, 5,
            dynamicWalls = true, huntMode = true, reshuffleEveryS = 5f, initialWalls = 10,
        ),
        ColorStage(
            9, "9단계", "색 바닥에서 찾기", emptyList(), 20,
            dynamicWalls = true, floorMode = true, reshuffleEveryS = 10f, initialWalls = 8,
        ),
        ColorStage(
            10, "10단계", "보였다 안 보여요", emptyList(), 10,
            floorMode = true, flipCycleS = 3f,
        ),
    )

    /** 헌트 모드: 12색 팔레트를 12개 위치에 무작위로 1:1 배치한다. */
    fun huntAssign(stage: ColorStage, random: Random = Random.Default): List<ColorZone> {
        val pal = palette12.shuffled(random)
        return stage.zones.mapIndexed { i, z ->
            val (col, name) = pal[i % pal.size]
            z.copy(name = name, color = col)
        }
    }

    /** 시작점·색칸을 피해 내부 빈 셀 [count]개를 벽으로 만든다. */
    private fun seedRandomWalls(
        maze: Maze,
        count: Int,
        protectedZones: List<ColorZone>,
        random: Random,
    ) {
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (c == maze.startCol && r == maze.startRow) continue
            if (protectedZones.any { it.contains(c, r) }) continue
            if (maze.grid[r][c] == Cell.EMPTY) candidates.add(c to r)
        }
        candidates.shuffle(random)
        for ((c, r) in candidates.take(count)) maze.grid[r][c] = Cell.WALL
    }

    /** 색칸 위치는 그대로 두고 색만 무작위로 재배치한다. */
    fun zonesWithShuffledColors(stage: ColorStage, random: Random = Random.Default): List<ColorZone> {
        val palette = stage.zones.map { it.name to it.color }.shuffled(random)
        return stage.zones.mapIndexed { i, z ->
            z.copy(name = palette[i].first, color = palette[i].second)
        }
    }

    fun stageOf(level: Int): ColorStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 단계의 미로. 지정된 레이아웃이 있으면 그것을, 없으면 빈 광장을 만든다. */
    fun buildArena(stage: ColorStage): Maze {
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
        val maze = Maze.fromAscii(lines)
        if (stage.initialWalls > 0) {
            seedRandomWalls(maze, stage.initialWalls, stage.zones, Random.Default)
        }
        return maze
    }

    /** 연속 중복이 없는 목표 색 인덱스 시퀀스. */
    fun targetSequence(stage: ColorStage, random: Random = Random.Default): List<Int> {
        val seq = ArrayList<Int>(stage.targetCount)
        var prev = -1
        repeat(stage.targetCount) {
            var idx = stage.zones.indices.random(random)
            while (idx == prev && stage.zones.size > 1) idx = stage.zones.indices.random(random)
            seq.add(idx)
            prev = idx
        }
        return seq
    }

    /** (c, r) 셀이 어떤 색칸에 속하는지. 없으면 null. */
    fun zoneAt(zones: List<ColorZone>, c: Int, r: Int): Int? =
        zones.indexOfFirst { it.contains(c, r) }.takeIf { it >= 0 }
}
