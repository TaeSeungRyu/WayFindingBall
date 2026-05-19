package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.rts.rys.ryy.wayfinding.game.Cell
import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.ui.theme.BallRed
import com.rts.rys.ryy.wayfinding.ui.theme.BallRedDeep
import com.rts.rys.ryy.wayfinding.ui.theme.FloorTile
import com.rts.rys.ryy.wayfinding.ui.theme.FloorTileAlt
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGoldDeep
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreenDeep
import com.rts.rys.ryy.wayfinding.ui.theme.WallTopLight
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MazeCanvas(
    maze: Maze,
    ballX: Float,
    ballY: Float,
    rotation: Float = 0f,
    squashAmount: Float = 0f,
    squashAxisIsX: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "maze")
    val goalPulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "goalPulse"
    )
    Canvas(modifier = modifier) {
        drawMaze(maze)
        drawGoal(maze, goalPulse)
        drawBall(maze, ballX, ballY, rotation, squashAmount, squashAxisIsX)
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

    // 바닥 (체크무늬 크림)
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] == Cell.WALL) continue
        val tl = Offset(origin.x + c * cs, origin.y + r * cs)
        val tile = if ((r + c) % 2 == 0) FloorTile else FloorTileAlt
        drawRect(color = tile, topLeft = tl, size = Size(cs, cs))
    }

    // 벽 (잔디 블록)
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] != Cell.WALL) continue
        val tl = Offset(origin.x + c * cs, origin.y + r * cs)
        drawWallTile(tl, cs)
    }

    // 외곽 라운드 보더
    drawRect(
        color = WallGreenDeep.copy(alpha = 0.4f),
        topLeft = origin,
        size = Size(cs * maze.cols, cs * maze.rows),
        style = Stroke(width = 4f)
    )
}

private fun DrawScope.drawWallTile(tl: Offset, cs: Float) {
    val sz = Size(cs, cs)
    // 본체 그라데이션
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(WallTopLight, WallGreen, WallGreenDeep),
            startY = tl.y,
            endY = tl.y + cs
        ),
        topLeft = tl,
        size = sz
    )
    // 외곽
    drawRect(
        color = WallGreenDeep,
        topLeft = tl,
        size = sz,
        style = Stroke(width = 1.5f)
    )
    // 상단 하이라이트
    drawLine(
        color = Color.White.copy(alpha = 0.55f),
        start = Offset(tl.x + cs * 0.12f, tl.y + cs * 0.18f),
        end = Offset(tl.x + cs * 0.88f, tl.y + cs * 0.18f),
        strokeWidth = cs * 0.06f
    )
    // 작은 잎사귀 점 두 개
    drawCircle(
        color = WallTopLight.copy(alpha = 0.9f),
        radius = cs * 0.06f,
        center = Offset(tl.x + cs * 0.3f, tl.y + cs * 0.55f)
    )
    drawCircle(
        color = WallTopLight.copy(alpha = 0.9f),
        radius = cs * 0.05f,
        center = Offset(tl.x + cs * 0.65f, tl.y + cs * 0.72f)
    )
}

private fun DrawScope.drawGoal(maze: Maze, pulse: Float) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + (maze.goalCol + 0.5f) * cs
    val cy = origin.y + (maze.goalRow + 0.5f) * cs

    // 후광 — 펄스에 따라 크기/투명도 변화
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoalGold.copy(alpha = 0.55f * pulse), GoalGold.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = cs * 0.95f * pulse
        ),
        center = Offset(cx, cy),
        radius = cs * 0.95f * pulse
    )
    // 별 — 펄스로 크기 변화
    drawStar(
        center = Offset(cx, cy),
        outerR = cs * 0.42f * pulse,
        innerR = cs * 0.18f * pulse,
        fill = GoalGold,
        stroke = GoalGoldDeep
    )
}

private fun DrawScope.drawStar(
    center: Offset,
    outerR: Float,
    innerR: Float,
    fill: Color,
    stroke: Color
) {
    val path = Path()
    val points = 5
    val step = Math.PI / points
    var angle = -Math.PI / 2
    path.moveTo(
        (center.x + outerR * cos(angle).toFloat()),
        (center.y + outerR * sin(angle).toFloat())
    )
    for (i in 1 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        angle += step
        path.lineTo(
            (center.x + r * cos(angle).toFloat()),
            (center.y + r * sin(angle).toFloat())
        )
    }
    path.close()
    drawPath(path = path, color = fill)
    drawPath(path = path, color = stroke, style = Stroke(width = 2.5f))
}

private fun DrawScope.drawBall(
    maze: Maze,
    bx: Float,
    by: Float,
    rotation: Float,
    squashAmount: Float,
    squashAxisIsX: Boolean
) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + bx * cs
    val cy = origin.y + by * cs
    val r = cs * 0.36f

    // squash: compress along impact axis, slightly bulge perpendicular
    val maxCompress = 0.22f
    val scaleAlong = 1f - squashAmount * maxCompress
    val scalePerp = 1f + squashAmount * maxCompress * 0.7f
    val scaleX = if (squashAxisIsX) scaleAlong else scalePerp
    val scaleY = if (squashAxisIsX) scalePerp else scaleAlong

    // 그림자 — 임팩트 시 살짝 퍼짐
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f + squashAmount * 0.06f),
        center = Offset(cx + 2f, cy + 5f),
        radius = r * (1.02f + squashAmount * 0.12f)
    )

    scale(scaleX, scaleY, pivot = Offset(cx, cy)) {
        // 본체 — 고정 광원(라디얼 그라데이션)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, BallRed, BallRedDeep),
                center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                radius = r * 1.5f
            ),
            center = Offset(cx, cy),
            radius = r
        )

        // 회전 표면 마커 — 굴러가는 느낌
        val rotDeg = rotation * 180f / Math.PI.toFloat()
        rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
            val markerColor = BallRedDeep.copy(alpha = 0.55f)
            val markerR = r * 0.16f
            val orbit = r * 0.55f
            // 4개의 점을 십자로 배치 → 회전이 한눈에 보임
            drawCircle(markerColor, markerR, Offset(cx + orbit, cy))
            drawCircle(markerColor, markerR, Offset(cx - orbit, cy))
            drawCircle(markerColor, markerR, Offset(cx, cy + orbit))
            drawCircle(markerColor, markerR, Offset(cx, cy - orbit))
            // 한 점만 더 진하게 → 회전 추적을 더 쉽게
            drawCircle(
                color = BallRedDeep.copy(alpha = 0.85f),
                radius = markerR * 1.1f,
                center = Offset(cx + orbit, cy)
            )
        }

        // 광원 하이라이트 — 회전과 무관하게 고정
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            center = Offset(cx - r * 0.38f, cy - r * 0.38f),
            radius = r * 0.22f
        )

        // 외곽선
        drawCircle(
            color = BallRedDeep,
            center = Offset(cx, cy),
            radius = r,
            style = Stroke(width = 2f)
        )
    }
}
