package com.sleepclimate.app.domain.model

import com.sleepclimate.app.data.vehicle.HvacMode

/**
 * 차량 상태 스냅샷
 * - Vehicle SDK에서 읽어온 값을 담는 모델
 * - null = SDK에서 해당 데이터를 제공하지 않거나 아직 미수신
 */
data class VehicleState(
    val interiorTempCelsius: Float? = null,     // 실내 온도
    val exteriorTempCelsius: Float? = null,     // 외부 온도
    val isHvacOn: Boolean? = null,              // 공조 작동 여부
    val hvacMode: HvacMode? = null,             // 공조 동작 모드 (냉방/난방 등)
    val hvacTargetTempCelsius: Float? = null,   // 공조 설정 온도
    val batteryPercent: Int? = null,            // 배터리 잔량 (EV)
    val fuelPercent: Int? = null,               // 연료 잔량 (ICE)
    val isParked: Boolean? = null,              // 주차 상태 여부
    val isConnected: Boolean = false,           // 차량 연결 여부
    val lastUpdatedMs: Long = 0L,               // 마지막 데이터 수신 시각
    val supportLevel: VehicleSupportLevel = VehicleSupportLevel.UNKNOWN
)

/**
 * SDK에서 확인된 기능 지원 수준
 * - UNKNOWN   : 아직 확인 안 됨
 * - READ_ONLY : 온도 읽기만 가능, 제어 불가
 * - FULL      : 온도 읽기 + 공조 제어 모두 가능
 * - NONE      : 연결은 됐으나 해당 데이터/제어 미지원
 */
enum class VehicleSupportLevel {
    UNKNOWN,
    READ_ONLY,
    FULL,
    NONE
}
