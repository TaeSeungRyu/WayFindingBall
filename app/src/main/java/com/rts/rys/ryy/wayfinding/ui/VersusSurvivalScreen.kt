package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.VersusRecord
import com.rts.rys.ryy.wayfinding.data.VersusRecordsRepository
import com.rts.rys.ryy.wayfinding.data.VersusResult
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.ChaserController
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.generateRandomMaze
import com.rts.rys.ryy.wayfinding.game.themeForLevel
import com.rts.rys.ryy.wayfinding.net.NearbyManager
import com.rts.rys.ryy.wayfinding.net.NearbyStatus
import com.rts.rys.ryy.wayfinding.net.VersusProtocol
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.random.Random

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f

private const val SURVIVE_MS = 60_000L
private const val MAZE_SIZE = 13
private const val CHASER_COUNT = 2
private const val CATCH_DIST = 0.45f

private enum class SurvivalPhase { WAITING, COUNTDOWN, RACE, RESULT }

private val GhostColor = Color(0xFF7E57C2)
private val EnemyColor = Color(0xFFE53935)

@Composable
fun VersusSurvivalScreen(
    game: Char,
    manager: NearbyManager,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val theme = remember { themeForLevel(8) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val tilt = remember { com.rts.rys.ryy.wayfinding.game.TiltSensor(context) }
    val sensorEnabled by AppSettings.sensorEnabled

    var seed by remember { mutableStateOf<Long?>(null) }
    var startSignal by remember { mutableStateOf(false) }
    val maze = remember(seed) { seed?.let { generateRandomMaze(MAZE_SIZE, Random(it)) } }
    val physics = remember(maze) { maze?.let { BallPhysics(it) } }
    // 적: randomMove=false + spawnIndex → RNG 없이 결정론적 스폰·추격(양쪽 동일).
    val chasers = remember(maze) {
        maze?.let { m ->
            List(CHASER_COUNT) { i ->
                ChaserController(m, moveIntervalS = 0.55f, spawnIndex = i, randomMove = false)
            }
        }
    }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var phase by remember { mutableStateOf(SurvivalPhase.WAITING) }
    var countdownN by remember { mutableIntStateOf(3) }

    var ballX by remember { mutableFloatStateOf(1.5f) }
    var ballY by remember { mutableFloatStateOf(1.5f) }
    var ballRotation by remember { mutableFloatStateOf(0f) }
    var ballHeading by remember { mutableFloatStateOf(0f) }
    var ballSquash by remember { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember { mutableStateOf(false) }
    var chaserTick by remember { mutableIntStateOf(0) } // 적 위치 리컴포즈 트리거

    var oppX by remember { mutableFloatStateOf(1.5f) }
    var oppY by remember { mutableFloatStateOf(1.5f) }

    var clockMs by remember { mutableLongStateOf(0L) }
    var myCaughtMs by remember { mutableStateOf<Long?>(null) }
    var oppCaughtMs by remember { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf<VersusResult?>(null) }

    var round by remember { mutableIntStateOf(0) }
    var iWantRematch by remember { mutableStateOf(false) }
    var oppWantsRematch by remember { mutableStateOf(false) }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    val startNewRound: (Long) -> Unit = { newSeed ->
        result = null
        phase = SurvivalPhase.WAITING
        clockMs = 0L
        myCaughtMs = null
        oppCaughtMs = null
        iWantRematch = false
        oppWantsRematch = false
        seed = newSeed
        startSignal = true
        round++
    }

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
                is VersusProtocol.Msg.Pos -> { oppX = m.x; oppY = m.y }
                is VersusProtocol.Msg.Finished -> if (oppCaughtMs == null) oppCaughtMs = m.elapsedMs
                is VersusProtocol.Msg.Rematch -> oppWantsRematch = true
                is VersusProtocol.Msg.NewRound -> startNewRound(m.seed)
                else -> {}
            }
        }
        onDispose { manager.onMessage = null }
    }

    LaunchedEffect(manager.status) {
        if (manager.status == NearbyStatus.DISCONNECTED && result == null && phase == SurvivalPhase.WAITING) {
            onExit()
        }
    }

    LaunchedEffect(iWantRematch, oppWantsRematch) {
        if (manager.isHost && iWantRematch && oppWantsRematch) {
            val s = Random.Default.nextLong()
            manager.send(VersusProtocol.newRound(s))
            startNewRound(s)
        }
    }

    BackHandler { onExit() }

    val ready = physics != null && startSignal

    LaunchedEffect(ready, round) {
        if (!ready) return@LaunchedEffect
        val phys = physics ?: return@LaunchedEffect
        val enemies = chasers ?: return@LaunchedEffect

        phase = SurvivalPhase.COUNTDOWN
        for (n in 3 downTo 1) { countdownN = n; delay(900) }
        phase = SurvivalPhase.RACE
        phys.reset()
        enemies.forEach { it.reset() }
        ballX = phys.x; ballY = phys.y

        var last = 0L
        var posAccumMs = 0L

        while (result == null) {
            val now = awaitFrame()
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            val deltaMs = (now - last) / 1_000_000L
            last = now
            clockMs += deltaMs

            if (myCaughtMs == null) {
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
                    phys.maxSpeed = KEYPAD_MAX_SPEED
                } else {
                    ax = sx * SENSOR_ACCEL_GAIN
                    ay = sy * SENSOR_ACCEL_GAIN
                    phys.maxSpeed = if (sensorEnabled) SENSOR_MAX_SPEED else KEYPAD_MAX_SPEED
                }
                phys.step(dt, ax, ay)
                ballX = phys.x
                ballY = phys.y
                ballRotation = phys.rotation
                ballHeading = phys.headingRad
                ballSquash = phys.squashAmount
                ballSquashIsX = phys.squashAxis == SquashAxis.X

                val bc = floor(phys.x).toInt()
                val br = floor(phys.y).toInt()
                enemies.forEach { it.tick(dt, bc, br) }
                chaserTick++

                for (ch in enemies) {
                    val ddx = ch.visualX - phys.x
                    val ddy = ch.visualY - phys.y
                    if (ddx * ddx + ddy * ddy < CATCH_DIST * CATCH_DIST) {
                        myCaughtMs = clockMs
                        manager.send(VersusProtocol.finished(clockMs))
                        break
                    }
                }

                posAccumMs += deltaMs
                if (posAccumMs >= 66L) {
                    posAccumMs = 0L
                    manager.send(VersusProtocol.pos(phys.x, phys.y, 0f))
                }
            }

            // 먼저 잡히는 쪽이 패배. 둘 다 60초 생존 시 무승부.
            result = when {
                manager.status == NearbyStatus.DISCONNECTED -> VersusResult.OPPONENT_LEFT
                myCaughtMs != null && oppCaughtMs != null -> decideBySurvival(myCaughtMs!!, oppCaughtMs!!)
                myCaughtMs != null -> VersusResult.LOSE
                oppCaughtMs != null -> VersusResult.WIN
                clockMs >= SURVIVE_MS -> VersusResult.DRAW
                else -> null
            }
        }
        phase = SurvivalPhase.RESULT
        VersusRecordsRepository(context).add(
            VersusRecord(
                opponentName = manager.peerName ?: "친구",
                game = game,
                result = result!!,
                elapsedMs = myCaughtMs ?: SURVIVE_MS,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    val elapsedSec = (clockMs / 1000L).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        if (maze == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛜", fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text("친구와 준비 중이에요…", color = InkDark, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BackChip(onClick = onExit)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${elapsedSec}초 버팀",
                        color = InkDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(64.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip(label = "나", alive = myCaughtMs == null)
                    StatusChip(label = manager.peerName ?: "친구", alive = oppCaughtMs == null)
                }
                Spacer(Modifier.height(10.dp))

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MazeCanvas(
                        maze = maze,
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
                        @Suppress("UNUSED_EXPRESSION") chaserTick // 적 위치 변화 시 재드로우
                        val cs = minOf(size.width / maze.cols, size.height / maze.rows)
                        val ox = (size.width - cs * maze.cols) / 2f
                        val oy = (size.height - cs * maze.rows) / 2f
                        // 상대 고스트
                        val gc = Offset(ox + oppX * cs, oy + oppY * cs)
                        drawCircle(GhostColor.copy(alpha = 0.40f), cs * 0.34f, gc)
                        drawCircle(GhostColor.copy(alpha = 0.85f), cs * 0.34f, gc, style = Stroke(width = cs * 0.06f))
                        // 내 적들
                        chasers?.forEach { ch ->
                            val ec = Offset(ox + ch.visualX * cs, oy + ch.visualY * cs)
                            drawCircle(EnemyColor, cs * 0.32f, ec)
                            drawCircle(Color.White, cs * 0.08f, Offset(ec.x - cs * 0.10f, ec.y - cs * 0.05f))
                            drawCircle(Color.White, cs * 0.08f, Offset(ec.x + cs * 0.10f, ec.y - cs * 0.05f))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DPad(
                        onInput = { dx, dy -> kx = dx; ky = dy },
                        enabled = phase == SurvivalPhase.RACE && myCaughtMs == null
                    )
                }
            }
        }

        if (phase == SurvivalPhase.COUNTDOWN) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$countdownN", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black)
            }
        }

        result?.let { res ->
            SurvivalResultOverlay(
                result = res,
                myCaughtMs = myCaughtMs,
                onExit = onExit,
                onRematch = {
                    if (!iWantRematch) {
                        iWantRematch = true
                        manager.send(VersusProtocol.rematch())
                    }
                },
                waitingRematch = iWantRematch,
                opponentGone = res == VersusResult.OPPONENT_LEFT,
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, alive: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (alive) "💚" else "💀", fontSize = 14.sp)
        Spacer(Modifier.size(6.dp))
        Text(label, color = InkDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
    }
}

@Composable
private fun SurvivalResultOverlay(
    result: VersusResult,
    myCaughtMs: Long?,
    onExit: () -> Unit,
    onRematch: () -> Unit,
    waitingRematch: Boolean,
    opponentGone: Boolean,
) {
    val (text, emoji, color) = when (result) {
        VersusResult.WIN -> Triple("이겼어요!", "🏆", WallGreen)
        VersusResult.LOSE -> Triple("잡혔어요!", "😢", CoralPink)
        VersusResult.DRAW -> Triple("둘 다 살아남았어요!", "🤝", Color(0xFF9E9E9E))
        VersusResult.OPPONENT_LEFT -> Triple("상대가 나갔어요", "🏁", WallGreen)
    }
    val sub = myCaughtMs?.let { "${(it / 1000L)}초 버팀" } ?: "60초 생존!"
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 40.dp, vertical = 32.dp)
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = InkDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            if (!opponentGone) {
                if (waitingRematch) {
                    Text("친구를 기다리는 중…", color = InkDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                } else {
                    ResultActionButton(label = "한 번 더", bg = color, onClick = onRematch)
                    Spacer(Modifier.height(12.dp))
                }
            }
            ResultActionButton(
                label = "나가기",
                bg = if (!opponentGone) Color(0xFFBDB7B0) else color,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun ResultActionButton(label: String, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun decideBySurvival(mine: Long, opp: Long): VersusResult = when {
    mine > opp -> VersusResult.WIN
    mine < opp -> VersusResult.LOSE
    else -> VersusResult.DRAW
}
