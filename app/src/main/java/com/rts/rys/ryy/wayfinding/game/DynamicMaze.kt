package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.floor

/**
 * Mutates [maze].grid over time to make level-5 mazes feel alive.
 *
 * Each cycle picks up to [maxChanges] cells from anywhere in the maze,
 * previews them for a brief fade-in window, then flips wall <-> empty
 * all at once. Path connectivity is intentionally not validated — the
 * maze may temporarily block; a later cycle will reopen it.
 * Protections per change:
 *  - Outer wall, start cell, goal cell are never touched
 *  - The cell the ball currently occupies is never converted to wall
 */
class DynamicMazeController(
    private val maze: Maze,
    private val cyclePeriodS: Float = 3f,
    private val previewS: Float = 0.8f,
    private val maxChanges: Int = 10,
) {
    var version by mutableIntStateOf(0)
        private set
    var previewProgress by mutableFloatStateOf(0f)
        private set
    val pendingPreview = mutableStateListOf<PreviewCell>()

    private var elapsedInCycle = 0f
    private var pending: List<PendingChange>? = null

    data class PreviewCell(val c: Int, val r: Int, val toWall: Boolean)
    private data class PendingChange(val c: Int, val r: Int, val toWall: Boolean)

    fun tick(dt: Float, ballX: Float, ballY: Float) {
        elapsedInCycle += dt
        val previewStart = cyclePeriodS - previewS
        if (pending == null && elapsedInCycle >= previewStart) {
            val choices = choose(ballX, ballY)
            if (choices.isNotEmpty()) {
                pending = choices
                pendingPreview.clear()
                pendingPreview.addAll(choices.map { PreviewCell(it.c, it.r, it.toWall) })
            }
        }
        previewProgress = if (pending != null) {
            ((elapsedInCycle - previewStart) / previewS).coerceIn(0f, 1f)
        } else 0f
        if (elapsedInCycle >= cyclePeriodS) {
            pending?.let { apply(it, ballX, ballY) }
            pending = null
            pendingPreview.clear()
            previewProgress = 0f
            elapsedInCycle = 0f
        }
    }

    private fun apply(changes: List<PendingChange>, ballX: Float, ballY: Float) {
        val bc = floor(ballX).toInt()
        val br = floor(ballY).toInt()
        // 공이 정확히 차지한 셀만 보호. 나머지는 무조건 적용.
        val safe = changes.filter { abs(it.c - bc) + abs(it.r - br) >= 1 }
        if (safe.isEmpty()) return
        for (p in safe) {
            maze.grid[p.r][p.c] = if (p.toWall) Cell.WALL else Cell.EMPTY
        }
        version++
    }

    private fun choose(ballX: Float, ballY: Float): List<PendingChange> {
        val bc = floor(ballX).toInt()
        val br = floor(ballY).toInt()
        val pool = mutableListOf<PendingChange>()
        for (r in 1 until maze.rows - 1) for (c in 1 until maze.cols - 1) {
            if (c == bc && r == br) continue
            if (c == maze.startCol && r == maze.startRow) continue
            if (c == maze.goalCol && r == maze.goalRow) continue
            val curr = maze.grid[r][c]
            if (curr != Cell.WALL && curr != Cell.EMPTY) continue
            val toWall = curr == Cell.EMPTY
            pool.add(PendingChange(c, r, toWall))
        }
        pool.shuffle()
        return pool.take(maxChanges)
    }
}
