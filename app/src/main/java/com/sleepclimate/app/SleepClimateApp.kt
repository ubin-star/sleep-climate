package com.sleepclimate.app

import android.app.Application
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.SwitchableVehicleRepository
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.data.vehicle.simulation.SimulatedVehicleRepository
import com.sleepclimate.app.domain.usecase.MonitorSessionUseCase

/**
 * Application 클래스 — 의존성 수동 주입
 * (Hilt/Dagger 없이 심플하게 유지)
 */
class SleepClimateApp : Application() {

    // 전역 싱글톤 — 뷰모델에서 참조
    val vehicleRepository by lazy {
        SwitchableVehicleRepository(
            context   = this,
            real      = VehicleRepository(this),
            simulated = SimulatedVehicleRepository(this)
        )
    }
    val sessionRepository by lazy { SessionRepository() }
    val monitorSessionUseCase by lazy {
        MonitorSessionUseCase(vehicleRepository, sessionRepository)
    }

    override fun onCreate() {
        super.onCreate()
        // SDK 초기화 (현재 stub, SDK URL 확인 후 실제 연동)
        vehicleRepository.initialize()
        // 알림 채널 등록 — 앱 시작 시 한 번만
        NotificationHelper.createChannels(this)
    }
}
