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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.rts.rys.ryy.wayfinding.game.Cell
import com.rts.rys.ryy.wayfinding.game.ChaserController
import com.rts.rys.ryy.wayfinding.game.DynamicMazeController
import com.rts.rys.ryy.wayfinding.game.generateRandomMaze
import com.rts.rys.ryy.wayfinding.game.KeyDoorController
import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.game.MovingGoalController
import com.rts.rys.ryy.wayfinding.game.RotatingMazeController
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.StarsController
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

private enum class BombState { IDLE, ARMED, COOLDOWN }

private class FireTrail(val col: Int, val row: Int, var age: Float)

private const val BOMB_FUSE_S = 3f
private const val BOMB_COOLDOWN_S = 10f

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f

@Composable
fun GameScreen(
    stage: Stage,
    onFinished: (elapsedMs: Long, caught: Boolean, clears: Int) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var attemptId by remember(stage.id) { mutableIntStateOf(0) }
    var infiniteRound by remember(stage.id) { mutableIntStateOf(0) }
    val isInfiniteClears = stage.level == 14
    val isSurvival = stage.level == 15
    val isIce = stage.level == 16
    val isFire = stage.level == 17
    val isGrow = stage.level == 18
    val isKeyDoor = stage.level == 19
    val isInfinite = isInfiniteClears || isSurvival || isIce || isFire || isGrow || isKeyDoor
    val growMultiplier = if (isGrow) (1f + infiniteRound * 0.15f).coerceAtMost(1.7f) else 1f
    val workingMaze = remember(stage.id, attemptId, infiniteRound) {
        if (isInfiniteClears || isIce || isFire || isGrow || isKeyDoor) {
            generateRandomMaze(13)
        } else if (isSurvival) {
            generateRandomMaze(21)
        } else {
            val src = stage.maze
            val grid = Array(src.rows) { r -> src.grid[r].copyOf() }
            Maze(src.cols, src.rows, grid).also {
                it.enemyCol = src.enemyCol
                it.enemyRow = src.enemyRow
                it.stars = src.stars
                it.portalA = src.portalA
                it.portalB = src.portalB
            }
        }
    }
    var portalCooldown by remember(stage.id, attemptId, infiniteRound) { mutableFloatStateOf(0f) }
    val physics = remember(stage.id, attemptId, infiniteRound) {
        BallPhysics(
            workingMaze,
            radius = 0.32f * growMultiplier,
            friction = if (isIce) 0.3f else 1.8f
        )
    }
    val dynamicMaze = remember(stage.id, attemptId, infiniteRound) {
        if (stage.level in 5..13 || isInfinite) DynamicMazeController(workingMaze) else null
    }
    val rotatingMaze = remember(stage.id, attemptId, infiniteRound) {
        if (stage.level == 12) RotatingMazeController(workingMaze) { m ->
            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val n = m.cols
            val nc = (n - 1 - br).coerceIn(1, n - 2)
            val nr = bc.coerceIn(1, n - 2)
            physics.setPositionAndStop(nc, nr)
        } else null
    }
    val movingGoal = remember(stage.id, attemptId, infiniteRound) {
        if (stage.level == 6 || stage.level == 10 || stage.level == 12) MovingGoalController(workingMaze) else null
    }
    var survivalEnemyCount by remember(stage.id, attemptId) { mutableIntStateOf(3) }
    var fireEnemyCount by remember(stage.id, attemptId) { mutableIntStateOf(1) }
    val chasers = remember(stage.id, attemptId, infiniteRound) {
        val initial: List<ChaserController> = when {
            isInfiniteClears -> List(infiniteRound + 1) { i ->
                ChaserController(workingMaze, spawnIndex = i, randomMove = true, randomSpawnMinDistance = 6)
            }
            isSurvival -> List(survivalEnemyCount.coerceAtLeast(1)) { i ->
                ChaserController(workingMaze, spawnIndex = i, randomMove = true, randomSpawnMinDistance = 8)
            }
            isFire -> List(fireEnemyCount.coerceAtLeast(1)) { i ->
                ChaserController(workingMaze, spawnIndex = i, randomMove = true, randomSpawnMinDistance = 6)
            }
            isGrow -> listOf(ChaserController(workingMaze, randomSpawnMinDistance = 6))
            stage.level == 8 || stage.level == 10 -> listOf(ChaserController(workingMaze))
            else -> emptyList()
        }
        mutableStateListOf<ChaserController>().apply { addAll(initial) }
    }
    var nextEnemySpawnIn by remember(stage.id, attemptId) { mutableFloatStateOf(10f) }
    val storms = remember(stage.id, attemptId, infiniteRound) {
        mutableStateListOf<Pair<Int, Int>>()
    }
    var stormTimer by remember(stage.id, attemptId, infiniteRound) { mutableFloatStateOf(0f) }
    var stormPhase by remember(stage.id, attemptId, infiniteRound) { mutableFloatStateOf(0f) }
    val fireTrails = remember(stage.id, attemptId, infiniteRound) {
        mutableStateListOf<FireTrail>()
    }
    var lastFireCell by remember(stage.id, attemptId, infiniteRound) { mutableStateOf(-1 to -1) }
    var fireVersion by remember(stage.id, attemptId, infiniteRound) { mutableIntStateOf(0) }
    val starsCtrl = remember(stage.id, attemptId, infiniteRound) {
        if (stage.level == 9 || stage.level == 10) StarsController(workingMaze) else null
    }
    val keyDoorCtrl = remember(stage.id, attemptId, infiniteRound) {
        if (isKeyDoor) KeyDoorController(workingMaze, keyCount = (3 + infiniteRound).coerceAtMost(7)) else null
    }
    val isDarkLevel = stage.level == 7
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
    var totalInfiniteMs by remember(stage.id, attemptId) { mutableLongStateOf(0L) }
    var finished by remember(stage.id) { mutableStateOf(false) }
    var paused by remember(stage.id) { mutableStateOf(false) }

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

    val bombEnabled = stage.level in 6..19
    var bombState by remember(stage.id, attemptId) { mutableStateOf(BombState.IDLE) }
    var bombTimer by remember(stage.id, attemptId) { mutableFloatStateOf(0f) }
    var bombVersion by remember(stage.id, attemptId) { mutableIntStateOf(0) }

    fun explodeBomb() {
        val bc = floor(physics.x).toInt()
        val br = floor(physics.y).toInt()
        var changed = false
        for (dr in -1..1) for (dc in -1..1) {
            if (dc == 0 && dr == 0) continue
            val c = bc + dc
            val r = br + dr
            if (c <= 0 || c >= workingMaze.cols - 1) continue
            if (r <= 0 || r >= workingMaze.rows - 1) continue
            if (c == workingMaze.startCol && r == workingMaze.startRow) continue
            if (c == workingMaze.goalCol && r == workingMaze.goalRow) continue
            // 19단계: G의 4방향 인접 셀(문 포함)은 폭탄으로 못 뚫음. 키로만 열림.
            if (isKeyDoor &&
                abs(c - workingMaze.goalCol) + abs(r - workingMaze.goalRow) == 1
            ) continue
            if (workingMaze.grid[r][c] == Cell.WALL) {
                workingMaze.grid[r][c] = Cell.EMPTY
                changed = true
            }
        }
        // 폭탄 범위 안의 적 제거 (3x3, 공이 있는 셀 포함)
        val toRemove = chasers.filter {
            abs(it.col - bc) <= 1 && abs(it.row - br) <= 1
        }
        if (toRemove.isNotEmpty()) {
            chasers.removeAll(toRemove)
            if (isSurvival) survivalEnemyCount = chasers.size
            if (isFire) fireEnemyCount = chasers.size
            changed = true
        }
        if (changed) bombVersion++
        repeat(24) {
            val angle = (Math.random() * 2 * Math.PI).toFloat()
            val speed = 4f + (Math.random() * 4).toFloat()
            dust.add(
                DustParticle(
                    x = physics.x,
                    y = physics.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    lifetime = 0.55f
                )
            )
        }
        shakeMs = 0.18f
        SoundManager.playBonk()
    }

    fun onBombClick() {
        when (bombState) {
            BombState.IDLE -> {
                bombState = BombState.ARMED
                bombTimer = BOMB_FUSE_S
            }
            BombState.ARMED -> {
                bombState = BombState.IDLE
                bombTimer = 0f
            }
            BombState.COOLDOWN -> Unit
        }
    }

    BackHandler(enabled = !paused) { paused = true }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(stage.id, attemptId, infiniteRound) {
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
            if (!celebrating) {
                val deltaMs = (now - last) / 1_000_000L
                accumulatedMs += deltaMs
                if (isInfinite) totalInfiniteMs += deltaMs
            }
            last = now

            if (!celebrating) {
                val sensitivity = AppSettings.sensorSensitivity.value
                val offX = AppSettings.sensorOffsetX.value
                val offY = AppSettings.sensorOffsetY.value
                val sx = if (sensorEnabled) ((tilt.tiltX - offX) * sensitivity).coerceIn(-1f, 1f) else 0f
                val sy = if (sensorEnabled) ((tilt.tiltY - offY) * sensitivity).coerceIn(-1f, 1f) else 0f
                val useKeypad = kx != 0f || ky != 0f
                var ax: Float
                var ay: Float
                if (useKeypad) {
                    ax = kx * KEYPAD_ACCEL_GAIN
                    ay = ky * KEYPAD_ACCEL_GAIN
                    physics.maxSpeed = KEYPAD_MAX_SPEED
                } else {
                    ax = sx * SENSOR_ACCEL_GAIN
                    ay = sy * SENSOR_ACCEL_GAIN
                    physics.maxSpeed = if (sensorEnabled) SENSOR_MAX_SPEED else KEYPAD_MAX_SPEED
                }
                // 16단계 눈폭풍 (phase 15s~19s): 가속도/최대속도 둘 다 크게 감소
                if (isIce) {
                    val phaseInLoop = accumulatedMs % 20000L
                    if (phaseInLoop in 15000L..19999L) {
                        ax *= 0.25f
                        ay *= 0.25f
                        physics.maxSpeed *= 0.25f
                    }
                }

                var reached = physics.step(dt, ax, ay)
                dynamicMaze?.tick(dt, physics.x, physics.y)
                movingGoal?.tick(dt, physics.x, physics.y)
                rotatingMaze?.tick(dt)
                if (rotatingMaze != null) {
                    ballX = physics.x
                    ballY = physics.y
                }
                portalCooldown = (portalCooldown - dt).coerceAtLeast(0f)
                if (portalCooldown == 0f) {
                    val pa = workingMaze.portalA
                    val pb = workingMaze.portalB
                    if (pa != null && pb != null) {
                        val bc = floor(physics.x).toInt()
                        val br = floor(physics.y).toInt()
                        val dest = when {
                            bc == pa.first && br == pa.second -> pb
                            bc == pb.first && br == pb.second -> pa
                            else -> null
                        }
                        if (dest != null) {
                            physics.teleport(dest.first, dest.second)
                            portalCooldown = 0.5f
                            SoundManager.playGoal()
                        }
                    }
                }
                if (keyDoorCtrl != null) {
                    val before = keyDoorCtrl.collected
                    keyDoorCtrl.tick(physics.x, physics.y)
                    if (keyDoorCtrl.collected > before) {
                        SoundManager.playGoal()
                    }
                    if (!keyDoorCtrl.allCollected) reached = false
                }
                if (starsCtrl != null) {
                    val before = starsCtrl.collected
                    starsCtrl.tick(physics.x, physics.y)
                    if (starsCtrl.collected > before) {
                        SoundManager.playGoal()
                        repeat(8) {
                            val angle = (Math.random() * 2 * Math.PI).toFloat()
                            val speed = 2f + (Math.random() * 3.0).toFloat()
                            dust.add(
                                DustParticle(
                                    x = physics.x,
                                    y = physics.y,
                                    vx = cos(angle) * speed,
                                    vy = sin(angle) * speed,
                                    lifetime = 0.45f
                                )
                            )
                        }
                    }
                    if (!starsCtrl.allCollected) reached = false
                }
                if (isFire) {
                    val bc = floor(physics.x).toInt()
                    val br = floor(physics.y).toInt()
                    val current = bc to br
                    if (current != lastFireCell &&
                        bc in 1 until workingMaze.cols - 1 &&
                        br in 1 until workingMaze.rows - 1
                    ) {
                        val (lc, lr) = lastFireCell
                        if (lc >= 0 && lr >= 0 &&
                            !(lc == workingMaze.startCol && lr == workingMaze.startRow) &&
                            !(lc == workingMaze.goalCol && lr == workingMaze.goalRow) &&
                            workingMaze.grid[lr][lc] == Cell.EMPTY
                        ) {
                            fireTrails.add(FireTrail(lc, lr, 0f))
                        }
                        lastFireCell = current
                    }
                    val it = fireTrails.listIterator()
                    var anyConverted = false
                    while (it.hasNext()) {
                        val t = it.next()
                        t.age += dt
                        if (t.age >= 1.5f) {
                            if (!(t.col == bc && t.row == br)) {
                                workingMaze.grid[t.row][t.col] = Cell.WALL
                                anyConverted = true
                            }
                            it.remove()
                        }
                    }
                    if (anyConverted) fireVersion++
                }
                if (isIce || isKeyDoor) {
                    stormTimer -= dt
                    stormPhase += dt
                    if (stormTimer <= 0f || storms.isEmpty()) {
                        storms.clear()
                        val bc = floor(physics.x).toInt()
                        val br = floor(physics.y).toInt()
                        val maxC = (workingMaze.cols - 3).coerceAtLeast(1)
                        val maxR = (workingMaze.rows - 3).coerceAtLeast(1)
                        val count = (3..6).random()
                        repeat(count) {
                            var tries = 0
                            var nc: Int
                            var nr: Int
                            do {
                                nc = (1..maxC).random()
                                nr = (1..maxR).random()
                                tries++
                                val cx = nc + 1
                                val cy = nr + 1
                                if (abs(cx - bc) + abs(cy - br) >= 4) break
                            } while (tries < 20)
                            storms.add(nc to nr)
                        }
                        stormTimer = 7f
                    }
                    // 첫 2초 = 예고 (충돌 X), 그 후 5초 = 활성 (충돌 O)
                    val isStormActive = stormTimer < 5f
                    if (isStormActive) {
                        val bc = floor(physics.x).toInt()
                        val br = floor(physics.y).toInt()
                        for ((sc, sr) in storms) {
                            if (bc in sc..(sc + 1) && br in sr..(sr + 1) && !reached && !finished) {
                                finished = true
                                shakeMs = 0.3f
                                SoundManager.playBonk()
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onFinished(totalInfiniteMs, true, infiniteRound + 1)
                                break
                            }
                        }
                    }
                }
                if (isSurvival || isFire) {
                    nextEnemySpawnIn -= dt
                    if (nextEnemySpawnIn <= 0f) {
                        val addCount = (2..3).random()
                        repeat(addCount) {
                            chasers.add(
                                ChaserController(
                                    workingMaze,
                                    spawnIndex = chasers.size,
                                    randomMove = true,
                                    randomSpawnMinDistance = if (isSurvival) 8 else 6,
                                )
                            )
                        }
                        if (isSurvival) survivalEnemyCount = chasers.size
                        if (isFire) fireEnemyCount = chasers.size
                        nextEnemySpawnIn = 10f
                    }
                }
                if (chasers.isNotEmpty()) {
                    val bc = floor(physics.x).toInt()
                    val br = floor(physics.y).toInt()
                    for (ch in chasers) {
                        ch.tick(dt, bc, br)
                        val dxc = physics.x - ch.visualX
                        val dyc = physics.y - ch.visualY
                        if (dxc * dxc + dyc * dyc < 0.55f * 0.55f && !reached && !finished) {
                            finished = true
                            shakeMs = 0.3f
                            SoundManager.playBonk()
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onFinished(
                                if (isInfinite) totalInfiniteMs else elapsedMs,
                                true,
                                if (isInfinite) infiniteRound + 1 else 0
                            )
                            break
                        }
                    }
                }
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

                if (reached && isInfinite && !finished) {
                    SoundManager.playGoal()
                    flashAlpha = 0.6f
                    infiniteRound++
                    break
                }
                if (reached && !celebrating) {
                    celebrating = true
                    SoundManager.playGoal()
                    flashAlpha = 0.7f
                    val gx = workingMaze.goalCol + 0.5f
                    val gy = workingMaze.goalRow + 0.5f
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
            // bomb timer tick
            if (!celebrating) {
                when (bombState) {
                    BombState.ARMED -> {
                        bombTimer -= dt
                        if (bombTimer <= 0f) {
                            explodeBomb()
                            bombState = BombState.COOLDOWN
                            bombTimer = if (isFire) 5f else BOMB_COOLDOWN_S
                        }
                    }
                    BombState.COOLDOWN -> {
                        bombTimer -= dt
                        if (bombTimer <= 0f) {
                            bombState = BombState.IDLE
                            bombTimer = 0f
                        }
                    }
                    BombState.IDLE -> Unit
                }
            }

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
                    onFinished(elapsedMs, false, 0)
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
                    text = when {
                        isInfiniteClears -> "무한 도전 · 통과 $infiniteRound"
                        isSurvival -> "생존 모드 · 적 ${chasers.size}"
                        isIce -> "얼음 미로 · 통과 $infiniteRound"
                        isFire -> "타는 길 · 통과 $infiniteRound"
                        isGrow -> "공이 커져요 · 통과 $infiniteRound"
                        isKeyDoor -> "열쇠 ${keyDoorCtrl?.collected ?: 0}/${keyDoorCtrl?.totalKeys ?: 0} · 통과 $infiniteRound"
                        else -> stage.name
                    },
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
                        text = formatElapsed(if (isInfinite) totalInfiniteMs else elapsedMs),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (starsCtrl != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "★ ${starsCtrl.collected} / ${starsCtrl.totalCount}",
                        color = if (starsCtrl.allCollected) GoalGold else InkDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // 20초 주기 스포트라이트: 10s 일반 → 5s 카운트 → 5s 어두움
            val phaseMs = elapsedMs % 20000L
            val countdownNum: Int = when (phaseMs) {
                in 10000L..14999L -> (5 - ((phaseMs - 10000L) / 1000L).toInt()).coerceIn(1, 5)
                else -> 0
            }
            val spotlightAlpha: Float = when (phaseMs) {
                in 15000L..19999L -> {
                    val t = (phaseMs - 15000L) / 5000f
                    when {
                        t < 0.10f -> (t / 0.10f).coerceIn(0f, 1f)
                        t > 0.90f -> ((1f - t) / 0.10f).coerceIn(0f, 1f)
                        else -> 1f
                    }
                }
                else -> 0f
            }

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
                @Suppress("UNUSED_VARIABLE") val mazeVersion = (dynamicMaze?.version ?: 0) + (movingGoal?.version ?: 0) + (starsCtrl?.collectVersion ?: 0) + (rotatingMaze?.version ?: 0) + (keyDoorCtrl?.version ?: 0) + bombVersion + fireVersion
                MazeCanvas(
                    maze = workingMaze,
                    ballX = ballX,
                    ballY = ballY,
                    rotation = ballRotation,
                    squashAmount = ballSquash,
                    squashAxisIsX = ballSquashIsX,
                    trail = trailPositions,
                    ballScale = ballScale * breath * growMultiplier,
                    headingRad = ballHeading,
                    isHappy = celebrating,
                    surpriseLevel = (surpriseTimer / 0.5f).coerceIn(0f, 1f),
                    theme = theme,
                    modifier = Modifier.fillMaxSize()
                )
                if (dynamicMaze != null) {
                    val previews = dynamicMaze.pendingPreview.toList()
                    if (previews.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                            val ox = (size.width - cs * workingMaze.cols) / 2f
                            val oy = (size.height - cs * workingMaze.rows) / 2f
                            val pulse = 0.30f + 0.50f * abs(sin(dynamicMaze.previewProgress * Math.PI.toFloat() * 3f))
                            for (p in previews) {
                                val color = if (p.toWall) Color(0xFFFF5677) else Color(0xFF6BD18B)
                                drawRect(
                                    color = color.copy(alpha = pulse),
                                    topLeft = Offset(ox + p.c * cs, oy + p.r * cs),
                                    size = Size(cs, cs)
                                )
                            }
                        }
                    }
                }
                if (movingGoal != null) {
                    val pg = movingGoal.pendingGoal
                    if (pg != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                            val ox = (size.width - cs * workingMaze.cols) / 2f
                            val oy = (size.height - cs * workingMaze.rows) / 2f
                            val pulse = 0.35f + 0.55f * abs(sin(movingGoal.previewProgress * Math.PI.toFloat() * 3f))
                            drawRect(
                                color = Color(0xFFFFD24A).copy(alpha = pulse),
                                topLeft = Offset(ox + pg.first * cs, oy + pg.second * cs),
                                size = Size(cs, cs)
                            )
                        }
                    }
                }
                if (bombState == BombState.ARMED) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        val bc = floor(ballX).toInt()
                        val br = floor(ballY).toInt()
                        val pulse = 0.35f + 0.30f * abs(sin((BOMB_FUSE_S - bombTimer) * Math.PI.toFloat() * 4f))
                        for (dr in -1..1) for (dc in -1..1) {
                            if (dc == 0 && dr == 0) continue
                            val c = bc + dc
                            val r = br + dr
                            if (c <= 0 || c >= workingMaze.cols - 1) continue
                            if (r <= 0 || r >= workingMaze.rows - 1) continue
                            drawRect(
                                color = Color(0xFF00BFFF).copy(alpha = pulse),
                                topLeft = Offset(ox + c * cs, oy + r * cs),
                                size = Size(cs, cs)
                            )
                        }
                    }
                }
                if (rotatingMaze != null && rotatingMaze.pending) {
                    val pulse = 0.4f + 0.4f * abs(sin(rotatingMaze.previewProgress * Math.PI.toFloat() * 4f))
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1A1A2E).copy(alpha = pulse))
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "↻ 회전!",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                val pa = workingMaze.portalA
                val pb = workingMaze.portalB
                if (pa != null && pb != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        for (p in listOf(pa, pb)) {
                            val center = Offset(ox + (p.first + 0.5f) * cs, oy + (p.second + 0.5f) * cs)
                            drawCircle(color = Color(0xFFB060E0), radius = cs * 0.36f, center = center)
                            drawCircle(color = Color(0xFF5236B5), radius = cs * 0.24f, center = center)
                            drawCircle(color = Color(0xFFE6D2FF), radius = cs * 0.12f, center = center)
                        }
                    }
                }
                if (keyDoorCtrl != null) {
                    val remainingKeys = keyDoorCtrl.keys.toList()
                    val doorList = keyDoorCtrl.doors.toList()
                    val isOpen = keyDoorCtrl.allCollected
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        // 문 (잠겨 있을 때만 그림자체로) — G 인접 셀 모두
                        if (!isOpen) {
                            for ((dc, dr) in doorList) {
                                drawRect(
                                    color = Color(0xFF6B4226),
                                    topLeft = Offset(ox + dc * cs, oy + dr * cs),
                                    size = Size(cs, cs)
                                )
                                // 자물쇠 표시
                                drawCircle(
                                    color = Color(0xFFFFD24A),
                                    radius = cs * 0.16f,
                                    center = Offset(ox + (dc + 0.5f) * cs, oy + (dr + 0.5f) * cs)
                                )
                                drawCircle(
                                    color = Color(0xFF6B4226),
                                    radius = cs * 0.06f,
                                    center = Offset(ox + (dc + 0.5f) * cs, oy + (dr + 0.5f) * cs)
                                )
                            }
                        }
                        // 키
                        for ((kc, kr) in remainingKeys) {
                            val cx = ox + (kc + 0.5f) * cs
                            val cy = oy + (kr + 0.5f) * cs
                            // 키 머리 (둥근 부분)
                            drawCircle(
                                color = Color(0xFFFFD24A),
                                radius = cs * 0.18f,
                                center = Offset(cx - cs * 0.15f, cy)
                            )
                            drawCircle(
                                color = Color(0xFF6B4226),
                                radius = cs * 0.07f,
                                center = Offset(cx - cs * 0.15f, cy)
                            )
                            // 키 줄기 (사각형)
                            drawRect(
                                color = Color(0xFFFFD24A),
                                topLeft = Offset(cx - cs * 0.05f, cy - cs * 0.06f),
                                size = Size(cs * 0.30f, cs * 0.12f)
                            )
                            // 키 이빨
                            drawRect(
                                color = Color(0xFFFFD24A),
                                topLeft = Offset(cx + cs * 0.18f, cy + cs * 0.06f),
                                size = Size(cs * 0.06f, cs * 0.12f)
                            )
                        }
                    }
                }
                if (starsCtrl != null) {
                    val remaining = starsCtrl.remaining.toList()
                    if (remaining.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                            val ox = (size.width - cs * workingMaze.cols) / 2f
                            val oy = (size.height - cs * workingMaze.rows) / 2f
                            for ((c, r) in remaining) {
                                drawCircle(
                                    color = Color(0xFFFFD24A),
                                    radius = cs * 0.22f,
                                    center = Offset(ox + (c + 0.5f) * cs, oy + (r + 0.5f) * cs)
                                )
                                drawCircle(
                                    color = Color(0xFFFF9F1C),
                                    radius = cs * 0.10f,
                                    center = Offset(ox + (c + 0.5f) * cs, oy + (r + 0.5f) * cs)
                                )
                            }
                        }
                    }
                }
                if (isFire && fireTrails.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        for (t in fireTrails) {
                            val progress = (t.age / 1.5f).coerceIn(0f, 1f)
                            // 노랑→주황→빨강으로 점진 변화
                            val r = (0.95f + progress * 0.0f)
                            val g = (0.7f - progress * 0.55f)
                            val b = (0.15f - progress * 0.10f).coerceAtLeast(0f)
                            val baseAlpha = 0.5f + 0.40f * progress
                            drawRect(
                                color = Color(r, g, b, baseAlpha),
                                topLeft = Offset(ox + t.col * cs, oy + t.row * cs),
                                size = Size(cs, cs)
                            )
                            // 깜빡임 효과
                            val flicker = 0.3f + 0.3f * abs(sin((t.age + t.col + t.row) * 12f))
                            drawCircle(
                                color = Color(1f, 0.85f, 0.3f, flicker * (1f - progress * 0.5f)),
                                radius = cs * 0.25f,
                                center = Offset(ox + (t.col + 0.5f) * cs, oy + (t.row + 0.5f) * cs)
                            )
                        }
                    }
                }
                if ((isIce || isKeyDoor) && storms.isNotEmpty()) {
                    val isStormActive = stormTimer < 5f
                    val previewAlpha = ((7f - stormTimer) / 2f).coerceIn(0f, 1f)
                    val canvasAlpha = if (isStormActive) 1f else previewAlpha
                    // 19단계 던전 테마 색 (브라운/골드), 16단계 얼음 테마 (블루/화이트)
                    val ringColor = if (isKeyDoor) Color(0xFF3D2615) else Color(0xFF3D708F)
                    val swirlColor = if (isKeyDoor) Color(0xFFFFD24A) else Color(0xFFE0F4FF)
                    val flakeColor = if (isKeyDoor) Color(0xFFE7B65A) else Color.White
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        val radius = cs * 1.05f
                        for ((sc, sr) in storms) {
                            val centerX = ox + (sc + 1f) * cs
                            val centerY = oy + (sr + 1f) * cs
                            drawCircle(
                                color = ringColor.copy(alpha = 0.55f * canvasAlpha),
                                radius = radius,
                                center = Offset(centerX, centerY)
                            )
                            val swirlPath = Path()
                            val turns = 2
                            val steps = 60
                            for (i in 0..steps) {
                                val t = i / steps.toFloat()
                                val angle = stormPhase * 2.5f + t * Math.PI.toFloat() * 2f * turns
                                val r = radius * (0.15f + 0.75f * t)
                                val x = centerX + r * cos(angle)
                                val y = centerY + r * sin(angle)
                                if (i == 0) swirlPath.moveTo(x, y) else swirlPath.lineTo(x, y)
                            }
                            drawPath(
                                path = swirlPath,
                                color = swirlColor.copy(alpha = 0.85f * canvasAlpha),
                                style = Stroke(
                                    width = radius * 0.16f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                            val flakeCount = 5
                            for (i in 0 until flakeCount) {
                                val a = stormPhase * 1.5f + i * (2f * Math.PI.toFloat() / flakeCount)
                                val fr = radius * 0.75f
                                val fx = centerX + fr * cos(a)
                                val fy = centerY + fr * sin(a)
                                drawCircle(
                                    color = flakeColor.copy(alpha = 0.85f * canvasAlpha),
                                    radius = radius * 0.10f,
                                    center = Offset(fx, fy)
                                )
                            }
                        }
                    }
                }
                if (chasers.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        val br = cs * 0.38f
                        for (ch in chasers) {
                            val cx = ox + ch.visualX * cs
                            val cy = oy + ch.visualY * cs
                            drawCircle(color = Color(0xFF1A0606), radius = br * 1.05f, center = Offset(cx, cy))
                            drawCircle(color = Color(0xFF6B1A0A), radius = br * 0.85f, center = Offset(cx, cy))
                            val eyeR = br * 0.18f
                            drawCircle(color = Color(0xFFFFD24A), radius = eyeR, center = Offset(cx - br * 0.32f, cy - br * 0.15f))
                            drawCircle(color = Color(0xFFFFD24A), radius = eyeR, center = Offset(cx + br * 0.32f, cy - br * 0.15f))
                            drawCircle(color = Color.Black, radius = eyeR * 0.45f, center = Offset(cx - br * 0.32f, cy - br * 0.15f))
                            drawCircle(color = Color.Black, radius = eyeR * 0.45f, center = Offset(cx + br * 0.32f, cy - br * 0.15f))
                        }
                    }
                }
                EffectsOverlay(
                    maze = workingMaze,
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
                val effectiveSpotlight = if (isDarkLevel) {
                    // 7단계: 카운트(10~15s) 동안 밝음. 9~10s 페이드인, 15~16s 페이드아웃.
                    when {
                        phaseMs in 10000L..14999L -> 0f
                        phaseMs in 9000L..9999L -> 1f - (phaseMs - 9000L) / 1000f
                        phaseMs in 15000L..15999L -> (phaseMs - 15000L) / 1000f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                } else spotlightAlpha
                if (effectiveSpotlight > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cs = minOf(size.width / workingMaze.cols, size.height / workingMaze.rows)
                        val ox = (size.width - cs * workingMaze.cols) / 2f
                        val oy = (size.height - cs * workingMaze.rows) / 2f
                        val ballPx = Offset(ox + ballX * cs, oy + ballY * cs)
                        val holeR = if (isDarkLevel) cs * 2.4f else cs * 2.0f
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0f),
                                    0.20f to Color.Black.copy(alpha = 0f),
                                    1f to Color.Black.copy(alpha = 0.92f * effectiveSpotlight)
                                ),
                                center = ballPx,
                                radius = holeR
                            ),
                            size = size
                        )
                    }
                }
                if (isIce && spotlightAlpha > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val alpha = spotlightAlpha
                        // 흰 안개 배경
                        drawRect(
                            color = Color.White.copy(alpha = 0.45f * alpha),
                            size = size
                        )
                        // 휘몰아치는 눈송이 입자
                        val now = elapsedMs / 1000f
                        val particleCount = 80
                        for (i in 0 until particleCount) {
                            val seed = (i * 73 + 11).toFloat()
                            val baseX = ((seed * 0.6543f) % 1f + 1f) % 1f
                            val baseY = ((seed * 0.7891f) % 1f + 1f) % 1f
                            val speedY = 0.35f + ((seed * 0.4567f) % 1f) * 0.5f
                            val wind = 0.18f * sin(now * 0.6f + i * 0.3f)
                            val xRaw = baseX + now * 0.08f + wind
                            val yRaw = baseY + now * speedY
                            val x = ((xRaw % 1f + 1f) % 1f) * size.width
                            val y = ((yRaw % 1f + 1f) % 1f) * size.height
                            val radius = 2.5f + (i % 4)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f * alpha),
                                radius = radius,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                if (bombEnabled) {
                    val bombBg = when (bombState) {
                        BombState.IDLE -> CoralPink
                        BombState.ARMED -> Color(0xFFCC1A1A)
                        BombState.COOLDOWN -> Color.White.copy(alpha = 0.7f)
                    }
                    val bombLabel = when (bombState) {
                        BombState.IDLE -> "폭탄"
                        BombState.ARMED -> "취소"
                        BombState.COOLDOWN -> "${(bombTimer + 0.99f).toInt()}초"
                    }
                    val centerText: String? = when (bombState) {
                        BombState.ARMED -> (bombTimer + 0.99f).toInt().coerceAtLeast(1).toString()
                        else -> null
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .shadow(6.dp, CircleShape)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(bombBg)
                                .clickable(
                                    enabled = bombState != BombState.COOLDOWN,
                                    onClick = ::onBombClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (centerText != null) {
                                Text(
                                    text = centerText,
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                BombIcon(
                                    state = bombState,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = bombLabel,
                            color = InkDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (countdownNum > 0) {
                        val secFrac = ((phaseMs - 10000L) % 1000L) / 1000f
                        val pulse = 1f + 0.30f * (1f - secFrac)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isIce) "곧 눈폭풍!" else "🌑 곧 깜깜!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoralPink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = countdownNum.toString(),
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoralPink,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = pulse
                                    scaleY = pulse
                                }
                            )
                        }
                    }
                }
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
                        .padding(end = 6.dp)
                        .alpha(if (sensorEnabled) 0.45f else 1f)
                )
            }
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
private fun BombIcon(state: BombState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val d = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f + d * 0.06f
        val bodyR = d * 0.34f
        val bodyColor = when (state) {
            BombState.IDLE -> Color(0xFF1E1E1E)
            BombState.ARMED -> Color(0xFF2A0000)
            BombState.COOLDOWN -> Color(0xFF888888)
        }
        // 폭탄 몸체
        drawCircle(color = bodyColor, radius = bodyR, center = Offset(cx, cy))
        // 광택
        drawCircle(
            color = Color.White.copy(alpha = if (state == BombState.COOLDOWN) 0.25f else 0.45f),
            radius = bodyR * 0.22f,
            center = Offset(cx - bodyR * 0.38f, cy - bodyR * 0.38f)
        )
        // 도화선 받침 (몸체 위 작은 갈색 박스)
        val capW = bodyR * 0.45f
        val capH = bodyR * 0.18f
        drawRect(
            color = Color(0xFF7A5A2E),
            topLeft = Offset(cx - capW / 2f, cy - bodyR - capH),
            size = Size(capW, capH)
        )
        // 도화선 (오른쪽 위로 비스듬히)
        val fuseStart = Offset(cx + capW * 0.2f, cy - bodyR - capH)
        val fuseEnd = Offset(cx + bodyR * 0.9f, cy - bodyR * 1.55f)
        drawLine(
            color = Color(0xFF8A6235),
            start = fuseStart,
            end = fuseEnd,
            strokeWidth = d * 0.05f,
            cap = StrokeCap.Round
        )
        if (state != BombState.COOLDOWN) {
            // 불꽃 외곽 (노랑)
            drawCircle(
                color = Color(0xFFFFD24A),
                radius = d * 0.10f,
                center = fuseEnd
            )
            // 불꽃 안쪽 (주황)
            drawCircle(
                color = Color(0xFFFF6A2C),
                radius = d * 0.06f,
                center = fuseEnd
            )
        }
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
