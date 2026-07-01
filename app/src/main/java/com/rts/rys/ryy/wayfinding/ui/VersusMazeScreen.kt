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
import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.generateRandomMaze
import com.rts.rys.ryy.wayfinding.game.themeForLevel
import com.rts.rys.ryy.wayfinding.net.NearbyManager
import com.rts.rys.ryy.wayfinding.net.NearbyStatus
import com.rts.rys.ryy.wayfinding.net.VersusProtocol
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
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

private const val TIME_LIMIT_MS = 60_000L
private const val FINISH_GRACE_MS = 5_000L
private const val MAZE_SIZE = 13

private enum class Phase { WAITING, COUNTDOWN, RACE, RESULT }

private val GhostColor = Color(0xFF7E57C2)

@Composable
fun VersusMazeScreen(
    game: Char,
    manager: NearbyManager,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val theme = remember { themeForLevel(2) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val tilt = remember { com.rts.rys.ryy.wayfinding.game.TiltSensor(context) }
    val sensorEnabled by AppSettings.sensorEnabled

    // 공유 시드로 양쪽이 동일한 무작위 맵을 생성한다(규칙 1).
    var seed by remember { mutableStateOf<Long?>(null) }
    var startSignal by remember { mutableStateOf(false) }
    val maze = remember(seed) { seed?.let { generateRandomMaze(MAZE_SIZE, Random(it)) } }
    val physics = remember(maze) { maze?.let { BallPhysics(it) } }
    val distFromGoal = remember(maze) { maze?.let { bfsDistFromGoal(it) } }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var phase by remember { mutableStateOf(Phase.WAITING) }
    var countdownN by remember { mutableIntStateOf(3) }

    var ballX by remember { mutableFloatStateOf(1.5f) }
    var ballY by remember { mutableFloatStateOf(1.5f) }
    var ballRotation by remember { mutableFloatStateOf(0f) }
    var ballHeading by remember { mutableFloatStateOf(0f) }
    var ballSquash by remember { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember { mutableStateOf(false) }

    var oppX by remember { mutableFloatStateOf(1.5f) }
    var oppY by remember { mutableFloatStateOf(1.5f) }
    var oppProgress by remember { mutableFloatStateOf(0f) }

    var clockMs by remember { mutableLongStateOf(0L) }
    var myProgress by remember { mutableFloatStateOf(0f) }
    var myFinishMs by remember { mutableStateOf<Long?>(null) }
    var oppFinishMs by remember { mutableStateOf<Long?>(null) }
    var result by remember { mutableStateOf<VersusResult?>(null) }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    // 호스트: 시드 생성 후 SEED → START 전송, 자기도 시작.
    LaunchedEffect(Unit) {
        if (manager.isHost) {
            val s = Random.Default.nextLong()
            seed = s
            manager.send(VersusProtocol.seed(s))
            manager.send(VersusProtocol.start())
            startSignal = true
        }
    }

    // 수신 처리
    DisposableEffect(manager) {
        manager.onMessage = { bytes ->
            when (val m = VersusProtocol.parse(bytes)) {
                is VersusProtocol.Msg.Seed -> if (seed == null) seed = m.seed
                is VersusProtocol.Msg.Start -> startSignal = true
                is VersusProtocol.Msg.Pos -> {
                    oppX = m.x; oppY = m.y; oppProgress = m.progress
                }
                is VersusProtocol.Msg.Finished -> {
                    if (oppFinishMs == null) oppFinishMs = m.elapsedMs
                }
                else -> {}
            }
        }
        onDispose { manager.onMessage = null }
    }

    // 준비 단계에서 상대가 끊기면 나간다.
    LaunchedEffect(manager.status) {
        if (manager.status == NearbyStatus.DISCONNECTED && result == null && phase == Phase.WAITING) {
            onExit()
        }
    }

    BackHandler { onExit() }

    val ready = physics != null && startSignal

    // 게임 루프 — 준비 완료 시 카운트다운 후 레이스.
    LaunchedEffect(ready) {
        if (!ready) return@LaunchedEffect
        val phys = physics ?: return@LaunchedEffect
        val dist = distFromGoal ?: return@LaunchedEffect
        val mz = maze ?: return@LaunchedEffect
        val startDist = dist[mz.startRow][mz.startCol].coerceAtLeast(1)

        phase = Phase.COUNTDOWN
        for (n in 3 downTo 1) { countdownN = n; delay(900) }
        phase = Phase.RACE
        phys.reset()
        ballX = phys.x; ballY = phys.y

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
                    phys.maxSpeed = KEYPAD_MAX_SPEED
                } else {
                    ax = sx * SENSOR_ACCEL_GAIN
                    ay = sy * SENSOR_ACCEL_GAIN
                    phys.maxSpeed = if (sensorEnabled) SENSOR_MAX_SPEED else KEYPAD_MAX_SPEED
                }
                val reached = phys.step(dt, ax, ay)
                ballX = phys.x
                ballY = phys.y
                ballRotation = phys.rotation
                ballHeading = phys.headingRad
                ballSquash = phys.squashAmount
                ballSquashIsX = phys.squashAxis == SquashAxis.X
                myProgress = progressOf(dist, startDist, phys.x, phys.y)

                posAccumMs += deltaMs
                if (posAccumMs >= 66L) {
                    posAccumMs = 0L
                    manager.send(VersusProtocol.pos(phys.x, phys.y, myProgress))
                }
                if (reached) {
                    myFinishMs = clockMs
                    myProgress = 1f
                    manager.send(VersusProtocol.finished(clockMs))
                }
            }

            if (firstFinishClockMs == null && (myFinishMs != null || oppFinishMs != null)) {
                firstFinishClockMs = clockMs
            }

            result = when {
                manager.status == NearbyStatus.DISCONNECTED ->
                    if (myFinishMs != null && oppFinishMs != null) decideByTime(myFinishMs!!, oppFinishMs!!)
                    else VersusResult.OPPONENT_LEFT
                myFinishMs != null && oppFinishMs != null -> decideByTime(myFinishMs!!, oppFinishMs!!)
                firstFinishClockMs != null && clockMs - firstFinishClockMs >= FINISH_GRACE_MS ->
                    if (myFinishMs != null) VersusResult.WIN else VersusResult.LOSE
                clockMs >= TIME_LIMIT_MS -> when {
                    myProgress > oppProgress + 0.02f -> VersusResult.WIN
                    oppProgress > myProgress + 0.02f -> VersusResult.LOSE
                    else -> VersusResult.DRAW
                }
                else -> null
            }
        }
        phase = Phase.RESULT
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
                        text = "${remainSec}초",
                        color = if (remainSec <= 10) CoralPink else InkDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(64.dp))
                }
                Spacer(Modifier.height(8.dp))
                ProgressBars(myProgress = myProgress, oppProgress = oppProgress, oppName = manager.peerName ?: "친구")
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
                        modifier = Modifier.matchParentSize()
                    )
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val cs = minOf(size.width / maze.cols, size.height / maze.rows)
                        val ox = (size.width - cs * maze.cols) / 2f
                        val oy = (size.height - cs * maze.rows) / 2f
                        val center = Offset(ox + oppX * cs, oy + oppY * cs)
                        val r = cs * 0.34f
                        drawCircle(GhostColor.copy(alpha = 0.40f), r, center)
                        drawCircle(GhostColor.copy(alpha = 0.85f), r, center, style = Stroke(width = cs * 0.06f))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DPad(
                        onInput = { dx, dy -> kx = dx; ky = dy },
                        enabled = phase == Phase.RACE && myFinishMs == null
                    )
                }
            }
        }

        if (phase == Phase.COUNTDOWN) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$countdownN", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black)
            }
        }

        result?.let { res -> ResultOverlay(result = res, onExit = onExit) }
    }
}

@Composable
private fun ProgressBars(myProgress: Float, oppProgress: Float, oppName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProgressRow(label = "나", progress = myProgress, color = CoralPink)
        ProgressRow(label = oppName, progress = oppProgress, color = GhostColor)
    }
}

@Composable
private fun ProgressRow(label: String, progress: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = InkDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(56.dp),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ResultOverlay(result: VersusResult, onExit: () -> Unit) {
    val (text, emoji, color) = when (result) {
        VersusResult.WIN -> Triple("이겼어요!", "🏆", WallGreen)
        VersusResult.LOSE -> Triple("졌어요", "😢", CoralPink)
        VersusResult.DRAW -> Triple("비겼어요", "🤝", Color(0xFF9E9E9E))
        VersusResult.OPPONENT_LEFT -> Triple("상대가 나갔어요", "🏁", WallGreen)
    }
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
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .height(60.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(color)
                    .clickable(onClick = onExit)
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("나가기", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun decideByTime(mine: Long, opp: Long): VersusResult = when {
    mine < opp -> VersusResult.WIN
    mine > opp -> VersusResult.LOSE
    else -> VersusResult.DRAW
}

private fun bfsDistFromGoal(maze: Maze): Array<IntArray> {
    val dist = Array(maze.rows) { IntArray(maze.cols) { -1 } }
    val queue = ArrayDeque<Pair<Int, Int>>()
    dist[maze.goalRow][maze.goalCol] = 0
    queue.add(maze.goalCol to maze.goalRow)
    val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    while (queue.isNotEmpty()) {
        val (c, r) = queue.removeFirst()
        for ((dc, dr) in dirs) {
            val nc = c + dc
            val nr = r + dr
            if (nc < 0 || nr < 0 || nc >= maze.cols || nr >= maze.rows) continue
            if (dist[nr][nc] != -1) continue
            if (maze.isWall(nc, nr)) continue
            dist[nr][nc] = dist[r][c] + 1
            queue.add(nc to nr)
        }
    }
    return dist
}

private fun progressOf(dist: Array<IntArray>, startDist: Int, x: Float, y: Float): Float {
    val c = floor(x).toInt().coerceIn(0, dist[0].size - 1)
    val r = floor(y).toInt().coerceIn(0, dist.size - 1)
    val d = dist[r][c]
    if (d < 0) return 0f
    return (1f - d.toFloat() / startDist.toFloat()).coerceIn(0f, 1f)
}
