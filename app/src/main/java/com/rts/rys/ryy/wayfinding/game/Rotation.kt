package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Periodically rotates the maze 90° clockwise.
 *
 * Each cycle counts up to [cyclePeriodS]. The last [previewS] seconds
 * expose [pending] = true so the UI can show a heads-up arrow. When the
 * cycle ends, [maze] is rotated in place and [onRotated] is fired so the
 * caller can re-sync the ball position.
 */
class RotatingMazeController(
    private val maze: Maze,
    private val cyclePeriodS: Float = 8f,
    private val previewS: Float = 1f,
    private val onRotated: (Maze) -> Unit,
) {
    var version by mutableIntStateOf(0)
        private set
    var previewProgress by mutableFloatStateOf(0f)
        private set
    var pending by mutableStateOf(false)
        private set

    private var elapsed = 0f

    fun tick(dt: Float) {
        elapsed += dt
        val previewStart = cyclePeriodS - previewS
        pending = elapsed >= previewStart
        previewProgress = if (pending) {
            ((elapsed - previewStart) / previewS).coerceIn(0f, 1f)
        } else 0f
        if (elapsed >= cyclePeriodS) {
            maze.rotateClockwise()
            onRotated(maze)
            elapsed = 0f
            pending = false
            previewProgress = 0f
            version++
        }
    }
}
