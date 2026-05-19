package com.rts.rys.ryy.wayfinding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.CustomMazesRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.BallPhysics
import com.rts.rys.ryy.wayfinding.game.Maze
import com.rts.rys.ryy.wayfinding.game.MazeValidationResult
import com.rts.rys.ryy.wayfinding.game.MazeValidator
import com.rts.rys.ryy.wayfinding.game.SquashAxis
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.TiltSensor
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import com.rts.rys.ryy.wayfinding.ui.theme.BallRed
import com.rts.rys.ryy.wayfinding.ui.theme.CoralPink
import com.rts.rys.ryy.wayfinding.ui.theme.CreamBg
import com.rts.rys.ryy.wayfinding.ui.theme.FloorTile
import com.rts.rys.ryy.wayfinding.ui.theme.FloorTileAlt
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGold
import com.rts.rys.ryy.wayfinding.ui.theme.GoalGoldDeep
import com.rts.rys.ryy.wayfinding.ui.theme.InkDark
import com.rts.rys.ryy.wayfinding.ui.theme.InkSoft
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBlue
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop
import com.rts.rys.ryy.wayfinding.ui.theme.SunYellow
import com.rts.rys.ryy.wayfinding.ui.theme.WallGreen
import kotlin.math.cos
import kotlin.math.sin

private fun sizeForLevel(level: Int): Int = when (level) {
    1 -> 9
    2 -> 13
    3 -> 15
    else -> 21
}

private fun initialBoard(level: Int): Array<CharArray> {
    val n = sizeForLevel(level)
    val board = Array(n) { CharArray(n) { ' ' } }
    for (i in 0 until n) {
        board[0][i] = '#'
        board[n - 1][i] = '#'
        board[i][0] = '#'
        board[i][n - 1] = '#'
    }
    board[1][1] = 'S'
    board[n - 2][n - 2] = 'G'
    return board
}

private enum class Tool { WALL, EMPTY, START, GOAL }

@Composable
fun MazeEditorScreen(
    initialLevel: Int,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var level by remember { mutableStateOf(initialLevel.coerceIn(1, 4)) }
    var board by remember(level) { mutableStateOf(initialBoard(level)) }
    var tool by remember { mutableStateOf(Tool.WALL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewMaze by remember { mutableStateOf<Maze?>(null) }

    fun tryPreview() {
        val lines = board.map { String(it) }
        when (val result = MazeValidator.validate(lines)) {
            is MazeValidationResult.Ok -> previewMaze = Maze.fromAscii(lines)
            is MazeValidationResult.Error -> errorMessage = result.message
        }
    }

    fun applyTool(c: Int, r: Int) {
        val n = sizeForLevel(level)
        if (c <= 0 || r <= 0 || c >= n - 1 || r >= n - 1) return
        val newBoard = Array(n) { board[it].copyOf() }
        val cur = newBoard[r][c]
        when (tool) {
            Tool.WALL -> if (cur != 'S' && cur != 'G') newBoard[r][c] = '#'
            Tool.EMPTY -> if (cur != 'S' && cur != 'G') newBoard[r][c] = ' '
            Tool.START -> {
                if (cur == 'G') return
                for (rr in 0 until n) for (cc in 0 until n) {
                    if (newBoard[rr][cc] == 'S') newBoard[rr][cc] = ' '
                }
                newBoard[r][c] = 'S'
            }
            Tool.GOAL -> {
                if (cur == 'S') return
                for (rr in 0 until n) for (cc in 0 until n) {
                    if (newBoard[rr][cc] == 'G') newBoard[rr][cc] = ' '
                }
                newBoard[r][c] = 'G'
            }
        }
        board = newBoard
    }

    fun trySave() {
        val lines = board.map { String(it) }
        when (val result = MazeValidator.validate(lines)) {
            is MazeValidationResult.Ok -> {
                val repo = CustomMazesRepository(context)
                repo.add(level, lines)
                val all = repo.load()
                Stages.setCustomStages(
                    all.groupBy { it.level }.flatMap { (_, ms) ->
                        ms.sortedBy { it.createdAt }
                            .mapIndexed { i, m -> m.toStage(i + 1) }
                    }
                )
                onSaved()
            }
            is MazeValidationResult.Error -> errorMessage = result.message
        }
    }

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
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "나만의 게임 만들기",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel("난이도")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (lv in 1..4) {
                    DifficultyPill(level = lv, selected = lv == level, onClick = { level = lv })
                }
            }
            Spacer(Modifier.height(10.dp))

            SectionLabel("도구")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolPill("벽", tool == Tool.WALL) { tool = Tool.WALL }
                ToolPill("길", tool == Tool.EMPTY) { tool = Tool.EMPTY }
                ToolPill("시작", tool == Tool.START) { tool = Tool.START }
                ToolPill("도착", tool == Tool.GOAL) { tool = Tool.GOAL }
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(CreamBg)
            ) {
                EditorGrid(board = board, onCellTap = ::applyTool)
            }

            Spacer(Modifier.height(12.dp))

            ActionButton(
                label = "미리보기",
                bg = SunYellow,
                modifier = Modifier.fillMaxWidth(),
                onClick = ::tryPreview
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionButton("취소", CoralPink, modifier = Modifier.weight(1f), onClick = onCancel)
                ActionButton("저장", SkyBlue, modifier = Modifier.weight(1f), onClick = ::trySave)
            }
        }

        previewMaze?.let { maze ->
            EditorPreview(
                maze = maze,
                onExit = { previewMaze = null }
            )
        }

        errorMessage?.let { msg ->
            Dialog(onDismissRequest = { errorMessage = null }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = msg,
                            color = InkDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(16.dp))
                        ActionButton(
                            label = "확인",
                            bg = SkyBlue,
                            onClick = { errorMessage = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = InkDark, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun EditorGrid(
    board: Array<CharArray>,
    onCellTap: (Int, Int) -> Unit
) {
    val n = board.size
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(n) {
                detectTapGestures { offset ->
                    val cs = size.width / n
                    val c = (offset.x / cs).toInt()
                    val r = (offset.y / cs).toInt()
                    if (c in 0 until n && r in 0 until n) onCellTap(c, r)
                }
            }
    ) {
        val cs = size.width / n
        for (r in 0 until n) for (c in 0 until n) {
            val ch = board[r][c]
            val tl = Offset(c * cs, r * cs)
            val sz = Size(cs, cs)
            when (ch) {
                '#' -> drawRect(WallGreen, tl, sz)
                ' ' -> {
                    val tile = if ((r + c) % 2 == 0) FloorTile else FloorTileAlt
                    drawRect(tile, tl, sz)
                }
                'S' -> {
                    drawRect(FloorTile, tl, sz)
                    drawCircle(
                        color = BallRed,
                        radius = cs * 0.32f,
                        center = Offset(tl.x + cs / 2f, tl.y + cs / 2f)
                    )
                }
                'G' -> {
                    drawRect(FloorTile, tl, sz)
                    drawStarShape(
                        center = Offset(tl.x + cs / 2f, tl.y + cs / 2f),
                        outerR = cs * 0.4f,
                        innerR = cs * 0.18f,
                        fill = GoalGold,
                        stroke = GoalGoldDeep
                    )
                }
            }
            drawRect(
                color = Color.Black.copy(alpha = 0.08f),
                topLeft = tl,
                size = sz,
                style = Stroke(width = 1f)
            )
        }
    }
}

private fun DrawScope.drawStarShape(
    center: Offset,
    outerR: Float,
    innerR: Float,
    fill: Color,
    stroke: Color
) {
    val path = Path()
    val points = 5
    val step = Math.PI / points
    var angle = -Math.PI / 2
    path.moveTo(
        center.x + outerR * cos(angle).toFloat(),
        center.y + outerR * sin(angle).toFloat()
    )
    for (i in 1 until points * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        angle += step
        path.lineTo(
            center.x + r * cos(angle).toFloat(),
            center.y + r * sin(angle).toFloat()
        )
    }
    path.close()
    drawPath(path = path, color = fill)
    drawPath(path = path, color = stroke, style = Stroke(width = 2f))
}

@Composable
private fun DifficultyPill(level: Int, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) levelColor(level) else Color.White
    val textColor = if (selected) Color.White else InkSoft
    Box(
        modifier = Modifier
            .shadow(if (selected) 4.dp else 2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = "난$level",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ToolPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) SunYellow else Color.White
    val textColor = if (selected) Color.White else InkSoft
    Box(
        modifier = Modifier
            .shadow(if (selected) 4.dp else 2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun EditorPreview(maze: Maze, onExit: () -> Unit) {
    BackHandler { onExit() }

    val context = LocalContext.current
    val physics = remember(maze) { BallPhysics(maze) }
    val tilt = remember { TiltSensor(context) }
    val sensorEnabled by AppSettings.sensorEnabled

    DisposableEffect(sensorEnabled) {
        if (sensorEnabled) tilt.start() else tilt.stop()
        onDispose { tilt.stop() }
    }

    var ballX by remember(maze) { mutableFloatStateOf(physics.x) }
    var ballY by remember(maze) { mutableFloatStateOf(physics.y) }
    var ballRotation by remember(maze) { mutableFloatStateOf(0f) }
    var ballSquash by remember(maze) { mutableFloatStateOf(0f) }
    var ballSquashIsX by remember(maze) { mutableStateOf(false) }
    var reached by remember(maze) { mutableStateOf(false) }
    var resetTrigger by remember { mutableIntStateOf(0) }
    var idleStrength by remember(maze, resetTrigger) { mutableFloatStateOf(1f) }
    var idleTime by remember(maze, resetTrigger) { mutableFloatStateOf(0f) }

    var kx by remember { mutableFloatStateOf(0f) }
    var ky by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(maze, resetTrigger) {
        physics.reset()
        ballX = physics.x
        ballY = physics.y
        ballRotation = 0f
        ballSquash = 0f
        ballSquashIsX = false
        reached = false
        var last = 0L
        while (true) {
            val now = awaitFrame()
            if (last == 0L) { last = now; continue }
            val dt = ((now - last).coerceAtMost(33_000_000L)) / 1_000_000_000f
            last = now

            val sensitivity = AppSettings.sensorSensitivity.value
            val offX = AppSettings.sensorOffsetX.value
            val offY = AppSettings.sensorOffsetY.value
            val sx = if (sensorEnabled) ((tilt.tiltX - offX) * sensitivity).coerceIn(-1f, 1f) else 0f
            val sy = if (sensorEnabled) ((tilt.tiltY - offY) * sensitivity).coerceIn(-1f, 1f) else 0f
            val useKeypad = kx != 0f || ky != 0f
            val ax: Float
            val ay: Float
            if (useKeypad) {
                ax = kx * 18f
                ay = ky * 18f
                physics.maxSpeed = 14f
            } else {
                ax = sx * 36f
                ay = sy * 36f
                physics.maxSpeed = if (sensorEnabled) 22f else 14f
            }

            val didReach = physics.step(dt, ax, ay)
            if (physics.justImpacted && !didReach) SoundManager.playBonk()
            ballX = physics.x
            ballY = physics.y
            ballRotation = physics.rotation
            ballSquash = physics.squashAmount
            ballSquashIsX = physics.squashAxis == SquashAxis.X
            val speed = sqrt(physics.vx * physics.vx + physics.vy * physics.vy)
            val targetIdle = if (!reached && speed < 0.5f) 1f else 0f
            idleStrength += (targetIdle - idleStrength) * (dt * 2.5f).coerceIn(0f, 1f)
            idleTime += dt
            if (didReach && !reached) {
                reached = true
                SoundManager.playGoal()
                delay(1200)
                resetTrigger++
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
            .padding(top = 28.dp, bottom = 20.dp, start = 14.dp, end = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                BackChip(
                    onClick = onExit,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = "미리보기",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(CreamBg)
            ) {
                val breath = 1f + 0.045f * idleStrength * sin(idleTime * (2f * Math.PI.toFloat() / 1.6f))
                MazeCanvas(
                    maze = maze,
                    ballX = ballX,
                    ballY = ballY,
                    rotation = ballRotation,
                    squashAmount = ballSquash,
                    squashAxisIsX = ballSquashIsX,
                    ballScale = breath,
                    modifier = Modifier.fillMaxSize()
                )
                if (reached) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ 잘 통과해요!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoalGold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            DPad(
                onInput = { dx, dy ->
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 0f) {
                        kx = dx / len
                        ky = dy / len
                    } else {
                        kx = 0f; ky = 0f
                    }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 6.dp)
                    .alpha(if (sensorEnabled) 0.45f else 1f)
            )
        }
    }
}
