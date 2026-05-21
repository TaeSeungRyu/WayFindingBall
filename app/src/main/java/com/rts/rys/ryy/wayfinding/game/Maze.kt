package com.rts.rys.ryy.wayfinding.game

enum class Cell { EMPTY, WALL, START, GOAL }

data class Maze(
    val cols: Int,
    val rows: Int,
    val grid: Array<Array<Cell>>
) {
    var startCol: Int
        internal set
    var startRow: Int
        internal set
    var goalCol: Int
        internal set
    var goalRow: Int
        internal set
    var enemyCol: Int = -1
        internal set
    var enemyRow: Int = -1
        internal set
    var stars: List<Pair<Int, Int>> = emptyList()
        internal set
    var portalA: Pair<Int, Int>? = null
        internal set
    var portalB: Pair<Int, Int>? = null
        internal set

    init {
        var sc = 0; var sr = 0; var gc = cols - 1; var gr = rows - 1
        for (r in 0 until rows) for (c in 0 until cols) {
            when (grid[r][c]) {
                Cell.START -> { sc = c; sr = r }
                Cell.GOAL -> { gc = c; gr = r }
                else -> {}
            }
        }
        startCol = sc; startRow = sr; goalCol = gc; goalRow = gr
    }

    /** Rotates the grid 90° clockwise in-place. Only valid for square mazes. */
    fun rotateClockwise() {
        val n = cols
        val rotated = Array(n) { r ->
            Array(n) { c -> grid[n - 1 - c][r] }
        }
        for (r in 0 until n) for (c in 0 until n) {
            grid[r][c] = rotated[r][c]
        }
        val ns = n - 1 - startRow to startCol
        startCol = ns.first; startRow = ns.second
        val ng = n - 1 - goalRow to goalCol
        goalCol = ng.first; goalRow = ng.second
        if (enemyCol >= 0 && enemyRow >= 0) {
            val ne = n - 1 - enemyRow to enemyCol
            enemyCol = ne.first; enemyRow = ne.second
        }
        stars = stars.map { (c, r) -> (n - 1 - r) to c }
        portalA = portalA?.let { (c, r) -> (n - 1 - r) to c }
        portalB = portalB?.let { (c, r) -> (n - 1 - r) to c }
    }

    fun isWall(c: Int, r: Int): Boolean {
        if (c < 0 || r < 0 || c >= cols || r >= rows) return true
        return grid[r][c] == Cell.WALL
    }

    companion object {
        fun fromAscii(lines: List<String>): Maze {
            val rows = lines.size
            val cols = lines[0].length
            var ec = -1
            var er = -1
            val starsBuilder = mutableListOf<Pair<Int, Int>>()
            var pA: Pair<Int, Int>? = null
            var pB: Pair<Int, Int>? = null
            val grid = Array(rows) { r ->
                Array(cols) { c ->
                    when (lines[r][c]) {
                        '#' -> Cell.WALL
                        'S' -> Cell.START
                        'G' -> Cell.GOAL
                        'E' -> {
                            ec = c
                            er = r
                            Cell.EMPTY
                        }
                        '*' -> {
                            starsBuilder.add(c to r)
                            Cell.EMPTY
                        }
                        'P' -> {
                            pA = c to r
                            Cell.EMPTY
                        }
                        'Q' -> {
                            pB = c to r
                            Cell.EMPTY
                        }
                        else -> Cell.EMPTY
                    }
                }
            }
            return Maze(cols, rows, grid).also {
                it.enemyCol = ec
                it.enemyRow = er
                it.stars = starsBuilder.toList()
                it.portalA = pA
                it.portalB = pB
            }
        }
    }
}
