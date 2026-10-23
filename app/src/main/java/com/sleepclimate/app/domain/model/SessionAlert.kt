package com.sleepclimate.app.domain.model

/**
 * 세션 중 발생하는 알림 이벤트
 */
data class SessionAlert(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AlertType,
    val message: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

enum class AlertType {
    VEHICLE_DISCONNECTED,       // 차량 연결 끊김
    DATA_STALE,                 // 데이터 갱신 지연
    COMMAND_REJECTED,           // 명령 거부됨
    COMMAND_FAILED,             // 명령 실패 (재시도 초과)
    BATTERY_LOW,                // 배터리 부족
    FUEL_LOW,                   // 연료 부족
    TEMP_OUT_OF_RANGE,          // 온도 허용 범위 벗어남
    NOT_PARKED,                 // 주차 상태 아님
    SESSION_ENDING_SOON,        // 세션 종료 5분 전
    PET_SAFETY_REMINDER,        // 반려동물 안전 확인 알림
    AUTO_CONTROL_STOPPED        // 에너지 보호를 위해 자동 제어 중단
}
