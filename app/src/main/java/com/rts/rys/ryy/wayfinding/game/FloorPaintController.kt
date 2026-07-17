package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * "바닥 색칠하기" 진행 상태. 공이 지나간 바닥 칸을 칠하고 남은 칸 수를 센다.
 *
 * 칠해야 할 대상은 **시작점에서 실제로 도달 가능한** 바닥 칸만이다. (벽으로 막혀
 * 공이 닿을 수 없는 칸을 포함하면 영원히 클리어할 수 없으므로 BFS로 걸러낸다.)
 * 시작 칸은 생성 시 즉시 칠한다.
 */
class FloorPaintController(private val maze: Maze) {
    private val painted = Array(maze.rows) { BooleanArray(maze.cols) }
    private val reachable = Array(maze.rows) { BooleanArray(maze.cols) }

    /** 칠해야 할 전체(도달 가능) 바닥 칸 수. */
    val total: Int

    /** 남은(안 칠한) 칸 수. */
    var remaining by mutableIntStateOf(0)
        private set

    /** 칸 상태가 바뀔 때마다 증가 — Canvas 재구성 트리거용. */
    var version by mutableIntStateOf(0)
        private set

    init {
        total = floodFillReachable(maze.startCol, maze.startRow)
        remaining = total
        paint(maze.startCol, maze.startRow)
    }

    fun isReachable(c: Int, r: Int): Boolean =
        r in 0 until maze.rows && c in 0 until maze.cols && reachable[r][c]

    fun isPainted(c: Int, r: Int): Boolean =
        r in 0 until maze.rows && c in 0 until maze.cols && painted[r][c]

    /** (c, r)를 칠한다. 새로 칠했으면 true. */
    fun paint(c: Int, r: Int): Boolean {
        if (!isReachable(c, r) || painted[r][c]) return false
        painted[r][c] = true
        remaining--
        version++
        return true
    }

    val done: Boolean get() = remaining <= 0

    /** 시작점에서 상하좌우로 이어진 벽이 아닌 칸을 표시하고 그 개수를 반환. */
    private fun floodFillReachable(startC: Int, startR: Int): Int {
        if (maze.isWall(startC, startR)) return 0
        val stack = ArrayList<Int>()
        stack.add(startR * maze.cols + startC)
        reachable[startR][startC] = true
        var count = 0
        val dirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
        while (stack.isNotEmpty()) {
            val v = stack.removeAt(stack.lastIndex)
            val c = v % maze.cols
            val r = v / maze.cols
            count++
            for ((dc, dr) in dirs) {
                val nc = c + dc
                val nr = r + dr
                if (nc in 0 until maze.cols && nr in 0 until maze.rows &&
                    !reachable[nr][nc] && !maze.isWall(nc, nr)
                ) {
                    reachable[nr][nc] = true
                    stack.add(nr * maze.cols + nc)
                }
            }
        }
        return count
    }
}
