package com.sleepclimate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ai.pleos.playground.vehicle.constant.PleosPermission
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.ui.theme.SleepClimateTheme

class MainActivity : ComponentActivity() {

    private val permissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* 거부된 기능은 화면에서 미지원으로 표시 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 세션 진행 화면을 켜 둔 채 잠들 수 있도록 화면 꺼짐 방지 (밝기는 수면 화면으로 낮춤)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestMissingPermissions()

        val app = application as SleepClimateApp
        app.vehicleRepository.initialize()
        setContent {
            SleepClimateTheme {
                AppNavigation(app)
            }
        }
    }

    private fun requestMissingPermissions() {
        val wanted = buildList {
            add(PleosPermission.CAR_CLIMATE)
            add(PleosPermission.CONTROL_CAR_CLIMATE)
            add(PleosPermission.CAR_ENERGY)
            add(PleosPermission.CAR_DRIVE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (wanted.isNotEmpty()) permissions.launch(wanted.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        // 화면 회전·분할 화면 전환이 아니라 앱을 닫을 때만 SDK 연결 해제
        if (isFinishing) (application as SleepClimateApp).vehicleRepository.release()
    }
}
