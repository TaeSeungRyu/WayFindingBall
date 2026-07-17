package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.PaintRecordsRepository
import com.rts.rys.ryy.wayfinding.game.PaintGame
import com.rts.rys.ryy.wayfinding.game.PaintStage
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop

@Composable
fun PaintStageSelectScreen(
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val context = LocalContext.current
    val bestTimes = remember {
        val repo = PaintRecordsRepository(context)
        PaintGame.stages.associate { it.level to repo.bestFor(it.level) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "바닥 색칠하기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = viewportHeight)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    PaintGame.stages.forEach { stage ->
                        PaintStageCard(
                            stage = stage,
                            bestMs = bestTimes[stage.level],
                            onClick = { onSelect(stage.level) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaintStageCard(
    stage: PaintStage,
    bestMs: Long?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(stage.paintColor)
            .clickable(onClick = onClick)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🖌️", fontSize = 32.sp)
            }
            Spacer(Modifier.size(20.dp))
            Column {
                Text(
                    text = stage.name,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stage.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (bestMs != null) "최고 기록  ${formatElapsed(bestMs)}" else "아직 기록 없어요",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}
