package com.rts.rys.ryy.wayfinding

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rts.rys.ryy.wayfinding.data.AppSettings
import com.rts.rys.ryy.wayfinding.data.CustomMazesRepository
import com.rts.rys.ryy.wayfinding.data.SoundManager
import com.rts.rys.ryy.wayfinding.game.Stages
import com.rts.rys.ryy.wayfinding.ui.CollectionScreen
import com.rts.rys.ryy.wayfinding.ui.GameScreen
import com.rts.rys.ryy.wayfinding.ui.HomeScreen
import com.rts.rys.ryy.wayfinding.ui.LevelSelectScreen
import com.rts.rys.ryy.wayfinding.ui.MazeEditorScreen
import com.rts.rys.ryy.wayfinding.ui.RecordsScreen
import com.rts.rys.ryy.wayfinding.ui.ResultScreen
import com.rts.rys.ryy.wayfinding.ui.Routes
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

private const val BACK_DEBOUNCE_MS = 350L

@Composable
fun MazeApp() {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val navController = rememberNavController()
    val lastBackRef = remember { longArrayOf(0L) }

    fun debounced(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastBackRef[0] >= BACK_DEBOUNCE_MS) {
            lastBackRef[0] = now
            action()
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStart = { navController.navigate(Routes.LEVEL_SELECT) },
                onRecords = { navController.navigate(Routes.RECORDS) },
                onCreate = { navController.navigate(Routes.editor(1)) },
                onCollection = { navController.navigate(Routes.COLLECTION) }
            )
        }
        composable(Routes.LEVEL_SELECT) {
            BackHandler { debounced { navController.popBackStack() } }
            LevelSelectScreen(
                onBack = { navController.popBackStack() },
                onSelect = { level ->
                    navController.navigate(Routes.stages(level))
                }
            )
        }
        composable(
            route = Routes.STAGE_SELECT,
            arguments = listOf(navArgument("level") { type = NavType.IntType })
        ) { entry ->
            BackHandler { debounced { navController.popBackStack() } }
            val level = entry.arguments?.getInt("level") ?: 1
            StageSelectScreen(
                level = level,
                onBack = { navController.popBackStack() },
                onSelect = { stageId ->
                    navController.navigate(Routes.game(stageId))
                }
            )
        }
        composable(
            route = Routes.GAME,
            arguments = listOf(navArgument("stageId") { type = NavType.IntType })
        ) { entry ->
            val stageId = entry.arguments?.getInt("stageId") ?: 1
            val stage = remember(stageId) { Stages.byId(stageId) }
            GameScreen(
                stage = stage,
                onFinished = { elapsed, caught, clears ->
                    navController.navigate(Routes.result(stageId, elapsed, caught, clears)) {
                        popUpTo(Routes.GAME) { inclusive = true }
                    }
                },
                onExit = {
                    navController.popBackStack(Routes.STAGE_SELECT, inclusive = false)
                }
            )
        }
        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("stageId") { type = NavType.IntType },
                navArgument("elapsed") { type = NavType.LongType },
                navArgument("caught") { type = NavType.BoolType },
                navArgument("clears") { type = NavType.IntType }
            )
        ) { entry ->
            BackHandler {
                debounced { navController.popBackStack(Routes.STAGE_SELECT, inclusive = false) }
            }
            val stageId = entry.arguments?.getInt("stageId") ?: 1
            val elapsed = entry.arguments?.getLong("elapsed") ?: 0L
            val caught = entry.arguments?.getBoolean("caught") ?: false
            val clears = entry.arguments?.getInt("clears") ?: 0
            ResultScreen(
                stageId = stageId,
                elapsedMs = elapsed,
                caught = caught,
                clears = clears,
                onRetry = {
                    navController.navigate(Routes.game(stageId)) {
                        popUpTo(Routes.STAGE_SELECT) { inclusive = false }
                    }
                },
                onHome = {
                    navController.popBackStack(Routes.STAGE_SELECT, inclusive = false)
                }
            )
        }
        composable(Routes.RECORDS) {
            BackHandler { debounced { navController.popBackStack() } }
            RecordsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.COLLECTION) {
            BackHandler { debounced { navController.popBackStack() } }
            CollectionScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("level") { type = NavType.IntType })
        ) { entry ->
            val initialLevel = entry.arguments?.getInt("level") ?: 1
            MazeEditorScreen(
                initialLevel = initialLevel,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
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
