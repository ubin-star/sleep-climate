package com.sleepclimate.app.data.vehicle

import android.content.Context
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.flow.Flow

/**
 * 차량 데이터·공조 제어 접근 지점
 *
 * 구현체
 * - [PleosVehicleRepository]       : Pleos Connect Vehicle SDK (실제 차량 / Pleos Connect Emulator)
 * - [simulation.SimulatedVehicleRepository] : 자동 제어 시연용 가상 차량
 * - [SwitchableVehicleRepository]  : 위 둘을 런타임에 전환
 *
 * 제어 명령은 지원이 확인된 구현에서만 동작하고, 기본값은 [CommandResult.NOT_SUPPORTED]이다.
 */
abstract class VehicleRepository(protected val context: Context) {

    open fun initialize() = Unit

    open fun release() = Unit

    /** 차량이 제공하는 기능 범위 확인 (SDK의 checkXXXCapabilities 대응) */
    abstract suspend fun checkSupportLevel(): VehicleSupportLevel

    /** 차량 상태 주기적 구독 */
    abstract fun observeVehicleState(): Flow<VehicleState>

    open suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult = CommandResult.NOT_SUPPORTED

    open suspend fun setHvacOnOff(on: Boolean): CommandResult = CommandResult.NOT_SUPPORTED

    open suspend fun setHvacMode(mode: HvacMode): CommandResult = CommandResult.NOT_SUPPORTED

    open suspend fun setFanSpeed(level: Int): CommandResult = CommandResult.NOT_SUPPORTED
}

enum class CommandResult {
    SENT,           // 명령 전송됨 (결과는 상태 변화로 확인)
    REJECTED,       // 차량이 명령 거부
    NOT_SUPPORTED,  // 해당 차량 또는 SDK에서 지원 안 함
    ERROR           // 전송 실패·응답 없음
}

/** 공조 동작 모드 */
enum class HvacMode {
    COOLING,  // 냉방 (에어컨)
    HEATING,  // 난방
    OFF,      // 공조 끄기
    AUTO      // 자동
}
