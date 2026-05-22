package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rts.rys.ryy.wayfinding.data.AchievementsRepository
import com.rts.rys.ryy.wayfinding.data.Badge
import com.rts.rys.ryy.wayfinding.data.Badges
import com.rts.rys.ryy.wayfinding.data.BallSkin
import com.rts.rys.ryy.wayfinding.data.BallSkins
import com.rts.rys.ryy.wayfinding.data.RecordsRepository
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop

private enum class CollectionTab { BADGES, SKINS }

@Composable
fun CollectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(CollectionTab.BADGES) }
    var unlocked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var currentSkinId by remember { mutableStateOf(BallSkins.DEFAULT.id) }

    LaunchedEffect(Unit) {
        // 화면 진입 시 현재 데이터 기준으로 배지 평가하여 누락분 보정 저장
        val records = RecordsRepository(context).load()
        val customCount = Stages.customStages.value.size
        val current = Badges.evaluate(records, customCount)
        val repo = AchievementsRepository(context)
        val saved = repo.loadUnlockedBadges()
        val merged = saved + current
        if (merged != saved) repo.saveUnlockedBadges(merged)
        unlocked = merged
        currentSkinId = repo.loadCurrentSkinId()
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "내 도감",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabChip(
                    label = "배지 ${unlocked.size}/${Badges.ALL.size}",
                    selected = tab == CollectionTab.BADGES,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = CollectionTab.BADGES }
                )
                TabChip(
                    label = "공 스킨",
                    selected = tab == CollectionTab.SKINS,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = CollectionTab.SKINS }
                )
            }
            Spacer(Modifier.height(14.dp))

            when (tab) {
                CollectionTab.BADGES -> BadgesGrid(unlocked, navBottom)
                CollectionTab.SKINS -> SkinsGrid(
                    unlockedBadges = unlocked,
                    currentSkinId = currentSkinId,
                    navBottom = navBottom,
                    onSelect = { skin ->
                        if (BallSkins.isUnlocked(skin, unlocked)) {
                            currentSkinId = skin.id
                            AchievementsRepository(context).saveCurrentSkinId(skin.id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BadgesGrid(unlocked: Set<String>, navBottom: androidx.compose.ui.unit.Dp) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + navBottom)
    ) {
        items(Badges.ALL, key = { it.id }) { badge ->
            BadgeCell(badge = badge, unlocked = badge.id in unlocked)
        }
    }
}

@Composable
private fun BadgeCell(badge: Badge, unlocked: Boolean) {
    val color = if (unlocked) Color(badge.colorArgb) else Color(0xFFBDB7B0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (unlocked) badge.emoji else "?",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = badge.title,
            color = if (unlocked) InkDark else InkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = badge.description,
            color = InkSoft,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun SkinsGrid(
    unlockedBadges: Set<String>,
    currentSkinId: String,
    navBottom: androidx.compose.ui.unit.Dp,
    onSelect: (BallSkin) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + navBottom)
    ) {
        items(BallSkins.ALL, key = { it.id }) { skin ->
            val unlocked = BallSkins.isUnlocked(skin, unlockedBadges)
            val selected = skin.id == currentSkinId
            SkinCell(
                skin = skin,
                unlocked = unlocked,
                selected = selected,
                unlockHint = unlockHintFor(skin),
                onClick = { onSelect(skin) }
            )
        }
    }
}

private fun unlockHintFor(skin: BallSkin): String {
    val badgeId = skin.unlockBadgeId ?: return ""
    val badge = Badges.byId(badgeId) ?: return ""
    return "${badge.title} 배지 획득"
}

@Composable
private fun SkinCell(
    skin: BallSkin,
    unlocked: Boolean,
    selected: Boolean,
    unlockHint: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) skin.deepColor else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(enabled = unlocked, onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (unlocked) Color.White else Color(0xFFEDEAE5)),
            contentAlignment = Alignment.Center
        ) {
            if (unlocked) BallPreview(skin) else {
                Text("🔒", fontSize = 28.sp, color = InkSoft)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = skin.name,
            color = if (unlocked) InkDark else InkSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                selected -> "선택됨"
                unlocked -> "탭하면 선택"
                else -> unlockHint
            },
            color = if (selected) skin.deepColor else InkSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (borderColor != Color.Transparent) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(borderColor)
            )
        }
    }
}

@Composable
private fun BallPreview(skin: BallSkin) {
    Canvas(modifier = Modifier.size(64.dp)) {
        val r = size.minDimension * 0.45f
        val cx = size.width / 2f
        val cy = size.height / 2f
        // 본체 그라데이션
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, skin.coreColor, skin.deepColor),
                center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                radius = r * 1.5f
            ),
            center = Offset(cx, cy),
            radius = r
        )
        // 하이라이트
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            center = Offset(cx - r * 0.38f, cy - r * 0.38f),
            radius = r * 0.20f
        )
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) SkyBlue else Color.White
    val fg = if (selected) Color.White else InkSoft
    Box(
        modifier = modifier
            .shadow(if (selected) 4.dp else 2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}
