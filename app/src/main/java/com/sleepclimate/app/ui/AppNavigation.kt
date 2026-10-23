package com.sleepclimate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.domain.model.SessionPreset
import com.sleepclimate.app.ui.components.LocalVoiceControl
import com.sleepclimate.app.ui.components.VoiceControl
import com.sleepclimate.app.ui.components.VoiceOverlay
import com.sleepclimate.app.ui.home.HomeViewModel
import com.sleepclimate.app.ui.screens.AlertsScreen
import com.sleepclimate.app.ui.screens.HomeScreen
import com.sleepclimate.app.ui.screens.ResultScreen
import com.sleepclimate.app.ui.screens.RunningScreen
import com.sleepclimate.app.ui.screens.SetupScreen
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.voice.VoiceState

private object Routes {
    const val HOME = "home"
    const val SETUP = "setup/{preset}"
    const val RUNNING = "running"
    const val ALERTS = "alerts"
    const val RESULT = "result"
    fun setup(preset: SessionPreset) = "setup/${preset.name}"
}

/** 홈 → 설정 → 진행 (↔ 알림 내역) → 결과 → 홈, 모든 화면에서 음성 명령 사용 가능 */
@Composable
fun AppNavigation(app: SleepClimateApp) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // 액티비티 범위 ViewModel — 설정·진행·알림·결과 화면이 같은 세션 상태를 공유
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.vehicleRepository))
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory(app))

    val voice = app.voiceAssistant
    val voiceState by voice.state.collectAsStateWithLifecycle()
    val engines by voice.engines.collectAsStateWithLifecycle()
    val handler = remember(homeViewModel, sessionViewModel) {
        VoiceCommandHandler(voice, homeViewModel, sessionViewModel) { navController.goToRunning() }
    }
    val listen = { voice.listen(handler::handle) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else voice.speak("음성 명령을 쓰려면 마이크 권한이 필요해요.")
    }
    val voiceControl = VoiceControl(listening = voiceState is VoiceState.Listening) {
        when {
            voiceState is VoiceState.Listening -> voice.stopListening()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> listen()
            else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    CompositionLocalProvider(LocalVoiceControl provides voiceControl) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(homeViewModel) { preset -> navController.navigate(Routes.setup(preset)) }
                }
                composable(Routes.SETUP, arguments = listOf(navArgument("preset") { type = NavType.StringType })) { entry ->
                    val preset = SessionPreset.valueOf(entry.arguments?.getString("preset") ?: SessionPreset.CUSTOM.name)
                    SetupScreen(
                        viewModel = sessionViewModel,
                        preset = preset,
                        onStarted = { navController.goToRunning() },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.RUNNING) {
                    RunningScreen(
                        viewModel = sessionViewModel,
                        onShowAlerts = { navController.navigate(Routes.ALERTS) },
                        onFinished = { navController.navigate(Routes.RESULT) { popUpTo(Routes.HOME) } }
                    )
                }
                composable(Routes.ALERTS) {
                    AlertsScreen(sessionViewModel) { navController.popBackStack() }
                }
                composable(Routes.RESULT) {
                    ResultScreen(sessionViewModel) { navController.popBackStack(Routes.HOME, inclusive = false) }
                }
            }
            VoiceOverlay(voiceState, engines, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun NavHostController.goToRunning() {
    if (currentDestination?.route == Routes.RUNNING) return
    navigate(Routes.RUNNING) { popUpTo(Routes.HOME) }
}
