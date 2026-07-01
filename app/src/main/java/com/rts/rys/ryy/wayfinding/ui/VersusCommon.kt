package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.VersusResult
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen

/** 대전 화면 공통 단계. */
enum class VersusPhase { WAITING, COUNTDOWN, RACE, RESULT }

/** 상대 고스트 표시 색(모든 대전 공용). */
val VersusGhostColor = Color(0xFF7E57C2)

/** 레이스형 판정: 완주 시간이 빠른 쪽 승리. */
fun versusDecideByTime(mine: Long, opp: Long): VersusResult = when {
    mine < opp -> VersusResult.WIN
    mine > opp -> VersusResult.LOSE
    else -> VersusResult.DRAW
}

@Composable
fun VersusProgressBars(myProgress: Float, oppProgress: Float, oppName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        VersusProgressRow(label = "나", progress = myProgress, color = CoralPink)
        VersusProgressRow(label = oppName, progress = oppProgress, color = VersusGhostColor)
    }
}

@Composable
private fun VersusProgressRow(label: String, progress: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = InkDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.width(56.dp),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun VersusRaceResultOverlay(result: VersusResult, onExit: () -> Unit) {
    val (text, emoji, color) = when (result) {
        VersusResult.WIN -> Triple("이겼어요!", "🏆", WallGreen)
        VersusResult.LOSE -> Triple("졌어요", "😢", CoralPink)
        VersusResult.DRAW -> Triple("비겼어요", "🤝", Color(0xFF9E9E9E))
        VersusResult.OPPONENT_LEFT -> Triple("상대가 나갔어요", "🏁", WallGreen)
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 40.dp, vertical = 32.dp)
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .height(60.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(color)
                    .clickable(onClick = onExit)
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("나가기", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
