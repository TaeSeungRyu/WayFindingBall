package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.ui.theme.BallRed
import com.rts.rys.ryy.wayfinding.ui.theme.BallRedDeep
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1800)
        onFinished()
    }

    val infinite = rememberInfiniteTransition(label = "splash")
    val bounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom))),
        contentAlignment = Alignment.Center
    ) {
        // 해님
        Canvas(modifier = Modifier
            .size(120.dp)
            .padding(top = 20.dp)
            .align(Alignment.TopEnd)) {
            drawCircle(
                color = SunYellow.copy(alpha = 0.3f),
                radius = size.minDimension / 2f,
                center = center
            )
            drawCircle(
                color = SunYellow,
                radius = size.minDimension / 3f,
                center = center
            )
        }
        // 구름
        Canvas(modifier = Modifier
            .size(150.dp)
            .padding(start = 8.dp, top = 60.dp)
            .align(Alignment.TopStart)) {
            val cy = size.height / 2f
            drawCircle(color = Color.White, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.3f, cy))
            drawCircle(color = Color.White, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.5f, cy - size.height * 0.06f))
            drawCircle(color = Color.White, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.7f, cy))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 통통 튀는 공
            Box(
                modifier = Modifier
                    .padding(bottom = ((1f - bounce) * 24).dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val r = size.minDimension / 2.4f
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.12f),
                        radius = r,
                        center = Offset(center.x + 4f, center.y + 8f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, BallRed, BallRedDeep),
                            center = Offset(center.x - r * 0.35f, center.y - r * 0.4f),
                            radius = r * 1.5f
                        ),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.95f),
                        radius = r * 0.22f,
                        center = Offset(center.x - r * 0.38f, center.y - r * 0.38f)
                    )
                    drawCircle(
                        color = BallRedDeep,
                        radius = r,
                        center = center,
                        style = Stroke(width = 3f)
                    )
                }
            }

            Text(
                text = "통통 미로",
                color = InkDark,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                text = "기울이고 굴려서 도착해요!",
                color = InkSoft,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
