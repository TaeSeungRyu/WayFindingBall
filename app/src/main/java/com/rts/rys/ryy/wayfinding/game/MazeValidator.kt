package com.rts.rys.ryy.wayfinding.game

sealed class MazeValidationResult {
    object Ok : MazeValidationResult()
    data class Error(val message: String) : MazeValidationResult()
}

object MazeValidator {
    fun validate(lines: List<String>): MazeValidationResult {
        if (lines.isEmpty()) return MazeValidationResult.Error("미로가 비었어요")
        val cols = lines[0].length
        if (lines.any { it.length != cols }) {
            return MazeValidationResult.Error("미로 줄 길이가 달라요")
        }
        val rows = lines.size

        var sc = -1; var sr = -1; var gc = -1; var gr = -1
        var ec = -1; var er = -1
        var sCount = 0; var gCount = 0; var eCount = 0
        for (r in 0 until rows) for (c in 0 until cols) {
            when (lines[r][c]) {
                'S' -> { sc = c; sr = r; sCount++ }
                'G' -> { gc = c; gr = r; gCount++ }
                'E' -> { ec = c; er = r; eCount++ }
            }
        }
        if (sCount != 1) return MazeValidationResult.Error("시작점(S)을 하나만 두세요")
        if (gCount != 1) return MazeValidationResult.Error("도착점(G)을 하나만 두세요")
        if (eCount > 1) return MazeValidationResult.Error("적은 한 마리만 두세요")

        val visited = Array(rows) { BooleanArray(cols) }
        val queue: MutableList<IntArray> = mutableListOf()
        queue.add(intArrayOf(sc, sr))
        visited[sr][sc] = true
        val dirs = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        while (queue.isNotEmpty()) {
            val cur = queue.removeAt(0)
            val c = cur[0]
            val r = cur[1]
            for (d in dirs) {
                val nc = c + d[0]
                val nr = r + d[1]
                if (nc !in 0 until cols || nr !in 0 until rows) continue
                if (visited[nr][nc]) continue
                if (lines[nr][nc] == '#') continue
                visited[nr][nc] = true
                queue.add(intArrayOf(nc, nr))
            }
        }
        if (!visited[gr][gc]) return MazeValidationResult.Error("도착점까지 길이 막혔어요")
        if (eCount == 1 && !visited[er][ec]) {
            return MazeValidationResult.Error("적이 갇혔어요. 다른 곳으로 옮겨요")
        }
        return MazeValidationResult.Ok
    }
}
