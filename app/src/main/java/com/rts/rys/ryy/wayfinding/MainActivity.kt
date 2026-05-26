package com.rts.rys.ryy.wayfinding

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.CustomMazesRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.DailyChallenge
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.CollectionScreen
import com.rts.rys.ryy.wayfinding.ui.GameScreen
import com.rts.rys.ryy.wayfinding.ui.HomeScreen
import com.rts.rys.ryy.wayfinding.ui.LevelSelectScreen
import com.rts.rys.ryy.wayfinding.ui.MazeEditorScreen
import com.rts.rys.ryy.wayfinding.ui.RecordsScreen
import com.rts.rys.ryy.wayfinding.ui.ResultScreen
import com.rts.rys.ryy.wayfinding.ui.SplashScreen
import com.rts.rys.ryy.wayfinding.ui.StageSelectScreen
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
    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
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

    BackHandler {
        if (backStack.size > 1) {
            forward = false
            backStack.removeAt(backStack.lastIndex)
        } else {
            activity?.finish()
        }
    }

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
        when (screen) {
            Screen.Home -> HomeScreen(
                onStart = { push(Screen.LevelSelect) },
                onDaily = {
                    val stage = DailyChallenge.today()
                    Stages.setDailyStage(stage)
                    push(Screen.Game(stage.id))
                },
                onRecords = { push(Screen.Records) },
                onCreate = { push(Screen.Editor(1)) },
                onCollection = { push(Screen.Collection) }
            )
            Screen.LevelSelect -> LevelSelectScreen(
                onBack = { pop() },
                onSelect = { level -> push(Screen.StageSelect(level)) }
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

private fun loadCustomMazes(context: Context) {
    val all = CustomMazesRepository(context).load()
    val stages = all.groupBy { it.level }.flatMap { (_, ms) ->
        ms.sortedBy { it.createdAt }.mapIndexed { i, m -> m.toStage(i + 1) }
    }
    Stages.setCustomStages(stages)
}
