package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue

@Composable
fun DPad(
    onInput: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val up = remember { mutableStateOf(false) }
    val down = remember { mutableStateOf(false) }
    val left = remember { mutableStateOf(false) }
    val right = remember { mutableStateOf(false) }

    LaunchedEffect(up.value, down.value, left.value, right.value) {
        val dx = (if (right.value) 1f else 0f) - (if (left.value) 1f else 0f)
        val dy = (if (down.value) 1f else 0f) - (if (up.value) 1f else 0f)
        onInput(dx, dy)
    }

    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DirButton("▲", up)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirButton("◀", left)
                Spacer(Modifier.size(64.dp))
                DirButton("▶", right)
            }
            DirButton("▼", down)
        }
    }
}

@Composable
private fun DirButton(glyph: String, pressed: MutableState<Boolean>) {
    val bg = if (pressed.value) CoralPink else SkyBlue
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(if (pressed.value) 2.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed.value = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed.value = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
