package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.PaintRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.FloorPaintController
import com.rts.rys.ryy.wayfinding.game.PaintGame
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.floor

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f

@Composable
fun PaintGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { PaintGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val arena = remember(attemptId) { PaintGame.buildArena(stage) }
    val physics = remember(attemptId) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) }
    val paintCtrl = remember(attemptId) { FloorPaintController(arena) }

    val tilt = remember { TiltSensor(context) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val sensorEnabled by AppSettings.sensorEnabled

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    var ballX by remember(attemptId) { mutableFloatStateOf(physics.x) }
    var ballY by remember(attemptId) { mutableFloatStateOf(physics.y) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    var paused by remember(level) { mutableStateOf(false) }
    // 색 고르기 모드: 지금 붓에 든 색 인덱스(팔레트 기준). 단색 모드는 항상 0.
    var colorIndex by remember(attemptId) { mutableIntStateOf(0) }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    BackHandler(enabled = !paused && !finished) { paused = true }

    LaunchedEffect(attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        elapsedMs = 0L
        finished = false
        var lastCell = floor(physics.x).toInt() to floor(physics.y).toInt()
        var moved = false

        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (paused) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            // 첫 칸을 새로 칠하기 전(공을 아직 안 굴린 상태)엔 시간이 흐르지 않게 한다.
            if (moved) elapsedMs += (now - last) / 1_000_000L
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

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val cell = bc to br
            if (cell != lastCell) {
                val res = paintCtrl.paint(bc, br, colorIndex)
                if (res != 0) {
                    moved = true
                    if (res == 2) {  // 처음 칠한 칸일 때만 소리·완료 판정.
                        SoundManager.playStarTone((paintCtrl.total - paintCtrl.remaining) % 12)
                        if (paintCtrl.done) {
                            isNewBest = PaintRecordsRepository(context).record(level, elapsedMs)
                            finished = true
                            SoundManager.playGoal()
                            SoundManager.speak("참 잘했어요")
                        }
                    }
                }
                lastCell = cell
            }
        }
    }

    val done = paintCtrl.total - paintCtrl.remaining

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더: 뒤로 + 진행도 + 시간
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = { paused = true }, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "$done / ${paintCtrl.total}",
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
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(stage.palette[colorIndex])
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = if (stage.chooseColor) "좋아하는 색으로 칠해요!" else "바닥을 모두 칠해요!",
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
                    PaintArenaCanvas(
                        arena = arena,
                        paint = paintCtrl,
                        palette = stage.palette,
                        ballX = ballX,
                        ballY = ballY,
                        skin = currentSkin,
                        pulse = pulse,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (stage.chooseColor) {
                    ColorPalettePicker(
                        palette = stage.palette,
                        selected = colorIndex,
                        onSelect = { colorIndex = it },
                        enabled = !finished,
                    )
                    Spacer(Modifier.height(10.dp))
                }
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
            PaintResultOverlay(
                elapsedMs = elapsedMs,
                stars = PaintGame.starsFor(paintCtrl.total, elapsedMs),
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
private fun PaintArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    paint: FloorPaintController,
    palette: List<Color>,
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
            val idx = paint.colorAt(c, r)
            drawRoundRect(
                color = if (idx >= 0) palette[idx.coerceIn(0, palette.lastIndex)] else unpainted,
                topLeft = Offset(c * cell + inset, r * cell + inset),
                size = Size(full, full),
                cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f),
            )
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

        // 공
        val r = cell * 0.4f
        val cx = ballX * cell
        val cy = ballY * cell
        drawBallDecoration(skin, cx, cy, r, phaseSec = pulse)
        drawBallBody(skin, cx, cy, r)
    }
}

@Composable
private fun ColorPalettePicker(
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
private fun PaintResultOverlay(
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
            Text("🎨", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "참 잘했어요!",
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
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
