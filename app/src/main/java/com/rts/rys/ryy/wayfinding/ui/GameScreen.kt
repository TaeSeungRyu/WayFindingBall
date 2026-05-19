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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.rts.rys.ryy.wayfinding.game.themeForLevel
import com.rts.rys.ryy.wayfinding.ui.theme.BallRed
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.CreamBg
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.Lavender
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
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
    val theme = remember(stage.level) { themeForLevel(stage.level) }

    val sensorEnabled by AppSettings.sensorEnabled

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var ballX by remember(stage.id) { mutableFloatStateOf(physics.x) }
    var ballY by remember(stage.id) { mutableFloatStateOf(physics.y) }
    var ballRotation by remember(stage.id) { mutableFloatStateOf(0f) }
    var ballHeading by remember(stage.id) { mutableFloatStateOf(0f) }
    var ballSquash by remember(stage.id) { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember(stage.id) { mutableStateOf(false) }
    var elapsedMs by remember(stage.id) { mutableLongStateOf(0L) }
    var finished by remember(stage.id) { mutableStateOf(false) }
    var paused by remember(stage.id) { mutableStateOf(false) }
    var attemptId by remember(stage.id) { mutableIntStateOf(0) }

    // Effect state
    val trailPositions = remember(stage.id, attemptId) { mutableStateListOf<Offset>() }
    val dust = remember(stage.id, attemptId) { mutableStateListOf<DustParticle>() }
    val confetti = remember(stage.id, attemptId) { mutableStateListOf<ConfettiParticle>() }
    var shakeMs by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }
    var shakeOffset by remember(stage.id, attemptId) { mutableStateOf(Offset.Zero) }
    var celebrating by remember(stage.id, attemptId) { mutableStateOf(false) }
    var celebrationTimer by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }
    var ballScale by remember(stage.id, attemptId) { mutableFloatStateOf(1f) }
    var flashAlpha by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }
    var idleStrength by remember(stage.id, attemptId) { mutableFloatStateOf(1f) }
    var idleTime by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }
    var surpriseTimer by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val shakeAmplitudePx = with(density) { 3.dp.toPx() }
    val confettiColors = listOf(BallRed, SkyBlue, SunYellow, CoralPink, GoalGold, Lavender, WallGreen, Color.White)

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
        celebrating = false
        celebrationTimer = 0f
        ballScale = 1f
        flashAlpha = 0f
        shakeMs = 0f
        shakeOffset = Offset.Zero
        trailPositions.clear()
        dust.clear()
        confetti.clear()

        var last = 0L
        var accumulatedMs = 0L
        while (!finished) {
            val now = awaitFrame()
            if (paused && !celebrating) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            if (!celebrating) accumulatedMs += ((now - last) / 1_000_000L)
            last = now

            if (!celebrating) {
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

                val reached = physics.step(dt, ax, ay)
                if (physics.justImpacted && !reached) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    SoundManager.playBonk()
                    repeat(5) {
                        val angle = (Math.random() * 2 * Math.PI).toFloat()
                        val speed = 1.6f + (Math.random() * 2.5).toFloat()
                        dust.add(
                            DustParticle(
                                x = physics.x,
                                y = physics.y,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed,
                                lifetime = 0.32f
                            )
                        )
                    }
                    shakeMs = 0.09f
                    surpriseTimer = 0.5f
                }
                ballX = physics.x
                ballY = physics.y
                ballRotation = physics.rotation
                ballHeading = physics.headingRad
                ballSquash = physics.squashAmount
                ballSquashIsX = physics.squashAxis == SquashAxis.X
                elapsedMs = accumulatedMs

                // trail update — capture when moving fast enough
                val ballSpeed = sqrt(physics.vx * physics.vx + physics.vy * physics.vy)
                if (ballSpeed > 2f) {
                    trailPositions.add(0, Offset(physics.x, physics.y))
                    while (trailPositions.size > 6) trailPositions.removeAt(trailPositions.size - 1)
                } else if (trailPositions.isNotEmpty()) {
                    trailPositions.removeAt(trailPositions.size - 1)
                }

                if (reached && !celebrating) {
                    celebrating = true
                    SoundManager.playGoal()
                    flashAlpha = 0.7f
                    val gx = stage.maze.goalCol + 0.5f
                    val gy = stage.maze.goalRow + 0.5f
                    repeat(30) {
                        val angle = (-Math.PI / 2 + (Math.random() - 0.5) * Math.PI * 0.9).toFloat()
                        val speed = 6f + (Math.random() * 8.0).toFloat()
                        confetti.add(
                            ConfettiParticle(
                                x = gx,
                                y = gy,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed,
                                rotation = (Math.random() * 360).toFloat(),
                                rotSpeed = ((Math.random() - 0.5) * 720).toFloat(),
                                color = confettiColors.random(),
                                lifetime = 1.3f
                            )
                        )
                    }
                }
            }

            // ----- effect update (also during celebration) -----
            // dust integrate
            val dustIter = dust.listIterator()
            while (dustIter.hasNext()) {
                val p = dustIter.next()
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 4f * dt
                p.age += dt
                if (p.age >= p.lifetime) dustIter.remove()
            }
            // confetti integrate
            val conIter = confetti.listIterator()
            while (conIter.hasNext()) {
                val p = conIter.next()
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 14f * dt
                p.rotation += p.rotSpeed * dt
                p.age += dt
                if (p.age >= p.lifetime) conIter.remove()
            }
            // shake decay
            shakeMs = (shakeMs - dt).coerceAtLeast(0f)
            shakeOffset = if (shakeMs > 0f) {
                val k = (shakeMs / 0.09f) * shakeAmplitudePx
                Offset(
                    ((Math.random() - 0.5) * 2 * k).toFloat(),
                    ((Math.random() - 0.5) * 2 * k).toFloat()
                )
            } else Offset.Zero
            // flash decay
            flashAlpha = (flashAlpha - dt * 2.4f).coerceAtLeast(0f)
            // surprise timer decay
            surpriseTimer = (surpriseTimer - dt).coerceAtLeast(0f)

            // idle breathing tracking
            val speed = sqrt(physics.vx * physics.vx + physics.vy * physics.vy)
            val targetIdle = if (!celebrating && speed < 0.5f) 1f else 0f
            val approach = (dt * 2.5f).coerceIn(0f, 1f)
            idleStrength += (targetIdle - idleStrength) * approach
            idleTime += dt

            if (celebrating) {
                celebrationTimer += dt
                ballScale = (1f - celebrationTimer / 0.45f).coerceIn(0f, 1f)
                if (celebrationTimer >= 0.75f) {
                    finished = true
                    onFinished(elapsedMs)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom)))
    ) {
        SkyAmbience(
            modifier = Modifier.fillMaxSize(),
            cloudOpacity = theme.cloudOpacity,
            sparkleColor = if (theme.isDark) Color.White else Color.White
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
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

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        translationX = shakeOffset.x
                        translationY = shakeOffset.y
                    }
            ) {
                val breath = 1f + 0.045f * idleStrength * sin(idleTime * (2f * Math.PI.toFloat() / 1.6f))
                MazeCanvas(
                    maze = stage.maze,
                    ballX = ballX,
                    ballY = ballY,
                    rotation = ballRotation,
                    squashAmount = ballSquash,
                    squashAxisIsX = ballSquashIsX,
                    trail = trailPositions,
                    ballScale = ballScale * breath,
                    headingRad = ballHeading,
                    isHappy = celebrating,
                    surpriseLevel = (surpriseTimer / 0.5f).coerceIn(0f, 1f),
                    theme = theme,
                    modifier = Modifier.fillMaxSize()
                )
                EffectsOverlay(
                    maze = stage.maze,
                    dust = dust,
                    confetti = confetti,
                    modifier = Modifier.fillMaxSize()
                )
                if (flashAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SunYellow.copy(alpha = flashAlpha * 0.55f))
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

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
                onCalibrate = { AppSettings.setSensorOffset(tilt.tiltX, tilt.tiltY) },
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
    onCalibrate: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = if (sensorEnabled) 420.dp else 360.dp)
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
                if (sensorEnabled) {
                    DialogButton(
                        label = "지금 각도를 가운데로",
                        bg = Lavender,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCalibrate
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
