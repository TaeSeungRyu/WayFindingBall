package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.ColorRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.ChaserController
import com.rts.rys.ryy.wayfinding.game.ColorGame
import com.rts.rys.ryy.wayfinding.game.DynamicMazeController
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.sin

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f

@Composable
fun ColorGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { ColorGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val arena = remember(attemptId) { ColorGame.buildArena(stage) }
    val physics = remember(attemptId) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) }
    val targetSeq = remember(attemptId) { ColorGame.targetSequence(stage) }
    val zones = remember(attemptId) {
        if (stage.shuffleColors) ColorGame.zonesWithShuffledColors(stage) else stage.zones
    }
    val dynamicWalls = remember(attemptId) {
        if (stage.dynamicWalls) {
            DynamicMazeController(
                arena,
                cyclePeriodS = 3.5f,
                maxChanges = 6,
                isProtected = { c, r -> zones.any { it.contains(c, r) } },
            )
        } else null
    }
    val chaser = remember(attemptId) {
        if (stage.chaser) {
            ChaserController(arena, moveIntervalS = 0.45f, randomSpawnMinDistance = 6)
        } else null
    }
    val tilt = remember { TiltSensor(context) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val sensorEnabled by AppSettings.sensorEnabled

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    var ballX by remember(attemptId) { mutableFloatStateOf(physics.x) }
    var ballY by remember(attemptId) { mutableFloatStateOf(physics.y) }
    var targetIndex by remember(attemptId) { mutableIntStateOf(0) }
    var score by remember(attemptId) { mutableIntStateOf(0) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var caught by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var wrongFlash by remember(attemptId) { mutableFloatStateOf(0f) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    var chaserX by remember(attemptId) { mutableFloatStateOf(0f) }
    var chaserY by remember(attemptId) { mutableFloatStateOf(0f) }
    var showMemorize by remember(attemptId) { mutableStateOf(stage.memorizeOrder) }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    LaunchedEffect(attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        targetIndex = 0
        score = 0
        elapsedMs = 0L
        finished = false
        wrongFlash = 0f
        var lastZone: Int? = ColorGame.zoneAt(zones, floor(physics.x).toInt(), floor(physics.y).toInt())

        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (showMemorize) { last = 0L; continue }  // 기억 단계 동안 정지
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
            chaser?.let { ch ->
                ch.tick(dt, floor(physics.x).toInt(), floor(physics.y).toInt())
                chaserX = ch.visualX
                chaserY = ch.visualY
                val dx = physics.x - ch.visualX
                val dy = physics.y - ch.visualY
                if (dx * dx + dy * dy < 0.55f * 0.55f) {
                    caught = true
                    finished = true
                    SoundManager.playBonk()
                }
            }
            ballX = physics.x
            ballY = physics.y
            wrongFlash = (wrongFlash - dt).coerceAtLeast(0f)

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val zone = ColorGame.zoneAt(zones, bc, br)
            if (zone != null && zone != lastZone) {
                if (zone == targetSeq[targetIndex]) {
                    score += 1
                    SoundManager.playGoal()
                    targetIndex += 1
                    if (targetIndex >= stage.targetCount) {
                        isNewBest = ColorRecordsRepository(context).record(level, elapsedMs)
                        finished = true
                    }
                } else {
                    SoundManager.playBonk()
                    wrongFlash = 0.4f
                }
            }
            lastZone = zone
        }
    }

    val target = zones[targetSeq.getOrElse(targetIndex) { targetSeq.last() }]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더: 뒤로 + 진행도
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = onExit, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "${score} / ${stage.targetCount}",
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

            // 목표 색 안내
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (stage.memorizeOrder) {
                    Text(
                        text = "기억한 순서대로 가요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(target.color)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "${target.name}으로 가요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 광장
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                ColorArenaCanvas(
                    arena = arena,
                    zones = zones,
                    ballX = ballX,
                    ballY = ballY,
                    targetZoneIndex = targetSeq.getOrElse(targetIndex) { targetSeq.last() },
                    skin = currentSkin,
                    pulse = pulse,
                    wrongFlash = wrongFlash,
                    dynamic = dynamicWalls,
                    dark = stage.dark,
                    chaserX = if (chaser != null) chaserX else null,
                    chaserY = if (chaser != null) chaserY else null,
                    highlightTarget = !stage.memorizeOrder,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 조작 패드
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DPad(
                    onInput = { dx, dy -> kx = dx; ky = dy },
                    enabled = !finished && !showMemorize
                )
            }
        }

        if (showMemorize) {
            MemorizeOverlay(
                steps = targetSeq.map { zones[it] },
                onDone = { showMemorize = false },
            )
        }

        if (finished) {
            ColorResultOverlay(
                score = score,
                elapsedMs = elapsedMs,
                isNewBest = isNewBest,
                caught = caught,
                onRetry = { attemptId += 1 },
                onHome = onExit,
            )
        }
    }
}

@Composable
private fun MemorizeOverlay(
    steps: List<com.rts.rys.ryy.wayfinding.game.ColorZone>,
    onDone: () -> Unit,
) {
    var highlight by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        delay(600)
        for (i in steps.indices) {
            highlight = i
            delay(850)
            highlight = -1
            delay(200)
        }
        delay(300)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("순서를 기억하세요!", color = InkDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, zone ->
                    val on = i == highlight
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (on) 56.dp else 40.dp)
                                .clip(CircleShape)
                                .background(zone.color.copy(alpha = if (on) 1f else 0.35f)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${i + 1}",
                            color = if (on) InkDark else InkSoft,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "잘 보고 순서대로 굴려요",
                color = InkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ColorArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    zones: List<com.rts.rys.ryy.wayfinding.game.ColorZone>,
    ballX: Float,
    ballY: Float,
    targetZoneIndex: Int,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
    wrongFlash: Float,
    dynamic: DynamicMazeController? = null,
    dark: Boolean = false,
    chaserX: Float? = null,
    chaserY: Float? = null,
    highlightTarget: Boolean = true,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF3EFE7))
    ) {
        val n = ColorGame.SIZE
        val cell = size.minDimension / n
        val wallColor = Color(0xFFCBB89B)

        // 벽 셀(테두리 + 내부 벽 모두). dynamic.version 읽기로 변경 시 재구성 트리거.
        dynamic?.version
        for (r in 0 until arena.rows) {
            for (c in 0 until arena.cols) {
                if (arena.isWall(c, r)) {
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
                    )
                }
            }
        }

        // 곧 생길/사라질 벽 미리보기(페이드)
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

        // 색칸
        for ((i, zone) in zones.withIndex()) {
            val left = zone.cMin * cell
            val top = zone.rMin * cell
            val w = (zone.cMax - zone.cMin + 1) * cell
            val h = (zone.rMax - zone.rMin + 1) * cell
            drawRoundRect(
                color = zone.color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f),
            )
            // 목표 색칸은 깜빡이는 테두리로 강조 (기억 모드에선 끔)
            if (i == targetZoneIndex && highlightTarget) {
                val glow = (sin(pulse * 4f) * 0.5f + 0.5f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.5f + 0.5f * glow),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f),
                    style = Stroke(width = cell * 0.18f)
                )
            }
        }

        // 어둠: 공 주변만 보이고 나머지는 가려짐 (색칸 위치를 기억해야 함)
        if (dark) {
            val ballPx = Offset(ballX * cell, ballY * cell)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0f),
                        0.45f to Color.Black.copy(alpha = 0f),
                        1f to Color.Black.copy(alpha = 0.94f),
                    ),
                    center = ballPx,
                    radius = cell * 3.2f,
                ),
                size = size,
            )
        }

        // 오답 시 붉은 깜빡임
        if (wrongFlash > 0f) {
            drawRoundRect(
                color = CoralPink.copy(alpha = wrongFlash * 0.4f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(24f, 24f),
            )
        }

        // 술래(적)
        if (chaserX != null && chaserY != null) {
            val er = cell * 0.4f
            val ex = chaserX * cell
            val ey = chaserY * cell
            drawCircle(Color(0xFF6A1B9A), radius = er, center = Offset(ex, ey))
            drawCircle(Color(0xFF4A148C), radius = er, center = Offset(ex, ey), style = Stroke(width = cell * 0.08f))
            // 눈
            val eyeDx = er * 0.32f
            val eyeY = ey - er * 0.08f
            drawCircle(Color.White, radius = er * 0.24f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.24f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.11f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.11f, center = Offset(ex + eyeDx, eyeY))
        }

        // 공 (어둠 위에 그려 항상 보이게)
        val r = cell * 0.4f
        val cx = ballX * cell
        val cy = ballY * cell
        drawBallDecoration(skin, cx, cy, r, phaseSec = pulse)
        drawBallBody(skin, cx, cy, r)
    }
}

@Composable
private fun ColorResultOverlay(
    score: Int,
    elapsedMs: Long,
    isNewBest: Boolean,
    caught: Boolean,
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
            Text(if (caught) "😵" else "🎉", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (caught) "잡혔어요!" else "참 잘했어요!",
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (caught) "술래에게 잡혔어요" else "색깔 ${score}개를 모두 찾았어요",
                color = InkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!caught) {
                Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultButton("나가기", SkyBlue, onHome, Modifier.weight(1f))
                ResultButton("다시 해요", CoralPink, onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultButton(label: String, bg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
