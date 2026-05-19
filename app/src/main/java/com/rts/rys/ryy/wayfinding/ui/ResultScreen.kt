package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.GameRecord
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.theme.DeepNight
import com.rts.rys.ryy.wayfinding.ui.theme.MidNight
import com.rts.rys.ryy.wayfinding.ui.theme.NeonCyan
import com.rts.rys.ryy.wayfinding.ui.theme.NeonPink
import com.rts.rys.ryy.wayfinding.ui.theme.NeonYellow
import com.rts.rys.ryy.wayfinding.ui.theme.SoftWhite

@Composable
fun ResultScreen(
    stageId: Int,
    elapsedMs: Long,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val stage = remember(stageId) { Stages.byId(stageId) }

    LaunchedEffect(stageId, elapsedMs) {
        val repo = RecordsRepository(context)
        repo.add(
            GameRecord(
                stageId = stage.id,
                stageName = stage.name,
                elapsedMs = elapsedMs,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    val infinite = rememberInfiniteTransition(label = "result")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(MidNight, DeepNight)))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size((90 * pulse).dp)) {
                drawArc(
                    color = NeonYellow.copy(alpha = 0.6f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6f)
                )
                drawCircle(
                    color = NeonYellow,
                    radius = size.minDimension / 3.4f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = size.minDimension / 12f,
                    center = androidx.compose.ui.geometry.Offset(center.x - 8f, center.y - 8f)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "CLEAR!",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = SoftWhite,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stage.name,
                fontSize = 14.sp,
                color = NeonCyan,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = NeonCyan, spotColor = NeonCyan)
                    .background(MidNight, RoundedCornerShape(18.dp))
                    .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", color = SoftWhite.copy(alpha = 0.5f), fontSize = 12.sp, letterSpacing = 4.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatElapsed(elapsedMs),
                        color = NeonCyan,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(
                    onClick = onHome,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPink.copy(alpha = 0.18f),
                        contentColor = NeonPink
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .border(1.5.dp, NeonPink, RoundedCornerShape(14.dp))
                ) {
                    Text("홈으로", fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan.copy(alpha = 0.18f),
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .border(1.5.dp, NeonCyan, RoundedCornerShape(14.dp))
                ) {
                    Text("다시하기", fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
            }
        }
    }
}
