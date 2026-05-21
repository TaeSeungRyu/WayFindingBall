package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.GameRecord
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.game.MazePar
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGoldDeep
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResultScreen(
    stageId: Int,
    elapsedMs: Long,
    caught: Boolean = false,
    clears: Int = 0,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val stage = remember(stageId) { Stages.byId(stageId) }
    val isInfinite = stage.level == 14
    val earnedStars = remember(stageId, elapsedMs, caught) {
        if (caught || isInfinite) 0 else MazePar.starsFor(stage, elapsedMs)
    }
    // 무한 모드는 "더 많은 단계를 통과할수록" 좋음 → cleared max. 일반 모드는 "빠를수록" 좋음 → elapsedMs min.
    val previousBestRecord = remember(stageId) {
        val records = RecordsRepository(context).load().filter { it.stageId == stageId }
        if (isInfinite) records.maxByOrNull { it.cleared }
        else records.minByOrNull { it.elapsedMs }
    }
    val previousBest = previousBestRecord?.elapsedMs
    val previousBestClears = previousBestRecord?.cleared ?: 0
    val isNewBest = when {
        isInfinite -> clears > previousBestClears
        caught -> false
        else -> previousBest == null || elapsedMs < previousBest
    }

    LaunchedEffect(stageId, elapsedMs, caught) {
        if (caught && !isInfinite) return@LaunchedEffect
        val repo = RecordsRepository(context)
        repo.add(
            GameRecord(
                stageId = stage.id,
                stageName = stage.name,
                elapsedMs = elapsedMs,
                timestamp = System.currentTimeMillis(),
                cleared = clears
            )
        )
    }

    val infinite = rememberInfiniteTransition(label = "result")
    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size((140 * pulse).dp)) {
                drawStar(
                    center = center,
                    outerR = size.minDimension * 0.45f,
                    innerR = size.minDimension * 0.2f,
                    fill = if (caught && !isInfinite) InkSoft else GoalGold,
                    stroke = if (caught && !isInfinite) InkDark else GoalGoldDeep
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = when {
                    isInfinite -> "끝까지 버텼어요!"
                    caught -> "사로잡혔어요"
                    else -> "참 잘했어요!"
                },
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (caught && !isInfinite) CoralPink else InkDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    isInfinite -> "오래 살아남았어요"
                    caught -> "${stage.name}에서 잡혔어요"
                    else -> "${stage.name} 도착!"
                },
                fontSize = 16.sp,
                color = InkSoft,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            if (!isInfinite) BigStarsRow(stars = earnedStars)
            if (isNewBest) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isInfinite) "★ 최고 기록! ★" else "★ 최고 기록! ★",
                    fontSize = 14.sp,
                    color = CoralPink,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (isInfinite && previousBestClears > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "최고 기록  ${previousBestClears}단계",
                    fontSize = 14.sp,
                    color = InkSoft,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isInfinite) "도달 단계" else "걸린 시간", color = InkSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isInfinite) "${clears}단계" else formatElapsed(elapsedMs),
                        color = CoralPink,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BigPillButton("단계 고르기", SkyBlue, onHome)
                BigPillButton("다시 해요", CoralPink, onRetry)
            }
        }
    }
}

@Composable
private fun BigStarsRow(stars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in 1..3) {
            Text(
                text = "★",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (i <= stars) GoalGold else InkSoft.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun BigPillButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(64.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    outerR: Float,
    innerR: Float,
    fill: Color,
    stroke: Color
) {
    val path = Path()
    val points = 5
    val step = Math.PI / points
    var angle = -Math.PI / 2
    path.moveTo(
        (center.x + outerR * cos(angle).toFloat()),
        (center.y + outerR * sin(angle).toFloat())
    )
    for (i in 1 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        angle += step
        path.lineTo(
            (center.x + r * cos(angle).toFloat()),
            (center.y + r * sin(angle).toFloat())
        )
    }
    path.close()
    drawPath(path = path, color = fill)
    drawPath(path = path, color = stroke, style = Stroke(width = 4f))
}
