package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Slow enemy that BFS-pathfinds toward the ball.
 *
 * Spawns at the empty cell furthest from the start. Moves one cell at a
 * time at [moveIntervalS] intervals. The visual position lerps smoothly
 * between cell centers so motion looks natural.
 */
class ChaserController(
    private val maze: Maze,
    private val moveIntervalS: Float = 0.6f,
) {
    var col by mutableIntStateOf(0)
        private set
    var row by mutableIntStateOf(0)
        private set
    var visualX by mutableFloatStateOf(0f)
        private set
    var visualY by mutableFloatStateOf(0f)
        private set

    private var elapsedSinceMove = 0f

    init { reset() }

    fun reset() {
        val (c, r) = if (
            maze.enemyCol in 0 until maze.cols &&
            maze.enemyRow in 0 until maze.rows &&
            maze.grid[maze.enemyRow][maze.enemyCol] != Cell.WALL
        ) {
            maze.enemyCol to maze.enemyRow
        } else {
            farthestFromStart()
        }
        col = c
        row = r
        visualX = c + 0.5f
        visualY = r + 0.5f
        elapsedSinceMove = 0f
    }

    fun tick(dt: Float, ballC: Int, ballR: Int) {
        elapsedSinceMove += dt
        if (elapsedSinceMove >= moveIntervalS) {
            elapsedSinceMove -= moveIntervalS
            advanceTowards(ballC, ballR)
        }
        val tx = col + 0.5f
        val ty = row + 0.5f
        val smooth = (dt * 6f).coerceIn(0f, 1f)
        visualX += (tx - visualX) * smooth
        visualY += (ty - visualY) * smooth
    }

    private fun advanceTowards(ballC: Int, ballR: Int) {
        if (col == ballC && row == ballR) return
        val rows = maze.rows
        val cols = maze.cols
        val parent = Array(rows) { arrayOfNulls<Pair<Int, Int>>(cols) }
        val visited = Array(rows) { BooleanArray(cols) }
        val queue: MutableList<Pair<Int, Int>> = mutableListOf()
        queue.add(col to row)
        visited[row][col] = true
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        var found = false
        while (queue.isNotEmpty()) {
            val (c, r) = queue.removeAt(0)
            if (c == ballC && r == ballR) { found = true; break }
            for ((dc, dr) in dirs) {
                val nc = c + dc
                val nr = r + dr
                if (nc !in 0 until cols || nr !in 0 until rows) continue
                if (visited[nr][nc]) continue
                if (maze.grid[nr][nc] == Cell.WALL) continue
                visited[nr][nc] = true
                parent[nr][nc] = c to r
                queue.add(nc to nr)
            }
        }
        if (!found) return
        var c = ballC
        var r = ballR
        while (true) {
            val p = parent[r][c] ?: return
            if (p.first == col && p.second == row) {
                col = c
                row = r
                return
            }
            c = p.first
            r = p.second
        }
    }

    private fun farthestFromStart(): Pair<Int, Int> {
        val rows = maze.rows
        val cols = maze.cols
        val dist = Array(rows) { IntArray(cols) { -1 } }
        val queue: MutableList<Pair<Int, Int>> = mutableListOf()
        queue.add(maze.startCol to maze.startRow)
        dist[maze.startRow][maze.startCol] = 0
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        var bestC = maze.goalCol
        var bestR = maze.goalRow
        var bestD = 0
        while (queue.isNotEmpty()) {
            val (c, r) = queue.removeAt(0)
            if (dist[r][c] > bestD) { bestD = dist[r][c]; bestC = c; bestR = r }
            for ((dc, dr) in dirs) {
                val nc = c + dc
                val nr = r + dr
                if (nc !in 0 until cols || nr !in 0 until rows) continue
                if (dist[nr][nc] != -1) continue
                if (maze.grid[nr][nc] == Cell.WALL) continue
                dist[nr][nc] = dist[r][c] + 1
                queue.add(nc to nr)
            }
        }
        return bestC to bestR
    }
}
