package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.ColorRecordsRepository
import com.rts.rys.ryy.wayfinding.data.GameRecord
import com.rts.rys.ryy.wayfinding.data.HitRecordsRepository
import com.rts.rys.ryy.wayfinding.data.PaintRecordsRepository
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.data.ShareUtils
import com.rts.rys.ryy.wayfinding.game.ColorGame
import com.rts.rys.ryy.wayfinding.game.ColorStage
import com.rts.rys.ryy.wayfinding.game.HitGame
import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.PaintGame
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.difficultyLabel
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LevelBest(
    val level: Int,
    val isInfinite: Boolean,
    val best: GameRecord?,  // null = 기록 없음. 일반(1~13)은 최근 기록, 무한(14~20)은 best cleared
    val starsEarned: Int = 0,
    val starsTotal: Int = 0
)

@Composable
fun RecordsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var records by remember { mutableStateOf<List<GameRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        records = RecordsRepository(context).load()
    }
    val levelBests = remember(records) {
        val byLevel: Map<Int, List<GameRecord>> = records.mapNotNull { r ->
            val lv = runCatching { Stages.byId(r.stageId).level }.getOrNull() ?: return@mapNotNull null
            lv to r
        }.groupBy({ it.first }, { it.second })

        (1..20).map { lv ->
            val isInf = lv in 14..20
            val list = byLevel[lv].orEmpty()
            if (isInf) {
                LevelBest(lv, true, list.maxByOrNull { it.cleared })
            } else {
                val stages = Stages.byLevel(lv)
                val recordsByStage = list.groupBy { it.stageId }
                val earned = stages.sumOf { stage ->
                    val sb = recordsByStage[stage.id]?.minByOrNull { it.elapsedMs }
                    if (sb != null) MazePar.starsFor(stage, sb.elapsedMs) else 0
                }
                val total = stages.size * 3
                val lastRec = list.maxByOrNull { it.timestamp }
                LevelBest(lv, false, lastRec, earned, total)
            }
        }
    }
    val colorBests = remember {
        val repo = ColorRecordsRepository(context)
        ColorGame.stages.map { it to repo.bestFor(it.level) }
    }
    val hitBests = remember {
        val repo = HitRecordsRepository(context)
        HitGame.stages.map { it.level to (it.name to repo.bestFor(it.level)) }
    }
    val paintBests = remember {
        val repo = PaintRecordsRepository(context)
        PaintGame.stages.map { stage ->
            PaintBest(
                level = stage.level,
                name = stage.name,
                timed = stage.countdownS > 0f,
                bestMs = repo.bestFor(stage.level),
                bestScore = repo.bestScoreFor(stage.level),
            )
        }
    }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "내 기록",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + navBottom)
            ) {
                item(key = "h_maze") { SectionLabel("미로 찾기") }
                items(levelBests, key = { "maze_${it.level}" }) { entry ->
                    LevelBestCard(
                        entry = entry,
                        onShare = { shareEntry(context, entry) }
                    )
                }
                item(key = "h_color") {
                    Spacer(Modifier.height(6.dp))
                    SectionLabel("색깔 찾기")
                }
                items(colorBests, key = { "color_${it.first.level}" }) { (stage, bestMs) ->
                    ColorBestCard(stage = stage, bestMs = bestMs)
                }
                item(key = "h_hit") {
                    Spacer(Modifier.height(6.dp))
                    SectionLabel("굴려서 맞히기")
                }
                items(hitBests, key = { "hit_${it.first}" }) { (level, pair) ->
                    val (name, bestMs) = pair
                    TimeBestCard(level = level, title = name, bestMs = bestMs, baseColor = WallGreen, emoji = "🎯")
                }
                item(key = "h_paint") {
                    Spacer(Modifier.height(6.dp))
                    SectionLabel("바닥 색칠하기")
                }
                items(paintBests, key = { "paint_${it.level}" }) { pb ->
                    val valueText = if (pb.timed) pb.bestScore?.let { "$it 칸" }
                                    else pb.bestMs?.let { formatElapsed(it) }
                    PaintBestCard(
                        title = pb.name,
                        valueText = valueText,
                        baseColor = Color(0xFF26A69A),
                    )
                }
            }
        }
    }
}

private fun shareEntry(context: android.content.Context, entry: LevelBest) {
    val rec = entry.best ?: return
    val bgColor = levelColor(entry.level).toArgb()
    val valueLabel = if (entry.isInfinite) "가장 멀리 도달한 단계" else "획득한 별"
    val valueText = if (entry.isInfinite) "${rec.cleared}단계"
                    else "★ ${entry.starsEarned} / ${entry.starsTotal}"
    val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        .format(Date(rec.timestamp))
    val bmp = ShareUtils.renderRecordCard(
        bgColor = bgColor,
        levelText = "${entry.level}",
        titleText = difficultyLabel(entry.level),
        valueLabel = valueLabel,
        valueText = valueText,
        dateText = dateText
    )
    ShareUtils.shareBitmap(context, bmp)
}

@Composable
private fun ShareIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = size.minDimension
        val r = s * 0.13f
        val left = Offset(s * 0.22f, s * 0.50f)
        val topRight = Offset(s * 0.78f, s * 0.22f)
        val bottomRight = Offset(s * 0.78f, s * 0.78f)
        val strokeW = s * 0.08f
        drawLine(Color.White, left, topRight, strokeWidth = strokeW)
        drawLine(Color.White, left, bottomRight, strokeWidth = strokeW)
        drawCircle(Color.White, r, left)
        drawCircle(Color.White, r, topRight)
        drawCircle(Color.White, r, bottomRight)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        color = InkDark,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
    )
}

@Composable
private fun ColorBestCard(stage: ColorStage, bestMs: Long?) {
    val hasRecord = bestMs != null
    val color = when {
        !hasRecord -> Color(0xFFBDB7B0)
        stage.level % 2 == 1 -> SkyBlue
        else -> CoralPink
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${stage.level}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stage.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hasRecord) "최고 기록" else "아직 도전해 보세요",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (hasRecord) formatElapsed(bestMs!!) else "기록 없음",
            color = Color.White,
            fontSize = if (hasRecord) 20.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private data class PaintBest(
    val level: Int,
    val name: String,
    val timed: Boolean,
    val bestMs: Long?,
    val bestScore: Int?,
)

/** 바닥 색칠하기 기록 카드 — 값(시간 또는 "N칸")을 문자열로 받아 그대로 표기. */
@Composable
private fun PaintBestCard(title: String, valueText: String?, baseColor: Color) {
    val hasRecord = valueText != null
    val color = if (hasRecord) baseColor else Color(0xFFBDB7B0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🖌️", fontSize = 26.sp)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hasRecord) "최고 기록" else "아직 도전해 보세요",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = valueText ?: "기록 없음",
            color = Color.White,
            fontSize = if (hasRecord) 20.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun TimeBestCard(level: Int, title: String, bestMs: Long?, baseColor: Color, emoji: String) {
    val hasRecord = bestMs != null
    val color = if (hasRecord) baseColor else Color(0xFFBDB7B0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (hasRecord) "최고 기록" else "아직 도전해 보세요",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (hasRecord) formatElapsed(bestMs!!) else "기록 없음",
            color = Color.White,
            fontSize = if (hasRecord) 20.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun LevelBestCard(entry: LevelBest, onShare: () -> Unit) {
    val hasRecord = entry.best != null
    val color = if (hasRecord) levelColor(entry.level) else Color(0xFFBDB7B0)
    val date = remember(entry.best?.timestamp) {
        entry.best?.let {
            SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(it.timestamp))
        }
    }
    val valueText = when {
        !hasRecord -> "기록 없음"
        entry.isInfinite -> "${entry.best!!.cleared}단계"
        else -> "★ ${entry.starsEarned}/${entry.starsTotal}"
    }
    val valueLabel = when {
        !hasRecord -> ""
        entry.isInfinite -> "최고 도달"
        else -> "획득한 별"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${entry.level}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = difficultyLabel(entry.level),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            if (hasRecord) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = valueLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = date ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            } else {
                Text(
                    text = "아직 도전해 보세요",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = valueText,
            color = Color.White,
            fontSize = if (hasRecord) 20.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (hasRecord) {
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                ShareIcon()
            }
        }
    }
}
