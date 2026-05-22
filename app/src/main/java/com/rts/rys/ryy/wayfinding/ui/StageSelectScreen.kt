package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.CustomMazesRepository
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.difficultyLabel
import com.rts.rys.ryy.wayfinding.ui.theme.BallRed
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.Lavender
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen

@Composable
fun StageSelectScreen(
    level: Int,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val customs by Stages.customStages
    var deleteCandidate by remember { mutableStateOf<Stage?>(null) }
    val stages = remember(customs, level) { Stages.byLevel(level) }
    val starsByStage = remember(customs, level) {
        val records = RecordsRepository(context).load()
        val bestByStage = records.groupBy { it.stageId }
            .mapValues { (_, rs) -> rs.minOf { it.elapsedMs } }
        stages.associate { stage ->
            val best = bestByStage[stage.id]
            stage.id to if (best != null) MazePar.starsFor(stage, best) else 0
        }
    }
    val difficulty = stages.firstOrNull()?.difficulty ?: difficultyLabel(level)
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
                    text = "스테이지 $level",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionHeader(level = level, difficulty = difficulty)
            Spacer(Modifier.height(8.dp))
            if (level in 14..20) {
                val firstStageId = stages.firstOrNull()?.id
                val infinityBestRecord = RecordsRepository(context).load()
                    .filter { it.stageId == firstStageId }
                    .maxByOrNull { it.cleared }
                stages.firstOrNull()?.let { stage ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        InfinityCard(
                            stage = stage,
                            bestMs = infinityBestRecord?.elapsedMs,
                            bestClears = infinityBestRecord?.cleared ?: 0,
                            showTime = false,
                            title = when (level) {
                                15 -> "생존 모드"
                                16 -> "얼음 미로"
                                17 -> "타는 길"
                                18 -> "공이 커져요"
                                19 -> "열쇠를 찾아요"
                                20 -> "내 그림자 미로"
                                else -> "무한 도전"
                            },
                            onClick = { onSelect(stage.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + navBottom)
                ) {
                    items(stages.chunked(3)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { stage ->
                                StageCard(
                                    stage = stage,
                                    stars = starsByStage[stage.id] ?: 0,
                                    onClick = { onSelect(stage.id) },
                                    onLongClick = if (stage.isCustom) {
                                        { deleteCandidate = stage }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        deleteCandidate?.let { candidate ->
            DeleteConfirmDialog(
                stage = candidate,
                onConfirm = {
                    val repo = CustomMazesRepository(context)
                    repo.delete(candidate.id)
                    val all = repo.load()
                    Stages.setCustomStages(
                        all.groupBy { it.level }.flatMap { (_, ms) ->
                            ms.sortedBy { it.createdAt }
                                .mapIndexed { i, m -> m.toStage(i + 1) }
                        }
                    )
                    deleteCandidate = null
                },
                onCancel = { deleteCandidate = null }
            )
        }
    }
}

@Composable
fun BackChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(40.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = SkyBlue,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "뒤로",
            color = InkDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SectionHeader(level: Int, difficulty: String) {
    val color = levelColor(level)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "스테이지 $level",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = InkDark
        )
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.22f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = difficulty,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
private fun InfinityCard(
    stage: Stage,
    bestMs: Long?,
    bestClears: Int = 0,
    showTime: Boolean = false,
    title: String = "무한 도전",
    onClick: () -> Unit
) {
    val color = levelColor(stage.level)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "∞",
                color = Color.White,
                fontSize = 72.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                val bestText = when {
                    showTime && bestMs != null -> "최고 기록  ${formatElapsed(bestMs)}"
                    !showTime && bestClears > 0 -> "최고 기록  ${bestClears}단계"
                    else -> "아직 기록 없어요"
                }
                Text(
                    text = bestText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StageCard(
    stage: Stage,
    stars: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val color = levelColor(stage.level)
    Box(
        modifier = modifier
            .height(132.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (stage.isCustom) "내" else "${stage.indexInLevel}",
                    fontSize = if (stage.isCustom) 18.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stage.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            StarsRow(stars = stars, size = 14)
        }
        if (stage.isCustom && onLongClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .clickable { onLongClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = CoralPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    stage: Stage,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "이 미로를 지울까요?",
                    color = InkDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stage.name,
                    color = levelColor(stage.level),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkyBlue)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("취소", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CoralPink)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("지우기", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StarsRow(stars: Int, size: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..3) {
            Text(
                text = "★",
                fontSize = size.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (i <= stars) GoalGold else Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

fun levelColor(level: Int): Color = when (level) {
    1 -> SkyBlue
    2 -> SunYellow
    3 -> CoralPink
    4 -> Lavender
    5 -> BallRed
    6 -> WallGreen
    7 -> Color(0xFF3D2C5C)
    8 -> Color(0xFFA42818)
    9 -> Color(0xFF2E9D5C)
    10 -> Color(0xFF6B1A8A)
    11 -> Color(0xFF7A3FE0)
    12 -> Color(0xFF5C7080)
    13 -> Color(0xFFD97742)
    14 -> Color(0xFF111111)
    15 -> Color(0xFF7B0E0E)
    16 -> Color(0xFF4FA8D8)
    17 -> Color(0xFFC23A14)
    18 -> Color(0xFFE07898)
    19 -> Color(0xFF8A6E48)
    else -> Color(0xFF1A1424)
}

fun stageColor(id: Int): Color {
    val stage = runCatching { Stages.byId(id) }.getOrNull() ?: return SkyBlue
    return levelColor(stage.level)
}
