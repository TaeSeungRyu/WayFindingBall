package com.rts.rys.ryy.wayfinding.game

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SquashAxis { NONE, X, Y }

/**
 * Ball position and velocity are expressed in maze-cell units.
 * (x, y) = (column, row) as continuous values. Cell (c, r) covers
 * the square [c, c+1] x [r, r+1]. The ball has radius < 0.5 so it
 * fits inside a corridor.
 */
class BallPhysics(
    private val maze: Maze,
    private val radius: Float = 0.32f,
    var maxSpeed: Float = 14f,
    private val friction: Float = 1.8f,
    /** 0이면 벽에서 멈춤(기본). 0보다 크면 당구처럼 그 비율로 튕긴다. */
    private val restitution: Float = 0f,
) {
    var x: Float = maze.startCol + 0.5f
        private set
    var y: Float = maze.startRow + 0.5f
        private set
    var vx: Float = 0f
        private set
    var vy: Float = 0f
        private set

    /** Accumulated rolling angle in radians. Used by the renderer for surface pattern rotation. */
    var rotation: Float = 0f
        private set
    /** Direction of travel in radians; the rolling pattern rotates around this axis (perpendicular to motion). */
    var headingRad: Float = 0f
        private set
    /** 0..1, how compressed the ball is from the most recent wall hit; decays over [SQUASH_DURATION_S]. */
    var squashAmount: Float = 0f
        private set
    /** Which axis the last impactful collision was on. */
    var squashAxis: SquashAxis = SquashAxis.NONE
        private set
    /** True for one [step] tick when an impactful collision occurred. Consumer reads then resets next step. */
    var justImpacted: Boolean = false
        private set

    fun reset() {
        x = maze.startCol + 0.5f
        y = maze.startRow + 0.5f
        vx = 0f
        vy = 0f
        rotation = 0f
        headingRad = 0f
        squashAmount = 0f
        squashAxis = SquashAxis.NONE
        justImpacted = false
    }

    /** Warp to the center of [col, row], keeping velocity and rotation. */
    fun teleport(col: Int, row: Int) {
        x = col + 0.5f
        y = row + 0.5f
    }

    /** 외부 충격을 속도에 더한다. 공-공 충돌(포켓볼 모드) 같은 즉발 임펄스 적용용. */
    fun applyImpulse(dvx: Float, dvy: Float) {
        vx += dvx
        vy += dvy
    }

    /** 위치를 미세하게 이동(공-공 충돌 후 침투 분리용). 큰 점프는 [teleport]를 쓸 것. */
    fun nudgePosition(dx: Float, dy: Float) {
        x += dx
        y += dy
    }

    /** Move to [col, row] center and stop. Used by rotation gimmick. */
    fun setPositionAndStop(col: Int, row: Int) {
        x = col + 0.5f
        y = row + 0.5f
        vx = 0f
        vy = 0f
        squashAmount = 0f
        squashAxis = SquashAxis.NONE
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

        // remember pre-collision velocity for squash strength
        val vxPre = vx
        val vyPre = vy

        // sub-step to avoid tunneling at high speeds
        val moveX = vx * dt
        val moveY = vy * dt
        val steps = max(1, ((max(abs(moveX), abs(moveY)) / 0.1f) + 1).toInt())
        val sx = moveX / steps
        val sy = moveY / steps
        var collidedX = false
        var collidedY = false
        for (i in 0 until steps) {
            // X axis
            if (!collidedX) {
                val newX = x + sx
                if (collides(newX, y)) {
                    collidedX = true
                    vx = if (restitution > 0f) -vx * restitution else 0f
                } else {
                    x = newX
                }
            }
            // Y axis
            if (!collidedY) {
                val newY = y + sy
                if (collides(x, newY)) {
                    collidedY = true
                    vy = if (restitution > 0f) -vy * restitution else 0f
                } else {
                    y = newY
                }
            }
        }

        // rolling rotation: accumulate based on actual distance traveled
        val speed = sqrt(vx * vx + vy * vy)
        if (speed > 0.001f) {
            rotation += speed * dt / radius
            headingRad = atan2(vy, vx)
        }

        // squash on impactful collisions
        val impactThreshold = 3f
        justImpacted = false
        if (collidedX && abs(vxPre) > impactThreshold) {
            squashAxis = SquashAxis.X
            squashAmount = 1f
            justImpacted = true
        } else if (collidedY && abs(vyPre) > impactThreshold) {
            squashAxis = SquashAxis.Y
            squashAmount = 1f
            justImpacted = true
        }
        squashAmount = (squashAmount - dt / SQUASH_DURATION_S).coerceAtLeast(0f)
        if (squashAmount <= 0f) squashAxis = SquashAxis.NONE

        // 부동소수점 누적/코너 끼임으로 벽에 박혀 모든 방향이 충돌 처리되는 상황 해제.
        // 작은 맵에서 통로 폭이 ball 직경에 근접할 때 발생. 8방향 nudge로 안전 위치 탐색.
        if (collides(x, y)) {
            val n = 0.02f
            val nudges = arrayOf(
                -n to 0f, n to 0f, 0f to -n, 0f to n,
                -n to -n, n to -n, -n to n, n to n
            )
            for ((dx, dy) in nudges) {
                if (!collides(x + dx, y + dy)) {
                    x += dx
                    y += dy
                    vx = 0f
                    vy = 0f
                    break
                }
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

    companion object {
        const val SQUASH_DURATION_S = 0.18f
    }
}
