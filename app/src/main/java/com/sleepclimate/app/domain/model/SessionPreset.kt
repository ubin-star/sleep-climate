package com.sleepclimate.app.domain.model

/** 홈의 휴식 모드 — 설정 화면의 초기값이자 음성 명령으로 바로 시작할 때의 값 */
enum class SessionPreset(
    val targetTempCelsius: Float,
    val rangeCelsius: Float,
    val durationHours: Int,
    val energyAlertPercent: Int
) {
    SUMMER_CAMPING(targetTempCelsius = 22f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 20),
    WINTER_CAMPING(targetTempCelsius = 20f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 30),
    SHORT_REST(targetTempCelsius = 22f, rangeCelsius = 1.0f, durationHours = 1, energyAlertPercent = 20),
    CUSTOM(targetTempCelsius = 22f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 20);

    fun toSession(nowMs: Long = System.currentTimeMillis()) = RestSession(
        targetTempCelsius = targetTempCelsius,
        tempRangeLow = targetTempCelsius - rangeCelsius,
        tempRangeHigh = targetTempCelsius + rangeCelsius,
        startTime = nowMs,
        endTime = nowMs + durationHours * 3_600_000L,
        energyAlertPercent = energyAlertPercent
    )
}
