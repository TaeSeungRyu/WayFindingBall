package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.Immutable

@Immutable
data class GameState(
    val ballX: Float,
    val ballY: Float,
    val velX: Float,
    val velY: Float,
    val elapsedMs: Long,
    val finished: Boolean
)
