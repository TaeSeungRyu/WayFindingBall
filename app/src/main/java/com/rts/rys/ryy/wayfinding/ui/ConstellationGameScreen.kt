package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.ConstellationRecordsRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.ConstellationStage
import com.rts.rys.ryy.wayfinding.game.ConstellationStar
import com.rts.rys.ryy.wayfinding.game.starsEarned
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val NightTop = Color(0xFF050B25)
private val NightBottom = Color(0xFF1B2A66)
private val NightInk = Color(0xFFE7E9FF)
private val StarYellow = Color(0xFFFFE38A)
private val StarYellowDeep = Color(0xFFFFB347)
private val LineGold = Color(0xFFFFD66B)

/** 별 명중 반경(캔버스 minDim 비율). 손가락 굵기 고려해 넉넉히. */
private const val STAR_HIT_R = 0.08f
/** 별똥별 탭 명중 반경. 빠르게 지나가는 점이라 별보다 더 넉넉히. */
private const val SHOOTING_HIT_R = 0.12f
/** 별똥별을 잡았을 때 깎이는 시간(ms). */
private const val SHOOTING_BONUS_MS = 2000L
/** 별이 정규 좌표 위에서 떠다니는 원형 궤도의 반지름(=화면 비율). 자녀가 따라잡을 수 있게 작게. */
private const val DRIFT_AMP = 0.012f
/** 콤보 임계. 빠른 hit 이만큼 연속하면 보너스. */
private const val COMBO_THRESHOLD = 3
/** 콤보 보너스로 깎이는 시간(ms). */
private const val COMBO_BONUS_MS = 1000L
/** 다음 hit이 이 시간(ms) 이내여야 콤보 누적. */
private const val COMBO_WINDOW_MS = 1500L
/** 완성 후 결과 오버레이가 뜨기까지의 연출 시간(초). */
private const val COMPLETION_DURATION = 1.0f

/** 별의 단계·순서 기반 결정적 위상. 같은 단계 같은 별은 항상 같은 위상. */
private fun driftPhase(stageLevel: Int, order: Int): Float {
    val h = (stageLevel * 73856093) xor (order * 19349663)
    return ((h and 0xFFFF) / 65535f) * 6.2831855f
}

/** 별마다 약간 다른 주기 — 별자리가 살아있게 보이도록 동기화 회피. */
private fun driftFreq(stageLevel: Int, order: Int): Float {
    val h = (stageLevel * 19349663) xor (order * 83492791)
    return 0.45f + (((h ushr 16) and 0xFF) / 255f) * 0.5f
}

/** 정규 좌표 위의 drift된 별 위치 — 같은 (stage, order, time)이면 같은 결과. */
private fun driftedX(star: ConstellationStar, stageLevel: Int, time: Float): Float =
    star.x + cos(time * driftFreq(stageLevel, star.order) + driftPhase(stageLevel, star.order)) * DRIFT_AMP

private fun driftedY(star: ConstellationStar, stageLevel: Int, time: Float): Float =
    star.y + sin(time * driftFreq(stageLevel, star.order) + driftPhase(stageLevel, star.order) * 1.37f) * DRIFT_AMP

/** 화면을 가로지르는 별똥별. 좌표는 정규화 0..1. */
private class Shooting(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float,
    val maxLife: Float,
)

/** 완성 직후 사방으로 분출되는 작은 별가루. vy는 중력으로 매 프레임 변함. */
private class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val hue: Float,  // 0=흰, 1=황금
)

/** 좌상단/우상단 근처에서 들어와 반대편 하단으로 흐르는 별똥별 한 개. */
private fun spawnShooting(r: Random): Shooting {
    val fromLeft = r.nextBoolean()
    val sx = if (fromLeft) -0.05f - r.nextFloat() * 0.15f else 1.05f + r.nextFloat() * 0.15f
    val sy = -0.05f - r.nextFloat() * 0.15f
    val tx = if (fromLeft) 0.55f + r.nextFloat() * 0.40f else 0.05f + r.nextFloat() * 0.40f
    val ty = 0.70f + r.nextFloat() * 0.35f
    val duration = 1.6f
    return Shooting(
        x = sx, y = sy,
        vx = (tx - sx) / duration,
        vy = (ty - sy) / duration,
        life = duration, maxLife = duration,
    )
}

@Composable
fun ConstellationGameScreen(
    stage: ConstellationStage,
    recordKey: String,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    var attemptId by remember(stage.level) { mutableIntStateOf(0) }

    // 배경 잔별: 단계 시드로 결정적 생성 (재시도해도 동일 패턴).
    val bgStars = remember(stage.level) {
        val r = Random(stage.level * 31 + 7)
        List(60) {
            Triple(r.nextFloat(), r.nextFloat(), r.nextFloat() * 6.28f)
        }
    }

    var reached by remember(attemptId) { mutableIntStateOf(0) }
    var dragEnd by remember(attemptId) { mutableStateOf<Offset?>(null) }
    var elapsedMs by remember(attemptId) { mutableLongStateOf(0L) }
    var finished by remember(attemptId) { mutableStateOf(false) }
    var showResult by remember(attemptId) { mutableStateOf(false) }
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    var driftTime by remember(attemptId) { mutableFloatStateOf(0f) }
    var completionSec by remember(attemptId) { mutableFloatStateOf(0f) }
    // 화면에 떠 있는 별똥별 목록. 매 프레임 위치 업데이트 + 만료된 건 제거.
    val shootings = remember(attemptId) { mutableStateListOf<Shooting>() }
    // 완성 직후 분출되는 별가루.
    val particles = remember(attemptId) { mutableStateListOf<Particle>() }
    // 별똥별 잡힘 토스트 — 잔여 시간(초).
    var bonusToastSec by remember(attemptId) { mutableFloatStateOf(0f) }
    // 콤보 보너스 토스트.
    var comboToastSec by remember(attemptId) { mutableFloatStateOf(0f) }
    // 콤보 보너스 발동 시 0.6초간 모든 별을 황금빛으로 깜빡이게 함.
    var comboFlashSec by remember(attemptId) { mutableFloatStateOf(0f) }
    // 콤보 상태 — 마지막 hit ms, 누적 카운트.
    var lastHitMs by remember(attemptId) { mutableLongStateOf(-1L) }
    var comboCount by remember(attemptId) { mutableIntStateOf(0) }
    var paused by remember(stage.level) { mutableStateOf(false) }

    BackHandler(enabled = !paused && !showResult) { paused = true }

    // 타이머 + 펄스 + drift + 별똥별 + 완성 연출 — 첫 별을 누른 뒤부터 시간 측정.
    LaunchedEffect(attemptId) {
        var last = 0L
        var gameTime = 0f
        var nextSpawnAt = 1f
        val rnd = Random(stage.level * 53L + 11L)
        while (!showResult) {
            val now = awaitFrame()
            if (paused) { last = 0L; continue }
            if (last == 0L) { last = now; continue }
            val deltaNs = now - last
            val dt = deltaNs / 1_000_000_000f
            val dtMs = deltaNs / 1_000_000L
            pulse += dt
            driftTime += dt
            if (reached in 1 until stage.stars.size) elapsedMs += dtMs
            if (bonusToastSec > 0f) bonusToastSec = (bonusToastSec - dt).coerceAtLeast(0f)
            if (comboToastSec > 0f) comboToastSec = (comboToastSec - dt).coerceAtLeast(0f)
            if (comboFlashSec > 0f) comboFlashSec = (comboFlashSec - dt).coerceAtLeast(0f)
            last = now

            if (finished) {
                // 완성 연출: 시간 진행 + 파티클 업데이트. 결과 오버레이는 1초 뒤에.
                completionSec += dt
                val pit = particles.iterator()
                while (pit.hasNext()) {
                    val p = pit.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.vy += 0.25f * dt
                    p.life -= dt
                    if (p.life <= 0f) pit.remove()
                }
                if (completionSec >= COMPLETION_DURATION) showResult = true
                continue
            }

            if (reached >= 1) {
                gameTime += dt
                if (gameTime >= nextSpawnAt) {
                    shootings.add(spawnShooting(rnd))
                    nextSpawnAt = gameTime + 1f + rnd.nextFloat() * 2f
                }
                val it = shootings.iterator()
                while (it.hasNext()) {
                    val s = it.next()
                    s.x += s.vx * dt
                    s.y += s.vy * dt
                    s.life -= dt
                    if (s.life <= 0f || s.x < -0.2f || s.x > 1.2f || s.y > 1.2f) {
                        it.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = { paused = true }, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "${reached} / ${stage.stars.size}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk,
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    text = formatElapsed(elapsedMs),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (reached == 0)
                            "1번 별부터 손가락으로 이어요!"
                        else
                            "${stage.description} ${stage.revealEmoji}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NightInk,
                    )
                    if (comboCount > 0) {
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = "⚡×$comboCount",
                            color = StarYellow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
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
                    ConstellationCanvas(
                        stars = stage.stars,
                        stageLevel = stage.level,
                        reached = reached,
                        dragEnd = dragEnd,
                        closeOnComplete = stage.closeOnComplete,
                        bgStars = bgStars,
                        shootings = shootings,
                        particles = particles,
                        driftTime = driftTime,
                        completionSec = completionSec,
                        comboFlashSec = comboFlashSec,
                        pulse = pulse,
                        finished = finished,
                        revealEmoji = stage.revealEmoji,
                        onGesture = { gesture ->
                            when (gesture) {
                                is StarGesture.Start -> {
                                    if (reached == 0) reached = 1
                                    dragEnd = gesture.pos
                                }
                                is StarGesture.Move -> {
                                    dragEnd = gesture.pos
                                    if (gesture.hitNext && reached < stage.stars.size) {
                                        reached += 1
                                        // 콤보: 마지막 hit 이후 COMBO_WINDOW_MS 안이면 누적.
                                        comboCount = if (
                                            lastHitMs >= 0L &&
                                            (elapsedMs - lastHitMs) <= COMBO_WINDOW_MS
                                        ) comboCount + 1 else 1
                                        lastHitMs = elapsedMs
                                        if (comboCount >= COMBO_THRESHOLD) {
                                            elapsedMs = max(0L, elapsedMs - COMBO_BONUS_MS)
                                            comboFlashSec = 0.6f
                                            comboToastSec = 1.4f
                                            comboCount = 0
                                            SoundManager.playGoal()
                                        }
                                        // 별마다 다른 음정으로. order-1 = 방금 이은 별의 인덱스.
                                        SoundManager.playStarTone(reached - 1)
                                        if (reached == stage.stars.size) {
                                            isNewBest = ConstellationRecordsRepository(context)
                                                .recordKey(recordKey, elapsedMs)
                                            finished = true
                                            dragEnd = null
                                            SoundManager.speak(stage.description)
                                            // 완성 폭죽 — 별마다 6개씩 사방으로.
                                            val pr = Random(stage.level * 17L + 91L)
                                            for (s in stage.stars) {
                                                repeat(6) {
                                                    val ang = pr.nextFloat() * 6.2832f
                                                    val speed = 0.18f + pr.nextFloat() * 0.25f
                                                    val maxL = 0.7f + pr.nextFloat() * 0.6f
                                                    particles.add(
                                                        Particle(
                                                            x = driftedX(s, stage.level, driftTime),
                                                            y = driftedY(s, stage.level, driftTime),
                                                            vx = cos(ang) * speed,
                                                            vy = sin(ang) * speed,
                                                            life = maxL, maxLife = maxL,
                                                            hue = if (pr.nextFloat() < 0.6f) 1f else 0f,
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                StarGesture.End -> { dragEnd = null }
                                is StarGesture.ShootingTap -> {
                                    if (gesture.index in shootings.indices) {
                                        shootings.removeAt(gesture.index)
                                        elapsedMs = max(0L, elapsedMs - SHOOTING_BONUS_MS)
                                        bonusToastSec = 1.4f
                                        SoundManager.playGoal()
                                    }
                                }
                            }
                        }
                    )

                    if (bonusToastSec > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(StarYellow.copy(alpha = 0.92f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "별똥별! -2초 ⏱",
                                color = Color(0xFF3A2A00),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                    if (comboToastSec > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFB347).copy(alpha = 0.95f))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "콤보! ⚡ -1초",
                                color = Color(0xFF3A2A00),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (reached == 0)
                    "✨ 시작 별을 눌러요"
                else
                    "✨ ${stage.stars.size - reached}개 남았어요",
                color = NightInk.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
        }

        if (showResult) {
            ConstellationResultOverlay(
                emoji = stage.revealEmoji,
                title = stage.description,
                elapsedMs = elapsedMs,
                starsEarned = stage.starsEarned(elapsedMs),
                isNewBest = isNewBest,
                onRetry = { attemptId += 1 },
                onHome = onExit,
            )
        }

        if (paused && !showResult) {
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
            )
        }
    }
}

private sealed class StarGesture {
    data class Start(val pos: Offset) : StarGesture()
    data class Move(val pos: Offset, val hitNext: Boolean) : StarGesture()
    data object End : StarGesture()
    /** 별똥별이 탭됐다 — 인덱스로 [shootings]에서 식별. */
    data class ShootingTap(val index: Int) : StarGesture()
}

@Composable
private fun ConstellationCanvas(
    stars: List<ConstellationStar>,
    stageLevel: Int,
    reached: Int,
    dragEnd: Offset?,
    closeOnComplete: Boolean,
    bgStars: List<Triple<Float, Float, Float>>,
    shootings: List<Shooting>,
    particles: List<Particle>,
    driftTime: Float,
    completionSec: Float,
    comboFlashSec: Float,
    pulse: Float,
    finished: Boolean,
    revealEmoji: String,
    onGesture: (StarGesture) -> Unit,
) {
    // pointerInput을 reached로 키잉하면 별 하나 닿을 때마다 재시작되어 드래그가 끊긴다.
    // rememberUpdatedState로 최신 값을 우회 참조. 별똥별/drift도 같은 이유로 우회.
    val reachedState = rememberUpdatedState(reached)
    val shootingsState = rememberUpdatedState(shootings)
    val driftTimeState = rememberUpdatedState(driftTime)
    val numberPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            color = Color(0xFF1B2A66).toArgb()
        }
    }
    val emojiPaint = remember {
        android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1340), Color(0xFF26367A))))
            .pointerInput(stars) {
                awaitEachGesture {
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val minSide = minOf(w, h)
                    val touchR = minSide * STAR_HIT_R
                    val shotR = minSide * SHOOTING_HIT_R
                    val down = awaitFirstDown()
                    val currentReached = reachedState.value

                    // 별똥별 탭 우선 처리 — 다른 손으로 별 잇는 중에도 잡을 수 있게.
                    // (한 손가락 게임이라 별 잇기 직전의 down도 별똥별로 친다.)
                    val activeShots = shootingsState.value
                    var hitIdx = -1
                    var bestD2 = Float.MAX_VALUE
                    for ((idx, sh) in activeShots.withIndex()) {
                        val dx = down.position.x - sh.x * w
                        val dy = down.position.y - sh.y * h
                        val d2 = dx * dx + dy * dy
                        if (d2 <= shotR * shotR && d2 < bestD2) {
                            bestD2 = d2
                            hitIdx = idx
                        }
                    }
                    if (hitIdx >= 0) {
                        down.consume()
                        onGesture(StarGesture.ShootingTap(hitIdx))
                        return@awaitEachGesture
                    }

                    // 모두 이은 뒤엔 입력 무시 (결과 오버레이가 떠 있다).
                    if (currentReached >= stars.size) return@awaitEachGesture
                    // 시작 별: 아직 첫 별을 안 눌렀으면 1번, 그 외엔 마지막으로 도달한 별.
                    val startOrder = if (currentReached == 0) 1 else currentReached
                    val startStar = stars[startOrder - 1]
                    val tStart = driftTimeState.value
                    val sx = driftedX(startStar, stageLevel, tStart) * w
                    val sy = driftedY(startStar, stageLevel, tStart) * h
                    val dx0 = down.position.x - sx
                    val dy0 = down.position.y - sy
                    if (dx0 * dx0 + dy0 * dy0 > touchR * touchR) return@awaitEachGesture
                    down.consume()
                    onGesture(StarGesture.Start(down.position))
                    var localReached = if (currentReached == 0) 1 else currentReached
                    drag(down.id) { change ->
                        val pos = change.position
                        var hit = false
                        if (localReached < stars.size) {
                            val nxt = stars[localReached]  // 다음 별 (index = localReached)
                            val tNow = driftTimeState.value
                            val nxx = driftedX(nxt, stageLevel, tNow) * w
                            val nyy = driftedY(nxt, stageLevel, tNow) * h
                            val dxn = pos.x - nxx
                            val dyn = pos.y - nyy
                            if (dxn * dxn + dyn * dyn <= touchR * touchR) {
                                localReached += 1
                                hit = true
                            }
                        }
                        onGesture(StarGesture.Move(pos, hit))
                        change.consume()
                    }
                    onGesture(StarGesture.End)
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val minSide = minOf(w, h)

        // 별 위치를 drift 적용해 미리 계산. hit과 동일한 함수라 시각/판정 일치.
        val sxs = FloatArray(stars.size) { driftedX(stars[it], stageLevel, driftTime) * w }
        val sys = FloatArray(stars.size) { driftedY(stars[it], stageLevel, driftTime) * h }

        // 별똥별 — 머리(밝은 점) + 꼬리(머리 반대 방향으로 점점 옅어지는 선).
        // bgStars보다 살짝 앞쪽이지만 별/연결선보다는 뒤에 그려 시야를 안 가린다.
        for (sh in shootings) {
            val hx = sh.x * w
            val hy = sh.y * h
            // 꼬리 길이는 1프레임 이동량 × 12 정도, 다만 최대치를 제한.
            val tailLen = minSide * 0.18f
            val ang = atan2(sh.vy, sh.vx)
            val tx = hx - cos(ang) * tailLen
            val ty = hy - sin(ang) * tailLen
            val alpha = (sh.life / sh.maxLife).coerceIn(0f, 1f)
            // 꼬리 (점점 굵어지는 글로우 두 줄)
            drawLine(
                color = StarYellow.copy(alpha = 0.25f * alpha),
                start = Offset(tx, ty), end = Offset(hx, hy),
                strokeWidth = minSide * 0.018f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f * alpha),
                start = Offset(tx, ty), end = Offset(hx, hy),
                strokeWidth = minSide * 0.006f,
                cap = StrokeCap.Round,
            )
            // 머리 (별 모양 동심원)
            val hr = minSide * 0.028f
            drawCircle(StarYellow.copy(alpha = 0.55f * alpha), radius = hr * 2.4f, center = Offset(hx, hy))
            drawCircle(StarYellow.copy(alpha = 0.85f * alpha), radius = hr * 1.4f, center = Offset(hx, hy))
            drawCircle(Color.White.copy(alpha = alpha), radius = hr, center = Offset(hx, hy))
        }

        // 배경 잔별 (반짝임)
        for ((bx, by, phase) in bgStars) {
            val tw = 0.35f + 0.65f * (sin(pulse * 2.2f + phase) * 0.5f + 0.5f)
            drawCircle(
                color = Color.White.copy(alpha = 0.7f * tw),
                radius = minSide * 0.005f,
                center = Offset(bx * w, by * h),
            )
        }

        // 가이드 선 (다음 별까지 연한 점선) — 가장 어둡게.
        if (!finished && reached in 1 until stars.size) {
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(sxs[reached - 1], sys[reached - 1]),
                end = Offset(sxs[reached], sys[reached]),
                strokeWidth = minSide * 0.006f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(minSide * 0.02f, minSide * 0.02f), 0f
                ),
            )
        }

        // 확정된 연결선들 — 외곽 글로우 + 안쪽 굵은 선. 완성 후엔 두께 살짝 ↑.
        val lineBoost = if (finished) 1f + (completionSec / COMPLETION_DURATION).coerceIn(0f, 1f) * 0.4f else 1f
        for (i in 1 until reached) {
            drawConstellationLine(
                sxs[i - 1], sys[i - 1],
                sxs[i], sys[i],
                minSide, boost = lineBoost,
            )
        }
        // 완성 시 닫힘 선
        if (finished && closeOnComplete && stars.size >= 3) {
            drawConstellationLine(
                sxs.last(), sys.last(),
                sxs.first(), sys.first(),
                minSide, boost = lineBoost,
            )
        }

        // 진행 중 드래그 라인 (마지막 도달 별 → 손가락)
        if (dragEnd != null && reached in 1 until stars.size) {
            drawLine(
                color = LineGold.copy(alpha = 0.55f),
                start = Offset(sxs[reached - 1], sys[reached - 1]),
                end = dragEnd,
                strokeWidth = minSide * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        // 콤보 발동 깜빡임 — 0.6초 동안 모든 별이 황금으로 펄스.
        val comboBoost = (comboFlashSec / 0.6f).coerceIn(0f, 1f)

        // 별 본체
        for (i in stars.indices) {
            val s = stars[i]
            val cx = sxs[i]
            val cy = sys[i]
            val isReached = s.order <= reached
            val isNext = (reached == 0 && s.order == 1) ||
                (reached in 1 until stars.size && s.order == reached + 1)
            val baseR = minSide * 0.032f
            val pulseR = if (isNext) baseR * (1.05f + 0.18f * sin(pulse * 4.5f)) else baseR

            // 글로우 헤일로
            val haloAlpha = when {
                isNext -> 0.55f + 0.25f * sin(pulse * 4.5f)
                isReached -> 0.45f
                else -> 0.20f
            } + comboBoost * 0.35f
            drawCircle(
                color = StarYellow.copy(alpha = (haloAlpha * 0.4f).coerceAtMost(1f)),
                radius = pulseR * (3.2f + comboBoost * 0.8f),
                center = Offset(cx, cy),
            )
            drawCircle(
                color = StarYellow.copy(alpha = (haloAlpha * 0.8f).coerceAtMost(1f)),
                radius = pulseR * 1.9f,
                center = Offset(cx, cy),
            )
            // 본체 — 콤보 깜빡임이면 모든 별이 황금으로 보이도록 lerp.
            val body = if (isReached || comboBoost > 0f) StarYellow else Color.White
            val rim = if (isReached || comboBoost > 0f) StarYellowDeep else Color(0xFFB6BEFF)
            drawCircle(rim, radius = pulseR * 1.12f, center = Offset(cx, cy))
            drawCircle(body, radius = pulseR, center = Offset(cx, cy))
            // 순서 번호
            numberPaint.textSize = pulseR * 1.1f
            drawContext.canvas.nativeCanvas.drawText(
                s.order.toString(), cx, cy + pulseR * 0.4f, numberPaint,
            )
        }

        // 완성 폭발 — 별자리 중심에서 큰 원이 부드럽게 확장하며 페이드.
        if (finished) {
            val avgX = (0 until stars.size).map { sxs[it] }.average().toFloat()
            val avgY = (0 until stars.size).map { sys[it] }.average().toFloat()
            val burstProg = (completionSec / 0.5f).coerceIn(0f, 1f)
            if (burstProg < 1f) {
                val burstR = minSide * (0.05f + burstProg * 0.55f)
                drawCircle(
                    color = StarYellow.copy(alpha = (0.55f * (1f - burstProg)).coerceAtLeast(0f)),
                    radius = burstR,
                    center = Offset(avgX, avgY),
                )
                drawCircle(
                    color = Color.White.copy(alpha = (0.35f * (1f - burstProg)).coerceAtLeast(0f)),
                    radius = burstR * 0.65f,
                    center = Offset(avgX, avgY),
                )
            }

            // 파티클 — 별가루 사방으로.
            for (p in particles) {
                val pa = (p.life / p.maxLife).coerceIn(0f, 1f)
                val col = if (p.hue > 0.5f) StarYellow else Color.White
                drawCircle(
                    color = col.copy(alpha = 0.9f * pa),
                    radius = minSide * 0.008f * (0.6f + pa * 0.6f),
                    center = Offset(p.x * w, p.y * h),
                )
            }

            // 가운데 큰 그림 — 폭발이 잦아든 뒤 등장 + 살짝 떠오름.
            val emojiProg = ((completionSec - 0.3f) / (COMPLETION_DURATION - 0.3f)).coerceIn(0f, 1f)
            if (emojiProg > 0f) {
                emojiPaint.textSize = minSide * 0.30f
                emojiPaint.alpha = (255 * emojiProg).toInt().coerceIn(0, 255)
                drawContext.canvas.nativeCanvas.drawText(
                    revealEmoji,
                    avgX,
                    avgY + minSide * 0.10f - emojiProg * minSide * 0.04f,
                    emojiPaint,
                )
            }
        }
    }
}

/** 별자리 연결선 — 노란 글로우 + 흰 코어. boost는 완성 시 라인 강도 증폭(>1f). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstellationLine(
    x1: Float, y1: Float, x2: Float, y2: Float, minSide: Float, boost: Float = 1f,
) {
    val s = Offset(x1, y1)
    val e = Offset(x2, y2)
    drawLine(
        color = LineGold.copy(alpha = (0.35f * boost).coerceAtMost(1f)),
        start = s, end = e,
        strokeWidth = minSide * 0.024f * boost,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = LineGold.copy(alpha = (0.75f * boost).coerceAtMost(1f)),
        start = s, end = e,
        strokeWidth = minSide * 0.014f * boost,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White,
        start = s, end = e,
        strokeWidth = minSide * 0.006f * boost,
        cap = StrokeCap.Round,
    )
}

@Composable
private fun ConstellationResultOverlay(
    emoji: String,
    title: String,
    elapsedMs: Long,
    starsEarned: Int,
    isNewBest: Boolean,
    onRetry: () -> Unit,
    onHome: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .shadow(10.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1B2A66), Color(0xFF2E3F8E))))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$title 완성!",
                color = NightInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            // 별 등급 ★★★ — 획득한 별만 채워서, 나머지는 흐리게.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val filled = i < starsEarned
                    Text(
                        text = "★",
                        color = if (filled) StarYellow else NightInk.copy(alpha = 0.22f),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = formatElapsed(elapsedMs),
                color = StarYellow,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
            )
            if (isNewBest) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "✨ 최고 기록! ✨",
                    color = StarYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultButton("나가기", Color(0xFF4A5DBE), onHome, Modifier.weight(1f))
                ResultButton("다시 해요", Color(0xFFFFB347), onRetry, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultButton(
    label: String,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}
