package com.rts.rys.ryy.wayfinding.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * 공이 지나온 경로를 [delaySec]만큼 늦게 따라오는 "그림자". 본체에 닿으면 실패.
 * 라운드가 진행될수록 [delaySec]가 줄어들도록 외부에서 조절한다.
 */
class ShadowChaserController(
    initialBallX: Float,
    initialBallY: Float,
    private val delaySec: Float,
) {
    private data class Sample(val time: Float, val x: Float, val y: Float)

    private val trail: ArrayDeque<Sample> = ArrayDeque()
    private var clock: Float = 0f

    var shadowX: Float = initialBallX
        private set
    var shadowY: Float = initialBallY
        private set
    var active: Boolean = false
        private set
    var version: Int by mutableIntStateOf(0)
        private set

    init {
        trail.add(Sample(0f, initialBallX, initialBallY))
    }

    fun tick(dt: Float, ballX: Float, ballY: Float) {
        clock += dt
        trail.add(Sample(clock, ballX, ballY))
        val targetTime = clock - delaySec
        if (targetTime <= 0f) {
            // 아직 그림자 출현 전: 사용자 도주 시간 확보
            active = false
        } else {
            active = true
            // targetTime을 포함하는 구간만 남기고 과거는 버림
            while (trail.size >= 2 && trail[1].time <= targetTime) {
                trail.removeAt(0)
            }
            if (trail.size >= 2) {
                val a = trail[0]
                val b = trail[1]
                val span = (b.time - a.time).coerceAtLeast(1e-4f)
                val t = ((targetTime - a.time) / span).coerceIn(0f, 1f)
                shadowX = a.x + (b.x - a.x) * t
                shadowY = a.y + (b.y - a.y) * t
            } else {
                shadowX = trail[0].x
                shadowY = trail[0].y
            }
        }
        version++
    }

    fun hit(ballX: Float, ballY: Float, radius: Float): Boolean {
        if (!active) return false
        val dx = ballX - shadowX
        val dy = ballY - shadowY
        val r = radius * 0.9f
        return dx * dx + dy * dy < r * r
    }

    /** 잔상용: 현재 그림자 위치 직전의 일부 샘플(가장 최근→과거 순). 빈 리스트면 잔상 없음. */
    fun afterimages(maxCount: Int): List<Pair<Float, Float>> {
        if (!active || trail.size < 2) return emptyList()
        val result = ArrayList<Pair<Float, Float>>(maxCount)
        // 그림자 앞쪽(미래 방향) 샘플을 잔상으로 보여줌 → 따라오는 느낌 강조
        val n = trail.size
        val step = (n / (maxCount + 1)).coerceAtLeast(1)
        var idx = 1
        while (result.size < maxCount && idx < n) {
            val s = trail[idx]
            result.add(s.x to s.y)
            idx += step
        }
        return result
    }
}
