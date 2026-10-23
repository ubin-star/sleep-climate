package com.sleepclimate.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.domain.model.SessionPreset
import com.sleepclimate.app.ui.home.HomeViewModel
import com.sleepclimate.app.ui.screens.AlertsScreen
import com.sleepclimate.app.ui.screens.HomeScreen
import com.sleepclimate.app.ui.screens.ResultScreen
import com.sleepclimate.app.ui.screens.RunningScreen
import com.sleepclimate.app.ui.screens.SetupScreen
import com.sleepclimate.app.ui.session.SessionViewModel

private object Routes {
    const val HOME = "home"
    const val SETUP = "setup/{preset}"
    const val RUNNING = "running"
    const val ALERTS = "alerts"
    const val RESULT = "result"
    fun setup(preset: SessionPreset) = "setup/${preset.name}"
}

/** 홈 → 설정 → 진행 (↔ 알림 내역) → 결과 → 홈 */
@Composable
fun AppNavigation(app: SleepClimateApp) {
    val navController = rememberNavController()
    // 액티비티 범위 ViewModel — 설정·진행·알림·결과 화면이 같은 세션 상태를 공유
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.vehicleRepository))
    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory(app))

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
                onStarted = { navController.navigate(Routes.RUNNING) { popUpTo(Routes.HOME) } },
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
}
