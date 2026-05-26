package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow

private data class TutorialPage(
    val emoji: String,
    val title: String,
    val body: String,
    val accent: Color,
)

private val pages = listOf(
    TutorialPage("📱", "공을 굴려요", "휴대폰을 살짝 기울이면\n공이 그쪽으로 굴러가요", SkyBlue),
    TutorialPage("🎮", "버튼으로도 OK", "화면 아래 버튼으로도\n공을 굴릴 수 있어요", SunYellow),
    TutorialPage("⭐", "별을 모아요", "빨리 도착할수록\n별을 많이 받아요", CoralPink),
    TutorialPage("🏁", "도착하면 끝!", "초록 깃발까지\n공을 굴려보세요", Color(0xFF66BB6A)),
)

@Composable
fun TutorialScreen(onFinished: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val current = pages[index]
    val isLast = index == pages.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp)
    ) {
        SkipButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = onFinished
        )

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut()) using
                    SizeTransform(clip = false)
            },
            modifier = Modifier.align(Alignment.Center),
            label = "tutorial-page"
        ) { i ->
            TutorialPageContent(pages[i])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DotsIndicator(count = pages.size, current = index, accent = current.accent)
            Spacer(Modifier.height(20.dp))
            NextButton(
                label = if (isLast) "시작하기" else "다음",
                bg = current.accent,
                onClick = {
                    if (isLast) onFinished() else index += 1
                }
            )
        }
    }
}

@Composable
private fun TutorialPageContent(page: TutorialPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(page.emoji, fontSize = 80.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            color = InkDark,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.body,
            color = InkSoft,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun DotsIndicator(count: Int, current: Int, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until count) {
            val isCurrent = i == current
            Box(
                modifier = Modifier
                    .size(width = if (isCurrent) 22.dp else 10.dp, height = 10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isCurrent) accent else Color.White.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun NextButton(label: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun SkipButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "건너뛰기",
            color = InkSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
