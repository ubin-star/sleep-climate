package com.sleepclimate.app.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.data.notification.NotificationHelper
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 화면 B·C 공용 ViewModel
 * - 세션 설정 → 세션 진행 흐름 담당
 */
class SessionViewModel(
    private val vehicleRepository: VehicleRepository,
    private val sessionRepository: SessionRepository,
    private val monitorUseCase: MonitorSessionUseCase,
    private val appContext: Context,
    val simulationEnabled: StateFlow<Boolean>
) : ViewModel() {

    val currentSession: StateFlow<RestSession?> = sessionRepository.currentSession
    val vehicleAlerts: StateFlow<List<SessionAlert>> = sessionRepository.alerts
    val controlEvents: StateFlow<List<ControlEvent>> = sessionRepository.controlEvents
    val tempSamples: StateFlow<List<TempSample>> = sessionRepository.tempSamples

    /**
     * 차량 상태 — stateIn()으로 단일 구독 보장.
     * WhileSubscribed(5000)으로 구독자 없어지고 5초 후 업스트림 취소.
     */
    val vehicleState: StateFlow<VehicleState> = vehicleRepository.observeVehicleState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = VehicleState()
        )

    private var monitorJob: Job? = null

    /**
     * 세션 시작 — vehicleState 재사용(새 Flow 생성 없음)
     */
    fun startSession(session: RestSession) {
        sessionRepository.startSession(session)
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

    /**
     * 세션 즉시 종료 (사용자가 언제든 종료 가능)
     */
    fun endSession() {
        monitorJob?.cancel()
        monitorJob = null
        sessionRepository.endSession()
    }

    class Factory(private val app: SleepClimateApp) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                app.vehicleRepository,
                app.sessionRepository,
                app.monitorSessionUseCase,
                app.applicationContext,
                app.vehicleRepository.simulationEnabled
            ) as T
        }
    }
}
