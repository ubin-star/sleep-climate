package com.sleepclimate.app.data.vehicle.simulation

import com.sleepclimate.app.data.vehicle.HvacMode

/**
 * 시연용 차량 실내 온도 모델 (실제 차량 데이터가 아님)
 *
 * - 공조가 꺼져 있으면 실내 온도가 외부 온도 쪽으로 서서히 수렴
 * - 냉방·난방이 켜지면 목표 온도 방향으로 이동
 * - 공조 가동 중 배터리 소모가 커짐
 *
 * 시연 영상에서 변화가 보이도록 1 tick(5초)에 수 분 분량의 변화를 압축해 적용한다.
 */
class CabinThermalSimulator(scenario: SimulationScenario = SimulationScenario.SUMMER_NIGHT) {

    companion object {
        /** tick당 외부 온도 쪽으로 수렴하는 비율 */
        const val LEAK_RATE = 0.02f
        /** tick당 공조가 실내 온도를 움직이는 최대 폭 (°C) */
        const val HVAC_STEP_CELSIUS = 0.6f
        const val BATTERY_DRAIN_HVAC_ON = 0.5f
        const val BATTERY_DRAIN_IDLE = 0.05f
    }

    var scenario: SimulationScenario = scenario
        private set
    var interiorTemp: Float = scenario.initialInteriorCelsius
        private set
    var exteriorTemp: Float = scenario.exteriorCelsius
        private set
    var batteryPercent: Float = scenario.initialBatteryPercent
        private set
    var hvacMode: HvacMode = HvacMode.OFF
        private set
    var hvacTargetTemp: Float = 22f
        private set

    val isHvacOn: Boolean get() = hvacMode != HvacMode.OFF

    @Synchronized
    fun reset(newScenario: SimulationScenario) {
        scenario = newScenario
        interiorTemp = newScenario.initialInteriorCelsius
        exteriorTemp = newScenario.exteriorCelsius
        batteryPercent = newScenario.initialBatteryPercent
        hvacMode = HvacMode.OFF
    }

    @Synchronized
    fun setMode(mode: HvacMode) {
        hvacMode = mode
    }

    @Synchronized
    fun setTarget(tempCelsius: Float) {
        hvacTargetTemp = tempCelsius
    }

    /** 한 tick 진행 */
    @Synchronized
    fun step() {
        interiorTemp += (exteriorTemp - interiorTemp) * LEAK_RATE

        val effectiveMode = if (hvacMode == HvacMode.AUTO) {
            if (interiorTemp > hvacTargetTemp) HvacMode.COOLING else HvacMode.HEATING
        } else {
            hvacMode
        }
        when (effectiveMode) {
            HvacMode.COOLING -> interiorTemp -= HVAC_STEP_CELSIUS
            HvacMode.HEATING -> interiorTemp += HVAC_STEP_CELSIUS
            else -> Unit
        }

        val drain = if (isHvacOn) BATTERY_DRAIN_HVAC_ON else BATTERY_DRAIN_IDLE
        batteryPercent = (batteryPercent - drain).coerceAtLeast(0f)
    }
}

/**
 * 시연 시나리오 — 외부 기온과 시작 상태
 */
enum class SimulationScenario(
    val exteriorCelsius: Float,
    val initialInteriorCelsius: Float,
    val initialBatteryPercent: Float
) {
    SUMMER_NIGHT(exteriorCelsius = 30f, initialInteriorCelsius = 23f, initialBatteryPercent = 80f),
    WINTER_NIGHT(exteriorCelsius = -3f, initialInteriorCelsius = 21f, initialBatteryPercent = 80f),
    LOW_BATTERY(exteriorCelsius = 30f, initialInteriorCelsius = 23.5f, initialBatteryPercent = 23f)
}
