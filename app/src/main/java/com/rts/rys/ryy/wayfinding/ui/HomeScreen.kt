package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.ui.theme.DeepNight
import com.rts.rys.ryy.wayfinding.ui.theme.MidNight
import com.rts.rys.ryy.wayfinding.ui.theme.NeonCyan
import com.rts.rys.ryy.wayfinding.ui.theme.NeonPink
import com.rts.rys.ryy.wayfinding.ui.theme.NeonYellow
import com.rts.rys.ryy.wayfinding.ui.theme.SoftWhite

@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onRecords: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(MidNight, DeepNight))
            )
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MAZE",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SoftWhite,
                letterSpacing = 10.sp
            )
            Text(
                text = "BALL",
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonCyan,
                letterSpacing = 10.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "기울여 굴려라",
                fontSize = 14.sp,
                color = SoftWhite.copy(alpha = 0.7f),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(64.dp))

            HomeButton(
                label = "게임 시작",
                color = NeonPink,
                onClick = onStart
            )
            Spacer(Modifier.height(20.dp))
            HomeButton(
                label = "기록",
                color = NeonYellow,
                onClick = onRecords
            )
        }
    }
}

@Composable
private fun HomeButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.18f),
            contentColor = color
        ),
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(64.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp), ambientColor = color, spotColor = color)
            .border(2.dp, color, RoundedCornerShape(18.dp))
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}
