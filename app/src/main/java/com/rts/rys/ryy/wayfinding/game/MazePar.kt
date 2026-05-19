package com.rts.rys.ryy.wayfinding.game

/**
 * Star rating for completed mazes, using fixed absolute thresholds.
 *
 * - 3 stars: under 1 minute
 * - 2 stars: under 2 minutes
 * - 1 star : any other completion
 */
object MazePar {
    private const val THREE_STAR_MS = 60_000L
    private const val TWO_STAR_MS = 120_000L

    fun starsFor(elapsedMs: Long): Int {
        if (elapsedMs <= 0L) return 0
        return when {
            elapsedMs < THREE_STAR_MS -> 3
            elapsedMs < TWO_STAR_MS -> 2
            else -> 1
        }
    }

    fun starsFor(@Suppress("UNUSED_PARAMETER") stage: Stage, elapsedMs: Long): Int =
        starsFor(elapsedMs)
}
