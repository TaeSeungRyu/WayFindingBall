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

    val stages: List<ColorStage> = listOf(
        ColorStage(1, "1단계", "색깔 4개", zones4, 10),
        ColorStage(2, "2단계", "색깔 6개", zones6, 12),
    )

    fun stageOf(level: Int): ColorStage = stages.firstOrNull { it.level == level } ?: stages.first()

    /** 가운데 출발점, 테두리 벽, 내부는 모두 빈 광장. */
    fun buildArena(): Maze {
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
    fun zoneAt(stage: ColorStage, c: Int, r: Int): Int? =
        stage.zones.indexOfFirst { it.contains(c, r) }.takeIf { it >= 0 }
}
