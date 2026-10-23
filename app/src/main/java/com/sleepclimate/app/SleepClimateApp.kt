package com.sleepclimate.app

import android.app.Application
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.PleosVehicleRepository
import com.sleepclimate.app.data.vehicle.SwitchableVehicleRepository
import com.sleepclimate.app.data.vehicle.simulation.SimulatedVehicleRepository
import com.sleepclimate.app.domain.usecase.MonitorSessionUseCase
import com.sleepclimate.app.voice.VoiceAssistant

/**
 * Application 클래스 — 의존성 수동 주입
 * (Hilt/Dagger 없이 심플하게 유지)
 */
class SleepClimateApp : Application() {

    companion object {
        /** Pleos Playground 프로젝트 클라이언트 ID (AndroidManifest meta-data와 동일) */
        const val PLEOS_CLIENT_ID = "HjGgahiwRkOVR-DTI2AE5w"
    }

    // 전역 싱글톤 — 뷰모델에서 참조
    val vehicleRepository by lazy {
        SwitchableVehicleRepository(
            context   = this,
            real      = PleosVehicleRepository(this),
            simulated = SimulatedVehicleRepository(this)
        )
    }
    val sessionRepository by lazy { SessionRepository() }
    val monitorSessionUseCase by lazy {
        MonitorSessionUseCase(vehicleRepository, sessionRepository)
    }
    val voiceAssistant by lazy { VoiceAssistant(this, PLEOS_CLIENT_ID) }

    override fun onCreate() {
        super.onCreate()
        // Pleos Vehicle SDK 서비스 연결
        vehicleRepository.initialize()
        // Pleos TTS·STT (없으면 Android 기본 음성 엔진)
        voiceAssistant.initialize()
        // 알림 채널 등록 — 앱 시작 시 한 번만
        NotificationHelper.createChannels(this)
    }
}
