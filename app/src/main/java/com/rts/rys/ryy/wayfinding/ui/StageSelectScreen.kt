package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.difficultyLabel
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.Lavender
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow

@Composable
fun StageSelectScreen(
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val customs by Stages.customStages
    val stagesByLevel = remember(customs) {
        (1..4).associateWith { Stages.byLevel(it) }
    }
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
                    text = "게임 고르기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                for (level in 1..4) {
                    val stages = stagesByLevel[level].orEmpty()
                    val difficulty = stages.firstOrNull()?.difficulty ?: difficultyLabel(level)
                    item(key = "header-$level") {
                        SectionHeader(level = level, difficulty = difficulty)
                    }
                    items(stages.chunked(3)) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { stage ->
                                StageCard(
                                    stage = stage,
                                    onClick = { onSelect(stage.id) },
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
            text = "난이도 $level",
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
private fun StageCard(stage: Stage, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = levelColor(stage.level)
    Box(
        modifier = modifier
            .height(120.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (stage.isCustom) "내" else "${stage.indexInLevel}",
                    fontSize = if (stage.isCustom) 20.sp else 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stage.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

fun levelColor(level: Int): Color = when (level) {
    1 -> SkyBlue
    2 -> SunYellow
    3 -> CoralPink
    else -> Lavender
}

fun stageColor(id: Int): Color {
    val stage = runCatching { Stages.byId(id) }.getOrNull() ?: return SkyBlue
    return levelColor(stage.level)
}
