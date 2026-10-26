package com.sleepclimate.app

import android.app.Application
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.PleosVehicleRepository
import com.sleepclimate.app.data.vehicle.SwitchableVehicleRepository
import com.sleepclimate.app.data.vehicle.simulation.SimulatedVehicleRepository
import com.sleepclimate.app.domain.usecase.MonitorSessionUseCase
import com.sleepclimate.app.voice.VoiceAssistant

class SleepClimateApp : Application() {

    companion object {
        // AndroidManifest의 ai.pleos.connect.CLIENT_ID와 같은 값
        const val PLEOS_CLIENT_ID = "HjGgahiwRkOVR-DTI2AE5w"
    }

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
        vehicleRepository.initialize()
        voiceAssistant.initialize()
        NotificationHelper.createChannels(this)
    }
}
