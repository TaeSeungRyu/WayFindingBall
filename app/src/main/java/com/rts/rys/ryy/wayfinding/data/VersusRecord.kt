package com.rts.rys.ryy.wayfinding.data

enum class VersusResult { WIN, LOSE, DRAW, OPPONENT_LEFT }

/**
 * 1:1 대전 한 판의 결과 기록.
 * game = 허브의 게임 구분(A·B·C·D). v1은 'A'(미로 찾기)만.
 * elapsedMs = 내 완주 시간(미완주면 0).
 */
data class VersusRecord(
    val opponentName: String,
    val game: Char,
    val result: VersusResult,
    val elapsedMs: Long,
    val timestamp: Long,
)
