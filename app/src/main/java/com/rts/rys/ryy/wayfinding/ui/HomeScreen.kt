package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.DailyRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import java.time.LocalDate

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onDaily: () -> Unit,
    onRecords: () -> Unit,
    onCollection: () -> Unit,
    onVersus: () -> Unit = {},
    onTutorial: () -> Unit = {},
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val dailyRepo = remember { DailyRepository(context) }
    val dailyCleared = remember { dailyRepo.bestFor(today) != null }
    val streak = remember { dailyRepo.streak(today) }
    var showSettings by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        // 배경 장식 - 구름
        Canvas(modifier = Modifier
            .size(160.dp)
            .padding(top = 40.dp, start = 12.dp)
            .align(Alignment.TopStart)) {
            val cy = size.height / 2f
            drawCircle(color = Color.White, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.3f, cy))
            drawCircle(color = Color.White, radius = size.minDimension * 0.28f, center = Offset(size.width * 0.5f, cy - 14f))
            drawCircle(color = Color.White, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.7f, cy))
        }
        Canvas(modifier = Modifier
            .size(110.dp)
            .padding(top = 80.dp, end = 12.dp)
            .align(Alignment.TopEnd)) {
            drawCircle(
                color = SunYellow.copy(alpha = 0.35f),
                radius = size.minDimension / 2f,
                center = center
            )
            drawCircle(color = SunYellow, radius = size.minDimension / 3f, center = center)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            Text(
                text = "또르르 미로",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkDark,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "기울이거나 버튼으로 공을 굴려요",
                fontSize = 14.sp,
                color = InkSoft,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(25.dp))

            BigButton(
                label = "놀러 가기",
                emoji = "▶",
                bg = CoralPink,
                onClick = onStart
            )
            Spacer(Modifier.height(20.dp))
            DailyButton(
                cleared = dailyCleared,
                streak = streak,
                onClick = onDaily
            )
            Spacer(Modifier.height(20.dp))
            BigButton(
                label = "내 기록",
                emoji = "★",
                bg = SunYellow,
                onClick = onRecords
            )
            Spacer(Modifier.height(20.dp))
            BigButton(
                label = "내 도감",
                emoji = "🏅",
                bg = SkyBlue,
                onClick = onCollection
            )
            // 1:1 대전모드 — Nearby Connections는 위치 권한 없이 동작하려면 API 32+ 필요.
            // 구버전 기기에서는 '지원 불가' 버튼으로 교체해 보여준다.
            Spacer(Modifier.height(20.dp))
            if (android.os.Build.VERSION.SDK_INT >= 32) {
                BigButton(
                    label = "1:1 대전모드",
                    emoji = "🤝",
                    bg = CoralPink,
                    onClick = onVersus
                )
            } else {
                DisabledBigButton(
                    label = "대전모드\n이 기기에서는 지원 불가",
                    emoji = "🤝"
                )
            }

            Spacer(Modifier.height(24.dp))
            SettingsButton(onClick = { showSettings = true })
            }
        }
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚙", fontSize = 20.sp)
        Spacer(Modifier.size(8.dp))
        Text(
            text = "설정",
            color = InkDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("설정", color = InkDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(20.dp))
            SettingRow(
                label = "기울기 센서",
                enabled = AppSettings.sensorEnabled.value,
                onSet = { AppSettings.setSensorEnabled(it) }
            )
            if (AppSettings.sensorEnabled.value) {
                Spacer(Modifier.height(10.dp))
                SensitivityRow()
            }
            Spacer(Modifier.height(14.dp))
            SettingRow(
                label = "소리",
                enabled = AppSettings.soundEnabled.value,
                onSet = { AppSettings.setSoundEnabled(it) }
            )
            Spacer(Modifier.height(14.dp))
            SettingRow(
                label = "음악",
                enabled = AppSettings.bgmEnabled.value,
                onSet = {
                    AppSettings.setBgmEnabled(it)
                    SoundManager.applyBgmEnabled()
                }
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SkyBlue)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("닫기", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun SensitivityRow() {
    val current = AppSettings.sensorSensitivity.value
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "민감도",
            color = InkDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.size(12.dp))
        SensorSegment(
            label = "낮음",
            selected = current < 0.85f,
            selectedColor = SkyBlue,
            onClick = { AppSettings.setSensorSensitivity(0.7f) }
        )
        Spacer(Modifier.size(6.dp))
        SensorSegment(
            label = "보통",
            selected = current in 0.85f..1.15f,
            selectedColor = SkyBlue,
            onClick = { AppSettings.setSensorSensitivity(1.0f) }
        )
        Spacer(Modifier.size(6.dp))
        SensorSegment(
            label = "높음",
            selected = current > 1.15f,
            selectedColor = SkyBlue,
            onClick = { AppSettings.setSensorSensitivity(1.4f) }
        )
    }
}

@Composable
private fun SettingRow(label: String, enabled: Boolean, onSet: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = InkDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.size(12.dp))
        SensorSegment(
            label = "사용",
            selected = enabled,
            selectedColor = SkyBlue,
            onClick = { onSet(true) }
        )
        Spacer(Modifier.size(8.dp))
        SensorSegment(
            label = "사용 안 함",
            selected = !enabled,
            selectedColor = CoralPink,
            onClick = { onSet(false) }
        )
    }
}

@Composable
private fun SensorSegment(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bg = if (selected) selectedColor else Color.White
    val textColor = if (selected) Color.White else InkSoft
    Box(
        modifier = Modifier
            .shadow(if (selected) 4.dp else 2.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun DailyButton(
    cleared: Boolean,
    streak: Int,
    onClick: () -> Unit
) {
    val bg = GoalGold
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (cleared) "✓" else "☼",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "오늘의 도전",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            val sub = when {
                cleared && streak > 1 -> "오늘 완료 · 🔥 ${streak}일 연속"
                cleared -> "오늘 완료!"
                streak > 0 -> "🔥 ${streak}일 연속 도전 중"
                else -> "매일 새 미로가 나와요"
            }
            Text(
                text = sub,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BigButton(
    label: String,
    emoji: String,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun DisabledBigButton(
    label: String,
    emoji: String
) {
    val disabledBg = Color(0xFFB8B5C6)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(disabledBg)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, color = Color.White.copy(alpha = 0.7f), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
    }
}
