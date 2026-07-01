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

/** 밀리초를 "N.NN초" 형태(소수 둘째 자리)로 포맷. 대전 시간 표시 공용. */
fun formatVersusSeconds(ms: Long): String {
    val cs = (ms.coerceAtLeast(0L)) / 10L   // centiseconds
    return "%d.%02d".format(cs / 100L, cs % 100L)
}

/** 레이스형 판정: 완주 시간이 빠른 쪽 승리. */
fun versusDecideByTime(mine: Long, opp: Long): VersusResult = when {
    mine < opp -> VersusResult.WIN
    mine > opp -> VersusResult.LOSE
    else -> VersusResult.DRAW
}

/**
 * 상대가 먼저 끝냈을 때, 아직 못 끝낸 쪽 화면에 뜨는 "서둘러요!" 3-2-1 카운트.
 * 게임을 가리지 않도록 반투명 배지로 상단에 띄운다.
 */
@Composable
fun VersusGraceCountdown(seconds: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CoralPink.copy(alpha = 0.92f))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("서둘러요!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        Text("$seconds", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
    }
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
fun VersusRaceResultOverlay(
    result: VersusResult,
    onExit: () -> Unit,
    subtitle: String? = null,
    onRematch: (() -> Unit)? = null,
    waitingRematch: Boolean = false,
    opponentGone: Boolean = false,
) {
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
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = InkDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            // 상대가 나갔으면 재대결 불가 — 나가기만.
            if (onRematch != null && !opponentGone) {
                if (waitingRematch) {
                    Text("친구를 기다리는 중…", color = InkDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                } else {
                    ResultButton(label = "한 번 더", bg = color, onClick = onRematch)
                    Spacer(Modifier.height(12.dp))
                }
            }
            ResultButton(
                label = "나가기",
                bg = if (onRematch != null && !opponentGone) Color(0xFFBDB7B0) else color,
                onClick = onExit
            )
        }
    }
}

@Composable
private fun ResultButton(label: String, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}
