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
import kotlin.math.sin

private const val SENSOR_ACCEL_GAIN = 30f
private const val KEYPAD_ACCEL_GAIN = 16f
private const val SENSOR_MAX_SPEED = 20f
private const val KEYPAD_MAX_SPEED = 14f
private const val HIT_RADIUS = 0.55f

@Composable
fun HitGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { HitGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val arena = remember(attemptId) { HitGame.buildArena(stage) }
    val physics = remember(attemptId) {
        BallPhysics(arena, radius = 0.3f, friction = 0.9f, restitution = 0.7f)
    }
    val targets = remember(attemptId) {
        mutableStateListOf<HitTarget>().apply { addAll(HitGame.spawnTargets(stage, arena)) }
    }
    val totalTargets = remember(attemptId) { targets.size }
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
            ballX = physics.x
            ballY = physics.y

            // 표적 충돌 검사
            val it = targets.iterator()
            var hitAny = false
            while (it.hasNext()) {
                val t = it.next()
                val dx = physics.x - t.cx
                val dy = physics.y - t.cy
                if (dx * dx + dy * dy < HIT_RADIUS * HIT_RADIUS) {
                    it.remove()
                    hitAny = true
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
                    text = "공을 굴려 표적을 모두 맞혀요!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                )
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                HitArenaCanvas(
                    arena = arena,
                    targets = targets,
                    ballX = ballX,
                    ballY = ballY,
                    skin = currentSkin,
                    pulse = pulse,
                )
            }

            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DPad(onInput = { dx, dy -> kx = dx; ky = dy }, enabled = !finished)
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
    targets: List<HitTarget>,
    ballX: Float,
    ballY: Float,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
) {
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

        // 벽
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

        // 표적 (과녁)
        val pulseScale = 1f + 0.08f * sin(pulse * 4f)
        for (t in targets) {
            val cx = t.cx * cell
            val cy = t.cy * cell
            val r = cell * 0.38f * pulseScale
            drawCircle(GoalGold, radius = r, center = Offset(cx, cy))
            drawCircle(Color.White, radius = r * 0.66f, center = Offset(cx, cy))
            drawCircle(GoalGold, radius = r * 0.36f, center = Offset(cx, cy))
            drawCircle(GoalGoldDeep, radius = r, center = Offset(cx, cy), style = Stroke(width = cell * 0.05f))
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
