package com.sleepclimate.app.domain.model

import com.sleepclimate.app.data.vehicle.HvacMode

/**
 * 세션 중 앱이 보낸 공조 제어 명령 기록
 * - 명령 전송만으로 성공 처리하지 않고, 이후 차량 상태로 반영 여부를 확인한다
 */
data class ControlEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val mode: HvacMode,
    val targetTempCelsius: Float,
    val status: ControlStatus,
    val timestampMs: Long = System.currentTimeMillis()
)

/** 세션 결과 그래프용 실내 온도 기록 */
data class TempSample(
    val timestampMs: Long,
    val tempCelsius: Float,
    val hvacOn: Boolean
)

enum class ControlStatus {
    PENDING,        // 전송됨, 차량 상태로 반영 확인 대기
    CONFIRMED,      // 차량 상태에서 반영 확인
    REJECTED,       // 차량이 거부했거나 전송 실패
    TIMED_OUT,      // 제한 시간 내 반영을 확인하지 못함
    NOT_SUPPORTED   // 차량·SDK 미지원
}
