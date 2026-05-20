package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.Lavender
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onRecords: () -> Unit,
    onCreate: () -> Unit
) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
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

            Spacer(Modifier.height(56.dp))

            BigButton(
                label = "놀러 가기",
                emoji = "▶",
                bg = CoralPink,
                onClick = onStart
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
                label = "나만의 게임 만들기",
                emoji = "＋",
                bg = Lavender,
                onClick = onCreate
            )

            Spacer(Modifier.height(24.dp))
            SettingRow(
                label = "기울기 센서",
                enabled = AppSettings.sensorEnabled.value,
                onSet = { AppSettings.setSensorEnabled(it) }
            )
            if (AppSettings.sensorEnabled.value) {
                Spacer(Modifier.height(8.dp))
                SensitivityRow()
            }
            Spacer(Modifier.height(10.dp))
            SettingRow(
                label = "소리",
                enabled = AppSettings.soundEnabled.value,
                onSet = { AppSettings.setSoundEnabled(it) }
            )
            Spacer(Modifier.height(10.dp))
            SettingRow(
                label = "음악",
                enabled = AppSettings.bgmEnabled.value,
                onSet = {
                    AppSettings.setBgmEnabled(it)
                    SoundManager.applyBgmEnabled()
                }
            )
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
private fun BigButton(
    label: String,
    emoji: String,
    bg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
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
                .size(54.dp)
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
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}
