package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.DeepNight
import com.rts.rys.ryy.wayfinding.ui.theme.MidNight
import com.rts.rys.ryy.wayfinding.ui.theme.NeonCyan
import com.rts.rys.ryy.wayfinding.ui.theme.NeonPink
import com.rts.rys.ryy.wayfinding.ui.theme.SoftWhite
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

    // keypad input
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
            .background(Brush.verticalGradient(listOf(MidNight, DeepNight)))
            .padding(top = 28.dp, bottom = 20.dp, start = 14.dp, end = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // top HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackChip(onClick = { paused = true })
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stage.name,
                    color = SoftWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.width(0.dp))
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = formatElapsed(elapsedMs),
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                MazeCanvas(
                    maze = stage.maze,
                    ballX = ballX,
                    ballY = ballY,
                    modifier = Modifier.fillMaxSize()
                )

                DPad(
                    onInput = { dx, dy ->
                        // normalize so diagonals aren't faster
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
                        .padding(end = 4.dp, bottom = 4.dp)
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
                .size(width = 280.dp, height = 200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MidNight)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("일시정지", color = SoftWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink.copy(alpha = 0.2f), contentColor = NeonPink),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("나가기") }
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f), contentColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("계속하기") }
                }
            }
        }
    }
}

fun formatElapsed(ms: Long): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val cs = (ms % 1000) / 10
    return String.format("%02d:%02d.%02d", m, s, cs)
}
