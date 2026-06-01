package com.rts.rys.ryy.wayfinding.ui

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

/** 화면을 가로지르는 별똥별. 좌표는 정규화 0..1. */
private class Shooting(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float,
    val maxLife: Float,
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
    var isNewBest by remember(attemptId) { mutableStateOf(false) }
    var pulse by remember(attemptId) { mutableFloatStateOf(0f) }
    // 화면에 떠 있는 별똥별 목록. 매 프레임 위치 업데이트 + 만료된 건 제거.
    val shootings = remember(attemptId) { mutableStateListOf<Shooting>() }
    // 별똥별 잡힘 토스트 표시용 — 보너스 메시지가 0보다 크면 잔여 시간(초) 동안 표시.
    var bonusToastSec by remember(attemptId) { mutableFloatStateOf(0f) }

    // 타이머 & 펄스 + 별똥별 spawn/이동 — 첫 별을 누른 뒤부터 시간 측정.
    LaunchedEffect(attemptId) {
        var last = 0L
        var gameTime = 0f
        // 첫 별똥별까지는 약간의 워밍업, 그 뒤로 7~12초 간격.
        var nextSpawnAt = 1f
        val rnd = Random(stage.level * 53L + 11L)
        while (!finished) {
            val now = awaitFrame()
            if (last == 0L) { last = now; continue }
            val deltaNs = now - last
            val dt = deltaNs / 1_000_000_000f
            val dtMs = deltaNs / 1_000_000L
            pulse += dt
            if (reached in 1 until stage.stars.size) elapsedMs += dtMs
            if (bonusToastSec > 0f) bonusToastSec = (bonusToastSec - dt).coerceAtLeast(0f)
            last = now

            // 별을 한 번이라도 누른 뒤부터 별똥별이 등장한다.
            if (reached >= 1 && !finished) {
                gameTime += dt
                if (gameTime >= nextSpawnAt) {
                    shootings.add(spawnShooting(rnd))
                    nextSpawnAt = gameTime + 1f + rnd.nextFloat() *2f
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
                BackChip(onClick = onExit, modifier = Modifier.align(Alignment.CenterStart))
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
                Text(
                    text = if (reached == 0)
                        "1번 별부터 손가락으로 이어요!"
                    else
                        "${stage.description} ${stage.revealEmoji}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk,
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
                    ConstellationCanvas(
                        stars = stage.stars,
                        reached = reached,
                        dragEnd = dragEnd,
                        closeOnComplete = stage.closeOnComplete,
                        bgStars = bgStars,
                        shootings = shootings,
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
                                        SoundManager.playGoal()
                                        if (reached == stage.stars.size) {
                                            isNewBest = ConstellationRecordsRepository(context)
                                                .recordKey(recordKey, elapsedMs)
                                            finished = true
                                            dragEnd = null
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

        if (finished) {
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
    reached: Int,
    dragEnd: Offset?,
    closeOnComplete: Boolean,
    bgStars: List<Triple<Float, Float, Float>>,
    shootings: List<Shooting>,
    pulse: Float,
    finished: Boolean,
    revealEmoji: String,
    onGesture: (StarGesture) -> Unit,
) {
    // pointerInput을 reached로 키잉하면 별 하나 닿을 때마다 재시작되어 드래그가 끊긴다.
    // rememberUpdatedState로 최신 값을 우회 참조. 별똥별 목록도 같은 이유로 우회.
    val reachedState = rememberUpdatedState(reached)
    val shootingsState = rememberUpdatedState(shootings)
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
                    val sx = startStar.x * w
                    val sy = startStar.y * h
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
                            val nxx = nxt.x * w
                            val nyy = nxt.y * h
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
            val from = stars[reached - 1]
            val to = stars[reached]
            drawLine(
                color = Color.White.copy(alpha = 0.18f),
                start = Offset(from.x * w, from.y * h),
                end = Offset(to.x * w, to.y * h),
                strokeWidth = minSide * 0.006f,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(minSide * 0.02f, minSide * 0.02f), 0f
                ),
            )
        }

        // 확정된 연결선들 — 외곽 글로우 + 안쪽 굵은 선
        for (i in 1 until reached) {
            drawConstellationLine(
                stars[i - 1].x * w, stars[i - 1].y * h,
                stars[i].x * w, stars[i].y * h,
                minSide,
            )
        }
        // 완성 시 닫힘 선
        if (finished && closeOnComplete && stars.size >= 3) {
            drawConstellationLine(
                stars.last().x * w, stars.last().y * h,
                stars.first().x * w, stars.first().y * h,
                minSide,
            )
        }

        // 진행 중 드래그 라인 (마지막 도달 별 → 손가락)
        if (dragEnd != null && reached in 1 until stars.size) {
            val from = stars[reached - 1]
            drawLine(
                color = LineGold.copy(alpha = 0.55f),
                start = Offset(from.x * w, from.y * h),
                end = dragEnd,
                strokeWidth = minSide * 0.012f,
                cap = StrokeCap.Round,
            )
        }

        // 별 본체
        for (s in stars) {
            val cx = s.x * w
            val cy = s.y * h
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
            }
            drawCircle(
                color = StarYellow.copy(alpha = haloAlpha * 0.4f),
                radius = pulseR * 3.2f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = StarYellow.copy(alpha = haloAlpha * 0.8f),
                radius = pulseR * 1.9f,
                center = Offset(cx, cy),
            )
            // 본체
            val body = if (isReached) StarYellow else Color.White
            val rim = if (isReached) StarYellowDeep else Color(0xFFB6BEFF)
            drawCircle(rim, radius = pulseR * 1.12f, center = Offset(cx, cy))
            drawCircle(body, radius = pulseR, center = Offset(cx, cy))
            // 순서 번호
            numberPaint.textSize = pulseR * 1.1f
            drawContext.canvas.nativeCanvas.drawText(
                s.order.toString(), cx, cy + pulseR * 0.4f, numberPaint,
            )
        }

        // 완성 시 가운데 큰 그림
        if (finished) {
            // 별들의 평균 위치 = 그림의 중심으로.
            val avgX = stars.map { it.x }.average().toFloat() * w
            val avgY = stars.map { it.y }.average().toFloat() * h
            emojiPaint.textSize = minSide * 0.30f
            emojiPaint.alpha = 230
            drawContext.canvas.nativeCanvas.drawText(
                revealEmoji, avgX, avgY + minSide * 0.10f, emojiPaint,
            )
        }
    }
}

/** 별자리 연결선 — 노란 글로우 + 흰 코어. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstellationLine(
    x1: Float, y1: Float, x2: Float, y2: Float, minSide: Float,
) {
    val s = Offset(x1, y1)
    val e = Offset(x2, y2)
    drawLine(
        color = LineGold.copy(alpha = 0.35f),
        start = s, end = e,
        strokeWidth = minSide * 0.024f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = LineGold.copy(alpha = 0.75f),
        start = s, end = e,
        strokeWidth = minSide * 0.014f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color.White,
        start = s, end = e,
        strokeWidth = minSide * 0.006f,
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
