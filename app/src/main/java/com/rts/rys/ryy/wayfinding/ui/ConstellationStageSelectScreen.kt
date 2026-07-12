package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.random.Random
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.ConstellationRecordsRepository
import com.rts.rys.ryy.wayfinding.data.CustomConstellationRepository
import com.rts.rys.ryy.wayfinding.game.Constellation
import com.rts.rys.ryy.wayfinding.game.ConstellationStage
import com.rts.rys.ryy.wayfinding.game.CustomConstellation
import com.rts.rys.ryy.wayfinding.game.Zodiac
import com.rts.rys.ryy.wayfinding.game.ZodiacEntry
import com.rts.rys.ryy.wayfinding.game.dateRangeText
import com.rts.rys.ryy.wayfinding.game.starsEarnedFor

private val NightTop = Color(0xFF050B25)
private val NightBottom = Color(0xFF1B2A66)
private val NightInk = Color(0xFFE7E9FF)
private val CardIndigo = Color(0xFF3949AB)
private val CardPurple = Color(0xFF6B3FA0)
private val CardDeep = Color(0xFF243170)
private val GoldRing = Color(0xFFFFD66B)

@Composable
fun ConstellationStageSelectScreen(
    onBack: () -> Unit,
    onSelect: (stageKey: String, recordKey: String) -> Unit,
    onCreate: () -> Unit,
    onDex: () -> Unit,
) {
    val context = LocalContext.current
    val birthMonth by AppSettings.birthMonth
    val birthDay by AppSettings.birthDay
    val myZodiac = remember(birthMonth, birthDay) {
        if (birthMonth == 0 || birthDay == 0) null
        else Zodiac.forBirthday(birthMonth, birthDay)
    }

    // 매 recomposition마다 SharedPreferences를 다시 읽어 최신 기록이 반영되도록 한다
    // (게임 클리어 후 pop으로 돌아오면 stage select가 재구성됨).
    val repo = remember { ConstellationRecordsRepository(context) }
    val levelBest = Constellation.stages.associate { it.level to repo.bestFor(it.level) }
    val zodiacBest = Zodiac.entries.associate { it.index to repo.bestForKey("zodiac_${it.index}") }

    // 자녀가 만든 별자리. deleteTick으로 삭제 즉시 목록을 다시 읽는다
    // (별자리 만들기 화면에서 돌아오면 화면 자체가 재구성되어 새 항목이 반영됨).
    val customRepo = remember { CustomConstellationRepository(context) }
    var deleteTick by remember { mutableIntStateOf(0) }
    val customs = remember(deleteTick) { customRepo.all() }
    val customBest = customs.associate { it.id to repo.bestForKey(it.recordKey) }
    var pendingDelete by remember { mutableStateOf<CustomConstellation?>(null) }

    var showBirthdayDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp)
            ) {
                BackChip(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "별자리 잇기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk,
                    modifier = Modifier.align(Alignment.Center)
                )
                DexChip(onClick = onDex, modifier = Modifier.align(Alignment.CenterEnd))
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                // 누적 야경 — 클리어한 별자리들이 모이는 "내가 만든 밤하늘".
                // 매 composition마다 다시 읽어 게임 클리어 직후 즉시 반영되게 한다.
                item { SectionHeader("내가 만든 밤하늘") }
                item {
                    val clearedStages =
                        Constellation.stages.filter { levelBest[it.level] != null } +
                            Zodiac.entries
                                .filter { zodiacBest[it.index] != null }
                                .map { it.stage } +
                            customs
                                .filter { customBest[it.id] != null }
                                .map { it.toStage() }
                    NightSkyPanel(clearedStages = clearedStages)
                }

                // 섹션: 내가 만든 별자리
                item {
                    SectionHeader("내가 만든 별자리")
                }
                item {
                    CreateStarCard(onClick = onCreate)
                }
                items(customs, key = { it.id }) { custom ->
                    CustomStarCard(
                        custom = custom,
                        bestMs = customBest[custom.id],
                        onClick = { onSelect(custom.stageKey, custom.recordKey) },
                        onDelete = { pendingDelete = custom },
                    )
                }

                // 섹션 1: 내 별자리
                item {
                    SectionHeader("내 별자리")
                }
                item {
                    MyZodiacCard(
                        zodiac = myZodiac,
                        birthMonth = birthMonth,
                        birthDay = birthDay,
                        bestMs = myZodiac?.let { zodiacBest[it.index] },
                        onClickInput = { showBirthdayDialog = true },
                        onClickPlay = { z ->
                            onSelect("zodiac_${z.index}", "zodiac_${z.index}")
                        },
                    )
                }
                // 섹션 2: 기본 별자리
                item {
                    SectionHeader("기본 별자리")
                }
                items(Constellation.stages) { stage ->
                    BasicStageCard(
                        stage = stage,
                        bg = if (stage.level % 2 == 1) CardIndigo else CardPurple,
                        bestMs = levelBest[stage.level],
                        onClick = {
                            onSelect("level_${stage.level}", "best_${stage.level}")
                        },
                    )
                }
                // 섹션 3: 황도 12궁
                item {
                    SectionHeader("황도 12궁")
                }
                items(Zodiac.entries) { entry ->
                    ZodiacRowCard(
                        entry = entry,
                        isMine = entry.index == myZodiac?.index,
                        bestMs = zodiacBest[entry.index],
                        onClick = {
                            onSelect("zodiac_${entry.index}", "zodiac_${entry.index}")
                        },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        pendingDelete?.let { target ->
            DeleteConfirmDialog(
                displayName = target.displayName,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    customRepo.delete(target.id)
                    pendingDelete = null
                    deleteTick++
                },
            )
        }

        if (showBirthdayDialog) {
            BirthdayPickerDialog(
                initialMonth = if (birthMonth in 1..12) birthMonth else 1,
                initialDay = if (birthDay in 1..31) birthDay else 1,
                onDismiss = { showBirthdayDialog = false },
                onConfirm = { m, d ->
                    AppSettings.setBirthday(m, d)
                    showBirthdayDialog = false
                },
            )
        }
    }
}

@Composable
private fun NightSkyPanel(clearedStages: List<ConstellationStage>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF050B25), Color(0xFF14215C))))
            .border(1.dp, GoldRing.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
    ) {
        // 잔별(반짝이지 않는 정적 점) — 배경 깊이감.
        val ambient = remember {
            val rr = Random(2025)
            List(80) { Triple(rr.nextFloat(), rr.nextFloat(), 0.3f + rr.nextFloat() * 0.5f) }
        }
        // 별자리 18개 슬롯 — deterministic 배치. 위쪽 6개, 가운데 6개, 아래 6개로 흩뿌림.
        val slots = remember {
            val r = Random(1207)
            List(18) {
                val cx = 0.06f + r.nextFloat() * 0.88f
                val cy = 0.10f + r.nextFloat() * 0.80f
                val sz = 0.13f + r.nextFloat() * 0.06f
                Triple(cx, cy, sz)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minSide = minOf(w, h)

            for ((bx, by, br) in ambient) {
                drawCircle(
                    color = Color.White.copy(alpha = br * 0.45f),
                    radius = minSide * 0.004f,
                    center = Offset(bx * w, by * h),
                )
            }

            // 클리어된 별자리만 슬롯에 그림.
            val stagesToShow = clearedStages.take(slots.size)
            for ((idx, stage) in stagesToShow.withIndex()) {
                val (cx, cy, sz) = slots[idx]
                val boxX = cx * w
                val boxY = cy * h
                val side = sz * w  // 가로 비율 기준
                val starsXs = FloatArray(stage.stars.size) {
                    boxX + (stage.stars[it].x - 0.5f) * side
                }
                val starsYs = FloatArray(stage.stars.size) {
                    boxY + (stage.stars[it].y - 0.5f) * side
                }
                // 연결선
                for (i in 1 until stage.stars.size) {
                    drawLine(
                        color = GoldRing.copy(alpha = 0.70f),
                        start = Offset(starsXs[i - 1], starsYs[i - 1]),
                        end = Offset(starsXs[i], starsYs[i]),
                        strokeWidth = minSide * 0.005f,
                        cap = StrokeCap.Round,
                    )
                }
                if (stage.closeOnComplete && stage.stars.size >= 3) {
                    drawLine(
                        color = GoldRing.copy(alpha = 0.70f),
                        start = Offset(starsXs.last(), starsYs.last()),
                        end = Offset(starsXs.first(), starsYs.first()),
                        strokeWidth = minSide * 0.005f,
                        cap = StrokeCap.Round,
                    )
                }
                // 별 점
                for (i in stage.stars.indices) {
                    drawCircle(
                        color = Color.White,
                        radius = minSide * 0.010f,
                        center = Offset(starsXs[i], starsYs[i]),
                    )
                    drawCircle(
                        color = GoldRing.copy(alpha = 0.4f),
                        radius = minSide * 0.018f,
                        center = Offset(starsXs[i], starsYs[i]),
                    )
                }
            }
        }

        // 우상단 진척도 뱃지.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "✨ ${clearedStages.size} / 18",
                color = GoldRing,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        if (clearedStages.isEmpty()) {
            Text(
                text = "별자리를 완성하면\n여기에 모여요 ✨",
                color = NightInk.copy(alpha = 0.75f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = NightInk.copy(alpha = 0.85f),
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun MyZodiacCard(
    zodiac: ZodiacEntry?,
    birthMonth: Int,
    birthDay: Int,
    bestMs: Long?,
    onClickInput: () -> Unit,
    onClickPlay: (ZodiacEntry) -> Unit,
) {
    if (zodiac == null) {
        // 생일 미입력 — 입력 유도 카드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF4A5DBE), Color(0xFF6B3FA0))))
                .border(2.dp, GoldRing, RoundedCornerShape(24.dp))
                .clickable(onClick = onClickInput)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎂", fontSize = 30.sp)
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        text = "생일을 알려주세요",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "내 별자리가 열려요 ✨",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1F2A6E), Color(0xFF4F2D8E))))
            .border(3.dp, GoldRing, RoundedCornerShape(24.dp))
            .clickable(onClick = { onClickPlay(zodiac) })
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(GoldRing.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(zodiac.stage.revealEmoji, fontSize = 38.sp)
            }
            Spacer(Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = zodiac.korName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = zodiac.symbol,
                        color = GoldRing,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${birthMonth}월 ${birthDay}일 · ${zodiac.dateRangeText()}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StarsBadge(count = zodiac.stage.starsEarnedFor(bestMs), starSize = 16)
                    Text(
                        text = if (bestMs != null) formatElapsed(bestMs) else "아직 기록 없어요",
                        color = GoldRing,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateStarCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF4A5DBE), Color(0xFF6B3FA0))))
            .border(2.dp, GoldRing, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GoldRing.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("➕", fontSize = 24.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    text = "별자리 만들기",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "밤하늘에 나만의 별자리를 그려요 ✨",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CustomStarCard(
    custom: CustomConstellation,
    bestMs: Long?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CardDeep)
            .border(1.dp, GoldRing.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GoldRing.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(custom.emoji, fontSize = 28.sp)
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = custom.displayName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "내가 만든 별자리 · 별 ${custom.stars.size}개",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StarsBadge(count = custom.toStage().starsEarnedFor(bestMs), starSize = 14)
                    Text(
                        text = if (bestMs != null) formatElapsed(bestMs) else "아직 기록 없어요",
                        color = GoldRing,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
                .border(2.dp, GoldRing, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🗑️ 지울까요?",
                    color = NightInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "\"$displayName\"를 지우면\n다시 볼 수 없어요.",
                    color = NightInk.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DialogButton(
                        label = "취소",
                        bg = Color.White.copy(alpha = 0.14f),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    DialogButton(
                        label = "지우기",
                        bg = GoldRing,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicStageCard(
    stage: ConstellationStage,
    bg: Color,
    bestMs: Long?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(stage.revealEmoji, fontSize = 28.sp)
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stage.name,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${stage.description} · 별 ${stage.stars.size}개",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StarsBadge(count = stage.starsEarnedFor(bestMs), starSize = 14)
                    Text(
                        text = if (bestMs != null) formatElapsed(bestMs) else "아직 기록 없어요",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZodiacRowCard(
    entry: ZodiacEntry,
    isMine: Boolean,
    bestMs: Long?,
    onClick: () -> Unit,
) {
    val mod = Modifier
        .fillMaxWidth()
        .height(96.dp)
        .shadow(4.dp, RoundedCornerShape(20.dp))
        .clip(RoundedCornerShape(20.dp))
        .background(if (isMine) CardDeep else Color.White.copy(alpha = 0.08f))
        .let { if (isMine) it.border(2.dp, GoldRing, RoundedCornerShape(20.dp)) else it }
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    Box(modifier = mod) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMine) GoldRing.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(entry.stage.revealEmoji, fontSize = 24.sp)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.korName,
                        color = NightInk,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = entry.symbol,
                        color = if (isMine) GoldRing else NightInk.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = entry.dateRangeText(),
                    color = NightInk.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StarsBadge(
                    count = entry.stage.starsEarnedFor(bestMs),
                    starSize = 13,
                    goldColor = if (isMine) GoldRing else NightInk.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = bestMs?.let { formatElapsed(it) } ?: "─",
                    color = if (isMine) GoldRing else NightInk.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun StarsBadge(
    count: Int,
    starSize: Int = 14,
    goldColor: Color = GoldRing,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(3) { i ->
            val filled = i < count
            Text(
                text = "★",
                color = if (filled) goldColor else Color.White.copy(alpha = 0.22f),
                fontSize = starSize.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun DexChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(GoldRing.copy(alpha = 0.20f))
            .border(1.dp, GoldRing, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("📖", fontSize = 16.sp)
        Spacer(Modifier.size(4.dp))
        Text(
            text = "도감",
            color = NightInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun BirthdayPickerDialog(
    initialMonth: Int,
    initialDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, day: Int) -> Unit,
) {
    var month by remember { mutableIntStateOf(initialMonth) }
    var day by remember { mutableIntStateOf(initialDay) }
    val maxDay = daysInMonth(month)
    // 큰 달에서 작은 달로 옮길 때 자동 조정 (예: 1/31 → 2월로 옮기면 29로).
    if (day > maxDay) day = maxDay

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
                .border(2.dp, GoldRing, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎂 생일을 알려주세요",
                    color = NightInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    NumberStepper("월", month, 1, 12) { month = it }
                    NumberStepper("일", day, 1, maxDay) { day = it }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DialogButton(
                        label = "취소",
                        bg = Color.White.copy(alpha = 0.14f),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    DialogButton(
                        label = "확인",
                        bg = GoldRing,
                        onClick = { onConfirm(month, day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = NightInk.copy(alpha = 0.75f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepperButton("−") { onChange(if (value > min) value - 1 else max) }
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 52.dp),
            )
            StepperButton("+") { onChange(if (value < max) value + 1 else min) }
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun DialogButton(
    label: String,
    bg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (bg == GoldRing) Color(0xFF2A1A00) else Color.White
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun daysInMonth(month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> 29  // 윤년 생일(2/29) 허용.
    else -> 31
}
