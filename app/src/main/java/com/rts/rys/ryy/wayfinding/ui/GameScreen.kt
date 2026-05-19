package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.CreamBg
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.sqrt

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f

@Composable
fun GameScreen(
    stage: Stage,
    onFinished: (elapsedMs: Long) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val physics = remember(stage.id) { BallPhysics(stage.maze) }
    val tilt = remember { TiltSensor(context) }

    val sensorEnabled by AppSettings.sensorEnabled

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var ballX by remember(stage.id) { mutableFloatStateOf(physics.x) }
    var ballY by remember(stage.id) { mutableFloatStateOf(physics.y) }
    var ballRotation by remember(stage.id) { mutableFloatStateOf(0f) }
    var ballSquash by remember(stage.id) { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember(stage.id) { mutableStateOf(false) }
    var elapsedMs by remember(stage.id) { mutableLongStateOf(0L) }
    var finished by remember(stage.id) { mutableStateOf(false) }
    var paused by remember(stage.id) { mutableStateOf(false) }
    var attemptId by remember(stage.id) { mutableIntStateOf(0) }

    BackHandler(enabled = !paused) { paused = true }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(stage.id, attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        ballRotation = 0f
        ballSquash = 0f
        ballSquashIsX = false
        elapsedMs = 0L
        finished = false
        var last = 0L
        var accumulatedMs = 0L
        while (!finished) {
            val now = awaitFrame()
            if (paused) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            accumulatedMs += ((now - last) / 1_000_000L)
            last = now

            val sx = if (sensorEnabled) tilt.tiltX else 0f
            val sy = if (sensorEnabled) tilt.tiltY else 0f
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

            val reached = physics.step(dt, ax, ay)
            if (physics.justImpacted) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                SoundManager.playBonk()
            }
            ballX = physics.x
            ballY = physics.y
            ballRotation = physics.rotation
            ballSquash = physics.squashAmount
            ballSquashIsX = physics.squashAxis == SquashAxis.X
            elapsedMs = accumulatedMs
            if (reached) {
                finished = true
                SoundManager.playGoal()
                onFinished(elapsedMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .padding(top = 28.dp, bottom = 20.dp, start = 14.dp, end = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(
                    onClick = { paused = true },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = stage.name,
                    color = InkDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 96.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SunYellow)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = formatElapsed(elapsedMs),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CreamBg)
            ) {
                MazeCanvas(
                    maze = stage.maze,
                    ballX = ballX,
                    ballY = ballY,
                    rotation = ballRotation,
                    squashAmount = ballSquash,
                    squashAxisIsX = ballSquashIsX,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(12.dp))

            DPad(
                onInput = { dx, dy ->
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 0f) {
                        kx = dx / len
                        ky = dy / len
                    } else {
                        kx = 0f; ky = 0f
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 6.dp)
                    .alpha(if (sensorEnabled) 0.45f else 1f)
            )
        }

        if (paused) {
            val soundEnabled by AppSettings.soundEnabled
            PauseDialog(
                sensorEnabled = sensorEnabled,
                soundEnabled = soundEnabled,
                onToggleSensor = { AppSettings.setSensorEnabled(!sensorEnabled) },
                onToggleSound = { AppSettings.setSoundEnabled(!soundEnabled) },
                onResume = { paused = false },
                onRestart = {
                    paused = false
                    attemptId++
                },
                onExit = onExit
            )
        }
    }
}

@Composable
private fun PillToggleChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) SkyBlue else InkSoft
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PauseDialog(
    sensorEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleSensor: () -> Unit,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 360.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("잠깐 멈췄어요", color = InkDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillToggleChip(
                        label = if (sensorEnabled) "센서 ON" else "센서 OFF",
                        enabled = sensorEnabled,
                        onClick = onToggleSensor
                    )
                    PillToggleChip(
                        label = if (soundEnabled) "소리 ON" else "소리 OFF",
                        enabled = soundEnabled,
                        onClick = onToggleSound
                    )
                }
                DialogButton(
                    label = "다시 시작",
                    bg = SunYellow,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestart
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DialogButton("그만할래요", CoralPink, modifier = Modifier.weight(1f), onClick = onExit)
                    DialogButton("계속해요", SkyBlue, modifier = Modifier.weight(1f), onClick = onResume)
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val cs = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", m, s, cs)
}
