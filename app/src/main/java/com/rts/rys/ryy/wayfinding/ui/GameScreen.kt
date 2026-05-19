package com.rts.rys.ryy.wayfinding.ui

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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rts.rys.ryy.wayfinding.game.BallPhysics
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

private const val SENSOR_ACCEL_GAIN = 22f
private const val KEYPAD_ACCEL_GAIN = 18f

@Composable
fun GameScreen(
    stage: Stage,
    onFinished: (elapsedMs: Long) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val physics = remember(stage.id) { BallPhysics(stage.maze) }
    val tilt = remember { TiltSensor(context) }

    DisposableEffect(Unit) {
        tilt.start()
        onDispose { tilt.stop() }
    }

    var ballX by remember(stage.id) { mutableFloatStateOf(physics.x) }
    var ballY by remember(stage.id) { mutableFloatStateOf(physics.y) }
    var elapsedMs by remember(stage.id) { mutableLongStateOf(0L) }
    var finished by remember(stage.id) { mutableStateOf(false) }
    var paused by remember(stage.id) { mutableStateOf(false) }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(stage.id) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
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

            val sx = tilt.tiltX
            val sy = tilt.tiltY
            val useKeypad = kx != 0f || ky != 0f
            val ax: Float
            val ay: Float
            if (useKeypad) {
                ax = kx * KEYPAD_ACCEL_GAIN
                ay = ky * KEYPAD_ACCEL_GAIN
            } else {
                ax = sx * SENSOR_ACCEL_GAIN
                ay = sy * SENSOR_ACCEL_GAIN
            }

            val reached = physics.step(dt, ax, ay)
            ballX = physics.x
            ballY = physics.y
            elapsedMs = accumulatedMs
            if (reached) {
                finished = true
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackChip(onClick = { paused = true })
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stage.name,
                    color = InkDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
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
                    .fillMaxSize()
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CreamBg)
            ) {
                MazeCanvas(
                    maze = stage.maze,
                    ballX = ballX,
                    ballY = ballY,
                    modifier = Modifier.fillMaxSize()
                )

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
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                )
            }
        }

        if (paused) {
            PauseDialog(
                onResume = { paused = false },
                onExit = onExit
            )
        }
    }
}

@Composable
private fun PauseDialog(onResume: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onResume) {
        Box(
            modifier = Modifier
                .size(width = 300.dp, height = 220.dp)
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
                Text("계속 놀까요?", color = InkSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
