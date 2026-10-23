package com.sleepclimate.app.domain.model

import java.time.LocalTime

/**
 * 휴식 세션 설정값
 * - 사용자가 잠들기 전에 한 번 설정하는 단위
 */
data class RestSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetTempCelsius: Float,       // 목표 온도 (°C)
    val tempRangeLow: Float,            // 허용 온도 하한
    val tempRangeHigh: Float,           // 허용 온도 상한
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long,                  // 세션 종료 예정 시각 (ms)
    val energyAlertPercent: Int = 20,   // 배터리/연료 알림 기준 (%)
    val hasPet: Boolean = false,        // 반려동물 동반 여부
    val status: SessionStatus = SessionStatus.IDLE
)

enum class SessionStatus {
    IDLE,       // 설정 전
    RUNNING,    // 세션 진행 중
    PAUSED,     // 일시 중단
    ENDED,      // 정상 종료
    ERROR       // 오류 종료
}
