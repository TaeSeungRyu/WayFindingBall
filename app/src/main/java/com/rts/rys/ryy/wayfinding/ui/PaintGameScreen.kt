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
import androidx.compose.runtime.mutableStateListOf
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
import com.rts.rys.ryy.wayfinding.game.Cell
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
import kotlin.math.sqrt
import kotlin.random.Random

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f
// 대결 AI 이동 속도는 스테이지별(PaintStage.aiMaxSpeed/aiAccelGain)로 지정한다.
/** AI가 칸 하나를 칠한 뒤 다음 목표로 가기 전 잠깐 쉬는 시간(초, 8단계 전용). */
private const val AI_THINK_PAUSE = 0.36f
// 동적 벽: 항상 3~6개가 랜덤 위치에 나타났다가 수명이 다하면 사라진다.
private const val WALL_MIN = 3
private const val WALL_MAX = 6
private const val WALL_LIFE_MIN = 2.0f   // 벽 하나가 유지되는 최소 시간(초)
private const val WALL_LIFE_MAX = 4.0f
/** 목표에 이 시간(초) 넘게 못 닿으면(벽에 막힘 등) 목표를 다시 고른다. */
private const val AI_TARGET_TIMEOUT = 2.5f

// 술래(방해꾼) — 1등(가장 많이 차지한) 공을 노린다. 닿으면 기절+칸 지움.
// 1등을 노리므로 앞설수록 사냥당해 순위가 계속 뒤집힌다. (속도는 피할 수 있게 느리게)
private const val CHASER_MAX_SPEED = 7.0f
private const val CHASER_ACCEL_GAIN = 20f
private const val CHASER_CATCH_R = 0.6f   // 이 거리(칸) 안이면 잡힘
private const val CHASER_STUN_S = 1.2f     // 잡힌 공이 멈추는 시간
private const val CHASER_COOLDOWN_S = 1.0f // 한 번 잡은 뒤 다시 잡기까지

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

    // 대결 모드(8·9단계) 상태.
    val versus = stage.versus
    val aiN = if (versus) stage.aiBalls else 0
    val timed = stage.countdownS > 0f
    val overwrite = stage.allowOverwrite
    val aiList = remember(attemptId) { List(aiN) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) } }
    val aiPos = remember(attemptId) { mutableStateListOf<Offset>().apply { repeat(aiN) { add(Offset.Zero) } } }
    // 각 색(팔레트 인덱스)이 차지한 칸 수. [0]=나, [1..]=AI.
    val counts = remember(attemptId) { mutableStateListOf<Int>().apply { repeat(stage.palette.size) { add(0) } } }
    var timeLeftMs by remember(attemptId) { mutableLongStateOf(0L) }
    var won by remember(attemptId) { mutableStateOf(false) }
    var draw by remember(attemptId) { mutableStateOf(false) }

    // 11단계 술래(방해꾼).
    val chaserOn = versus && stage.chaser
    val chaserPhysics = remember(attemptId) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) }
    var chaserPos by remember(attemptId) { mutableStateOf(Offset.Zero) }
    var playerStunned by remember(attemptId) { mutableStateOf(false) }

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
        val aiLast = Array(aiN) { -1 to -1 }
        val aiIdle = FloatArray(aiN)
        val aiTarget = arrayOfNulls<Pair<Int, Int>>(aiN)
        val aiTargetAge = FloatArray(aiN)
        val rnd = Random(attemptId + 101)
        val activeWalls = ArrayList<TempWall>()
        var wallTarget = WALL_MIN + rnd.nextInt(WALL_MAX - WALL_MIN + 1)  // 3~6
        var wallRetargetTimer = 0f
        var wallSpawnCd = 0f
        var playerStun = 0f
        val aiStun = FloatArray(aiN)
        var chaserCd = 0f
        var moved = false

        // 칸 하나를 [idx] 색으로 칠하고 점수판을 갱신. 덮어쓰기 불가 모드에선 빈 칸만.
        fun tryPaint(c: Int, r: Int, idx: Int): Int {
            if (!overwrite && paintCtrl.isPainted(c, r)) return 0
            val old = paintCtrl.colorAt(c, r)
            val res = paintCtrl.paint(c, r, idx)
            if (res != 0) {
                if (old in counts.indices) counts[old] = counts[old] - 1
                if (idx in counts.indices) counts[idx] = counts[idx] + 1
            }
            return res
        }

        if (versus) {
            // 시작 칸 점수 반영: 컨트롤러가 중앙을 idx0(나)으로 이미 칠해 둠.
            if (counts.isNotEmpty()) counts[0] = 1
            val corners = listOf(
                (arena.cols - 2) to (arena.rows - 2),
                1 to (arena.rows - 2),
                (arena.cols - 2) to 1,
                1 to 1,
            )
            for (i in 0 until aiN) {
                val (cc, cr) = corners[i % corners.size]
                aiList[i].setPositionAndStop(cc, cr)
                aiPos[i] = Offset(aiList[i].x, aiList[i].y)
                tryPaint(cc, cr, i + 1)
                aiLast[i] = cc to cr
            }
            if (chaserOn) {
                chaserPhysics.setPositionAndStop(arena.cols / 2, 1)
                chaserPos = Offset(chaserPhysics.x, chaserPhysics.y)
            }
            // 고정 장애물: 시작 위치를 피해 무작위 칸을 벽으로.
            if (stage.obstacles > 0) {
                val taken = HashSet<Pair<Int, Int>>()
                taken.add(arena.startCol to arena.startRow)
                for (i in 0 until aiN) {
                    taken.add(floor(aiList[i].x).toInt() to floor(aiList[i].y).toInt())
                }
                if (chaserOn) taken.add(floor(chaserPhysics.x).toInt() to floor(chaserPhysics.y).toInt())
                val cells = ArrayList<Pair<Int, Int>>()
                for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
                    if ((c to r) in taken || !paintCtrl.isReachable(c, r)) continue
                    cells.add(c to r)
                }
                cells.shuffle(rnd)
                for (k in 0 until minOf(stage.obstacles, cells.size)) {
                    val (oc, orow) = cells[k]
                    paintCtrl.wallify(oc, orow)
                    arena.grid[orow][oc] = Cell.WALL
                }
            }
            timeLeftMs = (stage.countdownS * 1000).toLong()
            moved = true  // 대결은 시작과 동시에 시간이 흐른다.
        }

        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (paused) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            val deltaMs = (now - last) / 1_000_000L
            // 첫 칸을 새로 칠하기 전(공을 아직 안 굴린 상태)엔 시간이 흐르지 않게 한다.
            if (moved) elapsedMs += deltaMs
            if (timed) timeLeftMs = (timeLeftMs - deltaMs).coerceAtLeast(0L)
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

            if (playerStun > 0f) {
                // 술래에 잡혀 기절 — 제자리에 멈춘다.
                playerStun -= dt
                physics.stop()
                if (playerStun <= 0f) playerStunned = false
            } else {
                physics.step(dt, ax, ay)
            }
            ballX = physics.x
            ballY = physics.y

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()
            val cell = bc to br
            if (cell != lastCell) {
                if (versus) {
                    // 대결: 내 색(0)으로. 덮어쓰기 모드면 남의 칸도 뺏는다.
                    if (tryPaint(bc, br, 0) == 2) SoundManager.playStarTone((counts[0]) % 12)
                } else {
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
                }
                lastCell = cell
            }

            // AI 공들: 각자 가장 가까운 '내 것이 아닌' 칸으로 굴러가 자기 색으로 칠한다.
            if (versus && !finished) {
                for (i in 0 until aiN) {
                    val ph = aiList[i]
                    if (aiStun[i] > 0f) {
                        // 술래에 잡혀 기절 — 이번 프레임은 멈춘다.
                        aiStun[i] -= dt
                        ph.stop()
                        aiPos[i] = Offset(ph.x, ph.y)
                        continue
                    }
                    ph.maxSpeed = stage.aiMaxSpeed
                    if (timed) {
                        // 목표를 하나 정해 도달까지 유지 + 가끔 무작위 목표 — 부드럽고 덜 단조롭게.
                        // 목표가 벽에 막히거나 오래 못 닿으면 다시 고른다.
                        val cur = floor(ph.x).toInt() to floor(ph.y).toInt()
                        aiTargetAge[i] += dt
                        var tgt = aiTarget[i]
                        if (tgt == null || tgt == cur ||
                            paintCtrl.colorAt(tgt.first, tgt.second) == i + 1 ||
                            !paintCtrl.isReachable(tgt.first, tgt.second) ||
                            aiTargetAge[i] > AI_TARGET_TIMEOUT
                        ) {
                            tgt = pickAiTarget(paintCtrl, arena, ph.x, ph.y, i + 1, rnd)
                            aiTarget[i] = tgt
                            aiTargetAge[i] = 0f
                        }
                        if (tgt != null) {
                            var dx = (tgt.first + 0.5f) - ph.x
                            var dy = (tgt.second + 0.5f) - ph.y
                            val len = sqrt(dx * dx + dy * dy)
                            if (len > 0.001f) { dx /= len; dy /= len }
                            ph.step(dt, dx * stage.aiAccelGain, dy * stage.aiAccelGain)
                        } else {
                            ph.step(dt, 0f, 0f)
                        }
                    } else {
                        // 8단계: 가장 가까운 빈 칸으로, 칸마다 잠깐 멈칫.
                        if (aiIdle[i] > 0f) {
                            aiIdle[i] -= dt
                            ph.step(dt, 0f, 0f)
                        } else {
                            val target = nearestUnpainted(paintCtrl, arena, ph.x, ph.y)
                            if (target != null) {
                                var dx = (target.first + 0.5f) - ph.x
                                var dy = (target.second + 0.5f) - ph.y
                                val len = sqrt(dx * dx + dy * dy)
                                if (len > 0.001f) { dx /= len; dy /= len }
                                ph.step(dt, dx * stage.aiAccelGain, dy * stage.aiAccelGain)
                            } else {
                                ph.step(dt, 0f, 0f)
                            }
                        }
                    }
                    aiPos[i] = Offset(ph.x, ph.y)
                    val ac = floor(ph.x).toInt()
                    val ar = floor(ph.y).toInt()
                    val acell = ac to ar
                    if (acell != aiLast[i]) {
                        val painted = tryPaint(ac, ar, i + 1)
                        if (painted != 0 && !timed) aiIdle[i] = AI_THINK_PAUSE
                        aiLast[i] = acell
                    }
                }

                // 공끼리 충돌 → 서로 튕겨나간다(나 + AI들. 술래는 제외).
                if (stage.ballBounce) {
                    val n = aiN + 1
                    val balls = Array(n) { if (it == 0) physics else aiList[it - 1] }
                    for (a in 0 until n) for (b in a + 1 until n) {
                        bounceBalls(balls[a], balls[b])
                    }
                    ballX = physics.x
                    ballY = physics.y
                    for (i in 0 until aiN) aiPos[i] = Offset(aiList[i].x, aiList[i].y)
                }

                // 술래: 기절 안 한 가장 가까운 공을 쫓다 닿으면 그 공을 기절시키고 그 칸을 지운다.
                if (chaserOn) {
                    val cph = chaserPhysics
                    cph.maxSpeed = CHASER_MAX_SPEED
                    var tx = 0f
                    var ty = 0f
                    var found = false
                    // 1순위: 1등(최다 색) 공. 기절 중이 아니면 그 공을 쫓는다.
                    var leadIdx = 0
                    var leadCount = -1
                    for (k in counts.indices) {
                        if (counts[k] > leadCount) { leadCount = counts[k]; leadIdx = k }
                    }
                    val leaderStunned = if (leadIdx == 0) playerStun > 0f else aiStun[leadIdx - 1] > 0f
                    if (!leaderStunned) {
                        tx = if (leadIdx == 0) physics.x else aiList[leadIdx - 1].x
                        ty = if (leadIdx == 0) physics.y else aiList[leadIdx - 1].y
                        found = true
                    } else {
                        // 1등이 기절 중이면 기절 안 한 가장 가까운 공으로.
                        var bestD = Float.MAX_VALUE
                        if (playerStun <= 0f) {
                            val dx = physics.x - cph.x
                            val dy = physics.y - cph.y
                            bestD = dx * dx + dy * dy
                            tx = physics.x; ty = physics.y; found = true
                        }
                        for (i in 0 until aiN) {
                            if (aiStun[i] > 0f) continue
                            val dx = aiList[i].x - cph.x
                            val dy = aiList[i].y - cph.y
                            val d = dx * dx + dy * dy
                            if (d < bestD) { bestD = d; tx = aiList[i].x; ty = aiList[i].y; found = true }
                        }
                    }
                    if (found) {
                        var dx = tx - cph.x
                        var dy = ty - cph.y
                        val len = sqrt(dx * dx + dy * dy)
                        if (len > 0.001f) { dx /= len; dy /= len }
                        cph.step(dt, dx * CHASER_ACCEL_GAIN, dy * CHASER_ACCEL_GAIN)
                    } else {
                        cph.step(dt, 0f, 0f)
                    }
                    chaserPos = Offset(cph.x, cph.y)

                    chaserCd -= dt
                    if (chaserCd <= 0f) {
                        val pdx = physics.x - cph.x
                        val pdy = physics.y - cph.y
                        if (playerStun <= 0f && pdx * pdx + pdy * pdy <= CHASER_CATCH_R * CHASER_CATCH_R) {
                            playerStun = CHASER_STUN_S
                            playerStunned = true
                            physics.stop()
                            val old = paintCtrl.erase(floor(physics.x).toInt(), floor(physics.y).toInt())
                            if (old in counts.indices) counts[old] = counts[old] - 1
                            chaserCd = CHASER_COOLDOWN_S
                            SoundManager.playBonk()
                        } else {
                            for (i in 0 until aiN) {
                                if (aiStun[i] > 0f) continue
                                val adx = aiList[i].x - cph.x
                                val ady = aiList[i].y - cph.y
                                if (adx * adx + ady * ady <= CHASER_CATCH_R * CHASER_CATCH_R) {
                                    aiStun[i] = CHASER_STUN_S
                                    aiList[i].stop()
                                    val old = paintCtrl.erase(floor(aiList[i].x).toInt(), floor(aiList[i].y).toInt())
                                    if (old in counts.indices) counts[old] = counts[old] - 1
                                    chaserCd = CHASER_COOLDOWN_S
                                    SoundManager.playBonk()
                                    break
                                }
                            }
                        }
                    }
                }

                // 동적 벽: 항상 3~6개가 나타났다 사라진다. 칠해진 칸 위에도 생기며,
                // 공이 올라가 있는 칸은 피한다. 수명이 다하면 빈 바닥으로 복원.
                if (stage.dynamicWalls) {
                    // 목표 개수를 주기적으로 3~6 사이에서 다시 뽑는다.
                    wallRetargetTimer += dt
                    if (wallRetargetTimer >= 3f) {
                        wallRetargetTimer = 0f
                        wallTarget = WALL_MIN + rnd.nextInt(WALL_MAX - WALL_MIN + 1)
                    }
                    // 수명이 끝난 벽 제거 → 바닥 복원.
                    val wit = activeWalls.iterator()
                    while (wit.hasNext()) {
                        val w = wit.next()
                        w.life -= dt
                        if (w.life <= 0f) {
                            paintCtrl.unwall(w.c, w.r)
                            arena.grid[w.r][w.c] = Cell.EMPTY
                            wit.remove()
                        }
                    }
                    // 목표 개수까지 하나씩(살짝 텀을 두고) 새 벽 생성.
                    wallSpawnCd -= dt
                    if (activeWalls.size < wallTarget && wallSpawnCd <= 0f) {
                        val occupied = HashSet<Pair<Int, Int>>()
                        occupied.add(floor(physics.x).toInt() to floor(physics.y).toInt())
                        for (i in 0 until aiN) {
                            occupied.add(floor(aiList[i].x).toInt() to floor(aiList[i].y).toInt())
                        }
                        val cands = ArrayList<Pair<Int, Int>>()
                        for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
                            if (!paintCtrl.isReachable(c, r)) continue
                            if ((c to r) in occupied) continue
                            cands.add(c to r)
                        }
                        if (cands.isNotEmpty()) {
                            val (wc, wr) = cands[rnd.nextInt(cands.size)]
                            val old = paintCtrl.wallify(wc, wr)
                            if (old in counts.indices) counts[old] = counts[old] - 1
                            arena.grid[wr][wc] = Cell.WALL
                            activeWalls.add(TempWall(wc, wr, WALL_LIFE_MIN + rnd.nextFloat() * (WALL_LIFE_MAX - WALL_LIFE_MIN)))
                            // 그 칸을 노리던 AI는 목표를 다시 고르게.
                            for (i in 0 until aiN) {
                                if (aiTarget[i] == (wc to wr)) aiTarget[i] = null
                            }
                            wallSpawnCd = 0.3f + rnd.nextFloat() * 0.4f
                        }
                    }
                }

                // 종료: 시간제는 타임업, 아니면 판이 다 찼을 때. 최다 색이 우승.
                val over = if (timed) timeLeftMs <= 0L else paintCtrl.done
                if (over) {
                    val my = counts[0]
                    val best = counts.maxOrNull() ?: 0
                    won = my == best && counts.count { it == best } == 1
                    draw = my == best && !won
                    isNewBest = if (timed) {
                        PaintRecordsRepository(context).recordScore(level, my)
                    } else if (won) {
                        PaintRecordsRepository(context).record(level, elapsedMs)
                    } else false
                    finished = true
                    SoundManager.playGoal()
                }
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
                if (versus) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        stage.palette.forEachIndexed { i, c ->
                            if (i > 0) {
                                Text(" : ", color = InkDark, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                "${counts.getOrElse(i) { 0 }}",
                                color = c,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                } else {
                    Text(
                        text = "$done / ${paintCtrl.total}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Text(
                    text = formatElapsed(if (timed) timeLeftMs else elapsedMs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (timed && timeLeftMs <= 5000L) CoralPink else InkSoft,
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
                    text = when {
                        stage.versus && stage.chaser -> "술래를 피해 땅을 넓혀요!"
                        stage.versus && stage.dynamicWalls -> "벽을 피해 땅을 넓혀요!"
                        stage.versus && overwrite -> "덮어 칠하며 땅을 넓혀요!"
                        stage.versus -> "많이 칠하면 이겨요!"
                        stage.chooseColor -> "좋아하는 색으로 칠해요!"
                        else -> "바닥을 모두 칠해요!"
                    },
                    fontSize = 20.sp,
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
                        rivals = if (versus) {
                            List(aiN) { aiPos[it] to stage.palette.getOrElse(it + 1) { Color.Red } }
                        } else emptyList(),
                        chaser = if (chaserOn) chaserPos else null,
                        playerStunned = playerStunned,
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
                versus = versus,
                won = won,
                draw = draw,
                counts = counts.toList(),
                colors = stage.palette,
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

/** 나타났다 사라지는 동적 벽 한 개. [life]는 남은 수명(초). */
private class TempWall(val c: Int, val r: Int, var life: Float)

/** 두 공이 겹치면 겹침을 풀고, 서로 다가오는 중이면 법선 방향으로 튕겨낸다(질량 동일·탄성 e). */
private fun bounceBalls(a: BallPhysics, b: BallPhysics, radius: Float = 0.32f, e: Float = 0.8f) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val distSq = dx * dx + dy * dy
    val minDist = radius * 2f
    if (distSq >= minDist * minDist || distSq < 1e-6f) return
    val dist = sqrt(distSq)
    val nx = dx / dist
    val ny = dy / dist
    // 겹침 분리 — 각자 절반씩 밀어낸다.
    val overlap = minDist - dist
    a.nudgePosition(-nx * overlap / 2f, -ny * overlap / 2f)
    b.nudgePosition(nx * overlap / 2f, ny * overlap / 2f)
    // 법선 방향 상대속도가 접근(음수)일 때만 튕김.
    val vn = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
    if (vn < 0f) {
        val j = -(1f + e) * vn / 2f
        a.applyImpulse(-j * nx, -j * ny)
        b.applyImpulse(j * nx, j * ny)
    }
}

/** AI 위치에서 가장 가까운 '도달 가능하고 아직 안 칠한' 칸을 찾는다. 없으면 null. */
private fun nearestUnpainted(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
): Pair<Int, Int>? {
    var best: Pair<Int, Int>? = null
    var bestD = Float.MAX_VALUE
    for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
        if (!paint.isReachable(c, r) || paint.isPainted(c, r)) continue
        val dx = (c + 0.5f) - x
        val dy = (r + 0.5f) - y
        val d = dx * dx + dy * dy
        if (d < bestD) { bestD = d; best = c to r }
    }
    return best
}

/**
 * [myIdx] 색이 아닌(빈 칸 또는 남의 색) 가장 가까운 도달 가능 칸. 땅따먹기 AI용.
 * 지금 서 있는 칸은 제외한다 — 그 칸이 뺏겨 최단이 되면 목표가 제자리라 AI가 얼어붙기 때문.
 */
private fun nearestNotMine(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
    myIdx: Int,
): Pair<Int, Int>? {
    val curC = floor(x).toInt()
    val curR = floor(y).toInt()
    var best: Pair<Int, Int>? = null
    var bestD = Float.MAX_VALUE
    for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
        if (c == curC && r == curR) continue
        if (!paint.isReachable(c, r) || paint.colorAt(c, r) == myIdx) continue
        val dx = (c + 0.5f) - x
        val dy = (r + 0.5f) - y
        val d = dx * dx + dy * dy
        if (d < bestD) { bestD = d; best = c to r }
    }
    return best
}

/**
 * 땅따먹기 AI의 다음 목표 칸. 대개 가장 가까운 '내 것 아닌' 칸으로 가되,
 * 30% 확률로 무작위 칸을 골라 세 공이 한곳에 뭉치지 않고 맵을 넓게 돌게 한다.
 */
private fun pickAiTarget(
    paint: FloorPaintController,
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    x: Float,
    y: Float,
    myIdx: Int,
    rnd: Random,
): Pair<Int, Int>? {
    if (rnd.nextFloat() < 0.3f) {
        val curC = floor(x).toInt()
        val curR = floor(y).toInt()
        val candidates = ArrayList<Pair<Int, Int>>()
        for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
            if (c == curC && r == curR) continue
            if (!paint.isReachable(c, r) || paint.colorAt(c, r) == myIdx) continue
            candidates.add(c to r)
        }
        if (candidates.isNotEmpty()) return candidates[rnd.nextInt(candidates.size)]
    }
    return nearestNotMine(paint, arena, x, y, myIdx)
}

@Composable
private fun PaintArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    paint: FloorPaintController,
    palette: List<Color>,
    ballX: Float,
    ballY: Float,
    rivals: List<Pair<Offset, Color>> = emptyList(),
    chaser: Offset? = null,
    playerStunned: Boolean = false,
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

        // AI 라이벌 공들 (대결 모드) — 눈 달린 색 공.
        for ((pos, rivalColor) in rivals) {
            val er = cell * 0.4f
            val ex = pos.x * cell
            val ey = pos.y * cell
            drawCircle(rivalColor.copy(alpha = 0.35f), radius = er * 1.5f, center = Offset(ex, ey))
            drawCircle(rivalColor, radius = er, center = Offset(ex, ey))
            val eyeDx = er * 0.32f
            val eyeY = ey - er * 0.06f
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.12f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.12f, center = Offset(ex + eyeDx, eyeY))
        }

        // 술래(방해꾼) — 어두운 공에 눈.
        if (chaser != null) {
            val er = cell * 0.42f
            val ex = chaser.x * cell
            val ey = chaser.y * cell
            drawCircle(Color(0xFF6A1B9A).copy(alpha = 0.35f), radius = er * 1.6f, center = Offset(ex, ey))
            drawCircle(Color(0xFF4A148C), radius = er, center = Offset(ex, ey))
            val eyeDx = er * 0.34f
            val eyeY = ey - er * 0.05f
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.26f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.13f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.13f, center = Offset(ex + eyeDx, eyeY))
        }

        // 공
        val r = cell * 0.4f
        val cx = ballX * cell
        val cy = ballY * cell
        drawBallDecoration(skin, cx, cy, r, phaseSec = pulse)
        drawBallBody(skin, cx, cy, r)
        // 기절 표시 — 붉은 링.
        if (playerStunned) {
            drawCircle(
                color = Color(0xFFE53935),
                radius = r * 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = cell * 0.06f),
            )
        }
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
    versus: Boolean = false,
    won: Boolean = false,
    draw: Boolean = false,
    counts: List<Int> = emptyList(),
    colors: List<Color> = emptyList(),
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
            Text(
                text = when {
                    !versus -> "🎨"
                    draw -> "🤝"
                    won -> "🏆"
                    else -> "😢"
                },
                fontSize = 56.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    !versus -> "참 잘했어요!"
                    draw -> "비겼어요!"
                    won -> "이겼어요!"
                    else -> "졌어요!"
                },
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            if (versus) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    colors.forEachIndexed { i, c ->
                        if (i > 0) {
                            Text(" : ", color = InkSoft, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "${counts.getOrElse(i) { 0 }}",
                            color = c,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "칸을 더 많이 차지하면 이겨요",
                    color = InkSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isNewBest) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "✨ 최고 점수! ✨",
                        color = CoralPink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            } else {
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
