package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rts.rys.ryy.wayfinding.game.Cell
import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.ui.theme.BallColor
import com.rts.rys.ryy.wayfinding.ui.theme.FloorColor
import com.rts.rys.ryy.wayfinding.ui.theme.GoalColor
import com.rts.rys.ryy.wayfinding.ui.theme.NeonCyan
import com.rts.rys.ryy.wayfinding.ui.theme.WallColor
import com.rts.rys.ryy.wayfinding.ui.theme.WallEdge

@Composable
fun MazeCanvas(
    maze: Maze,
    ballX: Float,
    ballY: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawMaze(maze)
        drawGoal(maze)
        drawBall(maze, ballX, ballY)
    }
}

private fun DrawScope.cellSize(maze: Maze): Float {
    return minOf(size.width / maze.cols, size.height / maze.rows)
}

private fun DrawScope.originOffset(maze: Maze, cs: Float): Offset {
    val totalW = cs * maze.cols
    val totalH = cs * maze.rows
    return Offset((size.width - totalW) / 2f, (size.height - totalH) / 2f)
}

private fun DrawScope.drawMaze(maze: Maze) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)

    // Floor background
    drawRect(
        color = FloorColor,
        topLeft = origin,
        size = Size(cs * maze.cols, cs * maze.rows)
    )

    // subtle grid
    val gridColor = Color.White.copy(alpha = 0.03f)
    for (c in 0..maze.cols) {
        val x = origin.x + c * cs
        drawLine(gridColor, Offset(x, origin.y), Offset(x, origin.y + cs * maze.rows), strokeWidth = 1f)
    }
    for (r in 0..maze.rows) {
        val y = origin.y + r * cs
        drawLine(gridColor, Offset(origin.x, y), Offset(origin.x + cs * maze.cols, y), strokeWidth = 1f)
    }

    // walls with gradient + edge highlight
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] == Cell.WALL) {
            val tl = Offset(origin.x + c * cs, origin.y + r * cs)
            val sz = Size(cs, cs)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(WallColor, WallEdge),
                    start = tl,
                    end = Offset(tl.x + cs, tl.y + cs)
                ),
                topLeft = tl,
                size = sz
            )
            drawRect(
                color = WallEdge.copy(alpha = 0.85f),
                topLeft = tl,
                size = sz,
                style = Stroke(width = 1.5f)
            )
            // top highlight
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(tl.x + 1f, tl.y + 1.5f),
                end = Offset(tl.x + cs - 1f, tl.y + 1.5f),
                strokeWidth = 1.5f
            )
        }
    }

    // outer glow border
    drawRect(
        color = NeonCyan.copy(alpha = 0.6f),
        topLeft = origin,
        size = Size(cs * maze.cols, cs * maze.rows),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawGoal(maze: Maze) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + (maze.goalCol + 0.5f) * cs
    val cy = origin.y + (maze.goalRow + 0.5f) * cs

    // outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoalColor.copy(alpha = 0.5f), GoalColor.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = cs * 0.9f
        ),
        center = Offset(cx, cy),
        radius = cs * 0.9f
    )
    drawCircle(
        color = GoalColor,
        center = Offset(cx, cy),
        radius = cs * 0.30f
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        center = Offset(cx - cs * 0.08f, cy - cs * 0.08f),
        radius = cs * 0.08f
    )
}

private fun DrawScope.drawBall(maze: Maze, bx: Float, by: Float) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + bx * cs
    val cy = origin.y + by * cs
    val r = cs * 0.34f

    // shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        center = Offset(cx + 2f, cy + 4f),
        radius = r
    )
    // body gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, BallColor, BallColor.copy(alpha = 0.85f)),
            center = Offset(cx - r * 0.3f, cy - r * 0.3f),
            radius = r * 1.4f
        ),
        center = Offset(cx, cy),
        radius = r
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        center = Offset(cx - r * 0.35f, cy - r * 0.35f),
        radius = r * 0.18f
    )
}
