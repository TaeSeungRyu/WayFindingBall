package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.rts.rys.ryy.wayfinding.data.BallDecoration
import com.rts.rys.ryy.wayfinding.data.BallShape
import com.rts.rys.ryy.wayfinding.data.BallSkin
import kotlin.math.cos
import kotlin.math.sin

/**
 * 공 본체(외곽선 포함). 표정/회전 마커/스쿼시는 호출자가 별도 처리.
 * 좌표 (cx, cy)는 공 중심, r은 외접원 반지름.
 */
fun DrawScope.drawBallBody(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    when (skin.shape) {
        BallShape.POOP -> drawPoopBody(skin, cx, cy, r)
        BallShape.CIRCLE -> drawCircleBody(skin, cx, cy, r)
    }
}

/** 모양에 따른 눈 중심의 Y 오프셋 (cy 기준, 위쪽이 음수). */
fun eyeOffsetY(skin: BallSkin, r: Float): Float = when (skin.shape) {
    BallShape.POOP -> -r * 0.05f
    BallShape.CIRCLE -> -r * 0.08f
}

fun shouldDrawRotationMarkers(skin: BallSkin): Boolean = skin.shape == BallShape.CIRCLE

private fun DrawScope.drawCircleBody(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, skin.coreColor, skin.deepColor),
            center = Offset(cx - r * 0.35f, cy - r * 0.4f),
            radius = r * 1.5f
        ),
        center = Offset(cx, cy),
        radius = r
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        center = Offset(cx - r * 0.38f, cy - r * 0.38f),
        radius = r * 0.22f
    )
    drawCircle(
        color = skin.deepColor,
        center = Offset(cx, cy),
        radius = r,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawPoopBody(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    data class Bump(val c: Offset, val rr: Float, val highlightOffset: Offset)
    val bumps = listOf(
        Bump(Offset(cx, cy + r * 0.45f), r * 0.85f, Offset(-r * 0.25f, r * 0.10f)),
        Bump(Offset(cx + r * 0.05f, cy + r * 0.00f), r * 0.62f, Offset(-r * 0.20f, -r * 0.15f)),
        Bump(Offset(cx - r * 0.02f, cy - r * 0.42f), r * 0.38f, Offset(-r * 0.12f, -r * 0.20f)),
    )
    // 본체 (아래→위 순으로 적층)
    for (b in bumps) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.6f), skin.coreColor, skin.deepColor),
                center = Offset(b.c.x + b.highlightOffset.x, b.c.y + b.highlightOffset.y),
                radius = b.rr * 1.6f
            ),
            center = b.c,
            radius = b.rr
        )
    }
    // 외곽선 (각 bump)
    for (b in bumps) {
        drawCircle(
            color = skin.deepColor,
            center = b.c,
            radius = b.rr,
            style = Stroke(width = 2f)
        )
    }
    // 작은 광택
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        center = Offset(bumps[2].c.x - r * 0.10f, bumps[2].c.y - r * 0.10f),
        radius = r * 0.10f
    )
}

/** 본체 위에 그릴 장식 (광선/잎/왕관 등). 호출 순서는 본체 다음. */
fun DrawScope.drawBallDecoration(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float = 0f) {
    when (skin.decoration) {
        BallDecoration.SUN_RAYS -> { drawSunRays(skin, cx, cy, r, phaseSec); drawSparkleHalo(cx, cy, r, phaseSec, Color(0xFFFFF59D), 5) }
        BallDecoration.WAVE_BELOW -> { drawWaveBelow(skin, cx, cy, r, phaseSec); drawBubbles(cx, cy, r, phaseSec); drawWaterSplash(cx, cy, r, phaseSec) }
        BallDecoration.LEAF_TOP -> { drawLeafTop(skin, cx, cy, r); drawFallingPetals(skin, cx, cy, r, phaseSec) }
        BallDecoration.CROWN -> { drawRoyalGlow(skin, cx, cy, r, phaseSec); drawCrown(skin, cx, cy, r); drawJewelSparkle(cx, cy - r * 0.95f, r, phaseSec); drawRoyalGems(cx, cy, r, phaseSec) }
        BallDecoration.FLAME_TOP -> { drawFireGlow(cx, cy, r, phaseSec); drawFlameTop(skin, cx, cy, r, phaseSec); drawEmbers(cx, cy, r, phaseSec); drawFireSparks(cx, cy, r, phaseSec) }
        BallDecoration.SHADOW_TWIN -> { drawShadowTwin(skin, cx, cy, r); drawShadowDust(skin, cx, cy, r, phaseSec) }
        BallDecoration.RAINBOW_RING -> { drawRainbowRing(skin, cx, cy, r); drawStarSparkles(cx, cy, r, phaseSec) }
        BallDecoration.POOP_STEAM -> drawPoopSteam(cx, cy, r, phaseSec)
        BallDecoration.NONE -> {}
    }
}

/** 본체 둘레에 작은 점들이 깜빡임 (햇살 공). */
private fun DrawScope.drawSparkleHalo(cx: Float, cy: Float, r: Float, phaseSec: Float, color: Color, count: Int) {
    val orbit = r * 1.35f
    for (i in 0 until count) {
        val baseAngle = (i * 2.0 * Math.PI / count).toFloat()
        val angle = baseAngle + phaseSec * 0.6f
        val sx = cx + cos(angle) * orbit
        val sy = cy + sin(angle) * orbit
        val pulse = (sin(phaseSec * 3f + i * 1.3f) * 0.5f + 0.5f)
        val alpha = pulse
        val rr = r * (0.08f + 0.04f * pulse)
        drawCircle(color = color.copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
        drawCircle(color = Color.White.copy(alpha = alpha * 0.6f), radius = rr * 0.45f, center = Offset(sx, sy))
    }
}

/** 본체 위로 거품이 활기차게 떠오름 (바다 공). 개수·속도·진폭 모두 강화. */
private fun DrawScope.drawBubbles(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 6
    for (i in 0 until count) {
        val t = ((phaseSec * 1.2f + i * 0.17f) % 1f)
        val spread = ((i - count / 2f) / count) * r * 1.8f
        val sx = cx + spread + sin(phaseSec * 3.5f + i * 1.3f) * r * 0.22f
        val sy = cy + r * 1.2f - t * r * 2.4f
        val alpha = (1f - t) * 0.85f
        val rr = r * (0.10f + t * 0.07f)
        drawCircle(color = Color(0xFFB3E5FC).copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
        drawCircle(color = Color.White.copy(alpha = alpha * 0.85f), radius = rr * 0.5f,
            center = Offset(sx - rr * 0.3f, sy - rr * 0.3f))
    }
}

/** 본체 둘레 사방으로 작은 물방울이 튀어나감 (바다 공). */
private fun DrawScope.drawWaterSplash(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 8
    for (i in 0 until count) {
        val baseAngle = (i * 2.0 * Math.PI / count).toFloat()
        // 각 입자가 다른 phase로 0→1 사이클
        val t = ((phaseSec * 1.4f + i * 0.13f) % 1f)
        val dist = r * (0.9f + t * 0.8f)
        val sx = cx + cos(baseAngle) * dist
        val sy = cy + sin(baseAngle) * dist + t * r * 0.3f // 살짝 아래로 떨어짐
        val alpha = (1f - t) * 0.75f
        val rr = r * (0.07f * (1f - t * 0.5f))
        drawCircle(color = Color(0xFF81D4FA).copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
    }
}

/** 본체 옆으로 작은 꽃잎이 회전하며 떨어짐 (숲 공). */
private fun DrawScope.drawFallingPetals(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 2
    for (i in 0 until count) {
        val t = ((phaseSec * 0.5f + i * 0.5f) % 1f)
        val sideSign = if (i % 2 == 0) -1f else 1f
        val sx = cx + sideSign * r * (0.9f + sin(phaseSec * 1.5f + i) * 0.25f)
        val sy = cy - r * 0.8f + t * r * 2.4f
        val alpha = (1f - t * 0.8f).coerceAtLeast(0f)
        val petalR = r * 0.12f
        rotate(degrees = (phaseSec * 80f + i * 110f) % 360f, pivot = Offset(sx, sy)) {
            drawOval(
                color = skin.coreColor.copy(alpha = alpha),
                topLeft = Offset(sx - petalR, sy - petalR * 0.5f),
                size = Size(petalR * 2f, petalR)
            )
        }
    }
}

/** 보석 위에서 +자 반짝임 (로열 공). */
private fun DrawScope.drawJewelSparkle(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val pulse = (sin(phaseSec * 4f) * 0.5f + 0.5f)
    val len = r * (0.20f + 0.10f * pulse)
    val w = r * 0.05f
    drawLine(Color.White.copy(alpha = pulse), Offset(cx - len, cy), Offset(cx + len, cy), strokeWidth = w, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = pulse), Offset(cx, cy - len), Offset(cx, cy + len), strokeWidth = w, cap = StrokeCap.Round)
}

/** 본체 둘레 보라 후광 (로열 공). */
private fun DrawScope.drawRoyalGlow(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val pulse = (sin(phaseSec * 3f) * 0.5f + 0.5f)
    drawCircle(
        color = Color(0xFFCE93D8).copy(alpha = 0.22f + 0.10f * pulse),
        radius = r * (1.50f + 0.08f * pulse),
        center = Offset(cx, cy)
    )
    drawCircle(
        color = skin.coreColor.copy(alpha = 0.18f + 0.06f * pulse),
        radius = r * (1.22f + 0.05f * pulse),
        center = Offset(cx, cy)
    )
}

/** 본체 둘레로 금색 다이아 보석 5개가 천천히 공전하며 깜빡 (로열 공). */
private fun DrawScope.drawRoyalGems(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 5
    val orbit = r * 1.45f
    for (i in 0 until count) {
        val angle = (i * 2.0 * Math.PI / count).toFloat() + phaseSec * 0.5f
        val sx = cx + cos(angle) * orbit
        val sy = cy + sin(angle) * orbit
        val pulse = (sin(phaseSec * 3.5f + i * 1.3f) * 0.5f + 0.5f)
        val s = r * (0.14f + 0.05f * pulse)
        // 회전된 정사각형 = 다이아몬드(마름모) 모양
        rotate(degrees = 45f, pivot = Offset(sx, sy)) {
            drawRect(
                color = Color(0xFFFFD24A).copy(alpha = 0.65f + 0.30f * pulse),
                topLeft = Offset(sx - s / 2f, sy - s / 2f),
                size = Size(s, s)
            )
            drawRect(
                color = Color(0xFFFFA000).copy(alpha = 0.85f),
                topLeft = Offset(sx - s / 2f, sy - s / 2f),
                size = Size(s, s),
                style = Stroke(width = r * 0.02f)
            )
            // 안쪽 작은 하이라이트
            drawRect(
                color = Color.White.copy(alpha = pulse * 0.85f),
                topLeft = Offset(sx - s * 0.18f, sy - s * 0.18f),
                size = Size(s * 0.30f, s * 0.30f)
            )
        }
    }
}

/** 본체 위로 불씨가 활기차게 떠오름 (불꽃 공). 개수·속도·진폭 강화. */
private fun DrawScope.drawEmbers(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 7
    for (i in 0 until count) {
        val t = ((phaseSec * 1.6f + i * 0.14f) % 1f)
        val spread = ((i - count / 2f) / count) * r * 1.5f
        val sx = cx + spread + sin(phaseSec * 4f + i * 1.7f) * r * 0.35f
        val sy = cy - r * 1.45f - t * r * 1.6f
        val alpha = (1f - t) * 0.95f
        val rr = r * (0.07f + (1f - t) * 0.06f)
        drawCircle(color = Color(0xFFFFD24A).copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
        drawCircle(color = Color(0xFFFF7043).copy(alpha = alpha * 0.55f), radius = rr * 1.5f, center = Offset(sx, sy))
    }
}

/** 본체 둘레 사방으로 불꽃 파편이 튀어나감 (불꽃 공). */
private fun DrawScope.drawFireSparks(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 10
    for (i in 0 until count) {
        val baseAngle = (i * 2.0 * Math.PI / count).toFloat()
        val t = ((phaseSec * 1.8f + i * 0.11f) % 1f)
        val dist = r * (0.9f + t * 0.7f)
        val sx = cx + cos(baseAngle) * dist
        val sy = cy + sin(baseAngle) * dist - t * r * 0.25f // 살짝 위로
        val alpha = (1f - t) * 0.80f
        val rr = r * (0.06f * (1f - t * 0.4f))
        drawCircle(color = Color(0xFFFFE082).copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
        drawCircle(color = Color(0xFFFF8A65).copy(alpha = alpha * 0.6f), radius = rr * 1.3f, center = Offset(sx, sy))
    }
}

/** 본체 주변에 박동하는 따뜻한 광원 (불꽃 공). */
private fun DrawScope.drawFireGlow(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val pulse = (sin(phaseSec * 6f) * 0.5f + 0.5f)
    val radius = r * (1.55f + 0.10f * pulse)
    drawCircle(
        color = Color(0xFFFFAB91).copy(alpha = 0.25f + 0.10f * pulse),
        radius = radius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color(0xFFFF7043).copy(alpha = 0.18f + 0.08f * pulse),
        radius = radius * 0.78f,
        center = Offset(cx, cy)
    )
}

/** 본체 둘레로 어두운 입자가 회전 (그림자 공). */
private fun DrawScope.drawShadowDust(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 6
    val orbit = r * 1.20f
    for (i in 0 until count) {
        val angle = (i * 2.0 * Math.PI / count).toFloat() + phaseSec * 1.1f
        val sx = cx + cos(angle) * orbit
        val sy = cy + sin(angle) * orbit
        val pulse = (sin(phaseSec * 2f + i) * 0.5f + 0.5f)
        drawCircle(
            color = skin.deepColor.copy(alpha = 0.35f + 0.30f * pulse),
            radius = r * 0.08f,
            center = Offset(sx, sy)
        )
    }
}

/** 무지개 링 주위에 별 모양 반짝임 (무지개 공). */
private fun DrawScope.drawStarSparkles(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 4
    val orbit = r * 1.55f
    for (i in 0 until count) {
        val angle = (i * 2.0 * Math.PI / count).toFloat() + phaseSec * 0.4f
        val sx = cx + cos(angle) * orbit
        val sy = cy + sin(angle) * orbit
        val pulse = (sin(phaseSec * 5f + i * 1.7f) * 0.5f + 0.5f)
        val len = r * (0.10f + 0.05f * pulse)
        val w = r * 0.04f
        val a = pulse
        drawLine(Color.White.copy(alpha = a), Offset(sx - len, sy), Offset(sx + len, sy), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = a), Offset(sx, sy - len), Offset(sx, sy + len), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** 본체 위로 회색 김이 모락모락 (응가 공). */
private fun DrawScope.drawPoopSteam(cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val count = 3
    for (i in 0 until count) {
        val t = ((phaseSec * 0.6f + i * 0.33f) % 1f)
        val sideSign = (i - 1).toFloat()
        val sx = cx + sideSign * r * 0.30f + sin(phaseSec * 2f + i * 1.7f) * r * 0.18f
        val sy = cy - r * 0.95f - t * r * 1.3f
        val alpha = ((1f - t) * 0.55f).coerceAtLeast(0f)
        val rr = r * (0.18f + t * 0.10f)
        drawCircle(color = Color(0xFFBDBDBD).copy(alpha = alpha), radius = rr, center = Offset(sx, sy))
        drawCircle(
            color = Color(0xFF9E9E9E).copy(alpha = alpha * 0.7f),
            radius = rr,
            center = Offset(sx, sy),
            style = Stroke(width = r * 0.03f)
        )
    }
}

private fun DrawScope.drawSunRays(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val rays = 8
    val outer = r * 1.45f
    val inner = r * 1.10f
    val w = r * 0.18f
    val rotDeg = (phaseSec * 30f) % 360f
    rotate(degrees = rotDeg, pivot = Offset(cx, cy)) {
        for (i in 0 until rays) {
            val a = (i * 2.0 * Math.PI / rays).toFloat()
            val x1 = cx + cos(a) * inner
            val y1 = cy + sin(a) * inner
            val x2 = cx + cos(a) * outer
            val y2 = cy + sin(a) * outer
            drawLine(
                color = skin.deepColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = w,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawWaveBelow(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val baseY = cy + r * 1.15f
    val width = r * 2.4f
    val ampBase = r * 0.18f
    val steps = 36
    val x0 = cx - width / 2f
    // 메인 물결: 위상이 좌→우로 흐름, 진폭도 시간에 따라 살짝 박동
    val ampPulse = ampBase * (1f + 0.20f * sin(phaseSec * 1.5f))
    val mainPath = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val xx = x0 + width * t
        val yy = baseY + sin(t * Math.PI.toFloat() * 4f - phaseSec * 4f) * ampPulse
        if (i == 0) mainPath.moveTo(xx, yy) else mainPath.lineTo(xx, yy)
    }
    drawPath(
        path = mainPath,
        color = skin.deepColor,
        style = Stroke(width = r * 0.18f, cap = StrokeCap.Round)
    )
    // 보조 물결: 반대 방향 + 옅은 색 → 입체감
    val sub = Path()
    val subAmp = ampBase * 0.55f
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val xx = x0 + width * t
        val yy = baseY + r * 0.18f + sin(t * Math.PI.toFloat() * 5f + phaseSec * 3f) * subAmp
        if (i == 0) sub.moveTo(xx, yy) else sub.lineTo(xx, yy)
    }
    drawPath(
        path = sub,
        color = skin.coreColor.copy(alpha = 0.55f),
        style = Stroke(width = r * 0.10f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawLeafTop(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    val tip = Offset(cx, cy - r * 1.5f)
    val left = Offset(cx - r * 0.32f, cy - r * 1.0f)
    val right = Offset(cx + r * 0.32f, cy - r * 1.0f)
    val path = Path().apply {
        moveTo(left.x, left.y)
        quadraticTo(cx - r * 0.55f, cy - r * 1.3f, tip.x, tip.y)
        quadraticTo(cx + r * 0.55f, cy - r * 1.3f, right.x, right.y)
        close()
    }
    drawPath(path = path, color = skin.coreColor)
    drawPath(path = path, color = skin.deepColor, style = Stroke(width = r * 0.10f))
    // 잎맥
    drawLine(
        color = skin.deepColor.copy(alpha = 0.7f),
        start = Offset(cx, cy - r * 1.0f),
        end = tip,
        strokeWidth = r * 0.06f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCrown(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    val base = cy - r * 1.0f
    val top = cy - r * 1.55f
    val w = r * 1.4f
    val left = cx - w / 2f
    val right = cx + w / 2f
    // 띠
    drawRoundRect(
        color = skin.coreColor,
        topLeft = Offset(left, base - r * 0.05f),
        size = Size(w, r * 0.30f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.08f, r * 0.08f)
    )
    // 뾰족한 5점
    val path = Path().apply {
        moveTo(left, base)
        lineTo(left + w * 0.20f, top + r * 0.30f)
        lineTo(left + w * 0.30f, base - r * 0.10f)
        lineTo(cx, top)
        lineTo(right - w * 0.30f, base - r * 0.10f)
        lineTo(right - w * 0.20f, top + r * 0.30f)
        lineTo(right, base)
        close()
    }
    drawPath(path = path, color = skin.coreColor)
    drawPath(path = path, color = skin.deepColor, style = Stroke(width = r * 0.08f))
    // 가운데 보석
    drawCircle(color = Color(0xFFFFD24A), radius = r * 0.12f, center = Offset(cx, base + r * 0.10f))
}

private fun DrawScope.drawFlameTop(skin: BallSkin, cx: Float, cy: Float, r: Float, phaseSec: Float) {
    val wobble = sin(phaseSec * 6f) * 0.06f
    val tip = Offset(cx + r * wobble, cy - r * 1.55f)
    val left = Offset(cx - r * 0.45f, cy - r * 0.90f)
    val right = Offset(cx + r * 0.45f, cy - r * 0.90f)
    val path = Path().apply {
        moveTo(left.x, left.y)
        quadraticTo(cx - r * 0.30f, cy - r * 1.35f, tip.x, tip.y)
        quadraticTo(cx + r * 0.30f, cy - r * 1.35f, right.x, right.y)
        close()
    }
    drawPath(path = path, color = Color(0xFFFFC107))
    drawPath(path = path, color = skin.deepColor, style = Stroke(width = r * 0.08f))
    // 안쪽 작은 불꽃
    val innerTip = Offset(cx + r * wobble * 0.5f, cy - r * 1.25f)
    val innerPath = Path().apply {
        moveTo(cx - r * 0.20f, cy - r * 0.90f)
        quadraticTo(cx - r * 0.12f, cy - r * 1.15f, innerTip.x, innerTip.y)
        quadraticTo(cx + r * 0.12f, cy - r * 1.15f, cx + r * 0.20f, cy - r * 0.90f)
        close()
    }
    drawPath(path = innerPath, color = Color(0xFFFF7043))
}

private fun DrawScope.drawShadowTwin(skin: BallSkin, cx: Float, cy: Float, r: Float) {
    drawCircle(
        color = skin.coreColor.copy(alpha = 0.30f),
        center = Offset(cx + r * 0.85f, cy + r * 0.30f),
        radius = r * 0.85f
    )
    drawCircle(
        color = skin.deepColor.copy(alpha = 0.55f),
        center = Offset(cx + r * 0.85f, cy + r * 0.30f),
        radius = r * 0.85f,
        style = Stroke(width = r * 0.10f)
    )
}

private fun DrawScope.drawRainbowRing(@Suppress("UNUSED_PARAMETER") skin: BallSkin, cx: Float, cy: Float, r: Float) {
    val ringR = r * 1.25f
    val width = r * 0.16f
    val colors = listOf(
        Color(0xFFFF5252), Color(0xFFFFB300), Color(0xFFFFEB3B),
        Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFAB47BC)
    )
    val sweep = 360f / colors.size
    for (i in colors.indices) {
        drawArc(
            color = colors[i],
            startAngle = sweep * i - 90f,
            sweepAngle = sweep + 1f,
            useCenter = false,
            topLeft = Offset(cx - ringR, cy - ringR),
            size = Size(ringR * 2f, ringR * 2f),
            style = Stroke(width = width, cap = StrokeCap.Butt)
        )
    }
}
