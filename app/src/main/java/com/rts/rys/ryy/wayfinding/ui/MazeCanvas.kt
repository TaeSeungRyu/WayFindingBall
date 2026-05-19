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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlin.math.PI
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
    trail: List<androidx.compose.ui.geometry.Offset> = emptyList(),
    ballScale: Float = 1f,
    headingRad: Float = 0f,
    isHappy: Boolean = false,
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
    val rayRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRot"
    )
    val sparkleTime by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleT"
    )
    Canvas(modifier = modifier) {
        drawMaze(maze)
        drawGoal(maze, goalPulse, rayRotation, sparkleTime)
        if (trail.isNotEmpty() && ballScale > 0f) drawTrail(maze, trail)
        if (ballScale > 0f) drawBall(maze, ballX, ballY, rotation, squashAmount, squashAxisIsX, ballScale, headingRad, isHappy)
    }
}

private fun DrawScope.drawTrail(maze: Maze, trail: List<androidx.compose.ui.geometry.Offset>) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val r = cs * 0.36f
    for (i in trail.indices) {
        val pos = trail[i]
        val frac = i.toFloat() / trail.size
        val alpha = (1f - frac) * 0.35f
        val rr = r * (1f - frac * 0.4f)
        drawCircle(
            color = BallRed.copy(alpha = alpha),
            center = Offset(origin.x + pos.x * cs, origin.y + pos.y * cs),
            radius = rr
        )
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

    // 바닥 위 작은 디테일 (셀 ~15%에 잎/꽃잎/조약돌)
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] == Cell.WALL) continue
        val tl = Offset(origin.x + c * cs, origin.y + r * cs)
        drawFloorDetail(c, r, tl, cs)
    }

    // 벽 그림자 — 우하단 방향으로 인접 바닥에 드리워짐
    val shadowOffset = cs * 0.15f
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] != Cell.WALL) continue
        val tl = Offset(origin.x + c * cs + shadowOffset, origin.y + r * cs + shadowOffset)
        drawRect(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = tl,
            size = Size(cs, cs)
        )
    }

    // 벽 (잔디 블록) — 셀별로 변주
    for (r in 0 until maze.rows) for (c in 0 until maze.cols) {
        if (maze.grid[r][c] != Cell.WALL) continue
        val tl = Offset(origin.x + c * cs, origin.y + r * cs)
        drawWallTile(c, r, tl, cs)
    }

}

private fun DrawScope.drawFloorDetail(c: Int, r: Int, tl: Offset, cs: Float) {
    // 시드와 별도 솔트(99)로 벽 변주와 겹치지 않게
    val seed = tileSeed(c, r) xor 0x6f7c
    val roll = seedFloat(seed, 99)
    if (roll > 0.18f) return // ~18%만 디테일
    val variant = (seedFloat(seed, 98) * 100f).toInt()
    when {
        variant < 45 -> {
            // 작은 잎 2개
            val cx = tl.x + cs * (0.30f + seedFloat(seed, 80) * 0.40f)
            val cy = tl.y + cs * (0.30f + seedFloat(seed, 81) * 0.40f)
            val leafColor = Color(0xFFA8C49B).copy(alpha = 0.85f)
            drawCircle(leafColor, cs * 0.05f, Offset(cx, cy))
            drawCircle(leafColor, cs * 0.04f, Offset(cx + cs * 0.10f, cy + cs * 0.06f))
        }
        variant < 75 -> {
            // 작은 조약돌
            val cx = tl.x + cs * (0.30f + seedFloat(seed, 82) * 0.40f)
            val cy = tl.y + cs * (0.30f + seedFloat(seed, 83) * 0.40f)
            val rr = cs * 0.055f
            drawCircle(Color.Black.copy(alpha = 0.10f), rr, Offset(cx + 1f, cy + 1.5f))
            drawCircle(Color(0xFFCBB995), rr, Offset(cx, cy))
            drawCircle(Color.White.copy(alpha = 0.5f), rr * 0.35f, Offset(cx - rr * 0.3f, cy - rr * 0.3f))
        }
        else -> {
            // 살짝 진한 바닥 얼룩 (paint splash)
            val cx = tl.x + cs * (0.25f + seedFloat(seed, 84) * 0.50f)
            val cy = tl.y + cs * (0.25f + seedFloat(seed, 85) * 0.50f)
            drawCircle(
                color = Color(0xFFE8D9B5).copy(alpha = 0.65f),
                radius = cs * 0.13f,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawWallTile(c: Int, r: Int, tl: Offset, cs: Float) {
    val sz = Size(cs, cs)
    val seed = tileSeed(c, r)
    // 본체 그라데이션 — 셀별 미세한 밝기 변동
    val tintShift = (seedFloat(seed, 7) - 0.5f) * 0.06f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                shiftBrightness(WallTopLight, tintShift),
                shiftBrightness(WallGreen, tintShift),
                shiftBrightness(WallGreenDeep, tintShift)
            ),
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

    // 변주: 100분위 분포 → 꽃 / 돌 / 잎사귀 점들
    val variant = (seedFloat(seed, 1) * 100f).toInt()
    when {
        variant < 10 -> drawTileRock(seed, tl, cs)
        variant < 24 -> drawTileFlower(seed, tl, cs)
        else -> drawTileLeaves(seed, tl, cs)
    }
}

private fun DrawScope.drawTileLeaves(seed: Int, tl: Offset, cs: Float) {
    val count = 1 + (seedFloat(seed, 11) * 3f).toInt().coerceIn(1, 3)
    for (i in 0 until count) {
        val fx = 0.18f + seedFloat(seed, 20 + i) * 0.65f
        val fy = 0.42f + seedFloat(seed, 30 + i) * 0.45f
        val rr = cs * (0.045f + seedFloat(seed, 40 + i) * 0.030f)
        drawCircle(
            color = WallTopLight.copy(alpha = 0.88f),
            radius = rr,
            center = Offset(tl.x + cs * fx, tl.y + cs * fy)
        )
    }
}

private fun DrawScope.drawTileRock(seed: Int, tl: Offset, cs: Float) {
    val cx = tl.x + cs * (0.32f + seedFloat(seed, 50) * 0.36f)
    val cy = tl.y + cs * (0.55f + seedFloat(seed, 51) * 0.28f)
    val rr = cs * (0.10f + seedFloat(seed, 52) * 0.04f)
    // 그림자
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = rr,
        center = Offset(cx + 1.2f, cy + 2.0f)
    )
    // 본체
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFC4CED4), Color(0xFF8A98A2)),
            startY = cy - rr,
            endY = cy + rr
        ),
        radius = rr,
        center = Offset(cx, cy)
    )
    // 하이라이트
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = rr * 0.28f,
        center = Offset(cx - rr * 0.35f, cy - rr * 0.35f)
    )
}

private fun DrawScope.drawTileFlower(seed: Int, tl: Offset, cs: Float) {
    val cx = tl.x + cs * (0.30f + seedFloat(seed, 60) * 0.42f)
    val cy = tl.y + cs * (0.40f + seedFloat(seed, 61) * 0.40f)
    val petalR = cs * (0.065f + seedFloat(seed, 62) * 0.020f)
    val petalDist = cs * 0.07f
    val petalColor = when ((seedFloat(seed, 63) * 3f).toInt().coerceIn(0, 2)) {
        0 -> Color(0xFFFFC1CC) // 분홍
        1 -> Color(0xFFFFD89C) // 살구
        else -> Color(0xFFCDB6F5) // 라벤더
    }
    for (i in 0 until 5) {
        val a = (i * (2.0 * PI / 5.0) - PI / 2.0).toFloat()
        val px = cx + cos(a) * petalDist
        val py = cy + sin(a) * petalDist
        drawCircle(color = petalColor, radius = petalR, center = Offset(px, py))
    }
    drawCircle(
        color = Color(0xFFFFE066),
        radius = cs * 0.048f,
        center = Offset(cx, cy)
    )
}

private fun tileSeed(c: Int, r: Int): Int = (c * 73856093) xor (r * 19349663)

private fun seedFloat(seed: Int, salt: Int): Float {
    var v = (seed xor (salt * 0x27d4eb2d.toInt())).toLong() and 0xFFFFFFFFL
    v = (v xor (v shr 16)) and 0xFFFFFFFFL
    v = (v * 0x85ebca6bL) and 0xFFFFFFFFL
    v = (v xor (v shr 13)) and 0xFFFFFFFFL
    return v.toFloat() / 0xFFFFFFFFL.toFloat()
}

private fun shiftBrightness(color: Color, shift: Float): Color {
    val s = 1f + shift
    return Color(
        red = (color.red * s).coerceIn(0f, 1f),
        green = (color.green * s).coerceIn(0f, 1f),
        blue = (color.blue * s).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

private fun DrawScope.drawGoal(maze: Maze, pulse: Float, rayRotation: Float, sparkleTime: Float) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + (maze.goalCol + 0.5f) * cs
    val cy = origin.y + (maze.goalRow + 0.5f) * cs
    val center = Offset(cx, cy)

    // 회전 광선
    drawGoalRays(center, cs, rayRotation)

    // 후광
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoalGold.copy(alpha = 0.55f * pulse), GoalGold.copy(alpha = 0f)),
            center = center,
            radius = cs * 0.95f * pulse
        ),
        center = center,
        radius = cs * 0.95f * pulse
    )

    // 별 — 펄스에 따라 크기 + ±5° wobble
    val wobble = (pulse - 1.0f) * 60f
    rotate(degrees = wobble, pivot = center) {
        drawStar(
            center = center,
            outerR = cs * 0.42f * pulse,
            innerR = cs * 0.18f * pulse,
            fill = GoalGold,
            stroke = GoalGoldDeep
        )
    }

    // 궤도 위 반짝임
    drawGoalSparkles(center, cs, sparkleTime)
}

private fun DrawScope.drawGoalRays(center: Offset, cs: Float, rotationDeg: Float) {
    val rayCount = 8
    val innerR = cs * 0.55f
    rotate(degrees = rotationDeg, pivot = center) {
        for (i in 0 until rayCount) {
            val isLong = i % 2 == 0
            val outerR = if (isLong) cs * 1.45f else cs * 1.05f
            val aDeg = i * (360f / rayCount) - 90f
            val a = aDeg * (PI.toFloat() / 180f)
            val sx = center.x + cos(a) * innerR
            val sy = center.y + sin(a) * innerR
            val ex = center.x + cos(a) * outerR
            val ey = center.y + sin(a) * outerR
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GoalGold.copy(alpha = if (isLong) 0.42f else 0.24f),
                        GoalGold.copy(alpha = 0f)
                    ),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey)
                ),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = cs * if (isLong) 0.11f else 0.08f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawGoalSparkles(center: Offset, cs: Float, time: Float) {
    val count = 4
    for (i in 0 until count) {
        val baseAngle = i * (360f / count)
        val breathePhase = (time * 2f + i * 0.3f) * 2f * PI.toFloat()
        val orbitRadius = cs * (0.82f + 0.10f * sin(breathePhase))
        val orbitAngle = (baseAngle + time * 90f) * (PI.toFloat() / 180f)
        val px = center.x + cos(orbitAngle) * orbitRadius
        val py = center.y + sin(orbitAngle) * orbitRadius
        val twinklePhase = (time * 1.5f + i * 0.25f) % 1f
        val twinkle = (0.5f + 0.5f * sin(twinklePhase * 2f * PI.toFloat())).coerceIn(0f, 1f)
        drawCircle(
            color = GoalGold.copy(alpha = twinkle * 0.85f),
            radius = cs * 0.055f * (0.7f + 0.3f * twinkle),
            center = Offset(px, py)
        )
    }
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
    squashAxisIsX: Boolean,
    ballScale: Float = 1f,
    headingRad: Float = 0f,
    isHappy: Boolean = false
) {
    val cs = cellSize(maze)
    val origin = originOffset(maze, cs)
    val cx = origin.x + bx * cs
    val cy = origin.y + by * cs
    val r = cs * 0.36f * ballScale.coerceIn(0f, 1f)
    if (r <= 0f) return

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

        // 눈
        val eyeSpacing = r * 0.34f
        val eyeY = cy - r * 0.08f
        if (isHappy) {
            val eyeW = r * 0.28f
            val eyeH = r * 0.16f
            val stroke = r * 0.09f
            drawHappyEye(Offset(cx - eyeSpacing, eyeY), eyeW, eyeH, stroke)
            drawHappyEye(Offset(cx + eyeSpacing, eyeY), eyeW, eyeH, stroke)
        } else {
            val eyeR = r * 0.22f
            val pupilR = r * 0.10f
            val pupilDx = cos(headingRad) * r * 0.06f
            val pupilDy = sin(headingRad) * r * 0.06f
            // 흰자
            drawCircle(Color.White, eyeR, Offset(cx - eyeSpacing, eyeY))
            drawCircle(Color.White, eyeR, Offset(cx + eyeSpacing, eyeY))
            // 동공 — 이동 방향으로 살짝 이동
            drawCircle(
                Color(0xFF1A1A1A),
                pupilR,
                Offset(cx - eyeSpacing + pupilDx, eyeY + pupilDy)
            )
            drawCircle(
                Color(0xFF1A1A1A),
                pupilR,
                Offset(cx + eyeSpacing + pupilDx, eyeY + pupilDy)
            )
            // 동공 광택
            drawCircle(
                Color.White.copy(alpha = 0.8f),
                pupilR * 0.32f,
                Offset(cx - eyeSpacing + pupilDx - pupilR * 0.3f, eyeY + pupilDy - pupilR * 0.3f)
            )
            drawCircle(
                Color.White.copy(alpha = 0.8f),
                pupilR * 0.32f,
                Offset(cx + eyeSpacing + pupilDx - pupilR * 0.3f, eyeY + pupilDy - pupilR * 0.3f)
            )
        }
    }
}

private fun DrawScope.drawHappyEye(center: Offset, width: Float, height: Float, strokeW: Float) {
    val path = Path().apply {
        moveTo(center.x - width / 2f, center.y + height / 2f)
        lineTo(center.x, center.y - height / 2f)
        lineTo(center.x + width / 2f, center.y + height / 2f)
    }
    drawPath(
        path = path,
        color = Color(0xFF1A1A1A),
        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
