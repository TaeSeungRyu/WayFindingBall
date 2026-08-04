package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.LearnRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.LearnGame
import com.rts.rys.ryy.wayfinding.game.LearnItem
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
fun LearnGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { LearnGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val arena = remember(attemptId) { LearnGame.buildArena() }
    val physics = remember(attemptId) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) }
    val tilt = remember { TiltSensor(context) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val sensorEnabled by AppSettings.sensorEnabled

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    var ballX by remember(attemptId) { mutableFloatStateOf(physics.x) }
    var ballY by remember(attemptId) { mutableFloatStateOf(physics.y) }
    var reached by remember(attemptId) { mutableIntStateOf(0) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    var wrongFlash by remember(attemptId) { mutableFloatStateOf(0f) }
    var paused by remember(level) { mutableStateOf(false) }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    BackHandler(enabled = !paused && !finished) { paused = true }

    LaunchedEffect(attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        reached = 0
        elapsedMs = 0L
        finished = false
        var lastCell = floor(physics.x).toInt() to floor(physics.y).toInt()
        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (paused) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            elapsedMs += (now - last) / 1_000_000L
            pulse += dt
            wrongFlash = (wrongFlash - dt).coerceAtLeast(0f)
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

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val cell = bc to br
            if (cell != lastCell) {
                if (reached < stage.items.size) {
                    val cur = stage.items[reached]
                    if (cur.contains(bc, br)) {
                        reached += 1
                        SoundManager.speak(cur.label)  // 밟은 숫자/글자를 읽어준다.
                        SoundManager.playGoal()
                        if (reached >= stage.items.size) {
                            isNewBest = LearnRecordsRepository(context).record(level, elapsedMs)
                            finished = true
                            SoundManager.speak("참 잘했어요")
                        }
                    } else if (stage.items.any { it.contains(bc, br) }) {
                        // 순서가 아닌 타일을 밟음 — 살짝 알려주고 무시.
                        wrongFlash = 0.4f
                        SoundManager.playBonk()
                    }
                }
                lastCell = cell
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
                BackChip(onClick = { paused = true }, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "$reached / ${stage.items.size}",
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
                Text(
                    text = if (reached < stage.items.size) "다음:  ${stage.items[reached].label}"
                    else "다 했어요!",
                    fontSize = 22.sp,
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
                    LearnArenaCanvas(
                        arenaCols = LearnGame.SIZE,
                        items = stage.items,
                        reached = reached,
                        ballX = ballX,
                        ballY = ballY,
                        skin = currentSkin,
                        pulse = pulse,
                        wrongFlash = wrongFlash,
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
                DPad(
                    onInput = { dx, dy -> kx = dx; ky = dy },
                    enabled = !finished && !sensorEnabled
                )
            }
        }

        if (finished) {
            LearnResultOverlay(
                elapsedMs = elapsedMs,
                stars = LearnGame.starsFor(stage.items.size, elapsedMs),
                isNewBest = isNewBest,
                onRetry = { attemptId += 1 },
                onHome = onExit,
            )
        }

        if (paused && !finished) {
            val soundEnabled by AppSettings.soundEnabled
            PauseDialog(
                onResume = { paused = false },
                onRestart = {
                    paused = false
                    attemptId += 1
                },
                onExit = onExit,
                soundEnabled = soundEnabled,
                onToggleSound = { AppSettings.setSoundEnabled(!soundEnabled) },
                sensorEnabled = sensorEnabled,
                onToggleSensor = { AppSettings.setSensorEnabled(!sensorEnabled) },
            )
        }
    }
}

@Composable
private fun LearnArenaCanvas(
    arenaCols: Int,
    items: List<LearnItem>,
    reached: Int,
    ballX: Float,
    ballY: Float,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
    wrongFlash: Float,
) {
    val labelPaint = remember {
        android.graphics.Paint().apply {
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
            .background(Color(0xFFF3EFE7))
    ) {
        val cell = size.minDimension / arenaCols
        val tile = LearnGame.TILE * cell  // 타일 한 변 픽셀(2x2).

        // 타일 — 이미 밟음(초록·연하게)/지금 차례(노랑 강조·펄스)/아직(흰 타일).
        items.forEachIndexed { i, item ->
            val done = i < reached
            val current = i == reached
            val left = item.col * cell
            val top = item.row * cell
            val pad = cell * 0.1f
            val w = tile - pad * 2
            val glow = 0.5f + 0.5f * sin(pulse * 4f)
            val tileColor = when {
                done -> Color(0xFFA5D6A7)
                current -> Color(0xFFFFF59D)
                else -> Color.White
            }
            drawRoundRect(
                color = tileColor,
                topLeft = Offset(left + pad, top + pad),
                size = Size(w, w),
                cornerRadius = CornerRadius(cell * 0.3f, cell * 0.3f),
            )
            if (current) {
                drawRoundRect(
                    color = Color(0xFFFFB300).copy(alpha = 0.5f + 0.5f * glow),
                    topLeft = Offset(left + pad, top + pad),
                    size = Size(w, w),
                    cornerRadius = CornerRadius(cell * 0.3f, cell * 0.3f),
                    style = Stroke(width = cell * 0.12f),
                )
            }
            labelPaint.textSize = tile * 0.55f
            labelPaint.color = if (done) android.graphics.Color.parseColor("#2E7D32")
            else android.graphics.Color.parseColor("#3A2E10")
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                left + tile / 2f,
                top + tile / 2f + tile * 0.2f,
                labelPaint,
            )
        }

        // 오답 시 붉은 깜빡임
        if (wrongFlash > 0f) {
            drawRoundRect(
                color = CoralPink.copy(alpha = wrongFlash * 0.35f),
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
private fun LearnResultOverlay(
    elapsedMs: Long,
    stars: Int,
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
            Text("🎓", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "참 잘했어요!",
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "순서대로 다 맞혔어요",
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
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LearnResultButton("나가기", SkyBlue, onHome, Modifier.weight(1f))
                LearnResultButton("다시 해요", CoralPink, onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LearnResultButton(label: String, bg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
