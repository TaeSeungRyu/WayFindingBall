package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.FloorPaintController
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// 공끼리 충돌(튕김) — bounceBalls에서만 사용.
private const val BOUNCE_E = 1.0f          // 탄성(1=완전 튕김)
private const val BOUNCE_MIN = 4.0f        // 살짝 닿아도 이 속도로는 튕긴다

/** 나타났다 사라지는 동적 벽 한 개. [life]는 남은 수명(초). */
internal class TempWall(val c: Int, val r: Int, var life: Float)

/** 도화선이 타들어가는 폭탄. [fuse]는 터지기까지 남은 시간(초). */
internal class Bomb(val c: Int, val r: Int, var fuse: Float)

/** 폭발 연출 한 개. [age]는 시작 후 경과(초). */
internal class Blast(val x: Float, val y: Float, var age: Float)

/**
 * 두 공이 겹치면 겹침을 풀고 법선 방향으로 튕겨낸다(질량 동일·탄성 e).
 * @return 실제로 튕김 임펄스를 준 경우 true(호출부에서 넉백 시간 부여용).
 */
internal fun bounceBalls(a: BallPhysics, b: BallPhysics, radius: Float = 0.32f, e: Float = BOUNCE_E): Boolean {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val distSq = dx * dx + dy * dy
    val minDist = radius * 2f
    if (distSq >= minDist * minDist || distSq < 1e-6f) return false
    val dist = sqrt(distSq)
    val nx = dx / dist
    val ny = dy / dist
    // 겹침 분리 — 각자 절반씩 밀어낸다.
    val overlap = minDist - dist
    a.nudgePosition(-nx * overlap / 2f, -ny * overlap / 2f)
    b.nudgePosition(nx * overlap / 2f, ny * overlap / 2f)
    // 법선 방향 상대속도가 접근(음수)일 때 튕김. 살짝 닿아도 최소 세기는 준다.
    val vn = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
    if (vn < 0.5f) {
        val j = maxOf(-(1f + e) * vn / 2f, BOUNCE_MIN)
        a.applyImpulse(-j * nx, -j * ny)
        b.applyImpulse(j * nx, j * ny)
        return true
    }
    return false
}

/** AI 위치에서 가장 가까운 '도달 가능하고 아직 안 칠한' 칸을 찾는다. 없으면 null. */
internal fun nearestUnpainted(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
): Pair<Int, Int>? {
    var best: Pair<Int, Int>? = null
    var bestD = Float.MAX_VALUE
    for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
        if (!paint.isReachable(c, r) || paint.isPainted(c, r)) continue
        val dx = (c + 0.5f) - x
        val dy = (r + 0.5f) - y
        val d = dx * dx + dy * dy
        if (d < bestD) { bestD = d; best = c to r }
    }
    return best
}

/**
 * [myIdx] 색이 아닌(빈 칸 또는 남의 색) 가장 가까운 도달 가능 칸. 땅따먹기 AI용.
 * 지금 서 있는 칸은 제외한다 — 그 칸이 뺏겨 최단이 되면 목표가 제자리라 AI가 얼어붙기 때문.
 */
private fun nearestNotMine(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
    myIdx: Int,
): Pair<Int, Int>? {
    val curC = floor(x).toInt()
    val curR = floor(y).toInt()
    var best: Pair<Int, Int>? = null
    var bestD = Float.MAX_VALUE
    for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
        if (c == curC && r == curR) continue
        if (!paint.isReachable(c, r) || paint.colorAt(c, r) == myIdx) continue
        val dx = (c + 0.5f) - x
        val dy = (r + 0.5f) - y
        val d = dx * dx + dy * dy
        if (d < bestD) { bestD = d; best = c to r }
    }
    return best
}

/**
 * 땅따먹기 AI의 다음 목표 칸. 대개 가장 가까운 '내 것 아닌' 칸으로 가되,
 * 30% 확률로 무작위 칸을 골라 세 공이 한곳에 뭉치지 않고 맵을 넓게 돌게 한다.
 */
internal fun pickAiTarget(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
    myIdx: Int,
    rnd: Random,
    starCells: Set<Pair<Int, Int>> = emptySet(),
): Pair<Int, Int>? {
    // 별 구역이 있으면 절반은 가장 가까운 '내 것 아닌' 별 칸을 노린다 — 핵심 구역 쟁탈.
    if (starCells.isNotEmpty() && rnd.nextFloat() < 0.5f) {
        var best: Pair<Int, Int>? = null
        var bestD = Float.MAX_VALUE
        for ((c, r) in starCells) {
            if (!paint.isReachable(c, r) || paint.colorAt(c, r) == myIdx) continue
            val dx = (c + 0.5f) - x
            val dy = (r + 0.5f) - y
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = c to r }
        }
        if (best != null) return best
    }
    if (rnd.nextFloat() < 0.3f) {
        val curC = floor(x).toInt()
        val curR = floor(y).toInt()
        val candidates = ArrayList<Pair<Int, Int>>()
        for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
            if (c == curC && r == curR) continue
            if (!paint.isReachable(c, r) || paint.colorAt(c, r) == myIdx) continue
            candidates.add(c to r)
        }
        if (candidates.isNotEmpty()) return candidates[rnd.nextInt(candidates.size)]
    }
    return nearestNotMine(paint, arena, x, y, myIdx)
}

@Composable
internal fun PaintArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    paint: FloorPaintController,
    palette: List<Color>,
    ballX: Float,
    ballY: Float,
    rivals: List<Pair<Offset, Color>> = emptyList(),
    chaser: Offset? = null,
    playerStunned: Boolean = false,
    bombs: List<Pair<Offset, Float>> = emptyList(),
    blasts: List<Pair<Offset, Float>> = emptyList(),
    starCells: Set<Pair<Int, Int>> = emptySet(),
    cnTarget: Array<IntArray>? = null,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
) {
    val starPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FFF3B0")
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF3EFE7))
    ) {
        val n = arena.cols
        val cell = size.minDimension / n
        val wallColor = Color(0xFFCBB89B)
        val unpainted = Color(0xFFE7E0D3)
        val inset = cell * 0.06f
        val full = cell - inset * 2

        // 바닥 칸(도달 가능한 칸만): 칠한 칸은 그 칸의 색, 아직 안 칠한 칸은 연한 베이스.
        paint.version  // 변경 시 재구성 트리거
        for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
            if (!paint.isReachable(c, r)) continue
            val painted = paint.colorAt(c, r)
            val col = if (cnTarget != null) {
                // 색칠 도안: 도안 칸은 안내(연한 목표색)→맞게 칠하면 진하게, 틀리면 칠한 색. 배경은 베이스.
                val tgt = cnTarget[r][c]
                when {
                    tgt < 0 -> unpainted
                    painted < 0 -> palette[tgt.coerceIn(0, palette.lastIndex)].copy(alpha = 0.25f)
                    painted == tgt -> palette[tgt.coerceIn(0, palette.lastIndex)]
                    else -> palette[painted.coerceIn(0, palette.lastIndex)]
                }
            } else {
                if (painted >= 0) palette[painted.coerceIn(0, palette.lastIndex)] else unpainted
            }
            drawRoundRect(
                color = col,
                topLeft = Offset(c * cell + inset, r * cell + inset),
                size = Size(full, full),
                cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f),
            )
        }

        // 별 구역 표시 — 2배 점수 칸에 ★ (칠한 색 위에 살짝 얹어 표시).
        if (starCells.isNotEmpty()) {
            starPaint.textSize = cell * 0.82f
            starPaint.alpha = 235
            for ((c, r) in starCells) {
                drawContext.canvas.nativeCanvas.drawText(
                    "★", c * cell + cell / 2f, r * cell + cell * 0.72f, starPaint,
                )
            }
        }

        // 벽 셀(테두리 + 내부 벽)
        for (r in 0 until arena.rows) for (c in 0 until arena.cols) {
            if (arena.isWall(c, r)) {
                drawRoundRect(
                    color = wallColor,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
                )
            }
        }

        // 폭탄 — 검은 공에 심지. 터질 때가 가까울수록(fuseFrac→0) 빨갛게 빠르게 깜빡.
        for ((pos, fuseFrac) in bombs) {
            val bx = pos.x * cell
            val by = pos.y * cell
            val br = cell * 0.32f
            val danger = 1f - fuseFrac
            val blink = 0.5f + 0.5f * sin(pulse * (6f + danger * 34f))
            drawCircle(
                color = Color(0xFFE53935).copy(alpha = (0.2f + 0.6f * danger * blink).coerceIn(0f, 1f)),
                radius = br * (1.6f + danger),
                center = Offset(bx, by),
            )
            drawCircle(Color(0xFF212121), radius = br, center = Offset(bx, by))
            drawCircle(Color(0xFFFFB300), radius = br * 0.22f, center = Offset(bx, by - br * 1.05f))
        }

        // AI 라이벌 공들 (대결 모드) — 눈 달린 색 공.
        for ((pos, rivalColor) in rivals) {
            val er = cell * 0.4f
            val ex = pos.x * cell
            val ey = pos.y * cell
            drawCircle(rivalColor.copy(alpha = 0.35f), radius = er * 1.5f, center = Offset(ex, ey))
            drawCircle(rivalColor, radius = er, center = Offset(ex, ey))
            val eyeDx = er * 0.32f
            val eyeY = ey - er * 0.06f
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.12f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.12f, center = Offset(ex + eyeDx, eyeY))
        }

        // 술래(방해꾼) — 어두운 공에 눈.
        if (chaser != null) {
            val er = cell * 0.42f
            val ex = chaser.x * cell
            val ey = chaser.y * cell
            drawCircle(Color(0xFF6A1B9A).copy(alpha = 0.35f), radius = er * 1.6f, center = Offset(ex, ey))
            drawCircle(Color(0xFF4A148C), radius = er, center = Offset(ex, ey))
            val eyeDx = er * 0.34f
            val eyeY = ey - er * 0.05f
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.13f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.13f, center = Offset(ex + eyeDx, eyeY))
        }

        // 공
        val r = cell * 0.4f
        val cx = ballX * cell
        val cy = ballY * cell
        drawBallDecoration(skin, cx, cy, r, phaseSec = pulse)
        drawBallBody(skin, cx, cy, r)
        // 기절 표시 — 붉은 링.
        if (playerStunned) {
            drawCircle(
                color = Color(0xFFE53935),
                radius = r * 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = cell * 0.06f),
            )
        }

        // 폭발 연출 — 퍼지며 옅어지는 원.
        for ((pos, prog) in blasts) {
            val ex = pos.x * cell
            val ey = pos.y * cell
            val rad = cell * (0.4f + prog * BOMB_BLAST_R)
            drawCircle(Color(0xFFFFC107).copy(alpha = (1f - prog) * 0.6f), radius = rad, center = Offset(ex, ey))
            drawCircle(Color.White.copy(alpha = (1f - prog) * 0.5f), radius = rad * 0.6f, center = Offset(ex, ey))
        }
    }
}

@Composable
internal fun ColorPalettePicker(
    palette: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
    enabled: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        palette.forEachIndexed { i, c ->
            val isSel = i == selected
            Box(
                modifier = Modifier
                    .size(if (isSel) 48.dp else 40.dp)
                    .shadow(if (isSel) 6.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(c)
                    .then(
                        if (isSel) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .clickable(enabled = enabled) { onSelect(i) }
            )
        }
    }
}

@Composable
internal fun PaintResultOverlay(
    elapsedMs: Long,
    stars: Int,
    isNewBest: Boolean,
    versus: Boolean = false,
    won: Boolean = false,
    draw: Boolean = false,
    counts: List<Int> = emptyList(),
    colors: List<Color> = emptyList(),
    onSaveArt: (() -> Unit)? = null,
    onRetry: () -> Unit,
    onHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when {
                    !versus -> "🎨"
                    draw -> "🤝"
                    won -> "🏆"
                    else -> "😢"
                },
                fontSize = 56.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    !versus -> "참 잘했어요!"
                    draw -> "비겼어요!"
                    won -> "이겼어요!"
                    else -> "졌어요!"
                },
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            if (versus) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    colors.forEachIndexed { i, c ->
                        if (i > 0) {
                            Text(" : ", color = InkSoft, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "${counts.getOrElse(i) { 0 }}",
                            color = c,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "칸을 더 많이 차지하면 이겨요",
                    color = InkSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isNewBest) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "✨ 최고 점수! ✨",
                        color = CoralPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            } else {
                Text(
                    text = "바닥을 모두 칠했어요",
                    color = InkSoft,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Text(
                            text = "★",
                            color = if (i < stars) CoralPink else InkSoft.copy(alpha = 0.25f),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = formatElapsed(elapsedMs),
                    color = CoralPink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                )
                if (isNewBest) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "★ 최고 기록! ★",
                        color = CoralPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            if (onSaveArt != null) {
                Spacer(Modifier.height(16.dp))
                PaintResultButton("📷 사진 저장", Color(0xFF26A69A), onSaveArt, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaintResultButton("나가기", SkyBlue, onHome, Modifier.weight(1f))
                PaintResultButton("다시 해요", CoralPink, onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PaintResultButton(label: String, bg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
