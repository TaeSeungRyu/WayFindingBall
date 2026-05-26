package com.rts.rys.ryy.wayfinding.game

import kotlin.random.Random

/**
 * Generates a random maze of [size] x [size] (must be odd).
 *
 * Uses DFS recursive backtracking so the carved area is connected. The
 * caller is fine with disconnected layouts too — this is intentionally
 * simple and may leave isolated dead-ends.
 *
 * Start is placed at (1,1); goal at the cell with the greatest BFS
 * distance from start (or (n-2, n-2) as a fallback).
 *
 * Pass a seeded [random] for deterministic generation (used by the daily
 * challenge). [starCount] sprinkles collectible stars at random empty
 * cells. [withPortals] places a P/Q pair at two random empty cells.
 */
fun generateRandomMaze(
    size: Int,
    random: Random = Random.Default,
    starCount: Int = 0,
    withPortals: Boolean = false,
): Maze {
    val n = if (size % 2 == 1) size else size - 1
    val board = Array(n) { CharArray(n) { '#' } }
    board[1][1] = ' '
    val stack: MutableList<Pair<Int, Int>> = mutableListOf()
    stack.add(1 to 1)
    val dirs = listOf(2 to 0, -2 to 0, 0 to 2, 0 to -2)
    while (stack.isNotEmpty()) {
        val (c, r) = stack.last()
        var moved = false
        for ((dc, dr) in dirs.shuffled(random)) {
            val nc = c + dc
            val nr = r + dr
            if (nc in 1 until n - 1 && nr in 1 until n - 1 && board[nr][nc] == '#') {
                board[(r + nr) / 2][(c + nc) / 2] = ' '
                board[nr][nc] = ' '
                stack.add(nc to nr)
                moved = true
                break
            }
        }
        if (!moved) stack.removeAt(stack.lastIndex)
    }
    val (gc, gr) = farthestEmpty(board, 1, 1)
    board[1][1] = 'S'
    board[gr][gc] = 'G'

    val emptyCells = mutableListOf<Pair<Int, Int>>()
    for (r in 0 until n) for (c in 0 until n) {
        if (board[r][c] == ' ') emptyCells.add(c to r)
    }
    emptyCells.shuffle(random)

    if (withPortals && emptyCells.size >= 2) {
        val (pc, pr) = emptyCells.removeAt(emptyCells.lastIndex)
        val (qc, qr) = emptyCells.removeAt(emptyCells.lastIndex)
        board[pr][pc] = 'P'
        board[qr][qc] = 'Q'
    }
    repeat(starCount.coerceAtMost(emptyCells.size)) {
        val (sc, sr) = emptyCells.removeAt(emptyCells.lastIndex)
        board[sr][sc] = '*'
    }

    return Maze.fromAscii(board.map { String(it) })
}

private fun farthestEmpty(board: Array<CharArray>, sc: Int, sr: Int): Pair<Int, Int> {
    val n = board.size
    val dist = Array(n) { IntArray(n) { -1 } }
    val queue: MutableList<Pair<Int, Int>> = mutableListOf()
    queue.add(sc to sr)
    dist[sr][sc] = 0
    var bestC = sc
    var bestR = sr
    val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    while (queue.isNotEmpty()) {
        val (c, r) = queue.removeAt(0)
        for ((dc, dr) in dirs) {
            val nc = c + dc
            val nr = r + dr
            if (nc !in 0 until n || nr !in 0 until n) continue
            if (dist[nr][nc] != -1) continue
            if (board[nr][nc] == '#') continue
            dist[nr][nc] = dist[r][c] + 1
            if (dist[nr][nc] > dist[bestR][bestC]) {
                bestC = nc
                bestR = nr
            }
            queue.add(nc to nr)
        }
    }
    return bestC to bestR
}
