package com.sleepclimate.app.domain.model

/** 잠들기 전에 한 번 설정하는 휴식 세션 */
data class RestSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val targetTempCelsius: Float,
    val tempRangeLow: Float,
    val tempRangeHigh: Float,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long,
    val energyAlertPercent: Int = 20,   // 배터리·연료가 이 값 이하이면 자동 제어를 멈추고 알림
    val hasPet: Boolean = false,
    val status: SessionStatus = SessionStatus.IDLE
)

enum class SessionStatus { IDLE, RUNNING, ENDED }
