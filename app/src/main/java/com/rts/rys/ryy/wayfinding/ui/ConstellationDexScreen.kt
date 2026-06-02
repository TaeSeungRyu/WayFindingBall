package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.data.ConstellationRecordsRepository
import com.rts.rys.ryy.wayfinding.game.Constellation
import com.rts.rys.ryy.wayfinding.game.ConstellationStage
import com.rts.rys.ryy.wayfinding.game.Zodiac
import com.rts.rys.ryy.wayfinding.game.starsEarnedFor

private val NightTop = Color(0xFF050B25)
private val NightBottom = Color(0xFF1B2A66)
private val NightInk = Color(0xFFE7E9FF)
private val GoldRing = Color(0xFFFFD66B)
private val DexCardBg = Color(0xFF1A2356)
private val DexCardLocked = Color(0xFF131A3D)

/** 도감에 표시할 한 항목: 별자리 + 키. */
private data class DexEntry(
    val stage: ConstellationStage,
    val recordKey: String,
    val title: String,
)

@Composable
fun ConstellationDexScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { ConstellationRecordsRepository(context) }
    val entries: List<DexEntry> = remember {
        Constellation.stages.map { s ->
            DexEntry(s, "best_${s.level}", s.description)
        } + Zodiac.entries.map { z ->
            DexEntry(z.stage, "zodiac_${z.index}", z.korName)
        }
    }
    val bests = entries.associate { it.recordKey to repo.bestForKey(it.recordKey) }
    val total = entries.size
    val cleared = bests.values.count { it != null }
    val totalStars = entries.sumOf { it.stage.starsEarnedFor(bests[it.recordKey]) }
    val maxStars = total * 3

    // 탭한 별자리의 신화 모달. null이면 닫힘.
    var openMyth by remember { mutableStateOf<DexEntry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
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
                    text = "별자리 도감",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(12.dp))

            // 진척도 배지
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(1.dp, GoldRing.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "모은 별자리",
                        color = NightInk.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$cleared / $total",
                        color = NightInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "모은 별",
                        color = GoldRing.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "★ $totalStars / $maxStars",
                        color = GoldRing,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(entries, key = { it.recordKey }) { entry ->
                    DexCard(
                        title = entry.title,
                        stage = entry.stage,
                        bestMs = bests[entry.recordKey],
                        onClick = if (bests[entry.recordKey] != null) {
                            { openMyth = entry }
                        } else null,
                    )
                }
            }
        }

        openMyth?.let { entry ->
            MythDialog(
                title = entry.title,
                emoji = entry.stage.revealEmoji,
                myth = entry.stage.myth.ifBlank { entry.stage.lore },
                onDismiss = { openMyth = null },
                onSpeak = { SoundManager.speak(entry.title) },
            )
        }
    }
}

@Composable
private fun MythDialog(
    title: String,
    emoji: String,
    myth: String,
    onDismiss: () -> Unit,
    onSpeak: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF101A50), Color(0xFF2E3F8E))))
                .border(2.dp, GoldRing.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 64.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = NightInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(GoldRing.copy(alpha = 0.22f))
                            .border(1.dp, GoldRing.copy(alpha = 0.6f), RoundedCornerShape(17.dp))
                            .clickable(onClick = onSpeak),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔊", fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = myth,
                    color = NightInk.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldRing)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "닫기",
                        color = Color(0xFF2A1A00),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DexCard(
    title: String,
    stage: ConstellationStage,
    bestMs: Long?,
    onClick: (() -> Unit)? = null,
) {
    val cleared = bestMs != null
    val stars = stage.starsEarnedFor(bestMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (cleared) DexCardBg else DexCardLocked)
            .border(
                width = if (cleared) 1.dp else 0.dp,
                color = if (cleared) GoldRing.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 별자리 미니 미리보기 — 클리어 전엔 점만, 클리어 후엔 선까지.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0A1340), Color(0xFF1E2A66)))),
        ) {
            ConstellationMiniature(stage = stage, revealed = cleared)
            if (cleared) {
                Text(
                    text = stage.revealEmoji,
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            } else {
                Text(
                    text = "?",
                    color = NightInk.copy(alpha = 0.35f),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (cleared) title else "?????",
            color = if (cleared) NightInk else NightInk.copy(alpha = 0.5f),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(3) { i ->
                val filled = i < stars
                Text(
                    text = "★",
                    color = if (filled) GoldRing else NightInk.copy(alpha = 0.20f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (cleared) stage.lore.ifEmpty { " " } else "아직 만나지 못했어요",
            color = NightInk.copy(alpha = if (cleared) 0.75f else 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ConstellationMiniature(stage: ConstellationStage, revealed: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height
        val minSide = minOf(w, h)
        val stars = stage.stars
        if (revealed) {
            // 연결선 — 부드러운 황금색 한 줄
            for (i in 1 until stars.size) {
                val a = stars[i - 1]
                val b = stars[i]
                drawLine(
                    color = GoldRing.copy(alpha = 0.85f),
                    start = Offset(a.x * w, a.y * h),
                    end = Offset(b.x * w, b.y * h),
                    strokeWidth = minSide * 0.020f,
                    cap = StrokeCap.Round,
                )
            }
            if (stage.closeOnComplete && stars.size >= 3) {
                drawLine(
                    color = GoldRing.copy(alpha = 0.85f),
                    start = Offset(stars.last().x * w, stars.last().y * h),
                    end = Offset(stars.first().x * w, stars.first().y * h),
                    strokeWidth = minSide * 0.020f,
                    cap = StrokeCap.Round,
                )
            }
        }
        // 점만 표시 (잠금 상태도 점은 흐릿하게 보임 — 모양 짐작 가능하게)
        val dotColor = if (revealed) Color.White else NightInk.copy(alpha = 0.35f)
        val r = minSide * (if (revealed) 0.035f else 0.025f)
        for (s in stars) {
            drawCircle(dotColor, radius = r, center = Offset(s.x * w, s.y * h))
        }
    }
}
