package com.sleepclimate.app.domain.usecase

import com.sleepclimate.app.FakeVehicleRepository
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.ControlStatus
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 자동 공조 제어(FULL 지원 차량) 단위 테스트
 * - 목표 22°C, 허용 범위 20~24°C
 */
class AutoClimateControlTest {

    private lateinit var fakeVehicleRepo: FakeVehicleRepository
    private lateinit var sessionRepo: SessionRepository
    private lateinit var useCase: MonitorSessionUseCase

    private fun state(
        temp: Float,
        hvacOn: Boolean = false,
        mode: HvacMode? = if (hvacOn) HvacMode.COOLING else HvacMode.OFF,
        battery: Int = 80
    ) = VehicleState(
        interiorTempCelsius = temp,
        isHvacOn            = hvacOn,
        hvacMode            = mode,
        batteryPercent      = battery,
        isParked            = true,
        isConnected         = true,
        lastUpdatedMs       = System.currentTimeMillis(),
        supportLevel        = VehicleSupportLevel.FULL
    )

    @Before
    fun setUp() {
        fakeVehicleRepo = FakeVehicleRepository().apply { hvacCommandResult = CommandResult.SENT }
        sessionRepo = SessionRepository()
        useCase = MonitorSessionUseCase(fakeVehicleRepo, sessionRepo)
        sessionRepo.startSession(RestSession(
            targetTempCelsius  = 22.0f,
            tempRangeLow       = 20.0f,
            tempRangeHigh      = 24.0f,
            endTime            = System.currentTimeMillis() + 60 * 60 * 1000L,
            energyAlertPercent = 20
        ))
    }

    @Test
    fun `상한 초과 시 냉방을 켠다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 25.0f))

        assertEquals(listOf(HvacMode.COOLING), fakeVehicleRepo.sentModes)
        assertEquals(ControlStatus.PENDING, sessionRepo.controlEvents.value.last().status)
    }

    @Test
    fun `하한 미만 시 난방을 켠다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 18.5f))

        assertEquals(listOf(HvacMode.HEATING), fakeVehicleRepo.sentModes)
    }

    @Test
    fun `반영 확인 전에는 같은 명령을 다시 보내지 않는다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 25.0f))
        useCase.onVehicleStateUpdate(state(temp = 25.0f))

        assertEquals(1, fakeVehicleRepo.sentModes.size)
    }

    @Test
    fun `차량 상태에 냉방이 반영되면 CONFIRMED로 기록한다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 25.0f))
        useCase.onVehicleStateUpdate(state(temp = 24.5f, hvacOn = true, mode = HvacMode.COOLING))

        assertEquals(ControlStatus.CONFIRMED, sessionRepo.controlEvents.value.last().status)
    }

    @Test
    fun `냉방 중 목표 온도에 도달하면 공조를 끈다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 21.9f, hvacOn = true, mode = HvacMode.COOLING))

        assertEquals(listOf(HvacMode.OFF), fakeVehicleRepo.sentModes)
    }

    @Test
    fun `허용 범위 안에서 공조가 꺼져 있으면 아무 명령도 보내지 않는다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 23.0f))

        assertTrue(fakeVehicleRepo.sentModes.isEmpty())
    }

    @Test
    fun `에너지가 기준 이하이면 공조를 끄고 자동 제어를 중단한다`() = runTest {
        useCase.onVehicleStateUpdate(state(temp = 25.0f, hvacOn = true, battery = 15))

        assertEquals(listOf(HvacMode.OFF), fakeVehicleRepo.sentModes)
        assertTrue(sessionRepo.autoControlSuspended)
        assertTrue(sessionRepo.alerts.value.any { it.type == AlertType.AUTO_CONTROL_STOPPED })

        // 중단 이후에는 온도가 벗어나도 더 이상 제어하지 않는다
        useCase.onVehicleStateUpdate(state(temp = 26.0f, battery = 15))
        assertEquals(1, fakeVehicleRepo.sentModes.size)
    }

    @Test
    fun `차량이 명령을 거부하면 REJECTED로 기록하고 알림을 남긴다`() = runTest {
        fakeVehicleRepo.hvacCommandResult = CommandResult.REJECTED
        useCase.onVehicleStateUpdate(state(temp = 25.0f))

        assertEquals(ControlStatus.REJECTED, sessionRepo.controlEvents.value.last().status)
        assertTrue(sessionRepo.alerts.value.any { it.type == AlertType.COMMAND_REJECTED })
        assertEquals(1, sessionRepo.commandFailCount.value)
    }
}
