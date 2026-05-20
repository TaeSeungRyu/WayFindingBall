package com.rts.rys.ryy.wayfinding.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.difficultyLabel
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop

@Composable
fun LevelSelectScreen(
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val customs by Stages.customStages
    val starsByLevel = remember(customs) {
        val records = RecordsRepository(context).load()
        val bestByStage = records.groupBy { it.stageId }
            .mapValues { (_, rs) -> rs.minOf { it.elapsedMs } }
        (1..5).associateWith { level ->
            val stages = Stages.byLevel(level)
            val earned = stages.sumOf { stage ->
                val best = bestByStage[stage.id] ?: return@sumOf 0
                MazePar.starsFor(stage, best)
            }
            val total = stages.size * 3
            earned to total
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
                    text = "난이도 고르기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp + navBottom)
            ) {
                items((1..5).toList(), key = { it }) { level ->
                    val (earned, total) = starsByLevel[level] ?: (0 to 0)
                    LevelCard(
                        level = level,
                        difficulty = difficultyLabel(level),
                        earnedStars = earned,
                        totalStars = total,
                        onClick = { onSelect(level) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: Int,
    difficulty: String,
    earnedStars: Int,
    totalStars: Int,
    onClick: () -> Unit
) {
    val color = levelColor(level)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .shadow(8.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$level",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        Spacer(Modifier.size(18.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "난이도 $level",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = difficulty,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "★",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoalGold
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = "$earnedStars / $totalStars",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
