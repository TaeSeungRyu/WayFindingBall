package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.rts.rys.ryy.wayfinding.data.ColorRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.ChaserController
import com.rts.rys.ryy.wayfinding.game.ColorGame
import com.rts.rys.ryy.wayfinding.game.DynamicMazeController
import com.rts.rys.ryy.wayfinding.game.FloorColorController
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.sin

private const val SENSOR_ACCEL_GAIN = 36f
private const val KEYPAD_ACCEL_GAIN = 18f
private const val SENSOR_MAX_SPEED = 22f
private const val KEYPAD_MAX_SPEED = 14f
private const val FLIP_DURATION_S = 0.45f

@Composable
fun ColorGameScreen(
    level: Int,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val stage = remember(level) { ColorGame.stageOf(level) }
    var attemptId by remember(level) { mutableIntStateOf(0) }

    val arena = remember(attemptId) { ColorGame.buildArena(stage) }
    val physics = remember(attemptId) { BallPhysics(arena, radius = 0.32f, friction = 1.8f) }
    val targetSeq = remember(attemptId) {
        if (stage.zones.isEmpty()) emptyList() else ColorGame.targetSequence(stage)
    }
    var zones by remember(attemptId) {
        mutableStateOf(
            when {
                stage.huntMode -> ColorGame.huntAssign(stage)
                stage.shuffleColors -> ColorGame.zonesWithShuffledColors(stage)
                else -> stage.zones
            }
        )
    }
    // 헌트 모드에서 모아야 할 고정 색 (팔레트 중 하나, 항상 12칸에 존재)
    val huntTarget = remember(attemptId) { ColorGame.palette12.random() }
    // 색 바닥 모드 컨트롤러 + 정답 색
    val floorCtrl = remember(attemptId) {
        if (stage.floorMode) {
            FloorColorController(arena, ColorGame.floorPalette.size, ColorGame.floorPalette.indices.random())
        } else null
    }
    val floorTarget = remember(attemptId) { floorCtrl?.let { ColorGame.floorPalette[it.target] } }
    val dynamicWalls = remember(attemptId) {
        if (stage.dynamicWalls) {
            val protect: (Int, Int) -> Boolean = if (stage.floorMode) {
                { c, r -> floorCtrl?.isTargetCell(c, r) == true }
            } else {
                { c, r -> stage.zones.any { it.contains(c, r) } }
            }
            DynamicMazeController(arena, cyclePeriodS = 3.5f, maxChanges = 6, isProtected = protect)
        } else null
    }
    val chaser = remember(attemptId) {
        if (stage.chaser) {
            ChaserController(arena, moveIntervalS = 0.45f, randomSpawnMinDistance = 6)
        } else null
    }
    val tilt = remember { TiltSensor(context) }
    val currentSkin = remember { BallSkins.byId(AchievementsRepository(context).loadCurrentSkinId()) }
    val sensorEnabled by AppSettings.sensorEnabled

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    var ballX by remember(attemptId) { mutableFloatStateOf(physics.x) }
    var ballY by remember(attemptId) { mutableFloatStateOf(physics.y) }
    var targetIndex by remember(attemptId) { mutableIntStateOf(0) }
    var score by remember(attemptId) { mutableIntStateOf(0) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var caught by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var wrongFlash by remember(attemptId) { mutableFloatStateOf(0f) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    var chaserX by remember(attemptId) { mutableFloatStateOf(0f) }
    var chaserY by remember(attemptId) { mutableFloatStateOf(0f) }
    var showMemorize by remember(attemptId) { mutableStateOf(stage.memorizeOrder) }
    var reshuffleTimer by remember(attemptId) { mutableFloatStateOf(0f) }
    // 카드 뒤집기 공개: 시작은 전부 숨김. 3초마다 전체가 뒤집히며 보임↔숨김 전환.
    var floorVisible by remember(attemptId) { mutableStateOf(false) }
    var flipping by remember(attemptId) { mutableStateOf(false) }
    var flipProgress by remember(attemptId) { mutableFloatStateOf(0f) }
    var holdTimer by remember(attemptId) { mutableFloatStateOf(0f) }

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    LaunchedEffect(attemptId) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        targetIndex = 0
        score = 0
        elapsedMs = 0L
        finished = false
        wrongFlash = 0f
        var lastZone: Int? = ColorGame.zoneAt(zones, floor(physics.x).toInt(), floor(physics.y).toInt())
        var lastCell: Pair<Int, Int> = floor(physics.x).toInt() to floor(physics.y).toInt()

        var last = 0L
        while (!finished) {
            val now = awaitFrame()
            if (showMemorize) { last = 0L; continue }  // 기억 단계 동안 정지
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            elapsedMs += (now - last) / 1_000_000L
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

            physics.step(dt, ax, ay)
            dynamicWalls?.tick(dt, physics.x, physics.y)
            chaser?.let { ch ->
                ch.tick(dt, floor(physics.x).toInt(), floor(physics.y).toInt())
                chaserX = ch.visualX
                chaserY = ch.visualY
                val dx = physics.x - ch.visualX
                val dy = physics.y - ch.visualY
                if (dx * dx + dy * dy < 0.55f * 0.55f) {
                    caught = true
                    finished = true
                    SoundManager.playBonk()
                }
            }
            ballX = physics.x
            ballY = physics.y
            wrongFlash = (wrongFlash - dt).coerceAtLeast(0f)

            // 카드 뒤집기 공개/숨김 (10단계)
            if (stage.flipCycleS > 0f) {
                if (flipping) {
                    flipProgress += dt / FLIP_DURATION_S
                    if (flipProgress >= 1f) {
                        flipping = false
                        flipProgress = 0f
                        floorVisible = !floorVisible
                        // 막 공개되면 현재 올라가 있는 칸도 다시 판정 대상이 되게 한다.
                        if (floorVisible) lastCell = -1 to -1
                    }
                } else {
                    holdTimer += dt
                    if (holdTimer >= stage.flipCycleS) {
                        holdTimer = 0f
                        flipping = true
                        flipProgress = 0f
                    }
                }
            }

            // 일정 시간마다 색 재배치 (헌트/색 바닥)
            if (stage.reshuffleEveryS > 0f) {
                reshuffleTimer += dt
                if (reshuffleTimer >= stage.reshuffleEveryS) {
                    if (stage.floorMode) floorCtrl?.reshuffle()
                    else zones = ColorGame.huntAssign(stage)
                    reshuffleTimer = 0f
                }
            }

            val bc = floor(physics.x).toInt()
            val br = floor(physics.y).toInt()

            // 색 바닥 모드: 정답 색 칸을 밟으면 수집
            // 단, 카드가 뒤집힌(숨김) 상태에선 색이 안 보이므로 수집 불가.
            if (stage.floorMode && floorCtrl != null) {
                val canCollect = stage.flipCycleS <= 0f || (floorVisible && !flipping)
                val cell = bc to br
                if (cell != lastCell) {
                    if (canCollect && !arena.isWall(bc, br) && floorCtrl.isTargetCell(bc, br)) {
                        score += 1
                        SoundManager.playGoal()
                        floorCtrl.consume(bc, br)
                        if (score >= stage.targetCount) {
                            isNewBest = ColorRecordsRepository(context).record(level, elapsedMs)
                            finished = true
                        }
                    }
                    lastCell = cell
                }
                continue
            }

            val zone = ColorGame.zoneAt(zones, bc, br)
            if (zone != null && zone != lastZone) {
                if (stage.huntMode) {
                    if (zones[zone].name == huntTarget.second) {
                        score += 1
                        SoundManager.playGoal()
                        zones = ColorGame.huntAssign(stage)  // 모으면 즉시 재배치
                        reshuffleTimer = 0f
                        if (score >= stage.targetCount) {
                            isNewBest = ColorRecordsRepository(context).record(level, elapsedMs)
                            finished = true
                        }
                    } else {
                        SoundManager.playBonk()
                        wrongFlash = 0.4f
                    }
                } else if (zone == targetSeq[targetIndex]) {
                    score += 1
                    SoundManager.playGoal()
                    targetIndex += 1
                    if (targetIndex >= stage.targetCount) {
                        isNewBest = ColorRecordsRepository(context).record(level, elapsedMs)
                        finished = true
                    }
                } else {
                    SoundManager.playBonk()
                    wrongFlash = 0.4f
                }
            }
            lastZone = zone
        }
    }

    val target = if (zones.isNotEmpty() && targetSeq.isNotEmpty())
        zones[targetSeq.getOrElse(targetIndex) { targetSeq.lastOrNull() ?: 0 }]
    else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 헤더: 뒤로 + 진행도
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = onExit, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "${score} / ${stage.targetCount}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = formatElapsed(elapsedMs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkSoft,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 목표 색 안내
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
                if (stage.huntMode) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(huntTarget.first)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "${huntTarget.second}색을 모아요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                } else if (stage.floorMode && floorTarget != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(floorTarget.first)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "${floorTarget.second}색을 찾아요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                } else if (stage.memorizeOrder) {
                    Text(
                        text = "기억한 순서대로 가요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                } else if (target != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(target.color)
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = "${target.name}으로 가요!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkDark,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 광장
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
                    ColorArenaCanvas(
                        arena = arena,
                        zones = zones,
                        ballX = ballX,
                        ballY = ballY,
                        targetZoneIndex = targetSeq.getOrElse(targetIndex) { targetSeq.lastOrNull() ?: -1 },
                        skin = currentSkin,
                        pulse = pulse,
                        wrongFlash = wrongFlash,
                        dynamic = dynamicWalls,
                        dark = stage.dark,
                        chaserX = if (chaser != null) chaserX else null,
                        chaserY = if (chaser != null) chaserY else null,
                        highlightTarget = !stage.memorizeOrder && !stage.huntMode && !stage.floorMode,
                        floor = floorCtrl,
                        flip = stage.flipCycleS > 0f,
                        floorShowColor = when {
                            !flipping -> floorVisible
                            flipProgress < 0.5f -> floorVisible       // 전반: 기존 면
                            else -> !floorVisible                     // 후반: 새 면
                        },
                        floorScaleX = if (!flipping) 1f
                            else if (flipProgress < 0.5f) 1f - flipProgress * 2f
                            else (flipProgress - 0.5f) * 2f,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 조작 패드
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SensorToggleChip(
                    sensorOn = sensorEnabled,
                    onToggle = { AppSettings.setSensorEnabled(!sensorEnabled) }
                )
                Spacer(Modifier.height(8.dp))
                DPad(
                    onInput = { dx, dy -> kx = dx; ky = dy },
                    enabled = !finished && !showMemorize && !sensorEnabled
                )
            }
        }

        if (showMemorize) {
            MemorizeOverlay(
                steps = targetSeq.map { zones[it] },
                onDone = { showMemorize = false },
            )
        }

        if (finished) {
            ColorResultOverlay(
                score = score,
                elapsedMs = elapsedMs,
                isNewBest = isNewBest,
                caught = caught,
                onRetry = { attemptId += 1 },
                onHome = onExit,
            )
        }
    }
}

@Composable
private fun MemorizeOverlay(
    steps: List<com.rts.rys.ryy.wayfinding.game.ColorZone>,
    onDone: () -> Unit,
) {
    var highlight by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        delay(600)
        for (i in steps.indices) {
            highlight = i
            delay(850)
            highlight = -1
            delay(200)
        }
        delay(300)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("순서를 기억하세요!", color = InkDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { i, zone ->
                    val on = i == highlight
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (on) 56.dp else 40.dp)
                                .clip(CircleShape)
                                .background(zone.color.copy(alpha = if (on) 1f else 0.35f)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${i + 1}",
                            color = if (on) InkDark else InkSoft,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "잘 보고 순서대로 굴려요",
                color = InkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ColorArenaCanvas(
    arena: com.rts.rys.ryy.wayfinding.game.Maze,
    zones: List<com.rts.rys.ryy.wayfinding.game.ColorZone>,
    ballX: Float,
    ballY: Float,
    targetZoneIndex: Int,
    skin: com.rts.rys.ryy.wayfinding.data.BallSkin,
    pulse: Float,
    wrongFlash: Float,
    dynamic: DynamicMazeController? = null,
    dark: Boolean = false,
    chaserX: Float? = null,
    chaserY: Float? = null,
    highlightTarget: Boolean = true,
    floor: FloorColorController? = null,
    flip: Boolean = false,
    floorShowColor: Boolean = true,
    floorScaleX: Float = 1f,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF3EFE7))
    ) {
        val n = ColorGame.SIZE
        val cell = size.minDimension / n
        val wallColor = Color(0xFFCBB89B)

        // 색 바닥: 벽이 아닌 모든 내부 셀에 색을 칠한다.
        // flip 모드면 전체가 카드 뒤집기(가로 스케일) 효과로 보임↔숨김 전환.
        if (floor != null) {
            floor.version  // 변경 시 재구성 트리거
            val inset = cell * 0.06f
            val full = cell - inset * 2
            for (r in 1 until arena.rows - 1) for (c in 1 until arena.cols - 1) {
                if (arena.isWall(c, r)) continue
                val idx = floor.colorAt(c, r)
                if (flip) {
                    if (floorShowColor && idx < 0) continue  // 시작점 등 색 없음
                    val faceColor = if (floorShowColor) ColorGame.floorPalette[idx].first
                                    else Color(0xFF2A2A2A)
                    val w = (full * floorScaleX).coerceAtLeast(0f)
                    val cxp = c * cell + cell / 2f
                    drawRoundRect(
                        color = faceColor,
                        topLeft = Offset(cxp - w / 2f, r * cell + inset),
                        size = Size(w, full),
                        cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f),
                    )
                } else {
                    if (idx < 0) continue
                    drawRoundRect(
                        color = ColorGame.floorPalette[idx].first,
                        topLeft = Offset(c * cell + inset, r * cell + inset),
                        size = Size(full, full),
                        cornerRadius = CornerRadius(cell * 0.18f, cell * 0.18f),
                    )
                }
            }
        }

        // 벽 셀(테두리 + 내부 벽 모두). dynamic.version 읽기로 변경 시 재구성 트리거.
        dynamic?.version
        for (r in 0 until arena.rows) {
            for (c in 0 until arena.cols) {
                if (arena.isWall(c, r)) {
                    drawRoundRect(
                        color = wallColor,
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
                    )
                }
            }
        }

        // 곧 생길/사라질 벽 미리보기(페이드)
        if (dynamic != null) {
            val prog = dynamic.previewProgress
            for (p in dynamic.pendingPreview) {
                if (!p.toWall) continue
                drawRoundRect(
                    color = wallColor.copy(alpha = 0.55f * prog),
                    topLeft = Offset(p.c * cell, p.r * cell),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.15f, cell * 0.15f),
                )
            }
        }

        // 색칸
        for ((i, zone) in zones.withIndex()) {
            val left = zone.cMin * cell
            val top = zone.rMin * cell
            val w = (zone.cMax - zone.cMin + 1) * cell
            val h = (zone.rMax - zone.rMin + 1) * cell
            drawRoundRect(
                color = zone.color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f),
            )
            // 목표 색칸은 깜빡이는 테두리로 강조 (기억 모드에선 끔)
            if (i == targetZoneIndex && highlightTarget) {
                val glow = (sin(pulse * 4f) * 0.5f + 0.5f)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.5f + 0.5f * glow),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cell * 0.4f, cell * 0.4f),
                    style = Stroke(width = cell * 0.18f)
                )
            }
        }

        // 어둠: 공 주변만 보이고 나머지는 가려짐 (색칸 위치를 기억해야 함)
        if (dark) {
            val ballPx = Offset(ballX * cell, ballY * cell)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0f),
                        0.45f to Color.Black.copy(alpha = 0f),
                        1f to Color.Black.copy(alpha = 0.94f),
                    ),
                    center = ballPx,
                    radius = cell * 3.2f,
                ),
                size = size,
            )
        }

        // 오답 시 붉은 깜빡임
        if (wrongFlash > 0f) {
            drawRoundRect(
                color = CoralPink.copy(alpha = wrongFlash * 0.4f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(24f, 24f),
            )
        }

        // 술래(적)
        if (chaserX != null && chaserY != null) {
            val er = cell * 0.4f
            val ex = chaserX * cell
            val ey = chaserY * cell
            drawCircle(Color(0xFF6A1B9A), radius = er, center = Offset(ex, ey))
            drawCircle(Color(0xFF4A148C), radius = er, center = Offset(ex, ey), style = Stroke(width = cell * 0.08f))
            // 눈
            val eyeDx = er * 0.32f
            val eyeY = ey - er * 0.08f
            drawCircle(Color.White, radius = er * 0.24f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.White, radius = er * 0.24f, center = Offset(ex + eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.11f, center = Offset(ex - eyeDx, eyeY))
            drawCircle(Color.Black, radius = er * 0.11f, center = Offset(ex + eyeDx, eyeY))
        }

        // 공 (어둠 위에 그려 항상 보이게)
        val r = cell * 0.4f
        val cx = ballX * cell
        val cy = ballY * cell
        drawBallDecoration(skin, cx, cy, r, phaseSec = pulse)
        drawBallBody(skin, cx, cy, r)
    }
}

@Composable
private fun ColorResultOverlay(
    score: Int,
    elapsedMs: Long,
    isNewBest: Boolean,
    caught: Boolean,
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
            Text(if (caught) "😵" else "🎉", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (caught) "잡혔어요!" else "참 잘했어요!",
                color = InkDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (caught) "술래에게 잡혔어요" else "색깔 ${score}개를 모두 찾았어요",
                color = InkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (!caught) {
                Spacer(Modifier.height(16.dp))
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
                ResultButton("나가기", SkyBlue, onHome, Modifier.weight(1f))
                ResultButton("다시 해요", CoralPink, onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultButton(label: String, bg: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
