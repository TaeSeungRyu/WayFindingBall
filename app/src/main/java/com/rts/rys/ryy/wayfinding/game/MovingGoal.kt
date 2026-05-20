package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor

/**
 * Periodically warps the goal cell to a new random empty location.
 *
 * Each cycle picks one cell far enough from the ball, previews it during
 * a fade-in window, then moves the goal. The old goal cell becomes empty.
 * Picks: empty cells only, Manhattan distance >= [minDistanceFromBall]
 * from the ball and different from the current goal/start.
 */
class MovingGoalController(
    private val maze: Maze,
    private val cyclePeriodS: Float = 5f,
    private val previewS: Float = 1.0f,
    private val minDistanceFromBall: Int = 4,
) {
    var version by mutableIntStateOf(0)
        private set
    var previewProgress by mutableFloatStateOf(0f)
        private set
    var pendingGoal by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    private var elapsedInCycle = 0f

    fun tick(dt: Float, ballX: Float, ballY: Float) {
        elapsedInCycle += dt
        val previewStart = cyclePeriodS - previewS
        if (pendingGoal == null && elapsedInCycle >= previewStart) {
            val next = choose(ballX, ballY)
            if (next != null) pendingGoal = next
        }
        previewProgress = if (pendingGoal != null) {
            ((elapsedInCycle - previewStart) / previewS).coerceIn(0f, 1f)
        } else 0f
        if (elapsedInCycle >= cyclePeriodS) {
            pendingGoal?.let { apply(it) }
            pendingGoal = null
            previewProgress = 0f
            elapsedInCycle = 0f
        }
    }

    private fun apply(next: Pair<Int, Int>) {
        val (nc, nr) = next
        if (maze.goalRow in 0 until maze.rows && maze.goalCol in 0 until maze.cols) {
            if (maze.grid[maze.goalRow][maze.goalCol] == Cell.GOAL) {
                maze.grid[maze.goalRow][maze.goalCol] = Cell.EMPTY
            }
        }
        maze.grid[nr][nc] = Cell.GOAL
        maze.goalCol = nc
        maze.goalRow = nr
        version++
    }

    private fun choose(ballX: Float, ballY: Float): Pair<Int, Int>? {
        val bc = floor(ballX).toInt()
        val br = floor(ballY).toInt()
        val pool = mutableListOf<Pair<Int, Int>>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (c == maze.startCol && r == maze.startRow) continue
            if (c == maze.goalCol && r == maze.goalRow) continue
            if (abs(c - bc) + abs(r - br) < minDistanceFromBall) continue
            if (maze.grid[r][c] != Cell.EMPTY) continue
            pool.add(c to r)
        }
        if (pool.isEmpty()) return null
        return pool.random()
    }
}
