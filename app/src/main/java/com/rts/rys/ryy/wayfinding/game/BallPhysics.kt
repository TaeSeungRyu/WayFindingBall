package com.rts.rys.ryy.wayfinding.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Ball position and velocity are expressed in maze-cell units.
 * (x, y) = (column, row) as continuous values. Cell (c, r) covers
 * the square [c, c+1] x [r, r+1]. The ball has radius < 0.5 so it
 * fits inside a corridor.
 */
class BallPhysics(
    private val maze: Maze,
    private val radius: Float = 0.32f,
    private val maxSpeed: Float = 14f,
    private val friction: Float = 1.8f
) {
    var x: Float = maze.startCol + 0.5f
        private set
    var y: Float = maze.startRow + 0.5f
        private set
    var vx: Float = 0f
        private set
    var vy: Float = 0f
        private set

    fun reset() {
        x = maze.startCol + 0.5f
        y = maze.startRow + 0.5f
        vx = 0f
        vy = 0f
    }

    /**
     * Advance the simulation by [dt] seconds with the given acceleration
     * (cells per second^2). Returns true if the ball reached the goal.
     */
    fun step(dt: Float, ax: Float, ay: Float): Boolean {
        // integrate velocity
        vx += ax * dt
        vy += ay * dt

        // friction
        val frictionDvx = friction * dt
        vx = if (abs(vx) <= frictionDvx) 0f else vx - frictionDvx * sign(vx)
        val frictionDvy = friction * dt
        vy = if (abs(vy) <= frictionDvy) 0f else vy - frictionDvy * sign(vy)

        // clamp speed
        vx = vx.coerceIn(-maxSpeed, maxSpeed)
        vy = vy.coerceIn(-maxSpeed, maxSpeed)

        // sub-step to avoid tunneling at high speeds
        val moveX = vx * dt
        val moveY = vy * dt
        val steps = max(1, ((max(abs(moveX), abs(moveY)) / 0.1f) + 1).toInt())
        val sx = moveX / steps
        val sy = moveY / steps
        for (i in 0 until steps) {
            // X axis
            val newX = x + sx
            if (collides(newX, y)) {
                vx = 0f
            } else {
                x = newX
            }
            // Y axis
            val newY = y + sy
            if (collides(x, newY)) {
                vy = 0f
            } else {
                y = newY
            }
        }

        // goal check (center inside goal cell)
        val cc = floor(x).toInt()
        val rr = floor(y).toInt()
        if (cc in 0 until maze.cols && rr in 0 until maze.rows) {
            if (cc == maze.goalCol && rr == maze.goalRow) return true
        }
        return false
    }

    private fun collides(cx: Float, cy: Float): Boolean {
        val minC = floor(cx - radius).toInt()
        val maxC = floor(cx + radius).toInt()
        val minR = floor(cy - radius).toInt()
        val maxR = floor(cy + radius).toInt()
        for (r in minR..maxR) for (c in minC..maxC) {
            if (maze.isWall(c, r)) {
                val nx = min(max(cx, c.toFloat()), (c + 1).toFloat())
                val ny = min(max(cy, r.toFloat()), (r + 1).toFloat())
                val dx = cx - nx
                val dy = cy - ny
                if (dx * dx + dy * dy < radius * radius) return true
            }
        }
        return false
    }

    private fun sign(v: Float): Float = if (v >= 0f) 1f else -1f
}
