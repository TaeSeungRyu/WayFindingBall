package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/**
 * "색 바닥" 모드(9단계): 모든 바닥 셀에 색을 칠하고, 지정된 [target] 색 칸을
 * 찾아 밟아 모은다.
 *
 * - [reshuffle]: 모든 셀 색을 무작위로 다시 칠한다(10초 주기 등).
 * - [consume]: 모은 칸을 정답이 아닌 색으로 바꿔 재수집을 막는다.
 * - 정답 색 칸은 벽으로 덮이면 안 되므로, 색을 칠할 때마다 정답 칸 위의 벽을 치운다.
 *   (동적 벽 컨트롤러에는 isProtected로 정답 칸을 넘겨 새 벽도 안 생기게 한다.)
 */
class FloorColorController(
    private val maze: Maze,
    private val colorCount: Int,
    val target: Int,
) {
    /** grid[r][c] = 색 인덱스(0..colorCount-1), 테두리/시작점은 -1. */
    val grid: Array<IntArray> = Array(maze.rows) { IntArray(maze.cols) { -1 } }

    var version by mutableIntStateOf(0)
        private set

    init { reshuffle() }

    fun colorAt(c: Int, r: Int): Int =
        if (r in 0 until maze.rows && c in 0 until maze.cols) grid[r][c] else -1

    fun isTargetCell(c: Int, r: Int): Boolean = colorAt(c, r) == target

    fun reshuffle(random: Random = Random.Default) {
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            grid[r][c] =
                if (c == maze.startCol && r == maze.startRow) -1
                else random.nextInt(colorCount)
        }
        clearWallsOnTarget()
        version++
    }

    fun consume(c: Int, r: Int, random: Random = Random.Default) {
        if (colorCount <= 1) return
        var next = random.nextInt(colorCount)
        if (next == target) next = (next + 1) % colorCount
        grid[r][c] = next
        version++
    }

    private fun clearWallsOnTarget() {
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (grid[r][c] == target && maze.grid[r][c] == Cell.WALL) {
                maze.grid[r][c] = Cell.EMPTY
            }
        }
    }
}
