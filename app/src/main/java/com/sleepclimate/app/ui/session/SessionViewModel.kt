package com.sleepclimate.app.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.data.session.SessionForegroundService
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.ControlEvent
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.domain.model.SessionStatus
import com.sleepclimate.app.domain.model.TempSample
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.usecase.MonitorSessionUseCase
import com.sleepclimate.app.voice.VoiceAssistant
import com.sleepclimate.app.voice.spoken
import com.sleepclimate.app.voice.spokenDuration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정·진행·알림·리포트 화면이 함께 쓰는 휴식 세션 상태 */
class SessionViewModel(
    private val vehicleRepository: VehicleRepository,
    private val sessionRepository: SessionRepository,
    private val monitorUseCase: MonitorSessionUseCase,
    private val appContext: Context,
    val simulationEnabled: StateFlow<Boolean>,
    private val voice: VoiceAssistant
) : ViewModel() {

    val currentSession: StateFlow<RestSession?> = sessionRepository.currentSession
    val vehicleAlerts: StateFlow<List<SessionAlert>> = sessionRepository.alerts
    val controlEvents: StateFlow<List<ControlEvent>> = sessionRepository.controlEvents
    val tempSamples: StateFlow<List<TempSample>> = sessionRepository.tempSamples

    // 화면과 세션 감시가 같은 구독을 공유 (차량 SDK 폴링은 한 번만)
    val vehicleState: StateFlow<VehicleState> = vehicleRepository.observeVehicleState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VehicleState()
        )

    private var monitorJob: Job? = null

    fun startSession(session: RestSession) {
        sessionRepository.startSession(session)
        SessionForegroundService.start(appContext)
        voice.speak(
            "휴식을 시작합니다. 목표 온도 ${session.targetTempCelsius.spoken()}도로 관리하고, " +
                "${spokenDuration(session.endTime - session.startTime)} 뒤에 깨워드릴게요."
        )
        monitorJob?.cancel()
        monitorJob = viewModelScope.launch {
            // stateIn으로 이미 공유된 vehicleState 를 consume
            vehicleState.collect { state ->
                // 아직 차량 데이터를 한 번도 받지 못한 초기값은 감시 대상이 아니다
                if (state.lastUpdatedMs == 0L) return@collect
                if (sessionRepository.currentSession.value?.status == SessionStatus.RUNNING) {
                    state.interiorTempCelsius?.let {
                        sessionRepository.recordSample(TempSample(state.lastUpdatedMs, it, state.isHvacOn == true))
                    }
                    val prevAlertCount = sessionRepository.alerts.value.size
                    monitorUseCase.onVehicleStateUpdate(state)
                    // 새 알림이 추가됐으면 시스템 알림 전송
                    val newAlerts = sessionRepository.alerts.value
                    if (newAlerts.size > prevAlertCount) {
                        val latest = newAlerts.last()
                        dispatchNotification(latest)
                    }
                }
            }
        }
    }

    private fun dispatchNotification(alert: SessionAlert) {
        // 깨워야 하는 알림만 음성으로 — 자동 제어가 정상 동작 중일 때는 조용히 둔다
        if (alert.type in SPOKEN_ALERTS) voice.speak(alert.message)
        when (alert.type) {
            AlertType.TEMP_OUT_OF_RANGE,
            AlertType.COMMAND_REJECTED,
            AlertType.COMMAND_FAILED,
            AlertType.NOT_PARKED,
            AlertType.SESSION_ENDING_SOON,
            AlertType.PET_SAFETY_REMINDER ->
                NotificationHelper.sendTempAlert(appContext, alert)

            AlertType.BATTERY_LOW,
            AlertType.FUEL_LOW,
            AlertType.AUTO_CONTROL_STOPPED ->
                NotificationHelper.sendEnergyAlert(appContext, alert)

            AlertType.VEHICLE_DISCONNECTED,
            AlertType.DATA_STALE ->
                NotificationHelper.sendConnectionAlert(appContext, alert)
        }
    }

    /** 기상 화면의 "10분 더 쉬기" */
    fun extendSession(minutes: Int) {
        sessionRepository.extendSession(minutes * 60_000L)
        voice.speak("${minutes}분 더 쉬실 수 있게 실내 온도를 계속 관리할게요.")
    }

    /** 휴식 중 목표 온도 변경 (16~30°C) */
    fun setTarget(celsius: Float) {
        sessionRepository.updateTarget(celsius.coerceIn(MIN_TARGET, MAX_TARGET))
    }

    fun adjustTarget(delta: Float) {
        val current = sessionRepository.currentSession.value ?: return
        setTarget(current.targetTempCelsius + delta)
    }

    private val _sleepScreenRequests = MutableStateFlow(0)
    /** 음성 명령 등으로 수면 화면 전환을 요청한 횟수 — 진행 화면이 값 변화를 관찰 */
    val sleepScreenRequests: StateFlow<Int> = _sleepScreenRequests.asStateFlow()

    fun requestSleepScreen() {
        _sleepScreenRequests.value += 1
    }

    /** 기상 화면이 열릴 때 한 번 */
    fun announceWake() {
        val temp = vehicleState.value.interiorTempCelsius
        voice.speak(
            "휴식 시간이 끝났습니다." + (temp?.let { " 실내 온도는 ${it.spoken()}도입니다." } ?: "")
        )
    }

    fun endSession() {
        monitorJob?.cancel()
        monitorJob = null
        sessionRepository.endSession()
        SessionForegroundService.stop(appContext)
    }

    companion object {
        private const val MIN_TARGET = 16f
        private const val MAX_TARGET = 30f
        private val SPOKEN_ALERTS = setOf(
            AlertType.TEMP_OUT_OF_RANGE,
            AlertType.COMMAND_FAILED,
            AlertType.NOT_PARKED,
            AlertType.BATTERY_LOW,
            AlertType.FUEL_LOW,
            AlertType.AUTO_CONTROL_STOPPED,
            AlertType.VEHICLE_DISCONNECTED,
            AlertType.PET_SAFETY_REMINDER
        )
    }

    class Factory(private val app: SleepClimateApp) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                app.vehicleRepository,
                app.sessionRepository,
                app.monitorSessionUseCase,
                app.applicationContext,
                app.vehicleRepository.simulationEnabled,
                app.voiceAssistant
            ) as T
        }
    }
}
