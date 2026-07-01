package com.rts.rys.ryy.wayfinding.net

import java.nio.ByteBuffer

/**
 * 대전 메시지 프레임. 모두 BYTES 페이로드(작게 유지).
 * byte[0] = 타입, 이후 타입별 본문.
 *
 * 미로 대전(A) 기준:
 * - SEED: 호스트 → 게스트, 공유 시드(양쪽이 동일 무작위 맵 생성)
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
    private const val TYPE_SEED: Byte = 4
    private const val TYPE_REMATCH: Byte = 5
    private const val TYPE_NEW_ROUND: Byte = 6
    private const val TYPE_SYNC_REQ: Byte = 7
    private const val TYPE_SYNC_RESP: Byte = 8

    fun start(): ByteArray = byteArrayOf(TYPE_START)

    /** 게스트 → 호스트: 시작 시각 동기화 요청(게스트는 보낸 시각을 로컬에 기억). */
    fun syncReq(): ByteArray = byteArrayOf(TYPE_SYNC_REQ)

    /** 호스트 → 게스트: 호스트 수신시각(t2) + 시작 목표시각(둘 다 호스트 단조시계). */
    fun syncResp(t2Host: Long, startAtHost: Long): ByteArray =
        ByteBuffer.allocate(1 + 16).apply {
            put(TYPE_SYNC_RESP); putLong(t2Host); putLong(startAtHost)
        }.array()

    /** "한 번 더 하고 싶어요" 의사 표시(양쪽이 보내면 호스트가 새 라운드를 연다). */
    fun rematch(): ByteArray = byteArrayOf(TYPE_REMATCH)

    fun seed(seed: Long): ByteArray =
        ByteBuffer.allocate(1 + 8).apply {
            put(TYPE_SEED); putLong(seed)
        }.array()

    /** 호스트 → 게스트: 이 시드로 리셋하고 새 라운드 시작. */
    fun newRound(seed: Long): ByteArray =
        ByteBuffer.allocate(1 + 8).apply {
            put(TYPE_NEW_ROUND); putLong(seed)
        }.array()

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
        data object Rematch : Msg
        data object SyncReq : Msg
        data class SyncResp(val t2Host: Long, val startAtHost: Long) : Msg
        data class Seed(val seed: Long) : Msg
        data class NewRound(val seed: Long) : Msg
        data class Pos(val x: Float, val y: Float, val progress: Float) : Msg
        data class Finished(val elapsedMs: Long) : Msg
    }

    fun parse(bytes: ByteArray): Msg? {
        if (bytes.isEmpty()) return null
        val buf = ByteBuffer.wrap(bytes)
        return when (buf.get()) {
            TYPE_START -> Msg.Start
            TYPE_REMATCH -> Msg.Rematch
            TYPE_SYNC_REQ -> Msg.SyncReq
            TYPE_SYNC_RESP -> if (bytes.size >= 17) Msg.SyncResp(buf.long, buf.long) else null
            TYPE_SEED -> if (bytes.size >= 9) Msg.Seed(buf.long) else null
            TYPE_NEW_ROUND -> if (bytes.size >= 9) Msg.NewRound(buf.long) else null
            TYPE_POS -> if (bytes.size >= 13) Msg.Pos(buf.float, buf.float, buf.float) else null
            TYPE_FINISHED -> if (bytes.size >= 9) Msg.Finished(buf.long) else null
            else -> null
        }
    }
}
