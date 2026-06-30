package com.rts.rys.ryy.wayfinding.net

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

enum class NearbyStatus { IDLE, ADVERTISING, DISCOVERING, CONNECTING, CONNECTED, DISCONNECTED, ERROR }

data class DiscoveredRoom(val endpointId: String, val name: String)

/**
 * 1:1 로컬 P2P 연결 래퍼. Nearby Connections 콜백은 메인 스레드로 들어오므로
 * Compose `mutableStateOf` 상태를 직접 갱신해도 안전하다.
 * - 호스트: [startHosting] (광고)
 * - 게스트: [startDiscovery] → 방 목록([rooms]) → [connectTo]
 */
class NearbyManager(context: Context, private val localName: String) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context.applicationContext)

    var status by mutableStateOf(NearbyStatus.IDLE)
        private set
    var isHost by mutableStateOf(false)
        private set
    var peerName by mutableStateOf<String?>(null)
        private set
    val rooms = mutableStateListOf<DiscoveredRoom>()

    /** 수신 페이로드 콜백(메인 스레드). 활성 대전 화면이 설정한다. */
    var onMessage: ((ByteArray) -> Unit)? = null

    private var connectedEndpointId: String? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes -> onMessage?.invoke(bytes) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            peerName = info.endpointName
            // 1:1 캐주얼 — 양쪽 자동 수락
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                connectedEndpointId = endpointId
                status = NearbyStatus.CONNECTED
                client.stopAdvertising()
                client.stopDiscovery()
            } else if (status != NearbyStatus.CONNECTED) {
                status = NearbyStatus.DISCONNECTED
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (endpointId == connectedEndpointId) {
                connectedEndpointId = null
                status = NearbyStatus.DISCONNECTED
            }
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId == SERVICE_ID && rooms.none { it.endpointId == endpointId }) {
                rooms.add(DiscoveredRoom(endpointId, info.endpointName))
            }
        }

        override fun onEndpointLost(endpointId: String) {
            rooms.removeAll { it.endpointId == endpointId }
        }
    }

    fun startHosting() {
        isHost = true
        status = NearbyStatus.ADVERTISING
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        client.startAdvertising(localName, SERVICE_ID, lifecycleCallback, options)
            .addOnFailureListener { status = NearbyStatus.ERROR }
    }

    fun startDiscovery() {
        isHost = false
        rooms.clear()
        status = NearbyStatus.DISCOVERING
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        client.startDiscovery(SERVICE_ID, discoveryCallback, options)
            .addOnFailureListener { status = NearbyStatus.ERROR }
    }

    fun connectTo(endpointId: String) {
        status = NearbyStatus.CONNECTING
        client.requestConnection(localName, endpointId, lifecycleCallback)
            .addOnFailureListener { status = NearbyStatus.ERROR }
    }

    fun send(bytes: ByteArray) {
        val id = connectedEndpointId ?: return
        client.sendPayload(id, Payload.fromBytes(bytes))
    }

    fun stop() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpointId = null
        rooms.clear()
        peerName = null
        status = NearbyStatus.IDLE
    }

    companion object {
        private const val SERVICE_ID = "com.rts.rys.ryy.wayfinding.versus"
        private val STRATEGY = Strategy.P2P_POINT_TO_POINT
    }
}
