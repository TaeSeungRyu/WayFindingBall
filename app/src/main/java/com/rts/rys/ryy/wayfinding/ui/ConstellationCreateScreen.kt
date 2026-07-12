package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rts.rys.ryy.wayfinding.data.CustomConstellationRepository
import com.rts.rys.ryy.wayfinding.game.ConstellationStar
import kotlin.random.Random

private val NightTop = Color(0xFF050B25)
private val NightBottom = Color(0xFF1B2A66)
private val NightInk = Color(0xFFE7E9FF)
private val GoldRing = Color(0xFFFFD66B)

/** 별자리 하나에 담을 수 있는 최소/최대 별 개수. */
private const val MIN_STARS = 3
private const val MAX_STARS = 12

private val EmojiChoices = listOf("🌟", "💫", "⭐", "✨", "🌙", "🪐", "🐻", "🐰", "🦁", "🐟", "🌈", "❤️")

@Composable
fun ConstellationCreateScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { CustomConstellationRepository(context) }

    // 찍은 별들 — 정규 좌표(0..1). 찍은 순서가 곧 연결 순서.
    val stars = remember { mutableStateListOf<Offset>() }
    var showSaveDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 4.dp),
            ) {
                BackChip(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "별자리 만들기",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NightInk,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "밤하늘을 탭해서 별을 놓아요. 놓은 순서대로 이어져요 ✨",
                color = NightInk.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            StarCanvas(
                stars = stars,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    label = "되돌리기",
                    bg = Color.White.copy(alpha = 0.14f),
                    enabled = stars.isNotEmpty(),
                    onClick = { if (stars.isNotEmpty()) stars.removeAt(stars.lastIndex) },
                    modifier = Modifier.weight(1f),
                )
                ControlButton(
                    label = "전체 지우기",
                    bg = Color.White.copy(alpha = 0.14f),
                    enabled = stars.isNotEmpty(),
                    onClick = { stars.clear() },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            ControlButton(
                label = if (stars.size < MIN_STARS) "별 ${MIN_STARS}개 이상 놓아요 (${stars.size}/$MAX_STARS)"
                else "이름 짓고 저장하기",
                bg = GoldRing,
                enabled = stars.size >= MIN_STARS,
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, emoji ->
                repo.add(
                    name = name,
                    emoji = emoji,
                    stars = stars.mapIndexed { i, o -> ConstellationStar(o.x, o.y, i + 1) },
                )
                showSaveDialog = false
                onSaved()
            },
        )
    }
}

@Composable
private fun StarCanvas(
    stars: SnapshotStateList<Offset>,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val ambient = remember {
        val r = Random(777)
        List(70) { Triple(r.nextFloat(), r.nextFloat(), 0.3f + r.nextFloat() * 0.5f) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF050B25), Color(0xFF14215C))))
            .border(1.dp, GoldRing.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val w = canvasSize.width
                    val h = canvasSize.height
                    if (w > 0 && h > 0 && stars.size < MAX_STARS) {
                        stars.add(
                            Offset(
                                (off.x / w).coerceIn(0.04f, 0.96f),
                                (off.y / h).coerceIn(0.04f, 0.96f),
                            )
                        )
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minSide = minOf(w, h)

            for ((bx, by, br) in ambient) {
                drawCircle(
                    color = Color.White.copy(alpha = br * 0.4f),
                    radius = minSide * 0.004f,
                    center = Offset(bx * w, by * h),
                )
            }

            // 연결선(놓은 순서대로).
            for (i in 1 until stars.size) {
                drawLine(
                    color = GoldRing.copy(alpha = 0.8f),
                    start = Offset(stars[i - 1].x * w, stars[i - 1].y * h),
                    end = Offset(stars[i].x * w, stars[i].y * h),
                    strokeWidth = minSide * 0.007f,
                    cap = StrokeCap.Round,
                )
            }
            // 별 점.
            for (s in stars) {
                val c = Offset(s.x * w, s.y * h)
                drawCircle(color = GoldRing.copy(alpha = 0.35f), radius = minSide * 0.028f, center = c)
                drawCircle(color = Color.White, radius = minSide * 0.016f, center = c)
            }
        }

        if (stars.isEmpty()) {
            Text(
                text = "여기를 탭해서\n첫 별을 놓아요 ⭐",
                color = NightInk.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun SaveDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(EmojiChoices.first()) }
    val trimmed = name.trim()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(NightTop, NightBottom)))
                .border(2.dp, GoldRing, RoundedCornerShape(28.dp))
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "✨ 별자리 이름 짓기",
                    color = NightInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "이름을 넣으면 \"○○자리\"가 돼요",
                    color = NightInk.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                NameField(value = name, onChange = { name = it.take(8) })
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (trimmed.isEmpty()) " " else "${trimmed}자리",
                    color = GoldRing,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "그림 고르기",
                    color = NightInk.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                EmojiPicker(selected = emoji, onSelect = { emoji = it })

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ControlButton(
                        label = "취소",
                        bg = Color.White.copy(alpha = 0.14f),
                        enabled = true,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    ControlButton(
                        label = "저장",
                        bg = GoldRing,
                        enabled = trimmed.isNotEmpty(),
                        onClick = { onConfirm(trimmed, emoji) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, GoldRing.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(GoldRing),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "이름 입력",
                        color = NightInk.copy(alpha = 0.4f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EmojiChoices.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { e ->
                    val isSel = e == selected
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSel) GoldRing.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .let {
                                if (isSel) it.border(2.dp, GoldRing, CircleShape) else it
                            }
                            .clickable { onSelect(e) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    bg: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (bg == GoldRing) Color(0xFF2A1A00) else Color.White
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) textColor else textColor.copy(alpha = 0.6f),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
