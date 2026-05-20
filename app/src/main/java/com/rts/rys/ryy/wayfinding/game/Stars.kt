package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import kotlin.math.floor

/**
 * Tracks collectible stars for a stage.
 *
 * Stars come from [Maze.stars] — positions baked into the maze definition
 * (either an ASCII spec or what the user placed in the editor). The goal
 * cell only counts as reached after every star is collected.
 */
class StarsController(private val maze: Maze) {
    val remaining = mutableStateListOf<Pair<Int, Int>>()
    var totalCount by mutableIntStateOf(0)
        private set
    var collectVersion by mutableIntStateOf(0)
        private set

    init {
        totalCount = maze.stars.size
        remaining.clear()
        remaining.addAll(maze.stars)
    }

    val collected: Int get() = totalCount - remaining.size
    val allCollected: Boolean get() = remaining.isEmpty()

    fun tick(ballX: Float, ballY: Float) {
        if (remaining.isEmpty()) return
        val bc = floor(ballX).toInt()
        val br = floor(ballY).toInt()
        val hit = remaining.firstOrNull { it.first == bc && it.second == br } ?: return
        remaining.remove(hit)
        collectVersion++
    }
}
