package com.sleepclimate.app.data.session

import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.ControlEvent
import com.sleepclimate.app.domain.model.ControlStatus
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.domain.model.SessionStatus
import com.sleepclimate.app.domain.model.TempSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 진행 중인 휴식 세션과 그 기록(알림, 제어 명령, 온도) */
class SessionRepository {

    private val _currentSession = MutableStateFlow<RestSession?>(null)
    val currentSession: StateFlow<RestSession?> = _currentSession.asStateFlow()

    private val _alerts = MutableStateFlow<List<SessionAlert>>(emptyList())
    val alerts: StateFlow<List<SessionAlert>> = _alerts.asStateFlow()

    private val _commandFailCount = MutableStateFlow(0)
    val commandFailCount: StateFlow<Int> = _commandFailCount.asStateFlow()

    private val _controlEvents = MutableStateFlow<List<ControlEvent>>(emptyList())
    val controlEvents: StateFlow<List<ControlEvent>> = _controlEvents.asStateFlow()

    private val _tempSamples = MutableStateFlow<List<TempSample>>(emptyList())
    val tempSamples: StateFlow<List<TempSample>> = _tempSamples.asStateFlow()

    /** 에너지 보호로 이번 세션의 자동 제어를 멈췄는지 */
    var autoControlSuspended: Boolean = false
        private set

    private val lastAlertTypeMs = mutableMapOf<AlertType, Long>()

    /** 기상 준비 알림을 이미 보냈는지 (연장하면 다시 false) */
    var wakeNotified: Boolean = false
        private set

    companion object {
        const val MAX_COMMAND_RETRY = 3
        const val DATA_STALE_THRESHOLD_MS = 30_000L
        const val ALERT_COOLDOWN_MS = 120_000L

        // 연결 문제는 매번 바로 알려야 한다
        private val NO_COOLDOWN_TYPES = setOf(
            AlertType.VEHICLE_DISCONNECTED,
            AlertType.DATA_STALE
        )
    }

    fun startSession(session: RestSession) {
        _currentSession.value = session.copy(status = SessionStatus.RUNNING)
        _alerts.value = emptyList()
        _commandFailCount.value = 0
        _controlEvents.value = emptyList()
        _tempSamples.value = emptyList()
        autoControlSuspended = false
        lastAlertTypeMs.clear()
        wakeNotified = false
    }

    fun recordSample(sample: TempSample) {
        _tempSamples.value = _tempSamples.value + sample
    }

    fun addControlEvent(event: ControlEvent) {
        _controlEvents.value = _controlEvents.value + event
    }

    fun updateControlStatus(id: String, status: ControlStatus) {
        _controlEvents.value = _controlEvents.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    fun suspendAutoControl() {
        autoControlSuspended = true
    }

    /** 목표 온도 변경 — 허용 범위 폭은 그대로 유지 */
    fun updateTarget(targetCelsius: Float) {
        val session = _currentSession.value ?: return
        val half = (session.tempRangeHigh - session.tempRangeLow) / 2f
        _currentSession.value = session.copy(
            targetTempCelsius = targetCelsius,
            tempRangeLow = targetCelsius - half,
            tempRangeHigh = targetCelsius + half
        )
    }

    /** 휴식 연장 — 종료 시각을 늦추고 기상 준비 알림을 다시 받을 수 있게 한다 */
    fun extendSession(extraMs: Long) {
        val session = _currentSession.value ?: return
        val base = maxOf(session.endTime, System.currentTimeMillis())
        _currentSession.value = session.copy(endTime = base + extraMs)
        wakeNotified = false
    }

    fun endSession() {
        _currentSession.value = _currentSession.value?.copy(status = SessionStatus.ENDED)
    }

    /** 같은 종류의 알림은 [ALERT_COOLDOWN_MS] 안에 한 번만 */
    fun addAlert(alert: SessionAlert) {
        val now = System.currentTimeMillis()
        if (alert.type !in NO_COOLDOWN_TYPES) {
            val last = lastAlertTypeMs[alert.type]
            if (last != null && now - last < ALERT_COOLDOWN_MS) return
        }
        lastAlertTypeMs[alert.type] = now
        _alerts.value = _alerts.value + alert
    }

    fun markWakeNotified() {
        wakeNotified = true
    }

    fun incrementFailCount(): Int {
        val next = _commandFailCount.value + 1
        _commandFailCount.value = next
        return next
    }

    fun resetFailCount() {
        _commandFailCount.value = 0
    }
}
