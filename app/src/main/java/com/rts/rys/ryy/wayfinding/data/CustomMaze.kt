package com.rts.rys.ryy.wayfinding.data

import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.difficultyLabel

data class CustomMaze(
    val id: Int,
    val level: Int,
    val name: String,
    val lines: List<String>,
    val createdAt: Long
) {
    fun toStage(indexInLevel: Int): Stage = Stage(
        id = id,
        level = level,
        indexInLevel = indexInLevel,
        name = name,
        difficulty = difficultyLabel(level),
        maze = Maze.fromAscii(lines),
        isCustom = true
    )
}
