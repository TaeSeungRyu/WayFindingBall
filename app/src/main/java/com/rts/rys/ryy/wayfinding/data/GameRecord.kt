package com.rts.rys.ryy.wayfinding.data

data class GameRecord(
    val stageId: Int,
    val stageName: String,
    val elapsedMs: Long,
    val timestamp: Long,
    val cleared: Int = 0,
)
