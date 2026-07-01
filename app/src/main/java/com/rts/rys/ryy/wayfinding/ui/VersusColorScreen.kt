package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rts.rys.ryy.wayfinding.data.VersusRecord
import com.rts.rys.ryy.wayfinding.data.VersusRecordsRepository
import com.rts.rys.ryy.wayfinding.data.VersusResult
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.ColorGame
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.themeForLevel
import com.rts.rys.ryy.wayfinding.net.NearbyManager
import com.rts.rys.ryy.wayfinding.net.NearbyStatus
import com.rts.rys.ryy.wayfinding.net.VersusProtocol
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.random.Random

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f
private const val TIME_LIMIT_MS = 60_000L
private const val FINISH_GRACE_MS = 5_000L

@Composable
fun VersusColorScreen(
    game: Char,
    manager: NearbyManager,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember { ColorGame.stageOf(2) }
    val zones = remember { stage.zones }
    val arena = remember { ColorGame.buildArena(stage) }
    val physics = remember { BallPhysics(arena) }
    val theme = remember { themeForLevel(1) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val tilt = remember { com.rts.rys.ryy.wayfinding.game.TiltSensor(context) }
    val sensorEnabled by AppSettings.sensorEnabled

    var seed by remember { mutableStateOf<Long?>(null) }
    var startSignal by remember { mutableStateOf(false) }
    val targetSeq = remember(seed) { seed?.let { ColorGame.targetSequence(stage, Random(it)) } }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var phase by remember { mutableStateOf(VersusPhase.WAITING) }
    var countdownN by remember { mutableIntStateOf(3) }

    var ballX by remember { mutableFloatStateOf(physics.x) }
    var ballY by remember { mutableFloatStateOf(physics.y) }
    var ballRotation by remember { mutableFloatStateOf(0f) }
    var ballHeading by remember { mutableFloatStateOf(0f) }
    var ballSquash by remember { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember { mutableStateOf(false) }

    var oppX by remember { mutableFloatStateOf(physics.x) }
    var oppY by remember { mutableFloatStateOf(physics.y) }
    var oppProgress by remember { mutableFloatStateOf(0f) }

    var currentIdx by remember { mutableIntStateOf(0) }
    var clockMs by remember { mutableLongStateOf(0L) }
    var myProgress by remember { mutableFloatStateOf(0f) }
    var myFinishMs by remember { mutableStateOf<Long?>(null) }
    var oppFinishMs by remember { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf<VersusResult?>(null) }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (manager.isHost) {
            val s = Random.Default.nextLong()
            seed = s
            manager.send(VersusProtocol.seed(s))
            manager.send(VersusProtocol.start())
            startSignal = true
        }
    }

    DisposableEffect(manager) {
        manager.onMessage = { bytes ->
            when (val m = VersusProtocol.parse(bytes)) {
                is VersusProtocol.Msg.Seed -> if (seed == null) seed = m.seed
                is VersusProtocol.Msg.Start -> startSignal = true
                is VersusProtocol.Msg.Pos -> { oppX = m.x; oppY = m.y; oppProgress = m.progress }
                is VersusProtocol.Msg.Finished -> if (oppFinishMs == null) oppFinishMs = m.elapsedMs
                else -> {}
            }
        }
        onDispose { manager.onMessage = null }
    }

    LaunchedEffect(manager.status) {
        if (manager.status == NearbyStatus.DISCONNECTED && result == null && phase == VersusPhase.WAITING) {
            onExit()
        }
    }

    BackHandler { onExit() }

    val ready = targetSeq != null && startSignal

    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        val seq = targetSeq ?: return@LaunchedEffect
        phase = VersusPhase.COUNTDOWN
        for (n in 3 downTo 1) { countdownN = n; delay(900) }
        phase = VersusPhase.RACE
        physics.reset()
        ballX = physics.x; ballY = physics.y

        var last = 0L
        var posAccumMs = 0L
        var firstFinishClockMs: Long? = null

        while (result == null) {
            val now = awaitFrame()
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            val deltaMs = (now - last) / 1_000_000L
            last = now
            clockMs += deltaMs

            if (myFinishMs == null) {
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
                ballRotation = physics.rotation
                ballHeading = physics.headingRad
                ballSquash = physics.squashAmount
                ballSquashIsX = physics.squashAxis == SquashAxis.X

                if (currentIdx < seq.size) {
                    val bc = floor(physics.x).toInt()
                    val br = floor(physics.y).toInt()
                    if (ColorGame.zoneAt(zones, bc, br) == seq[currentIdx]) {
                        currentIdx++
                    }
                }
                myProgress = currentIdx.toFloat() / seq.size

                posAccumMs += deltaMs
                if (posAccumMs >= 66L) {
                    posAccumMs = 0L
                    manager.send(VersusProtocol.pos(physics.x, physics.y, myProgress))
                }
                if (currentIdx >= seq.size) {
                    myFinishMs = clockMs
                    manager.send(VersusProtocol.finished(clockMs))
                }
            }

            if (firstFinishClockMs == null && (myFinishMs != null || oppFinishMs != null)) {
                firstFinishClockMs = clockMs
            }

            result = when {
                manager.status == NearbyStatus.DISCONNECTED ->
                    if (myFinishMs != null && oppFinishMs != null) versusDecideByTime(myFinishMs!!, oppFinishMs!!)
                    else VersusResult.OPPONENT_LEFT
                myFinishMs != null && oppFinishMs != null -> versusDecideByTime(myFinishMs!!, oppFinishMs!!)
                firstFinishClockMs != null && clockMs - firstFinishClockMs >= FINISH_GRACE_MS ->
                    if (myFinishMs != null) VersusResult.WIN else VersusResult.LOSE
                clockMs >= TIME_LIMIT_MS -> when {
                    myProgress > oppProgress + 0.01f -> VersusResult.WIN
                    oppProgress > myProgress + 0.01f -> VersusResult.LOSE
                    else -> VersusResult.DRAW
                }
                else -> null
            }
        }
        phase = VersusPhase.RESULT
        VersusRecordsRepository(context).add(
            VersusRecord(
                opponentName = manager.peerName ?: "친구",
                game = game,
                result = result!!,
                elapsedMs = myFinishMs ?: 0L,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    val remainSec = ((TIME_LIMIT_MS - clockMs).coerceAtLeast(0L) / 1000L).toInt()
    val targetZone = targetSeq?.getOrNull(currentIdx)?.let { zones.getOrNull(it) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BackChip(onClick = onExit)
                Spacer(Modifier.weight(1f))
                if (targetZone != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(targetZone.color)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("${targetZone.name}으로!", color = InkDark, fontSize = 20.sp, fontWeight = FontWeight.Black)
                } else {
                    Text("완료!", color = InkDark, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
                Text("${remainSec}초", color = InkDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp))
            VersusProgressBars(myProgress = myProgress, oppProgress = oppProgress, oppName = manager.peerName ?: "친구")
            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                MazeCanvas(
                    maze = arena,
                    ballX = ballX,
                    ballY = ballY,
                    rotation = ballRotation,
                    squashAmount = ballSquash,
                    squashAxisIsX = ballSquashIsX,
                    headingRad = ballHeading,
                    theme = theme,
                    ballSkin = currentSkin,
                    showGoal = false,
                    modifier = Modifier.matchParentSize()
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cs = minOf(size.width / arena.cols, size.height / arena.rows)
                    val ox = (size.width - cs * arena.cols) / 2f
                    val oy = (size.height - cs * arena.rows) / 2f
                    val currentTarget = targetSeq?.getOrNull(currentIdx)
                    zones.forEachIndexed { i, z ->
                        val isTarget = i == currentTarget
                        val x0 = ox + z.cMin * cs
                        val y0 = oy + z.rMin * cs
                        val w = (z.cMax - z.cMin + 1) * cs
                        val h = (z.rMax - z.rMin + 1) * cs
                        drawRect(
                            color = z.color.copy(alpha = if (isTarget) 0.85f else 0.4f),
                            topLeft = Offset(x0, y0),
                            size = Size(w, h)
                        )
                        if (isTarget) {
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(x0, y0),
                                size = Size(w, h),
                                style = Stroke(width = cs * 0.12f)
                            )
                        }
                    }
                    val gc = Offset(ox + oppX * cs, oy + oppY * cs)
                    drawCircle(VersusGhostColor.copy(alpha = 0.40f), cs * 0.34f, gc)
                    drawCircle(VersusGhostColor.copy(alpha = 0.85f), cs * 0.34f, gc, style = Stroke(width = cs * 0.06f))
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DPad(
                    onInput = { dx, dy -> kx = dx; ky = dy },
                    enabled = phase == VersusPhase.RACE && myFinishMs == null
                )
            }
        }

        if (phase == VersusPhase.COUNTDOWN) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$countdownN", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black)
            }
        }
        if (phase == VersusPhase.WAITING) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text("친구와 준비 중이에요…", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        result?.let { res -> VersusRaceResultOverlay(result = res, onExit = onExit) }
    }
}
