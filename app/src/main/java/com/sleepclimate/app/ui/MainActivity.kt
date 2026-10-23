package com.sleepclimate.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.ui.theme.SleepClimateTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 거부 시 앱 내 알림만 표시 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 세션 진행 화면을 켜 둔 채 잠들 수 있도록 화면 꺼짐 방지 (밝기는 수면 화면으로 낮춤)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val app = application as SleepClimateApp
        setContent {
            SleepClimateTheme {
                AppNavigation(app)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SDK 자원 해제 — Activity 수명에 맞춰 release
        (application as SleepClimateApp).vehicleRepository.release()
    }
}
