package com.rts.rys.ryy.wayfinding.game

/**
 * Star rating for completed mazes.
 *
 * 3-star "par" time is derived from the BFS shortest path between start and
 * goal, plus a small detour per collectible star, scaled by a per-level
 * difficulty multiplier (darkness, chasers, etc. earn more slack).
 * 2-star is 1.6× par, anything slower is 1 star.
 */
object MazePar {
    private const val MIN_PAR_MS = 45_000L
    private const val SECONDS_PER_CELL = 1.5
    private const val STAR_DETOUR_CELLS = 8
    private const val TWO_STAR_RATIO = 1.6

    fun starsFor(stage: Stage, elapsedMs: Long): Int {
        if (elapsedMs <= 0L) return 0
        val par = parMs(stage)
        return when {
            elapsedMs <= par -> 3
            elapsedMs <= (par * TWO_STAR_RATIO).toLong() -> 2
            else -> 1
        }
    }

    /** Legacy fallback used by callers that don't have a Stage in scope. */
    fun starsFor(elapsedMs: Long): Int {
        if (elapsedMs <= 0L) return 0
        return when {
            elapsedMs < 60_000L -> 3
            elapsedMs < 96_000L -> 2
            else -> 1
        }
    }

    fun parMs(stage: Stage): Long {
        val maze = stage.maze
        val path = shortestPath(maze)
        val detour = maze.stars.size * STAR_DETOUR_CELLS
        val cells = path + detour
        val seconds = cells * SECONDS_PER_CELL * difficultyMultiplier(stage.level)
        return maxOf(MIN_PAR_MS, (seconds * 1000).toLong())
    }

    private fun difficultyMultiplier(level: Int): Double = when (level) {
        in 1..4 -> 1.0      // pure mazes
        5 -> 1.3            // shifting walls
        6 -> 1.4            // moving stars + shifting walls
        7 -> 1.5            // darkness
        8 -> 1.4            // chaser
        9 -> 1.4            // collect stars
        10 -> 1.7           // final boss combo
        11 -> 1.2           // portals (often shortcut)
        12 -> 1.5           // rotating
        13 -> 1.1           // endless big maze
        else -> 1.0
    }

    private fun shortestPath(maze: Maze): Int {
        val cols = maze.cols
        val rows = maze.rows
        val dist = Array(rows) { IntArray(cols) { -1 } }
        val queue: ArrayDeque<Pair<Int, Int>> = ArrayDeque()
        queue.add(maze.startCol to maze.startRow)
        dist[maze.startRow][maze.startCol] = 0
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (queue.isNotEmpty()) {
            val (c, r) = queue.removeFirst()
            if (c == maze.goalCol && r == maze.goalRow) return dist[r][c]
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
        return cols + rows
    }
}
