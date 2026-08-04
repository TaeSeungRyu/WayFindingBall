package com.rts.rys.ryy.wayfinding.ui

import android.widget.Toast
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.PaintRecordsRepository
import com.rts.rys.ryy.wayfinding.data.ShareUtils
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
import kotlin.math.sin
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

// 폭탄 — 랜덤 위치에 생겨 도화선이 타들어가다 터진다. 범위 안 공은 정지 + 그 자리 색 지움.
private const val BOMB_SPAWN_INTERVAL = 2.2f
private const val BOMB_FUSE = 1.8f         // 생긴 뒤 터지기까지(초)
private const val BOMB_MAX = 3             // 동시에 존재할 최대 폭탄 수
internal const val BOMB_BLAST_R = 1.6f     // 폭발 반경(칸) — 이 안의 공이 정지 (PaintArenaCanvas와 공유)
private const val BOMB_STUN_S = 3.0f       // 터졌을 때 공이 멈추는 시간
private const val BLAST_LIFE = 0.45f       // 폭발 연출 지속(초)

// 공끼리 충돌(튕김)·넉백. 탄성/최소 세기(BOUNCE_E·BOUNCE_MIN)는 PaintGameParts로 이동.
/** 부딪힌 뒤 조종을 잠깐 끊고 튕긴 속도로 미끄러지는 시간(초). 없으면 추진에 바로 상쇄됨. */
private const val KNOCK_S = 0.22f

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

    // 색칠 도안(14단계): 목표 색 격자(-1=배경) + 맞게 칠한 칸 수.
    val cnTarget = remember(attemptId) {
        val t = Array(arena.rows) { IntArray(arena.cols) { -1 } }
        stage.template?.forEachIndexed { tr, line ->
            line.forEachIndexed { tc, ch ->
                if (ch in '0'..'9') {
                    val r = tr + 1
                    val c = tc + 1
                    if (r in t.indices && c in t[r].indices) t[r][c] = ch - '0'
                }
            }
        }
        t
    }
    val cnTotal = remember(attemptId) { cnTarget.sumOf { row -> row.count { it >= 0 } } }
    var cnCorrect by remember(attemptId) { mutableIntStateOf(0) }

    // 대결 모드(8·9단계) 상태.
    val versus = stage.versus
    val aiN = if (versus) stage.aiBalls else 0
    // 팀전: 아군(AI0)은 나와 같은 색(0), 적(AI1,2)은 색1. 일반전은 각자 고유색(i+1).
    val teams = stage.teams
    val aiColorIdx = remember(attemptId, aiN) {
        IntArray(aiN) { i -> if (teams) (if (i == 0) 0 else 1) else i + 1 }
    }
    val timed = stage.countdownS > 0f
    val overwrite = stage.allowOverwrite
    val aiList = remember(attemptId) { List(aiN) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) } }
    val aiPos = remember(attemptId) { mutableStateListOf<Offset>().apply { repeat(aiN) { add(Offset.Zero) } } }
    // 각 색(팔레트 인덱스)이 차지한 칸 수. [0]=나, [1..]=AI.
    val counts = remember(attemptId) { mutableStateListOf<Int>().apply { repeat(stage.palette.size) { add(0) } } }
    // 별 구역(2x2) 칸 좌표 — 그 칸은 점수 2배. 색별 별 칸 소유 수는 starCounts.
    val starCells = remember(attemptId) {
        if (!stage.zones) emptySet()
        else {
            val tls = listOf(
                2 to 2, (arena.cols - 4) to 2, 2 to (arena.rows - 4), (arena.cols - 4) to (arena.rows - 4)
            )
            val s = HashSet<Pair<Int, Int>>()
            for ((tc, tr) in tls) for (dc in 0..1) for (dr in 0..1) {
                val c = tc + dc
                val r = tr + dr
                if (c in 1 until arena.cols - 1 && r in 1 until arena.rows - 1) s.add(c to r)
            }
            s
        }
    }
    val starCounts = remember(attemptId) { mutableStateListOf<Int>().apply { repeat(stage.palette.size) { add(0) } } }
    var timeLeftMs by remember(attemptId) { mutableLongStateOf(0L) }
    var won by remember(attemptId) { mutableStateOf(false) }
    var draw by remember(attemptId) { mutableStateOf(false) }
    // 폭탄/폭발 렌더용 (위치, 진행도).
    var bombRender by remember(attemptId) { mutableStateOf<List<Pair<Offset, Float>>>(emptyList()) }
    var blastRender by remember(attemptId) { mutableStateOf<List<Pair<Offset, Float>>>(emptyList()) }

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
        var playerKnock = 0f
        val aiKnock = FloatArray(aiN)
        var chaserCd = 0f
        val activeBombs = ArrayList<Bomb>()
        val blasts = ArrayList<Blast>()
        var bombSpawnCd = BOMB_SPAWN_INTERVAL
        var savedPeak = 0  // 시간제 대결에서 지금까지 기록한 내 최고 칸 수.
        var moved = false

        // 칸 하나를 [idx] 색으로 칠하고 점수판을 갱신. 덮어쓰기 불가 모드에선 빈 칸만.
        fun tryPaint(c: Int, r: Int, idx: Int): Int {
            if (!overwrite && paintCtrl.isPainted(c, r)) return 0
            val old = paintCtrl.colorAt(c, r)
            val res = paintCtrl.paint(c, r, idx)
            if (res != 0) {
                if (old in counts.indices) counts[old] = counts[old] - 1
                if (idx in counts.indices) counts[idx] = counts[idx] + 1
                if ((c to r) in starCells) {  // 별 구역은 점수판(starCounts)도 갱신 — 2배 반영용.
                    if (old in starCounts.indices) starCounts[old] = starCounts[old] - 1
                    if (idx in starCounts.indices) starCounts[idx] = starCounts[idx] + 1
                }
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
                tryPaint(cc, cr, aiColorIdx[i])
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

        // 색칠 도안: 컨트롤러가 자동으로 칠한 시작 칸을 비워 도안 안내가 보이게.
        if (stage.colorByNumber) paintCtrl.erase(arena.startCol, arena.startRow)

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
            } else if (playerKnock > 0f) {
                // 부딪혀 튕기는 중 — 조종 없이 튕긴 속도로 미끄러진다.
                playerKnock -= dt
                physics.step(dt, 0f, 0f)
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
                } else if (stage.colorByNumber) {
                    // 색칠 도안: 도안 칸에 '맞는 색'일 때만 칠해진다. 틀린 색은 아무 반응 없음.
                    val tgt = if (br in cnTarget.indices && bc in cnTarget[br].indices) cnTarget[br][bc] else -1
                    if (tgt >= 0 && colorIndex == tgt && paintCtrl.colorAt(bc, br) != tgt) {
                        paintCtrl.paint(bc, br, colorIndex)
                        moved = true
                        cnCorrect++
                        SoundManager.playStarTone(cnCorrect % 12)
                        if (cnCorrect >= cnTotal) {
                            isNewBest = PaintRecordsRepository(context).record(level, elapsedMs)
                            finished = true
                            SoundManager.playGoal()
                            SoundManager.speak("참 잘했어요")
                        }
                    }
                } else {
                    // 일반 칠하기 — 대칭 모드면 상하좌우 대칭 칸까지 함께 칠한다.
                    val targets = if (stage.mirror) {
                        val c2 = arena.cols - 1 - bc
                        val r2 = arena.rows - 1 - br
                        setOf(bc to br, c2 to br, bc to r2, c2 to r2)
                    } else {
                        setOf(bc to br)
                    }
                    var anyNew = false
                    var anyChange = false
                    for ((pc, pr) in targets) {
                        val res = paintCtrl.paint(pc, pr, colorIndex)
                        if (res != 0) anyChange = true
                        if (res == 2) anyNew = true
                    }
                    if (anyChange) moved = true
                    if (anyNew) {  // 처음 칠한 칸이 생겼을 때만 소리·완료 판정.
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
                    if (aiKnock[i] > 0f) {
                        // 부딪혀 튕기는 중 — 추적 없이 튕긴 속도로 미끄러진다.
                        aiKnock[i] -= dt
                        ph.step(dt, 0f, 0f)
                    } else if (timed) {
                        // 목표를 하나 정해 도달까지 유지 + 가끔 무작위 목표 — 부드럽고 덜 단조롭게.
                        // 목표가 벽에 막히거나 오래 못 닿으면 다시 고른다.
                        val cur = floor(ph.x).toInt() to floor(ph.y).toInt()
                        aiTargetAge[i] += dt
                        var tgt = aiTarget[i]
                        if (tgt == null || tgt == cur ||
                            paintCtrl.colorAt(tgt.first, tgt.second) == aiColorIdx[i] ||
                            !paintCtrl.isReachable(tgt.first, tgt.second) ||
                            aiTargetAge[i] > AI_TARGET_TIMEOUT
                        ) {
                            tgt = pickAiTarget(paintCtrl, arena, ph.x, ph.y, aiColorIdx[i], rnd, starCells)
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
                        val painted = tryPaint(ac, ar, aiColorIdx[i])
                        if (painted != 0 && !timed) aiIdle[i] = AI_THINK_PAUSE
                        aiLast[i] = acell
                    }
                }

                // 공끼리 충돌 → 서로 튕겨나간다(나 + AI들. 술래는 제외). 팀전은 같은 편 제외.
                if (stage.ballBounce) {
                    val n = aiN + 1
                    val balls = Array(n) { if (it == 0) physics else aiList[it - 1] }
                    // 각 공의 팀(=칠하는 색 인덱스). index0=나(0), 나머지는 aiColorIdx.
                    val teamOf = IntArray(n) { if (it == 0) 0 else aiColorIdx[it - 1] }
                    for (a in 0 until n) for (b in a + 1 until n) {
                        if (teams && teamOf[a] == teamOf[b]) continue  // 같은 편은 안 튕김
                        if (bounceBalls(balls[a], balls[b])) {
                            // 부딪힌 두 공에 넉백 시간 부여 — 잠깐 조종을 끊어 튕김이 보이게.
                            if (a == 0) playerKnock = KNOCK_S else aiKnock[a - 1] = KNOCK_S
                            if (b == 0) playerKnock = KNOCK_S else aiKnock[b - 1] = KNOCK_S
                        }
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

                // 폭탄: 랜덤 위치에 생겨 도화선이 타들어가다 터진다. 범위 안 공은 정지 + 그 자리 색 지움.
                if (stage.bombs) {
                    bombSpawnCd -= dt
                    if (bombSpawnCd <= 0f && activeBombs.size < BOMB_MAX) {
                        bombSpawnCd = BOMB_SPAWN_INTERVAL
                        val cands = ArrayList<Pair<Int, Int>>()
                        for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
                            if (paintCtrl.isReachable(c, r)) cands.add(c to r)
                        }
                        if (cands.isNotEmpty()) {
                            val (bc, br) = cands[rnd.nextInt(cands.size)]
                            activeBombs.add(Bomb(bc, br, BOMB_FUSE))
                        }
                    }
                    val bit = activeBombs.iterator()
                    while (bit.hasNext()) {
                        val bomb = bit.next()
                        bomb.fuse -= dt
                        if (bomb.fuse <= 0f) {
                            val bx = bomb.c + 0.5f
                            val by = bomb.r + 0.5f
                            // 폭발 범위 안의 공은 정지시킨다.
                            val pdx = physics.x - bx
                            val pdy = physics.y - by
                            if (pdx * pdx + pdy * pdy <= BOMB_BLAST_R * BOMB_BLAST_R) {
                                playerStun = BOMB_STUN_S
                                playerStunned = true
                                physics.stop()
                            }
                            for (i in 0 until aiN) {
                                val adx = aiList[i].x - bx
                                val ady = aiList[i].y - by
                                if (adx * adx + ady * ady <= BOMB_BLAST_R * BOMB_BLAST_R) {
                                    aiStun[i] = BOMB_STUN_S
                                    aiList[i].stop()
                                }
                            }
                            // 폭발 자리 3x3 색 지움.
                            for (dr in -1..1) for (dc in -1..1) {
                                val old = paintCtrl.erase(bomb.c + dc, bomb.r + dr)
                                if (old in counts.indices) counts[old] = counts[old] - 1
                            }
                            blasts.add(Blast(bx, by, 0f))
                            SoundManager.playBonk()
                            bit.remove()
                        }
                    }
                    val eit = blasts.iterator()
                    while (eit.hasNext()) {
                        val ex = eit.next()
                        ex.age += dt
                        if (ex.age > BLAST_LIFE) eit.remove()
                    }
                    bombRender = activeBombs.map {
                        Offset(it.c + 0.5f, it.r + 0.5f) to (it.fuse / BOMB_FUSE).coerceIn(0f, 1f)
                    }
                    blastRender = blasts.map {
                        Offset(it.x, it.y) to (it.age / BLAST_LIFE).coerceIn(0f, 1f)
                    }
                }

                // 점수 = 일반 칸 + 별 칸(2배라 starCounts만큼 가산). 별 구역 없으면 starCounts=0.
                fun scoreOf(i: Int) = counts[i] + starCounts.getOrElse(i) { 0 }

                // 시간제 대결: 진행 중에도 내 최고 점수를 계속 기록 — 중간에 나가도 남게.
                if (timed && scoreOf(0) > savedPeak) {
                    savedPeak = scoreOf(0)
                    if (PaintRecordsRepository(context).recordScore(level, savedPeak)) isNewBest = true
                }

                // 종료: 시간제는 타임업, 아니면 판이 다 찼을 때. 최고 점수 색이 우승.
                val over = if (timed) timeLeftMs <= 0L else paintCtrl.done
                if (over) {
                    val my = scoreOf(0)
                    val best = counts.indices.maxOf { scoreOf(it) }
                    won = my == best && counts.indices.count { scoreOf(it) == best } == 1
                    draw = my == best && !won
                    if (timed) {
                        if (PaintRecordsRepository(context).recordScore(level, my)) isNewBest = true
                    } else if (won) {
                        isNewBest = PaintRecordsRepository(context).record(level, elapsedMs)
                    }
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
                                "${counts.getOrElse(i) { 0 } + starCounts.getOrElse(i) { 0 }}",
                                color = c,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (stage.colorByNumber) "$cnCorrect / $cnTotal" else "$done / ${paintCtrl.total}",
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
                        stage.versus && stage.zones -> "★ 별 구역은 2배! 차지해요"
                        stage.versus && teams -> "우리 팀 색을 더 많이!"
                        stage.versus && stage.chaser -> "술래를 피해 땅을 넓혀요!"
                        stage.versus && stage.dynamicWalls -> "벽을 피해 땅을 넓혀요!"
                        stage.versus && overwrite -> "덮어 칠하며 땅을 넓혀요!"
                        stage.versus -> "많이 칠하면 이겨요!"
                        stage.colorByNumber -> "그림 색에 맞춰 칠해요!"
                        stage.mirror -> "칠하면 대칭으로 퍼져요!"
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
                            List(aiN) { aiPos[it] to stage.palette.getOrElse(aiColorIdx[it]) { Color.Red } }
                        } else emptyList(),
                        chaser = if (chaserOn) chaserPos else null,
                        playerStunned = playerStunned,
                        bombs = bombRender,
                        blasts = blastRender,
                        starCells = starCells,
                        cnTarget = if (stage.colorByNumber) cnTarget else null,
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
                if (stage.chooseColor || stage.colorByNumber) {
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
                stars = PaintGame.starsFor(if (stage.colorByNumber) cnTotal else paintCtrl.total, elapsedMs),
                isNewBest = isNewBest,
                versus = versus,
                won = won,
                draw = draw,
                counts = List(stage.palette.size) { counts.getOrElse(it) { 0 } + starCounts.getOrElse(it) { 0 } },
                colors = stage.palette,
                onSaveArt = if (stage.mirror || stage.colorByNumber) {
                    {
                        val argb = Array(arena.rows) { r ->
                            IntArray(arena.cols) { c ->
                                val idx = paintCtrl.colorAt(c, r)
                                if (idx >= 0) stage.palette[idx.coerceIn(0, stage.palette.lastIndex)].toArgb() else 0
                            }
                        }
                        val bmp = ShareUtils.renderGridArt(arena.cols, arena.rows, argb)
                        val ok = ShareUtils.saveBitmapToGallery(context, bmp)
                        Toast.makeText(
                            context,
                            if (ok) "사진에 저장했어요!" else "저장하지 못했어요",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } else null,
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
