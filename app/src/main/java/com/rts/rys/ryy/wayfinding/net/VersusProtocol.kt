package com.rts.rys.ryy.wayfinding.net

import java.nio.ByteBuffer

/**
 * 대전 메시지 프레임. 모두 BYTES 페이로드(작게 유지).
 * byte[0] = 타입, 이후 타입별 본문.
 *
 * v1은 미로 대전(A) 기준:
 * - START: 호스트 → 게스트, 카운트다운 시작 신호
 * - POS: 진행 중 내 공 위치 + 진행도(0..1) 연속 송신
 * - FINISHED: 골인 시 내 완주 시간(ms)
 *
 * 식별자/PII는 담지 않는다. (이름은 Nearby endpoint 이름으로만 전달)
 */
object VersusProtocol {
    private const val TYPE_START: Byte = 1
    private const val TYPE_POS: Byte = 2
    private const val TYPE_FINISHED: Byte = 3

    fun start(): ByteArray = byteArrayOf(TYPE_START)

    fun pos(x: Float, y: Float, progress: Float): ByteArray =
        ByteBuffer.allocate(1 + 12).apply {
            put(TYPE_POS); putFloat(x); putFloat(y); putFloat(progress)
        }.array()

    fun finished(elapsedMs: Long): ByteArray =
        ByteBuffer.allocate(1 + 8).apply {
            put(TYPE_FINISHED); putLong(elapsedMs)
        }.array()

    sealed interface Msg {
        data object Start : Msg
        data class Pos(val x: Float, val y: Float, val progress: Float) : Msg
        data class Finished(val elapsedMs: Long) : Msg
    }

    fun parse(bytes: ByteArray): Msg? {
        if (bytes.isEmpty()) return null
        val buf = ByteBuffer.wrap(bytes)
        return when (buf.get()) {
            TYPE_START -> Msg.Start
            TYPE_POS -> if (bytes.size >= 13) Msg.Pos(buf.float, buf.float, buf.float) else null
            TYPE_FINISHED -> if (bytes.size >= 9) Msg.Finished(buf.long) else null
            else -> null
        }
    }
}
