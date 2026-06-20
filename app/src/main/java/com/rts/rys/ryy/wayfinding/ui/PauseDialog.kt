package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.Lavender
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    sensorEnabled: Boolean? = null,
    onToggleSensor: (() -> Unit)? = null,
    onCalibrate: (() -> Unit)? = null,
) {
    val showSensor = sensorEnabled != null && onToggleSensor != null
    val showCalibrate = showSensor && sensorEnabled == true && onCalibrate != null
    val height = when {
        showCalibrate -> 420.dp
        showSensor -> 360.dp
        else -> 320.dp
    }
    Dialog(onDismissRequest = onResume) {
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = height)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("잠깐 멈췄어요", color = InkDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showSensor) {
                        PauseTogglePill(
                            label = if (sensorEnabled == true) "센서 ON" else "센서 OFF",
                            enabled = sensorEnabled == true,
                            onClick = { onToggleSensor!!.invoke() }
                        )
                    }
                    PauseTogglePill(
                        label = if (soundEnabled) "소리 ON" else "소리 OFF",
                        enabled = soundEnabled,
                        onClick = onToggleSound
                    )
                }
                if (showCalibrate) {
                    PauseDialogButton(
                        label = "지금 각도를 가운데로",
                        bg = Lavender,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCalibrate!!.invoke() }
                    )
                }
                PauseDialogButton(
                    label = "다시 시작",
                    bg = SunYellow,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestart
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    PauseDialogButton("그만할래요", CoralPink, modifier = Modifier.weight(1f), onClick = onExit)
                    PauseDialogButton("계속해요", SkyBlue, modifier = Modifier.weight(1f), onClick = onResume)
                }
            }
        }
    }
}

@Composable
private fun PauseTogglePill(label: String, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) SkyBlue else InkSoft
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun PauseDialogButton(label: String, bg: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
