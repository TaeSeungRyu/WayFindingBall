package com.rts.rys.ryy.wayfinding.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
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
import com.rts.rys.ryy.wayfinding.data.StarWallet
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
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
    var purchased by remember { mutableStateOf<Set<String>>(emptySet()) }
    var currentSkinId by remember { mutableStateOf(BallSkins.DEFAULT.id) }
    var balance by remember { mutableStateOf(0) }
    var pendingPurchase by remember { mutableStateOf<BallSkin?>(null) }

    LaunchedEffect(Unit) {
        // 실제 데이터 기준으로 배지를 다시 평가하여 SharedPreferences를 덮어쓴다.
        // (이전 임시 전체해제 흔적을 정리)
        val records = RecordsRepository(context).load()
        val customCount = Stages.customStages.value.size
        val current = Badges.evaluate(records, customCount)
        val repo = AchievementsRepository(context)
        repo.saveUnlockedBadges(current)
        unlocked = current
        purchased = repo.loadPurchasedSkins()
        val wallet = StarWallet(context)
        wallet.migrateOnce(records)
        balance = wallet.balance()
        // 현재 선택된 스킨이 잠긴 상태로 되돌아오면 기본 공으로 복귀
        val savedSkin = BallSkins.byId(repo.loadCurrentSkinId())
        if (!BallSkins.isUnlocked(savedSkin, current, purchased)) {
            repo.saveCurrentSkinId(BallSkins.DEFAULT.id)
            currentSkinId = BallSkins.DEFAULT.id
        } else {
            currentSkinId = savedSkin.id
        }
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
                CollectionTab.SKINS -> {
                    StarBalanceBar(balance = balance)
                    Spacer(Modifier.height(10.dp))
                    SkinsGrid(
                        unlockedBadges = unlocked,
                        purchasedSkins = purchased,
                        currentSkinId = currentSkinId,
                        navBottom = navBottom,
                        onSelect = { skin ->
                            if (BallSkins.isUnlocked(skin, unlocked, purchased)) {
                                currentSkinId = skin.id
                                AchievementsRepository(context).saveCurrentSkinId(skin.id)
                            }
                        },
                        onPurchase = { skin -> pendingPurchase = skin }
                    )
                }
            }
        }

        pendingPurchase?.let { skin ->
            PurchaseConfirmDialog(
                skin = skin,
                balance = balance,
                onCancel = { pendingPurchase = null },
                onConfirm = {
                    val price = skin.priceStars
                    if (price == null) {
                        pendingPurchase = null
                        return@PurchaseConfirmDialog
                    }
                    val wallet = StarWallet(context)
                    if (wallet.spend(price)) {
                        val repo = AchievementsRepository(context)
                        repo.markSkinPurchased(skin.id)
                        repo.saveCurrentSkinId(skin.id)
                        purchased = purchased + skin.id
                        currentSkinId = skin.id
                        balance = wallet.balance()
                        Toast.makeText(
                            context,
                            "${skin.name}을 샀어요!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "별이 부족해요 (보유 ${wallet.balance()} / 필요 $price)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    pendingPurchase = null
                }
            )
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
    purchasedSkins: Set<String>,
    currentSkinId: String,
    navBottom: androidx.compose.ui.unit.Dp,
    onSelect: (BallSkin) -> Unit,
    onPurchase: (BallSkin) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + navBottom)
    ) {
        items(BallSkins.ALL, key = { it.id }) { skin ->
            val unlocked = BallSkins.isUnlocked(skin, unlockedBadges, purchasedSkins)
            val selected = skin.id == currentSkinId
            SkinCell(
                skin = skin,
                unlocked = unlocked,
                selected = selected,
                unlockHint = unlockHintFor(skin),
                onClick = { onSelect(skin) },
                onPurchase = { onPurchase(skin) },
            )
        }
    }
}

private fun unlockHintFor(skin: BallSkin): String {
    skin.unlockBadgeId?.let { id ->
        val badge = Badges.byId(id) ?: return ""
        return "${badge.title} 배지 획득"
    }
    skin.priceStars?.let { return "별 ${it}개로 구매" }
    return ""
}

@Composable
private fun StarBalanceBar(balance: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "★",
            color = GoalGold,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "내 별",
            color = InkSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "$balance",
            color = InkDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "별을 모아 새 공을 사요",
            color = InkSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SkinCell(
    skin: BallSkin,
    unlocked: Boolean,
    selected: Boolean,
    unlockHint: String,
    onClick: () -> Unit,
    onPurchase: () -> Unit,
) {
    val borderColor = if (selected) skin.deepColor else Color.Transparent
    val showBuyButton = !unlocked && skin.priceStars != null
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
        if (showBuyButton) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CoralPink)
                    .clickable(onClick = onPurchase)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("★", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.size(4.dp))
                Text(
                    "${skin.priceStars} 구매",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
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
private fun PurchaseConfirmDialog(
    skin: BallSkin,
    balance: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val price = skin.priceStars ?: 0
    val canAfford = balance >= price
    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF6F2EC)),
                    contentAlignment = Alignment.Center
                ) {
                    BallPreview(skin)
                }
                Text(
                    text = skin.name,
                    color = InkDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★", color = GoalGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "$price 별",
                        color = InkDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = if (canAfford) "별 ${price}개로 살까요?" else "별이 부족해요 (보유 $balance)",
                    color = if (canAfford) InkSoft else CoralPink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkyBlue)
                            .clickable(onClick = onCancel),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("취소", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                    val buyBg = if (canAfford) CoralPink else Color(0xFFBDBDBD)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(buyBg)
                            .clickable(enabled = canAfford, onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("살래요", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BallPreview(skin: BallSkin) {
    val infinite = rememberInfiniteTransition(label = "skinPreview")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier = Modifier.size(72.dp)) {
        val r = size.minDimension * 0.28f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawBallDecoration(skin, cx, cy, r, phase)
        drawBallBody(skin, cx, cy, r)
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
