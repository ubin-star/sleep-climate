package com.sleepclimate.app.domain.usecase

import com.sleepclimate.app.FakeVehicleRepository
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 알림 모드(READ_ONLY) 차량에서의 알림 생성 규칙 */
class MonitorSessionUseCaseTest {

    private lateinit var fakeVehicleRepo: FakeVehicleRepository
    private lateinit var sessionRepo: SessionRepository
    private lateinit var useCase: MonitorSessionUseCase

    /** 기본 테스트 세션 — endTime을 충분히 멀리 설정 */
    private fun defaultSession(): RestSession = RestSession(
        targetTempCelsius  = 22.0f,
        tempRangeLow       = 20.0f,
        tempRangeHigh      = 24.0f,
        endTime            = System.currentTimeMillis() + 60 * 60 * 1000L, // 1시간 후
        energyAlertPercent = 20
    )

    /** 연결됨, 주차, READ_ONLY, 정상 온도 상태 */
    private fun normalState(
        temp: Float = 22.0f,
        battery: Int? = 80,
        isParked: Boolean? = true,
        isConnected: Boolean = true,
        supportLevel: VehicleSupportLevel = VehicleSupportLevel.READ_ONLY
    ) = VehicleState(
        interiorTempCelsius = temp,
        batteryPercent      = battery,
        isParked            = isParked,
        isConnected         = isConnected,
        lastUpdatedMs       = System.currentTimeMillis(),
        supportLevel        = supportLevel
    )

    @Before
    fun setUp() {
        fakeVehicleRepo = FakeVehicleRepository()
        sessionRepo     = SessionRepository()
        useCase         = MonitorSessionUseCase(fakeVehicleRepo, sessionRepo)
        sessionRepo.startSession(defaultSession())
    }

    @Test
    fun `온도 초과 시 TEMP_OUT_OF_RANGE 알림이 발생한다`() = runTest {
        val state = normalState(temp = 30.0f) // tempRangeHigh(24) 초과
        useCase.onVehicleStateUpdate(state)

        val alerts = sessionRepo.alerts.value
        assertTrue("알림이 1개 이상이어야 한다", alerts.isNotEmpty())
        assertTrue(
            "TEMP_OUT_OF_RANGE 알림이 있어야 한다",
            alerts.any { it.type == AlertType.TEMP_OUT_OF_RANGE }
        )
    }

    @Test
    fun `차량 연결 끊김 시 VEHICLE_DISCONNECTED 알림이 발생한다`() = runTest {
        val state = normalState(isConnected = false)
        useCase.onVehicleStateUpdate(state)

        val alerts = sessionRepo.alerts.value
        assertTrue(
            "VEHICLE_DISCONNECTED 알림이 있어야 한다",
            alerts.any { it.type == AlertType.VEHICLE_DISCONNECTED }
        )
    }

    @Test
    fun `배터리가 기준치 이하일 때 BATTERY_LOW 알림이 발생한다`() = runTest {
        val state = normalState(battery = 10) // energyAlertPercent(20) 이하
        useCase.onVehicleStateUpdate(state)

        val alerts = sessionRepo.alerts.value
        assertTrue(
            "BATTERY_LOW 알림이 있어야 한다",
            alerts.any { it.type == AlertType.BATTERY_LOW }
        )
    }

    @Test
    fun `주행 중일 때 NOT_PARKED 알림이 발생한다`() = runTest {
        val state = normalState(isParked = false)
        useCase.onVehicleStateUpdate(state)

        val alerts = sessionRepo.alerts.value
        assertTrue(
            "NOT_PARKED 알림이 있어야 한다",
            alerts.any { it.type == AlertType.NOT_PARKED }
        )
    }

    @Test
    fun `정상 조건에서는 알림이 발생하지 않는다`() = runTest {
        val state = normalState(temp = 22.0f, battery = 80)
        useCase.onVehicleStateUpdate(state)

        val alerts = sessionRepo.alerts.value
        val temperatureOrCriticalAlerts = alerts.filter { it.type in setOf(
            AlertType.TEMP_OUT_OF_RANGE,
            AlertType.BATTERY_LOW,
            AlertType.FUEL_LOW,
            AlertType.VEHICLE_DISCONNECTED,
            AlertType.NOT_PARKED,
            AlertType.COMMAND_FAILED,
            AlertType.COMMAND_REJECTED
        )}
        assertEquals("심각 알림이 없어야 한다", 0, temperatureOrCriticalAlerts.size)
    }

    @Test
    fun `쿨다운 기간 중 동일 AlertType 알림은 중복 추가되지 않는다`() = runTest {
        val state = normalState(temp = 30.0f) // TEMP_OUT_OF_RANGE 유발

        useCase.onVehicleStateUpdate(state)
        val countAfterFirst = sessionRepo.alerts.value
            .count { it.type == AlertType.TEMP_OUT_OF_RANGE }

        // 쿨다운 내에서 동일 상태로 재호출
        useCase.onVehicleStateUpdate(state)
        val countAfterSecond = sessionRepo.alerts.value
            .count { it.type == AlertType.TEMP_OUT_OF_RANGE }

        assertEquals("첫 번째 호출에서 알림 1개", 1, countAfterFirst)
        assertEquals("쿨다운 중 두 번째 호출은 추가되지 않음", 1, countAfterSecond)
    }
}
