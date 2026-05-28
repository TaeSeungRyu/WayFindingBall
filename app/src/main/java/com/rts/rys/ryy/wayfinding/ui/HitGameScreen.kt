package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.HitRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.DynamicMazeController
import com.rts.rys.ryy.wayfinding.game.HitGame
import com.rts.rys.ryy.wayfinding.game.HitTarget
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGoldDeep
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val SENSOR_ACCEL_GAIN = 30f
private const val KEYPAD_ACCEL_GAIN = 16f
private const val SENSOR_MAX_SPEED = 20f
private const val KEYPAD_MAX_SPEED = 14f
private const val HIT_RADIUS = 0.55f
private const val TARGET_SPEED = 2.4f  // 움직이는 표적 속도 (칸/초)
// 포켓볼 모드 (stage.pockets=true) 전용 상수
private const val CUE_RADIUS = 0.3f          // HitGame의 큐볼 BallPhysics radius와 일치
private const val TARGET_RADIUS = 0.3f       // 목적공 반지름
private const val TARGET_FRICTION = 0.9f     // 목적공 감속 (cell/s 매초)
private const val TARGET_MAX_SPEED = 14f
private const val TARGET_RESTITUTION = 0.85f // 목적공 벽 반사 (큐볼 0.7보다 살짝 통통)
// 포켓볼 모드 번호별 공 색 — 1번부터. 표적 수가 색 수보다 많으면 순환.
private val POOL_BALL_COLORS = listOf(
    Color(0xFFFFD24A), // 1: 노랑
    Color(0xFF4DA6FF), // 2: 파랑
    Color(0xFFE03B3B), // 3: 빨강
    Color(0xFFB060E0), // 4: 보라
    Color(0xFFFF8E3C), // 5: 주황
    Color(0xFF2DBE5B), // 6: 초록
    Color(0xFF8E3A1F), // 7: 갈색
    Color(0xFF1A1A1A), // 8: 검정
)

/** 런타임 표적: 부동 좌표 + 속도(정지 표적은 0). */
private class LiveTarget(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val order: Int,
)

@Composable
fun HitGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { HitGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val pockets = remember(stage) { HitGame.pocketsFor(stage) }
    val pocketSet = remember(pockets) { pockets.toSet() }
    val arena = remember(attemptId) { HitGame.buildArena(stage) }
    val physics = remember(attemptId) {
        BallPhysics(arena, radius = 0.3f, friction = 0.9f, restitution = 0.7f)
    }
    val targets = remember(attemptId) {
        val spawned = HitGame.spawnTargets(stage, arena).map { t ->
            if (stage.moving) {
                val ang = Random.nextFloat() * (2f * Math.PI.toFloat())
                LiveTarget(t.cx, t.cy, cos(ang) * TARGET_SPEED, sin(ang) * TARGET_SPEED, t.order)
            } else {
                LiveTarget(t.cx, t.cy, 0f, 0f, t.order)
            }
        }
        mutableStateListOf<LiveTarget>().apply { addAll(spawned) }
    }
    val totalTargets = remember(attemptId) { targets.size }
    val dynamicWalls = remember(attemptId) {
        if (stage.dynamicWalls) {
            DynamicMazeController(
                arena,
                cyclePeriodS = 3.5f,
                maxChanges = 5,
                // 현재 표적이 올라가 있는 칸에는 벽이 생기지 않게 보호
                isProtected = { c, r -> targets.any { floor(it.x).toInt() == c && floor(it.y).toInt() == r } },
            )
        } else null
    }
    val tilt = remember { TiltSensor(context) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val sensorEnabled by AppSettings.sensorEnabled

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    var ballX by remember(attemptId) { mutableFloatStateOf(physics.x) }
    var ballY by remember(attemptId) { mutableFloatStateOf(physics.y) }
    var score by remember(attemptId) { mutableIntStateOf(0) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    LaunchedEffect(attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        score = 0
        elapsedMs = 0L
        finished = false

        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            elapsedMs += (now - last) / 1_000_000L
            pulse += dt
            last = now

            val sensitivity = AppSettings.sensorSensitivity.value
            val offX = AppSettings.sensorOffsetX.value
            val offY = AppSettings.sensorOffsetY.value
            val sx = if (sensorEnabled) ((tilt.tiltX - offX) * sensitivity).coerceIn(-1f, 1f) else 0f
            val sy = if (sensorEnabled) ((tilt.tiltY - offY) * sensitivity).coerceIn(-1f, 1f) else 0f
            val useKeypad = kx != 0f || ky != 0f
            val ax: Float
            val ay: Float
            if (useKeypad) {
                ax = kx * KEYPAD_ACCEL_GAIN
                ay = ky * KEYPAD_ACCEL_GAIN
                physics.maxSpeed = KEYPAD_MAX_SPEED
            } else {
                ax = sx * SENSOR_ACCEL_GAIN
                ay = sy * SENSOR_ACCEL_GAIN
                physics.maxSpeed = if (sensorEnabled) SENSOR_MAX_SPEED else KEYPAD_MAX_SPEED
            }

            physics.step(dt, ax, ay)
            dynamicWalls?.tick(dt, physics.x, physics.y)
            ballX = physics.x
            ballY = physics.y

            // 움직이는 표적: 등속 이동 + 벽에서 반사 (포켓볼 모드 아닐 때만)
            if (stage.moving && !stage.pockets) {
                for (t in targets) {
                    val nx = t.x + t.vx * dt
                    if (arena.isWall(floor(nx).toInt(), floor(t.y).toInt())) t.vx = -t.vx
                    else t.x = nx
                    val ny = t.y + t.vy * dt
                    if (arena.isWall(floor(t.x).toInt(), floor(ny).toInt())) t.vy = -t.vy
                    else t.y = ny
                }
            }

            // 포켓볼 모드: 목적공도 굴러간다(마찰 + 벽 반사) → 큐볼과 등질량 탄성 충돌.
            if (stage.pockets) {
                val frDt = TARGET_FRICTION * dt
                for (t in targets) {
                    // 마찰로 감속
                    t.vx = if (abs(t.vx) <= frDt) 0f else t.vx - frDt * sign(t.vx)
                    t.vy = if (abs(t.vy) <= frDt) 0f else t.vy - frDt * sign(t.vy)
                    t.vx = t.vx.coerceIn(-TARGET_MAX_SPEED, TARGET_MAX_SPEED)
                    t.vy = t.vy.coerceIn(-TARGET_MAX_SPEED, TARGET_MAX_SPEED)
                    // 빠른 속도에서 벽 통과 방지를 위해 sub-step.
                    val moveX = t.vx * dt
                    val moveY = t.vy * dt
                    val steps = max(1, ((max(abs(moveX), abs(moveY)) / 0.1f) + 1).toInt())
                    val ssx = moveX / steps
                    val ssy = moveY / steps
                    repeat(steps) {
                        val nx2 = t.x + ssx
                        if (arena.isWall(floor(nx2).toInt(), floor(t.y).toInt())) {
                            t.vx = -t.vx * TARGET_RESTITUTION
                        } else {
                            t.x = nx2
                        }
                        val ny2 = t.y + ssy
                        if (arena.isWall(floor(t.x).toInt(), floor(ny2).toInt())) {
                            t.vy = -t.vy * TARGET_RESTITUTION
                        } else {
                            t.y = ny2
                        }
                    }
                }
                // 큐볼-목적공 등질량 탄성 충돌: 노멀 방향 속도 성분만 교환.
                val sumR = CUE_RADIUS + TARGET_RADIUS
                for (t in targets) {
                    val dxn = t.x - physics.x
                    val dyn = t.y - physics.y
                    val d2 = dxn * dxn + dyn * dyn
                    if (d2 >= sumR * sumR) continue
                    val d = sqrt(d2).coerceAtLeast(0.0001f)
                    val nx = dxn / d
                    val ny = dyn / d
                    val vn = (physics.vx - t.vx) * nx + (physics.vy - t.vy) * ny
                    if (vn <= 0f) continue  // 이미 멀어지는 중
                    physics.applyImpulse(-vn * nx, -vn * ny)
                    t.vx += vn * nx
                    t.vy += vn * ny
                    // 침투 분리(절반씩 밀어내기)
                    val overlap = sumR - d
                    physics.nudgePosition(-nx * overlap * 0.5f, -ny * overlap * 0.5f)
                    t.x += nx * overlap * 0.5f
                    t.y += ny * overlap * 0.5f
                    SoundManager.playBonk()
                }
                // 목적공끼리 등질량 탄성 충돌 (모든 쌍). 표적 5개면 10쌍이라 비용 무시 가능.
                val tSumR = TARGET_RADIUS + TARGET_RADIUS
                for (i in 0 until targets.size) {
                    for (j in i + 1 until targets.size) {
                        val a = targets[i]
                        val b = targets[j]
                        val dxn = b.x - a.x
                        val dyn = b.y - a.y
                        val d2 = dxn * dxn + dyn * dyn
                        if (d2 >= tSumR * tSumR) continue
                        val d = sqrt(d2).coerceAtLeast(0.0001f)
                        val nx = dxn / d
                        val ny = dyn / d
                        val vn = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny
                        if (vn <= 0f) continue  // 이미 멀어지는 중
                        a.vx -= vn * nx
                        a.vy -= vn * ny
                        b.vx += vn * nx
                        b.vy += vn * ny
                        val overlap = tSumR - d
                        a.x -= nx * overlap * 0.5f
                        a.y -= ny * overlap * 0.5f
                        b.x += nx * overlap * 0.5f
                        b.y += ny * overlap * 0.5f
                        // 가벼운 비빔 충돌은 무음, 의미 있는 임팩트만 소리.
                        if (vn > 1.0f) SoundManager.playBonk()
                    }
                }
                ballX = physics.x
                ballY = physics.y
            }

            // 표적 제거 판정
            var hitAny = false
            if (stage.pockets) {
                if (stage.ordered) {
                    // 순서대로 포켓(9단계+): 가장 작은 번호 표적만 포켓 입수 가능.
                    // 다른 번호가 포켓 위를 지나가도 그냥 통과(입수 무효).
                    val next = targets.minByOrNull { it.order }
                    if (next != null) {
                        val cell = floor(next.x).toInt() to floor(next.y).toInt()
                        if (cell in pocketSet) {
                            targets.remove(next)
                            hitAny = true
                        }
                    }
                } else {
                    // 자유 입수(8단계): 어느 공이든 포켓 셀에 들어오면 제거.
                    val it = targets.iterator()
                    while (it.hasNext()) {
                        val t = it.next()
                        val cell = floor(t.x).toInt() to floor(t.y).toInt()
                        if (cell in pocketSet) {
                            it.remove()
                            hitAny = true
                        }
                    }
                }
                // 큐볼이 포켓에 빠지면(스크래치) 시작점으로 리스폰. 점수 페널티는 없음(자녀용).
                val cueCell = floor(physics.x).toInt() to floor(physics.y).toInt()
                if (cueCell in pocketSet) {
                    physics.reset()
                    ballX = physics.x
                    ballY = physics.y
                    SoundManager.playBonk()
                }
            } else if (stage.ordered) {
                // 순서 모드: 가장 작은 번호 표적만 맞힐 수 있다.
                val next = targets.minByOrNull { it.order }
                if (next != null) {
                    val dx = physics.x - next.x
                    val dy = physics.y - next.y
                    if (dx * dx + dy * dy < HIT_RADIUS * HIT_RADIUS) {
                        targets.remove(next)
                        hitAny = true
                    }
                }
            } else {
                val it = targets.iterator()
                while (it.hasNext()) {
                    val t = it.next()
                    val dx = physics.x - t.x
                    val dy = physics.y - t.y
                    if (dx * dx + dy * dy < HIT_RADIUS * HIT_RADIUS) {
                        it.remove()
                        hitAny = true
                    }
                }
            }
            if (hitAny) {
                score = totalTargets - targets.size
                SoundManager.playGoal()
                if (targets.isEmpty()) {
                    isNewBest = HitRecordsRepository(context).record(level, elapsedMs)
                    finished = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = onExit, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "$score / $totalTargets",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = formatElapsed(elapsedMs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkSoft,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        stage.pockets && stage.ordered -> "1번부터 순서대로 포켓에 넣어요!"
                        stage.pockets -> "포켓에 공을 넣어요!"
                        stage.ordered -> "1번부터 순서대로 맞혀요!"
                        stage.dynamicWalls -> "벽이 생겼다 사라져요!"
                        else -> "공을 굴려 표적을 모두 맞혀요!"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                )
            }

            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val side = minOf(maxWidth, maxHeight)
                Box(
                    modifier = Modifier.size(side),
                    contentAlignment = Alignment.Center
                ) {
                    HitArenaCanvas(
                        arena = arena,
                        targets = targets,
                        ballX = ballX,
                        ballY = ballY,
                        skin = currentSkin,
                        pulse = pulse,
                        ordered = stage.ordered,
                        dynamic = dynamicWalls,
                        pockets = pockets,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SensorToggleChip(
                    sensorOn = sensorEnabled,
                    onToggle = { AppSettings.setSensorEnabled(!sensorEnabled) }
                )
                Spacer(Modifier.height(8.dp))
                DPad(onInput = { dx, dy -> kx = dx; ky = dy }, enabled = !finished && !sensorEnabled)
            }
        }

        if (finished) {
            HitResultOverlay(
                elapsedMs = elapsedMs,
                isNewBest = isNewBest,
                onRetry = { attemptId += 1 },
                onHome = onExit,
            )
        }
    }
}

@Composable
private fun HitArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    targets: List<LiveTarget>,
    ballX: Float,
    ballY: Float,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
    ordered: Boolean = false,
    dynamic: com.rts.rys.ryy.wayfinding.game.DynamicMazeController? = null,
    pockets: List<Pair<Int, Int>> = emptyList(),
) {
    val numberPaint = remember {
        android.graphics.Paint().apply {
            color = GoalGoldDeep.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFEAF4E0))
    ) {
        val n = HitGame.SIZE
        val cell = size.minDimension / n
        val wallColor = Color(0xFF7CB342)

        // 벽 (dynamic.version 읽기로 벽 변경 시 재구성 트리거)
        dynamic?.version
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

        // 곧 생길 벽 미리보기(페이드인 경고) — 갑자기 막히지 않도록 미리 알려준다.
        if (dynamic != null) {
            val prog = dynamic.previewProgress
            for (p in dynamic.pendingPreview) {
                if (!p.toWall) continue
                drawRoundRect(
                    color = wallColor.copy(alpha = 0.55f * prog),
                    topLeft = Offset(p.c * cell, p.r * cell),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
                )
            }
        }

        // 포켓(검은 구멍) — 공이 그 위로 굴러올라가도록 표적/공보다 아래에 그린다.
        for ((pc, pr) in pockets) {
            val pcx = pc * cell + cell / 2f
            val pcy = pr * cell + cell / 2f
            drawCircle(Color(0x33000000), radius = cell * 0.5f, center = Offset(pcx, pcy))   // 가장자리 그림자
            drawCircle(Color(0xFF1A1A1A), radius = cell * 0.42f, center = Offset(pcx, pcy))
            drawCircle(Color(0xFF000000), radius = cell * 0.32f, center = Offset(pcx, pcy))
        }

        // 표적 (과녁)
        val pulseScale = 1f + 0.08f * sin(pulse * 4f)
        for (t in targets) {
            val cx = t.x * cell
            val cy = t.y * cell
            val r = cell * 0.38f * pulseScale
            if (pockets.isNotEmpty()) {
                // 포켓볼 모드: 번호별 색 공 + 가운데 흰 번호판 (ordered 여부와 무관하게 번호 표시)
                val ballColor = POOL_BALL_COLORS[(t.order - 1).mod(POOL_BALL_COLORS.size)]
                drawCircle(ballColor, radius = r, center = Offset(cx, cy))
                drawCircle(
                    Color.Black.copy(alpha = 0.45f),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = cell * 0.05f),
                )
                drawCircle(Color.White, radius = r * 0.55f, center = Offset(cx, cy))
                numberPaint.color = InkDark.toArgb()
                numberPaint.textSize = cell * 0.36f
                drawContext.canvas.nativeCanvas.drawText(
                    t.order.toString(), cx, cy + cell * 0.13f, numberPaint
                )
            } else if (ordered) {
                // 일반 순서 모드(4·7단계): 금색 채운 원 + 번호
                drawCircle(GoalGold, radius = r, center = Offset(cx, cy))
                drawCircle(GoalGoldDeep, radius = r, center = Offset(cx, cy), style = Stroke(width = cell * 0.06f))
                numberPaint.color = GoalGoldDeep.toArgb()
                numberPaint.textSize = cell * 0.5f
                drawContext.canvas.nativeCanvas.drawText(
                    t.order.toString(), cx, cy + cell * 0.18f, numberPaint
                )
            } else {
                drawCircle(GoalGold, radius = r, center = Offset(cx, cy))
                drawCircle(Color.White, radius = r * 0.66f, center = Offset(cx, cy))
                drawCircle(GoalGold, radius = r * 0.36f, center = Offset(cx, cy))
                drawCircle(GoalGoldDeep, radius = r, center = Offset(cx, cy), style = Stroke(width = cell * 0.05f))
            }
        }

        // 공
        val br = cell * 0.4f
        drawBallDecoration(skin, ballX * cell, ballY * cell, br, phaseSec = pulse)
        drawBallBody(skin, ballX * cell, ballY * cell, br)
    }
}

@Composable
private fun HitResultOverlay(
    elapsedMs: Long,
    isNewBest: Boolean,
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
            Text("🎯", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text("다 맞혔어요!", color = InkDark, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Text(
                text = formatElapsed(elapsedMs),
                color = CoralPink,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
            if (isNewBest) {
                Spacer(Modifier.height(4.dp))
                Text("★ 최고 기록! ★", color = CoralPink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HitResultButton("나가기", SkyBlue, onHome, Modifier.weight(1f))
                HitResultButton("다시 해요", CoralPink, onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HitResultButton(label: String, bg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
