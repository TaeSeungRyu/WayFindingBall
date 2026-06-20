package com.rts.rys.ryy.wayfinding

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.CustomMazesRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.Constellation
import com.rts.rys.ryy.wayfinding.game.DailyChallenge
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.game.Zodiac
import com.rts.rys.ryy.wayfinding.ui.CollectionScreen
import com.rts.rys.ryy.wayfinding.ui.ColorGameScreen
import com.rts.rys.ryy.wayfinding.ui.ColorStageSelectScreen
import com.rts.rys.ryy.wayfinding.ui.ConstellationDexScreen
import com.rts.rys.ryy.wayfinding.ui.ConstellationGameScreen
import com.rts.rys.ryy.wayfinding.ui.ConstellationStageSelectScreen
import com.rts.rys.ryy.wayfinding.ui.GameScreen
import com.rts.rys.ryy.wayfinding.ui.HitGameScreen
import com.rts.rys.ryy.wayfinding.ui.HitStageSelectScreen
import com.rts.rys.ryy.wayfinding.ui.HomeScreen
import com.rts.rys.ryy.wayfinding.ui.LevelSelectScreen
import com.rts.rys.ryy.wayfinding.ui.MazeEditorScreen
import com.rts.rys.ryy.wayfinding.ui.ModeSelectScreen
import com.rts.rys.ryy.wayfinding.ui.RecordsScreen
import com.rts.rys.ryy.wayfinding.ui.ResultScreen
import com.rts.rys.ryy.wayfinding.ui.SplashScreen
import com.rts.rys.ryy.wayfinding.ui.StageSelectScreen
import com.rts.rys.ryy.wayfinding.ui.TutorialScreen
import com.rts.rys.ryy.wayfinding.ui.theme.ChildrenWayfindingTheme
import com.rts.rys.ryy.wayfinding.ui.theme.SkyBottom
import com.rts.rys.ryy.wayfinding.ui.theme.SkyTop

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(applicationContext)
        SoundManager.init(applicationContext)
        loadCustomMazes(applicationContext)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            ChildrenWayfindingTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
                ) {
                    MazeApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SoundManager.applyBgmEnabled()
    }

    override fun onPause() {
        super.onPause()
        SoundManager.pauseBgm()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.stopBgm()
    }
}

private sealed class Screen {
    data object Home : Screen()
    data object ModeSelect : Screen()
    data object ColorStageSelect : Screen()
    data class ColorGame(val level: Int) : Screen()
    data object HitStageSelect : Screen()
    data class HitGame(val level: Int) : Screen()
    data object ConstellationStageSelect : Screen()
    data class ConstellationGame(val stageKey: String, val recordKey: String) : Screen()
    data object ConstellationDex : Screen()
    data object LevelSelect : Screen()
    data class StageSelect(val level: Int) : Screen()
    data class Game(val stageId: Int) : Screen()
    data class Result(
        val stageId: Int,
        val elapsedMs: Long,
        val caught: Boolean,
        val clears: Int
    ) : Screen()
    data object Records : Screen()
    data object Collection : Screen()
    data class Editor(val level: Int) : Screen()
}

@Composable
fun MazeApp() {
    var showSplash by remember { mutableStateOf(true) }
    var showTutorial by remember { mutableStateOf(!AppSettings.tutorialSeen.value) }
    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }
    if (showTutorial) {
        TutorialScreen(onFinished = {
            AppSettings.setTutorialSeen(true)
            showTutorial = false
        })
        return
    }

    val activity = LocalContext.current as? Activity
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var forward by remember { mutableStateOf(true) }

    fun push(screen: Screen) {
        forward = true
        backStack.add(screen)
    }
    fun pop() {
        if (backStack.size > 1) {
            forward = false
            backStack.removeAt(backStack.lastIndex)
        }
    }
    fun popUntil(predicate: (Screen) -> Boolean) {
        if (backStack.size > 1 && !predicate(backStack.last())) {
            forward = false
            while (backStack.size > 1 && !predicate(backStack.last())) {
                backStack.removeAt(backStack.lastIndex)
            }
        }
    }
    fun replaceTop(screen: Screen) {
        forward = true
        backStack[backStack.lastIndex] = screen
    }

    val context = LocalContext.current
    var lastBackPressMs by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (backStack.size > 1) {
            forward = false
            backStack.removeAt(backStack.lastIndex)
        } else {
            val nowMs = SystemClock.uptimeMillis()
            if (nowMs - lastBackPressMs < 2000L) {
                activity?.finish()
            } else {
                lastBackPressMs = nowMs
                Toast.makeText(context, "한 번 더 누르면 종료해요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 화면이 dispose돼도 그 안의 rememberSaveable 상태(스크롤 위치 등)가 유지되도록
    // 화면 단위로 SaveableStateProvider로 감싼다. 키는 Screen의 toString().
    val saveableStateHolder = rememberSaveableStateHolder()

    AnimatedContent(
        targetState = backStack.last(),
        transitionSpec = {
            if (forward) {
                (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                    (slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut())
            } else {
                (slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()) togetherWith
                    (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
            }
        },
        label = "screen-transition"
    ) { screen ->
        saveableStateHolder.SaveableStateProvider(key = screen.toString()) {
        when (screen) {
            Screen.Home -> HomeScreen(
                onStart = { push(Screen.ModeSelect) },
                onDaily = {
                    val stage = DailyChallenge.today()
                    Stages.setDailyStage(stage)
                    push(Screen.Game(stage.id))
                },
                onRecords = { push(Screen.Records) },
                onCollection = { push(Screen.Collection) },
                onTutorial = { showTutorial = true }
            )
            Screen.ModeSelect -> ModeSelectScreen(
                onBack = { pop() },
                onMaze = { push(Screen.LevelSelect) },
                onColor = { push(Screen.ColorStageSelect) },
                onHit = { push(Screen.HitStageSelect) },
                onConstellation = { push(Screen.ConstellationStageSelect) }
            )
            Screen.ColorStageSelect -> ColorStageSelectScreen(
                onBack = { pop() },
                onSelect = { level -> push(Screen.ColorGame(level)) }
            )
            is Screen.ColorGame -> ColorGameScreen(
                level = screen.level,
                onExit = { pop() }
            )
            Screen.HitStageSelect -> HitStageSelectScreen(
                onBack = { pop() },
                onSelect = { level -> push(Screen.HitGame(level)) }
            )
            is Screen.HitGame -> HitGameScreen(
                level = screen.level,
                onExit = { pop() }
            )
            Screen.ConstellationStageSelect -> ConstellationStageSelectScreen(
                onBack = { pop() },
                onSelect = { stageKey, recordKey ->
                    push(Screen.ConstellationGame(stageKey, recordKey))
                },
                onDex = { push(Screen.ConstellationDex) }
            )
            Screen.ConstellationDex -> ConstellationDexScreen(onBack = { pop() })
            is Screen.ConstellationGame -> {
                val stage = remember(screen.stageKey) {
                    when {
                        screen.stageKey.startsWith("level_") ->
                            Constellation.stageOf(screen.stageKey.removePrefix("level_").toInt())
                        screen.stageKey.startsWith("zodiac_") ->
                            Zodiac.byIndex(screen.stageKey.removePrefix("zodiac_").toInt())?.stage
                                ?: Constellation.stages.first()
                        else -> Constellation.stages.first()
                    }
                }
                ConstellationGameScreen(
                    stage = stage,
                    recordKey = screen.recordKey,
                    onExit = { pop() }
                )
            }
            Screen.LevelSelect -> LevelSelectScreen(
                onBack = { pop() },
                onSelect = { level -> push(Screen.StageSelect(level)) },
                onCreate = { push(Screen.Editor(1)) }
            )
            is Screen.StageSelect -> StageSelectScreen(
                level = screen.level,
                onBack = { pop() },
                onSelect = { stageId -> push(Screen.Game(stageId)) }
            )
            is Screen.Game -> {
                val stage = remember(screen.stageId) { Stages.byId(screen.stageId) }
                GameScreen(
                    stage = stage,
                    onFinished = { elapsed, caught, clears ->
                        replaceTop(Screen.Result(screen.stageId, elapsed, caught, clears))
                    },
                    onExit = { popUntil { it is Screen.StageSelect } }
                )
            }
            is Screen.Result -> ResultScreen(
                stageId = screen.stageId,
                elapsedMs = screen.elapsedMs,
                caught = screen.caught,
                clears = screen.clears,
                onRetry = {
                    popUntil { it is Screen.StageSelect }
                    push(Screen.Game(screen.stageId))
                },
                onHome = { popUntil { it is Screen.StageSelect } }
            )
            Screen.Records -> RecordsScreen(onBack = { pop() })
            Screen.Collection -> CollectionScreen(onBack = { pop() })
            is Screen.Editor -> MazeEditorScreen(
                initialLevel = screen.level,
                onSaved = { pop() },
                onCancel = { pop() }
            )
        }
        }
    }
}

private fun loadCustomMazes(context: Context) {
    val all = CustomMazesRepository(context).load()
    val stages = all.groupBy { it.level }.flatMap { (_, ms) ->
        ms.sortedBy { it.createdAt }.mapIndexed { i, m -> m.toStage(i + 1) }
    }
    Stages.setCustomStages(stages)
}
