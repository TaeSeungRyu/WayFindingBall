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
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.ColorGame
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
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
    var wrongFlash by remember(attemptId) { mutableFloatStateOf(0f) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }

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
        var lastZone: Int? = ColorGame.zoneAt(stage, floor(physics.x).toInt(), floor(physics.y).toInt())

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
            ballX = physics.x
            ballY = physics.y
            wrongFlash = (wrongFlash - dt).coerceAtLeast(0f)

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val zone = ColorGame.zoneAt(stage, bc, br)
            if (zone != null && zone != lastZone) {
                if (zone == targetSeq[targetIndex]) {
                    score += 1
                    SoundManager.playGoal()
                    targetIndex += 1
                    if (targetIndex >= stage.targetCount) finished = true
                } else {
                    SoundManager.playBonk()
                    wrongFlash = 0.4f
                }
            }
            lastZone = zone
        }
    }

    val target = stage.zones[targetSeq.getOrElse(targetIndex) { targetSeq.last() }]

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
                    zones = stage.zones,
                    ballX = ballX,
                    ballY = ballY,
                    targetZoneIndex = targetSeq.getOrElse(targetIndex) { targetSeq.last() },
                    skin = currentSkin,
                    pulse = pulse,
                    wrongFlash = wrongFlash,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 조작 패드
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DPad(
                    onInput = { dx, dy -> kx = dx; ky = dy },
                    enabled = !finished
                )
            }
        }

        if (finished) {
            ColorResultOverlay(
                score = score,
                elapsedMs = elapsedMs,
                onRetry = { attemptId += 1 },
                onHome = onExit,
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

        // 벽 셀(테두리 + 내부 벽 모두)
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
            // 목표 색칸은 깜빡이는 테두리로 강조
            if (i == targetZoneIndex) {
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

        // 오답 시 붉은 깜빡임
        if (wrongFlash > 0f) {
            drawRoundRect(
                color = CoralPink.copy(alpha = wrongFlash * 0.4f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(24f, 24f),
            )
        }

        // 공
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
            Text("🎉", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "참 잘했어요!",
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "색깔 ${score}개를 모두 찾았어요",
                color = InkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = formatElapsed(elapsedMs),
                color = CoralPink,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
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
