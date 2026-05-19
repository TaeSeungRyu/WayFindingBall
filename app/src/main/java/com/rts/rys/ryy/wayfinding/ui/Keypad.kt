package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue

@Composable
fun DPad(
    onInput: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val up = remember { mutableStateOf(false) }
    val down = remember { mutableStateOf(false) }
    val left = remember { mutableStateOf(false) }
    val right = remember { mutableStateOf(false) }

    LaunchedEffect(up.value, down.value, left.value, right.value) {
        val dx = (if (right.value) 1f else 0f) - (if (left.value) 1f else 0f)
        val dy = (if (down.value) 1f else 0f) - (if (up.value) 1f else 0f)
        onInput(dx, dy)
    }

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DirButton(arrowAngleDeg = -90f, pressed = up)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirButton(arrowAngleDeg = 180f, pressed = left)
                Spacer(Modifier.size(64.dp))
                DirButton(arrowAngleDeg = 0f, pressed = right)
            }
            DirButton(arrowAngleDeg = 90f, pressed = down)
        }
    }
}

@Composable
private fun DirButton(arrowAngleDeg: Float, pressed: MutableState<Boolean>) {
    val pressScale by animateFloatAsState(
        targetValue = if (pressed.value) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "pressScale"
    )
    val darken = if (pressed.value) 0.20f else 0f
    val shadowDp = if (pressed.value) 2.dp else 7.dp
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(shadowDp, CircleShape)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed.value = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed.value = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.width / 2f
            val c = Offset(r, r)
            val lighter = lerp(SkyBlue, Color.White, 0.35f)
            val darker = lerp(SkyBlue, Color.Black, 0.30f + darken)
            // body radial gradient — light hits upper-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lighter, SkyBlue, darker),
                    center = Offset(r * 0.55f, r * 0.55f),
                    radius = r * 1.6f
                ),
                center = c,
                radius = r
            )
            // glossy top highlight
            val hlW = r * 1.30f
            val hlH = r * 0.55f
            drawOval(
                color = Color.White.copy(alpha = (0.38f - darken).coerceAtLeast(0f)),
                topLeft = Offset(r - hlW / 2f, r * 0.18f),
                size = Size(hlW, hlH)
            )
            // subtle inner ring for depth
            drawCircle(
                color = Color.Black.copy(alpha = 0.08f),
                center = c,
                radius = r * 0.93f,
                style = Stroke(width = 1.5f)
            )
            // chevron arrow
            rotate(degrees = arrowAngleDeg, pivot = c) {
                val chevR = r * 0.42f
                val chevStroke = r * 0.18f
                val path = Path().apply {
                    moveTo(r - chevR * 0.4f, r - chevR)
                    lineTo(r + chevR * 0.55f, r)
                    lineTo(r - chevR * 0.4f, r + chevR)
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.95f),
                    style = Stroke(
                        width = chevStroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
