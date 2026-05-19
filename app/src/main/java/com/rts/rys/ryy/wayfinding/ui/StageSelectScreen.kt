package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.game.Stage
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.theme.DeepNight
import com.rts.rys.ryy.wayfinding.ui.theme.MidNight
import com.rts.rys.ryy.wayfinding.ui.theme.NeonCyan
import com.rts.rys.ryy.wayfinding.ui.theme.NeonPink
import com.rts.rys.ryy.wayfinding.ui.theme.NeonYellow
import com.rts.rys.ryy.wayfinding.ui.theme.SoftWhite

@Composable
fun StageSelectScreen(
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidNight, DeepNight)))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackChip(onClick = onBack)
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "STAGE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SoftWhite,
                    letterSpacing = 6.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
            ) {
                items(Stages.all) { stage ->
                    StageCard(stage = stage, onClick = { onSelect(stage.id) })
                }
            }
        }
    }
}

@Composable
fun BackChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(SoftWhite.copy(alpha = 0.08f))
            .border(1.dp, SoftWhite.copy(alpha = 0.3f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "←",
            color = SoftWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StageCard(stage: Stage, onClick: () -> Unit) {
    val accent = when (stage.id % 3) {
        0 -> NeonYellow
        1 -> NeonCyan
        else -> NeonPink
    }
    Box(
        modifier = Modifier
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = accent, spotColor = accent)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(MidNight, DeepNight)
                )
            )
            .border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "0${stage.id}",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = accent
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stage.name,
                fontSize = 14.sp,
                color = SoftWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stage.difficulty,
                fontSize = 11.sp,
                color = SoftWhite.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
    }
}
