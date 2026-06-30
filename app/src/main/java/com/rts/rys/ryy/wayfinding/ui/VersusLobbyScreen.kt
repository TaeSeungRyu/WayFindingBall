package com.rts.rys.ryy.wayfinding.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rts.rys.ryy.wayfinding.net.NearbyManager
import com.rts.rys.ryy.wayfinding.net.NearbyStatus
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen

private fun versusPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
    return list.toTypedArray()
}

private fun hasPermissions(context: Context): Boolean =
    versusPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private fun gameTitle(game: Char): String = when (game) {
    'A' -> "미로 찾기"
    'B' -> "색깔 찾기"
    'C' -> "굴려서 맞히기"
    'D' -> "별자리 잇기"
    else -> "대전"
}

@Composable
fun VersusLobbyScreen(
    game: Char,
    manager: NearbyManager,
    onBack: () -> Unit,
    onMatchReady: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasPermissions(context)) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted = hasPermissions(context) }

    // 연결되면 매치 화면으로
    LaunchedEffect(manager.status) {
        if (manager.status == NearbyStatus.CONNECTED) onMatchReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                BackChip(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = gameTitle(game),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(20.dp))

            when {
                !granted -> PermissionPanel(onRequest = { launcher.launch(versusPermissions()) })
                manager.status == NearbyStatus.ADVERTISING -> WaitingPanel()
                manager.status == NearbyStatus.DISCOVERING -> RoomListPanel(manager, onPick = { manager.connectTo(it) })
                manager.status == NearbyStatus.CONNECTING -> InfoPanel("연결 중이에요…")
                manager.status == NearbyStatus.ERROR -> InfoPanel("연결에 문제가 생겼어요. 뒤로 갔다 다시 시도해요.")
                else -> ChoicePanel(
                    onHost = { manager.startHosting() },
                    onJoin = { manager.startDiscovery() }
                )
            }
        }
    }
}

@Composable
private fun PermissionPanel(onRequest: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "가까운 친구와 놀려면\n블루투스 권한이 필요해요",
            color = InkDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        Spacer(Modifier.height(20.dp))
        BigActionButton(label = "권한 허용하기", bg = SkyBlue, onClick = onRequest)
    }
}

@Composable
private fun ChoicePanel(onHost: () -> Unit, onJoin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        BigActionButton(label = "방 만들기", emoji = "➕", bg = CoralPink, onClick = onHost)
        BigActionButton(label = "방 참여하기", emoji = "🔍", bg = WallGreen, onClick = onJoin)
    }
}

@Composable
private fun WaitingPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🛜", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "친구를 기다리는 중…",
            color = InkDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "친구는 \"방 참여하기\"를 눌러요",
            color = InkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RoomListPanel(manager: NearbyManager, onPick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "찾은 방",
            color = InkDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        if (manager.rooms.isEmpty()) {
            Text(
                text = "방을 찾는 중이에요…",
                color = InkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(manager.rooms, key = { it.endpointId }) { room ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .clickable { onPick(room.endpointId) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SkyBlue),
                            contentAlignment = Alignment.Center
                        ) { Text("🙂", fontSize = 20.sp) }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = room.name,
                            color = InkDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("참여", color = WallGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPanel(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = InkDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BigActionButton(label: String, emoji: String? = null, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.size(12.dp))
        }
        Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}
