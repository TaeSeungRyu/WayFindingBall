package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * "바닥 색칠하기" 진행 상태. 공이 지나간 바닥 칸을 칠하고 남은 칸 수를 센다.
 *
 * 각 칸은 색 인덱스를 가진다(-1 = 안 칠함). 색 고르기 모드에선 이미 칠한 칸을 다른
 * 색으로 다시 칠할 수 있다(남은 칸 수는 그대로).
 *
 * 칠 대상은 **시작점에서 실제로 도달 가능한** 바닥 칸만이다. (벽으로 막혀 공이 닿을
 * 수 없는 칸을 포함하면 영원히 클리어할 수 없으므로 BFS로 걸러낸다.)
 * 시작 칸은 생성 시 즉시 칠한다.
 */
class FloorPaintController(private val maze: Maze) {
    private val colorIdx = Array(maze.rows) { IntArray(maze.cols) { -1 } }
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
        paint(maze.startCol, maze.startRow, 0)
    }

    fun isReachable(c: Int, r: Int): Boolean =
        r in 0 until maze.rows && c in 0 until maze.cols && reachable[r][c]

    fun isPainted(c: Int, r: Int): Boolean = colorAt(c, r) >= 0

    /** (c, r)의 색 인덱스. 안 칠했으면 -1. */
    fun colorAt(c: Int, r: Int): Int =
        if (r in 0 until maze.rows && c in 0 until maze.cols) colorIdx[r][c] else -1

    /**
     * (c, r)를 [idx] 색으로 칠한다.
     * @return 2 = 처음 칠함, 1 = 색만 바꿈, 0 = 변화 없음(도달 불가·같은 색).
     */
    fun paint(c: Int, r: Int, idx: Int): Int {
        if (!isReachable(c, r)) return 0
        val was = colorIdx[r][c]
        if (was == idx) return 0
        colorIdx[r][c] = idx
        version++
        return if (was < 0) { remaining--; 2 } else 1
    }

    val done: Boolean get() = remaining <= 0

    /**
     * (c, r)를 벽으로 만든다(동적 벽). 더는 칠할 수 없는 칸이 된다.
     * @return 벽이 되기 전 그 칸의 색 인덱스(-1=빈 칸), 대상이 아니면 -2.
     */
    fun wallify(c: Int, r: Int): Int {
        if (!isReachable(c, r)) return -2
        val old = colorIdx[r][c]
        reachable[r][c] = false
        colorIdx[r][c] = -1
        version++
        return old
    }

    /** [wallify]로 벽이 된 칸을 다시 빈 바닥(도달 가능·안 칠함)으로 되돌린다. */
    fun unwall(c: Int, r: Int) {
        if (r !in 1 until maze.rows - 1 || c !in 1 until maze.cols - 1) return
        reachable[r][c] = true
        colorIdx[r][c] = -1
        version++
    }

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
