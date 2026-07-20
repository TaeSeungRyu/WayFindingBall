package com.rts.rys.ryy.wayfinding.game

import androidx.compose.ui.graphics.Color

/**
 * "바닥 색칠하기" 모드 데이터.
 *
 * 공을 굴려 지나간 바닥 칸을 색칠하고, 모든 바닥 칸을 다 칠하면 클리어.
 * 기존 [BallPhysics]/기울기·키패드 조작과 [Maze] 격자를 그대로 재사용한다.
 * 앞 단계는 벽 없는 광장(크기 ↑), 뒤 단계는 미로 벽이 붙어 구석까지 칠해야 한다.
 */
data class PaintStage(
    val level: Int,
    val name: String,
    val description: String,
    /** null이면 [size] 크기의 테두리만 있는 빈 광장. 아니면 이 ASCII 미로를 사용. */
    val arenaLines: List<String>? = null,
    /** 광장 모드일 때 한 변 셀 수(테두리 포함). 홀수 권장(중앙 시작점). */
    val size: Int = 9,
    /** 카드 대표 색이자 단색 모드의 칠 색. */
    val paintColor: Color,
    /** true면 팔레트에서 색을 골라 칠한다(색 바꾸기 가능). false면 [paintColor] 단색. */
    val chooseColor: Boolean = false,
    /** true면 AI와 바닥 색칠 대결. palette[0]=나, palette[1..]=AI. */
    val versus: Boolean = false,
    /** 색을 직접 지정(대결 모드 등). null이면 규칙에 따라 자동 선택. */
    val colors: List<Color>? = null,
    /** 대결 AI 공 개수. */
    val aiBalls: Int = 1,
    /** 0보다 크면 이 초 동안의 카운트다운 대결(끝났을 때 최다 색이 우승). 0이면 판을 다 채울 때까지. */
    val countdownS: Float = 0f,
    /** true면 다른 색 칸도 덮어칠 수 있다(땅따먹기). */
    val allowOverwrite: Boolean = false,
    /** true면 진행 중 무작위 벽이 생긴다(칠해진 칸 위에도). */
    val dynamicWalls: Boolean = false,
    /** true면 술래(방해꾼)가 등장 — 닿은 공은 잠시 기절하고 그 칸이 지워진다. */
    val chaser: Boolean = false,
    /** true면 공끼리 부딪히면 서로 튕겨나간다. */
    val ballBounce: Boolean = false,
    /** 시작 시 무작위로 놓이는 고정 장애물(벽) 수. */
    val obstacles: Int = 0,
    /** true면 2:2 팀전 — 나+아군(색0) vs 적 둘(색1). 같은 편은 안 튕긴다. */
    val teams: Boolean = false,
    /** true면 랜덤 위치에 폭탄이 생겨 터진다 — 범위 안 공은 잠시 정지, 그 자리 색은 지워짐. */
    val bombs: Boolean = false,
    /** 대결 AI 이동 최고 속도. */
    val aiMaxSpeed: Float = 6.3f,
    /** 대결 AI 가속 계수. */
    val aiAccelGain: Float = 15.4f,
) {
    /** 칠에 쓰는 색 목록. 지정 색 > 색 고르기 팔레트 > 단색 순. */
    val palette: List<Color> get() = colors ?: if (chooseColor) CHOOSE_PALETTE else listOf(paintColor)
}

/** 색 고르기 모드(7단계) 팔레트 — 또렷이 구분되는 6색. */
private val CHOOSE_PALETTE = listOf(
    Color(0xFFE53935), // 빨강
    Color(0xFFFB8C00), // 주황
    Color(0xFFFDD835), // 노랑
    Color(0xFF43A047), // 초록
    Color(0xFF1E88E5), // 파랑
    Color(0xFF8E24AA), // 보라
)

/** 대결 모드 색 — 파랑(나) · 빨강(AI1) · 초록(AI2) · 주황(AI3). */
val VERSUS_ME = Color(0xFF1E88E5)
val VERSUS_AI = Color(0xFFE53935)
val VERSUS_AI2 = Color(0xFF43A047)
val VERSUS_AI3 = Color(0xFFFB8C00)

object PaintGame {
    private val MINT = Color(0xFF26C6A6)
    private val SKY = Color(0xFF4FC3F7)
    private val LIME = Color(0xFF9CCC65)
    private val PINK = Color(0xFFF06292)
    private val AMBER = Color(0xFFFFB74D)
    private val PURPLE = Color(0xFFBA68C8)

    // 4단계 미로: 십자 벽으로 네 구역을 나누되 가운데 통로로 모두 이어진다.
    private val stage4Arena = listOf(
        "###########",
        "#    #    #",
        "#    #    #",
        "#         #",
        "#    #    #",
        "## ##### ##",
        "#    #    #",
        "#         #",
        "#    #    #",
        "#    S    #",
        "###########",
    )

    // 5단계 미로: 기둥 사이를 지그재그로 칠한다. 통로 행으로 모든 칸이 이어진다.
    private val stage5Arena = listOf(
        "#############",
        "#           #",
        "# # # # # # #",
        "# # # # # # #",
        "# # # # # # #",
        "#           #",
        "# # # # # # #",
        "# # # # # # #",
        "# # # # # # #",
        "#           #",
        "# # # # # # #",
        "#     S     #",
        "#############",
    )

    // 6단계 미로: 한 줄로 이어진 구불구불(serpentine) 통로. 칸막이가 한쪽 끝만 열린다.
    private val stage6Arena = listOf(
        "#############",
        "#           #",
        "# ###########",
        "#           #",
        "########### #",
        "#           #",
        "# ###########",
        "#           #",
        "########### #",
        "#           #",
        "# ###########",
        "#     S     #",
        "#############",
    )

    val stages: List<PaintStage> = listOf(
        PaintStage(1, "1단계", "작은 광장을 칠해요", size = 7, paintColor = MINT),
        PaintStage(2, "2단계", "조금 더 넓어요", size = 9, paintColor = SKY),
        PaintStage(3, "3단계", "큰 광장을 칠해요", size = 11, paintColor = LIME),
        PaintStage(4, "4단계", "벽이 생겼어요", arenaLines = stage4Arena, paintColor = PINK),
        PaintStage(5, "5단계", "기둥 사이를 칠해요", arenaLines = stage5Arena, paintColor = AMBER),
        PaintStage(6, "6단계", "구불구불 미로", arenaLines = stage6Arena, paintColor = PURPLE),
        PaintStage(7, "7단계", "색을 골라 칠해요", size = 9, paintColor = Color(0xFFEC407A), chooseColor = true),
        PaintStage(
            8, "8단계", "AI와 색칠 대결!", size = 9, paintColor = VERSUS_ME,
            versus = true, colors = listOf(VERSUS_ME, VERSUS_AI),
        ),
        PaintStage(
            9, "9단계", "3색 땅따먹기 대결!", size = 11, paintColor = VERSUS_ME,
            versus = true, aiBalls = 2, countdownS = 30f, allowOverwrite = true,
            colors = listOf(VERSUS_ME, VERSUS_AI, VERSUS_AI2),
        ),
        PaintStage(
            10, "10단계", "4색 대난투 + 벽!", size = 11, paintColor = VERSUS_ME,
            versus = true, aiBalls = 3, countdownS = 35f, allowOverwrite = true,
            dynamicWalls = true,
            aiMaxSpeed = 6.3f * 0.8f, aiAccelGain = 15.4f * 0.8f,
            colors = listOf(VERSUS_ME, VERSUS_AI, VERSUS_AI2, VERSUS_AI3),
        ),
        PaintStage(
            11, "11단계", "술래가 1등을 노려요!", size = 11, paintColor = VERSUS_ME,
            versus = true, aiBalls = 3, countdownS = 35f, allowOverwrite = true,
            chaser = true, ballBounce = true, obstacles = 8,
            colors = listOf(VERSUS_ME, VERSUS_AI, VERSUS_AI2, VERSUS_AI3),
        ),
        PaintStage(
            12, "12단계", "팀 대결 2:2!", size = 11, paintColor = VERSUS_ME,
            versus = true, aiBalls = 3, countdownS = 35f, allowOverwrite = true,
            ballBounce = true, teams = true, bombs = true, dynamicWalls = true,
            colors = listOf(VERSUS_ME, VERSUS_AI),
        ),
    )

    fun stageOf(level: Int): PaintStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 단계의 미로. 지정 레이아웃이 있으면 그것을, 없으면 [PaintStage.size] 광장을 만든다. */
    fun buildArena(stage: PaintStage): Maze {
        stage.arenaLines?.let { return Maze.fromAscii(it) }
        val n = stage.size
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

    /**
     * 클리어 시간 → 별 개수(0..3). 칸 수 기준으로 par를 정해 크기가 달라도 공정하게.
     * 칸당 0.8초 이내면 ★★★, 1.4초 이내면 ★★, 그 외 ★.
     */
    fun starsFor(floorCount: Int, elapsedMs: Long): Int {
        val perCell = elapsedMs.toFloat() / floorCount.coerceAtLeast(1)
        return when {
            perCell <= 800f -> 3
            perCell <= 1400f -> 2
            else -> 1
        }
    }
}
