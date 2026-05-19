package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.rts.rys.ryy.wayfinding.game.Maze

class DustParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var age: Float = 0f,
    val lifetime: Float = 0.35f
)

class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    val rotSpeed: Float,
    var age: Float = 0f,
    val lifetime: Float = 1.2f,
    val color: Color,
    val width: Float = 0.20f,
    val height: Float = 0.10f
)

@Composable
fun EffectsOverlay(
    maze: Maze,
    dust: List<DustParticle>,
    confetti: List<ConfettiParticle>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cs = minOf(size.width / maze.cols, size.height / maze.rows)
        val origin = Offset(
            (size.width - cs * maze.cols) / 2f,
            (size.height - cs * maze.rows) / 2f
        )
        drawDust(cs, origin, dust)
        drawConfetti(cs, origin, confetti)
    }
}

private fun DrawScope.drawDust(cs: Float, origin: Offset, dust: List<DustParticle>) {
    for (p in dust) {
        val frac = (p.age / p.lifetime).coerceIn(0f, 1f)
        val alpha = (1f - frac) * 0.75f
        val r = cs * 0.10f * (1f - frac * 0.5f)
        val cx = origin.x + p.x * cs
        val cy = origin.y + p.y * cs
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawConfetti(cs: Float, origin: Offset, confetti: List<ConfettiParticle>) {
    for (p in confetti) {
        val frac = (p.age / p.lifetime).coerceIn(0f, 1f)
        val alpha = if (frac < 0.7f) 1f else (1f - (frac - 0.7f) / 0.3f)
        val w = cs * p.width
        val h = cs * p.height
        val cx = origin.x + p.x * cs
        val cy = origin.y + p.y * cs
        rotate(degrees = p.rotation, pivot = Offset(cx, cy)) {
            drawRect(
                color = p.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                topLeft = Offset(cx - w / 2f, cy - h / 2f),
                size = Size(w, h)
            )
        }
    }
}
