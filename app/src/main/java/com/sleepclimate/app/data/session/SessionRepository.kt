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

/**
 * 휴식 세션 상태 관리
 * - 앱 내부 세션 라이프사이클 전담
 */
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

    /** 에너지 보호 등으로 이번 세션의 자동 제어를 중단했으면 true */
    var autoControlSuspended: Boolean = false
        private set

    /** 마지막으로 각 AlertType 알림이 전송된 시각 (쿨다운 추적) */
    private val lastAlertTypeMs = mutableMapOf<AlertType, Long>()

    /**
     * 세션 종료 5분 전 알림이 이미 발송됐으면 true — 중복 발송 방지
     * startSession() 호출 시 초기화된다.
     */
    var endingSoonNotified: Boolean = false
        private set

    companion object {
        const val MAX_COMMAND_RETRY     = 3          // 최대 재시도 횟수 (무한 재시도 금지)
        const val DATA_STALE_THRESHOLD_MS = 30_000L  // 30초 이상 갱신 없으면 stale
        const val ALERT_COOLDOWN_MS     = 120_000L   // 동일 AlertType 쿨다운 2분

        /** 쿨다운 적용 제외 AlertType — 즉시 전달해야 하는 이벤트 */
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
        endingSoonNotified = false
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

    /** 휴식 연장 — 종료 시각을 늦추고 기상 준비 알림을 다시 받을 수 있게 한다 */
    fun extendSession(extraMs: Long) {
        val session = _currentSession.value ?: return
        val base = maxOf(session.endTime, System.currentTimeMillis())
        _currentSession.value = session.copy(endTime = base + extraMs)
        endingSoonNotified = false
    }

    fun endSession() {
        _currentSession.value = _currentSession.value?.copy(status = SessionStatus.ENDED)
    }

    /**
     * 알림 추가 — 동일 AlertType에 대해 [ALERT_COOLDOWN_MS] 쿨다운 적용.
     * [NO_COOLDOWN_TYPES]에 속한 타입은 쿨다운 없이 항상 추가된다.
     */
    fun addAlert(alert: SessionAlert) {
        val now = System.currentTimeMillis()
        if (alert.type !in NO_COOLDOWN_TYPES) {
            val last = lastAlertTypeMs[alert.type]
            if (last != null && now - last < ALERT_COOLDOWN_MS) {
                return // 쿨다운 중 — 중복 알림 억제
            }
        }
        lastAlertTypeMs[alert.type] = now
        _alerts.value = _alerts.value + alert
    }

    /** SESSION_ENDING_SOON 알림을 발송한 뒤 호출하여 중복 방지 플래그를 설정한다. */
    fun markEndingSoonNotified() {
        endingSoonNotified = true
    }

    fun incrementFailCount(): Int {
        val next = _commandFailCount.value + 1
        _commandFailCount.value = next
        return next
    }

    fun resetFailCount() {
        _commandFailCount.value = 0
    }

    fun clearSession() {
        _currentSession.value = null
    }
}
