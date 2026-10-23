package com.sleepclimate.app.domain.model

/**
 * 원터치 시작용 휴식 프리셋
 * 세션 설정 화면의 초기값이 되며, 사용자는 시작 전에 값을 조정할 수 있다.
 */
enum class SessionPreset(
    val emoji: String,
    val targetTempCelsius: Float,
    val rangeCelsius: Float,
    val durationHours: Int,
    val energyAlertPercent: Int
) {
    SUMMER_CAMPING("🌙", targetTempCelsius = 22f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 20),
    WINTER_CAMPING("❄️", targetTempCelsius = 20f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 30),
    SHORT_REST("☕", targetTempCelsius = 22f, rangeCelsius = 1.0f, durationHours = 1, energyAlertPercent = 20),
    CUSTOM("⚙️", targetTempCelsius = 22f, rangeCelsius = 1.5f, durationHours = 8, energyAlertPercent = 20)
}
