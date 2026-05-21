package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlin.math.floor

/**
 * Places [keyCount] keys at random empty cells and a single door wall
 * adjacent to the goal. The door opens (wall→empty) once every key is
 * collected; only then can the ball reach the goal.
 */
class KeyDoorController(
    private val maze: Maze,
    keyCount: Int = 3,
) {
    val keys = mutableStateListOf<Pair<Int, Int>>()
    val doors = mutableStateListOf<Pair<Int, Int>>()
    var totalKeys by mutableIntStateOf(0)
        private set
    var version by mutableIntStateOf(0)
        private set

    init {
        place(keyCount)
    }

    val collected: Int get() = totalKeys - keys.size
    val allCollected: Boolean get() = keys.isEmpty()

    fun tick(ballX: Float, ballY: Float) {
        if (keys.isEmpty()) return
        val bc = floor(ballX).toInt()
        val br = floor(ballY).toInt()
        val hit = keys.firstOrNull { it.first == bc && it.second == br } ?: return
        keys.remove(hit)
        version++
        if (keys.isEmpty()) {
            for ((c, r) in doors) {
                if (maze.grid[r][c] == Cell.WALL) {
                    maze.grid[r][c] = Cell.EMPTY
                }
            }
        }
    }

    private fun place(keyCount: Int) {
        // G 인접 4방향 EMPTY 셀 모두 WALL로 봉인 + 문(door)으로 등록. 키 모두 모으면 일제히 열림.
        val gc = maze.goalCol
        val gr = maze.goalRow
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        for ((dc, dr) in dirs) {
            val nc = gc + dc
            val nr = gr + dr
            if (nc !in 0 until maze.cols || nr !in 0 until maze.rows) continue
            if (nc == maze.startCol && nr == maze.startRow) continue
            if (maze.grid[nr][nc] == Cell.EMPTY) {
                doors.add(nc to nr)
                maze.grid[nr][nc] = Cell.WALL
            }
        }

        // 키 배치 (문 봉인 후 남은 빈 칸 후보 재계산)
        val empties = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (c == maze.startCol && r == maze.startRow) continue
            if (c == maze.goalCol && r == maze.goalRow) continue
            if (maze.grid[r][c] != Cell.EMPTY) continue
            empties.add(c to r)
        }
        empties.shuffle()
        val n = keyCount.coerceAtMost(empties.size)
        repeat(n) { keys.add(empties.removeAt(0)) }
        totalKeys = n
    }
}
