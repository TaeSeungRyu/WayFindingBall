package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

private data class CloudData(
    val yFrac: Float,
    val sizeFrac: Float,
    val speed: Float,
    val phase: Float,
    val alpha: Float
)

private data class SparkleData(
    val xFrac: Float,
    val yFrac: Float,
    val phase: Float,
    val sizePx: Float
)

@Composable
fun SkyAmbience(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "sky")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 32_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skyT"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skyTwinkle"
    )

    val clouds = remember {
        listOf(
            CloudData(yFrac = 0.10f, sizeFrac = 0.34f, speed = 1.0f, phase = 0.0f, alpha = 0.26f),
            CloudData(yFrac = 0.22f, sizeFrac = 0.22f, speed = 1.6f, phase = 0.35f, alpha = 0.18f),
            CloudData(yFrac = 0.06f, sizeFrac = 0.16f, speed = 0.7f, phase = 0.7f, alpha = 0.20f)
        )
    }
    val sparkles = remember {
        List(12) {
            SparkleData(
                xFrac = ((it * 73 + 17) % 100) / 100f,
                yFrac = (((it * 41 + 5) % 80) / 100f) + 0.02f,
                phase = ((it * 31) % 100) / 100f,
                sizePx = 1.4f + (it % 3) * 0.6f
            )
        }
    }

    Canvas(modifier = modifier) {
        for (c in clouds) {
            val cycle = (t * c.speed + c.phase) % 1f
            val x = cycle * (size.width + 220f) - 110f
            val y = c.yFrac * size.height
            val s = c.sizeFrac * size.minDimension
            drawCloud(Offset(x, y), s, c.alpha)
        }
        for (sp in sparkles) {
            val cx = sp.xFrac * size.width
            val cy = sp.yFrac * size.height
            val ph = (twinkle + sp.phase) % 1f
            val alpha = (0.5f + 0.5f * sin(ph * 2f * PI.toFloat())) * 0.55f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = sp.sizePx,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawCloud(center: Offset, size: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    drawCircle(color, radius = size * 0.34f, center = Offset(center.x - size * 0.30f, center.y + size * 0.04f))
    drawCircle(color, radius = size * 0.30f, center = Offset(center.x + size * 0.30f, center.y + size * 0.06f))
    drawCircle(color, radius = size * 0.42f, center = center)
    drawCircle(color, radius = size * 0.26f, center = Offset(center.x - size * 0.10f, center.y - size * 0.20f))
    drawCircle(color, radius = size * 0.22f, center = Offset(center.x + size * 0.18f, center.y - size * 0.18f))
}
