package com.sleepclimate.app.domain.usecase

import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.ControlEvent
import com.sleepclimate.app.domain.model.ControlStatus
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.flow.first

/**
 * 세션 진행 중 온도 범위 감시 + 자동 공조 제어 UseCase
 *
 * 자동 제어 규칙 (FULL 지원 차량에서만):
 * - 허용 범위 상한 초과 → 냉방 가동, 하한 미만 → 난방 가동
 * - 냉방은 목표 온도 이하, 난방은 목표 온도 이상이 되면 공조 끄기 (히스테리시스)
 * - 명령 전송 후 차량 상태로 반영을 확인해야 성공(CONFIRMED)으로 기록
 * - 명령 실패 MAX_RETRY 초과 시 알림으로 전환, 더 이상 재시도 안 함
 * - 배터리·연료가 알림 기준 이하이면 공조를 끄고 이번 세션의 자동 제어 중단
 * - 주차 상태 아닐 때 제어 차단
 */
class MonitorSessionUseCase(
    private val vehicleRepo: VehicleRepository,
    private val sessionRepo: SessionRepository
) {

    companion object {
        /** 세션 종료 X분 전부터 SESSION_ENDING_SOON 알림 발송 */
        private const val ENDING_SOON_THRESHOLD_MS = 5 * 60 * 1000L // 5분

        /** 반려동물 안전 확인 알림 주기 */
        private const val PET_REMINDER_INTERVAL_MS = 30 * 60 * 1000L // 30분

        /** 명령 전송 후 이 시간 안에 차량 상태로 반영이 확인되지 않으면 실패 처리 */
        const val CONFIRM_TIMEOUT_MS = 30_000L
    }

    /**
     * 차량 상태가 들어올 때마다 호출
     * - 온도 범위 이탈 시 자동 조정 시도 (지원 차량만)
     * - 각종 이상 상황 알림 생성
     */
    suspend fun onVehicleStateUpdate(state: VehicleState) {
        val session = sessionRepo.currentSession.first() ?: return

        // 1. 차량 연결 끊김
        if (!state.isConnected) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.VEHICLE_DISCONNECTED,
                message = "차량 연결이 끊겼습니다. 마지막 상태를 표시합니다."
            ))
            return
        }

        // 2. 데이터 stale 체크
        val elapsed = System.currentTimeMillis() - state.lastUpdatedMs
        if (elapsed > SessionRepository.DATA_STALE_THRESHOLD_MS) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.DATA_STALE,
                message = "차량 데이터가 ${elapsed / 1000}초 동안 갱신되지 않았습니다."
            ))
            return
        }

        // 3. 주차 상태 확인
        if (state.isParked == false) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.NOT_PARKED,
                message = "차량이 주행 중입니다. 세션을 일시 중단합니다."
            ))
            return
        }

        // 4. 배터리/연료 경고
        val energyLow = checkEnergyLevel(state, session.energyAlertPercent)

        // 5. 세션 종료 5분 전 알림 (한 번만)
        val remaining = session.endTime - System.currentTimeMillis()
        if (remaining in 0L..ENDING_SOON_THRESHOLD_MS && !sessionRepo.endingSoonNotified) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.SESSION_ENDING_SOON,
                message = "세션 종료까지 5분 미만 남았습니다."
            ))
            sessionRepo.markEndingSoonNotified()
        }

        // 6. 반려동물 안전 확인 알림 (30분 주기, hasPet 세션 한정)
        if (session.hasPet) {
            checkPetSafetyReminder(state)
        }

        // 7. 직전 제어 명령이 차량 상태에 반영됐는지 확인
        confirmPendingControl(state)

        // 8. 온도 범위 관리
        val currentTemp = state.interiorTempCelsius ?: return
        when (state.supportLevel) {
            VehicleSupportLevel.FULL -> controlClimate(session, state, currentTemp, energyLow)
            VehicleSupportLevel.READ_ONLY, VehicleSupportLevel.NONE -> {
                // 읽기만 가능 → 알림으로 안내
                if (currentTemp > session.tempRangeHigh || currentTemp < session.tempRangeLow) {
                    sessionRepo.addAlert(SessionAlert(
                        type = AlertType.TEMP_OUT_OF_RANGE,
                        message = "실내 온도 ${currentTemp}°C — 설정 범위를 벗어났습니다. 공조를 확인해 주세요."
                    ))
                }
            }
            VehicleSupportLevel.UNKNOWN -> { /* 확인 중 */ }
        }
    }

    private suspend fun controlClimate(
        session: RestSession,
        state: VehicleState,
        currentTemp: Float,
        energyLow: Boolean
    ) {
        if (sessionRepo.autoControlSuspended) return

        if (energyLow) {
            sessionRepo.suspendAutoControl()
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.AUTO_CONTROL_STOPPED,
                message = "에너지 잔량이 알림 기준 이하라 자동 공조 제어를 중단합니다. 차량 상태를 직접 확인해 주세요."
            ))
            if (state.isHvacOn == true) sendCommand(HvacMode.OFF, session.targetTempCelsius)
            return
        }

        val desired = desiredMode(session, state, currentTemp) ?: return

        // 같은 명령이 아직 반영 확인 대기 중이면 재전송하지 않는다
        val pending = sessionRepo.controlEvents.value.lastOrNull { it.status == ControlStatus.PENDING }
        if (pending != null && pending.mode == desired) return

        if (sessionRepo.commandFailCount.first() >= SessionRepository.MAX_COMMAND_RETRY) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.COMMAND_FAILED,
                message = "자동 제어 ${SessionRepository.MAX_COMMAND_RETRY}회 실패. 직접 공조를 조작해 주세요."
            ))
            return
        }

        sendCommand(desired, session.targetTempCelsius)
    }

    private fun desiredMode(session: RestSession, state: VehicleState, temp: Float): HvacMode? {
        val mode = if (state.isHvacOn == true) state.hvacMode else HvacMode.OFF
        return when {
            temp > session.tempRangeHigh && mode != HvacMode.COOLING -> HvacMode.COOLING
            temp < session.tempRangeLow && mode != HvacMode.HEATING  -> HvacMode.HEATING
            mode == HvacMode.COOLING && temp <= session.targetTempCelsius -> HvacMode.OFF
            mode == HvacMode.HEATING && temp >= session.targetTempCelsius -> HvacMode.OFF
            else -> null
        }
    }

    private suspend fun sendCommand(mode: HvacMode, targetTemp: Float) {
        var result = vehicleRepo.setHvacMode(mode)
        if (result == CommandResult.SENT && mode != HvacMode.OFF) {
            result = vehicleRepo.setHvacTargetTemp(targetTemp)
        }

        val status = when (result) {
            CommandResult.SENT -> ControlStatus.PENDING
            CommandResult.REJECTED, CommandResult.ERROR -> ControlStatus.REJECTED
            CommandResult.NOT_SUPPORTED -> ControlStatus.NOT_SUPPORTED
        }
        sessionRepo.addControlEvent(ControlEvent(mode = mode, targetTempCelsius = targetTemp, status = status))

        when (result) {
            CommandResult.SENT -> { /* 반영 여부는 다음 상태 업데이트에서 확인 */ }
            CommandResult.REJECTED, CommandResult.ERROR -> {
                val failCount = sessionRepo.incrementFailCount()
                sessionRepo.addAlert(SessionAlert(
                    type = AlertType.COMMAND_REJECTED,
                    message = "공조 명령이 거부됐습니다. ($failCount / ${SessionRepository.MAX_COMMAND_RETRY})"
                ))
            }
            CommandResult.NOT_SUPPORTED -> {
                sessionRepo.addAlert(SessionAlert(
                    type = AlertType.COMMAND_REJECTED,
                    message = "이 차량은 자동 공조 제어를 지원하지 않습니다."
                ))
            }
        }
    }

    private fun confirmPendingControl(state: VehicleState) {
        val pending = sessionRepo.controlEvents.value
            .lastOrNull { it.status == ControlStatus.PENDING } ?: return

        val applied = when (pending.mode) {
            HvacMode.OFF -> state.isHvacOn == false
            else -> state.isHvacOn == true && (state.hvacMode == null || state.hvacMode == pending.mode)
        }

        if (applied) {
            sessionRepo.updateControlStatus(pending.id, ControlStatus.CONFIRMED)
            sessionRepo.resetFailCount()
        } else if (System.currentTimeMillis() - pending.timestampMs > CONFIRM_TIMEOUT_MS) {
            sessionRepo.updateControlStatus(pending.id, ControlStatus.TIMED_OUT)
            sessionRepo.incrementFailCount()
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.COMMAND_FAILED,
                message = "공조 명령이 차량에 반영됐는지 확인하지 못했습니다. 공조 상태를 직접 확인해 주세요."
            ))
        }
    }

    /** @return 배터리 또는 연료가 기준 이하이면 true */
    private fun checkEnergyLevel(state: VehicleState, threshold: Int): Boolean {
        var low = false
        state.batteryPercent?.let {
            if (it <= threshold) {
                low = true
                sessionRepo.addAlert(SessionAlert(
                    type = AlertType.BATTERY_LOW,
                    message = "배터리가 ${it}% 남았습니다."
                ))
            }
        }
        state.fuelPercent?.let {
            if (it <= threshold) {
                low = true
                sessionRepo.addAlert(SessionAlert(
                    type = AlertType.FUEL_LOW,
                    message = "연료가 ${it}% 남았습니다."
                ))
            }
        }
        return low
    }

    /**
     * 반려동물 동반 세션 — 30분마다 확인 알림.
     * addAlert()의 기본 쿨다운(2분)보다 긴 주기이므로 alerts 목록의 마지막 발생 시각으로 직접 판단한다.
     *
     * NOTE: 주의 문구에 '안전 보장' 표현을 포함하지 않는다.
     */
    private fun checkPetSafetyReminder(state: VehicleState) {
        val lastPetAlert = sessionRepo.alerts.value
            .filter { it.type == AlertType.PET_SAFETY_REMINDER }
            .maxByOrNull { it.timestampMs }

        val now = System.currentTimeMillis()
        val shouldRemind = lastPetAlert == null ||
                (now - lastPetAlert.timestampMs) >= PET_REMINDER_INTERVAL_MS

        if (shouldRemind) {
            sessionRepo.addAlert(SessionAlert(
                type = AlertType.PET_SAFETY_REMINDER,
                message = "반려동물과 함께 계신가요? 실내 온도(${state.interiorTempCelsius}°C)와 환기 상태를 확인해 주세요."
            ))
        }
    }
}
