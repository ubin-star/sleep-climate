package com.sleepclimate.app.domain.usecase

import com.sleepclimate.app.FakeVehicleRepository
import com.sleepclimate.app.data.session.SessionRepository
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionPhase
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.domain.model.effectiveRangeAt
import com.sleepclimate.app.domain.model.phaseAt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 잠들기 → 수면 → 기상 단계와 기상 단계의 사전 온도 맞춤 */
class WakePhaseTest {

    private val hour = 60 * 60_000L
    private val minute = 60_000L

    private fun session(start: Long, end: Long) = RestSession(
        targetTempCelsius = 22f, tempRangeLow = 20.5f, tempRangeHigh = 23.5f,
        startTime = start, endTime = end
    )

    @Test
    fun `8시간 세션은 15분 잠들기, 10분 기상 구간을 가진다`() {
        val start = 0L
        val s = session(start, start + 8 * hour)
        assertEquals(SessionPhase.FALLING_ASLEEP, s.phaseAt(start + 14 * minute))
        assertEquals(SessionPhase.SLEEPING, s.phaseAt(start + 16 * minute))
        assertEquals(SessionPhase.SLEEPING, s.phaseAt(s.endTime - 11 * minute))
        assertEquals(SessionPhase.WAKING, s.phaseAt(s.endTime - 9 * minute))
    }

    @Test
    fun `기상 단계에서는 허용 범위가 절반으로 좁아진다`() {
        val s = session(0L, 8 * hour)
        assertEquals(20.5f..23.5f, s.effectiveRangeAt(4 * hour))
        val waking = s.effectiveRangeAt(s.endTime - 5 * minute)
        assertEquals(21.25f, waking.start, 0.001f)
        assertEquals(22.75f, waking.endInclusive, 0.001f)
    }

    @Test
    fun `기상 단계에서는 원래 범위 안이어도 목표에서 멀면 냉방을 켠다`() = runTest {
        val repo = FakeVehicleRepository().apply { hvacCommandResult = CommandResult.SENT }
        val sessionRepo = SessionRepository()
        val now = System.currentTimeMillis()
        sessionRepo.startSession(session(now - 2 * hour, now + 5 * minute))
        val useCase = MonitorSessionUseCase(repo, sessionRepo)

        useCase.onVehicleStateUpdate(
            VehicleState(
                interiorTempCelsius = 23.2f, isHvacOn = false, hvacMode = HvacMode.OFF, batteryPercent = 80,
                isParked = true, isConnected = true, lastUpdatedMs = now, supportLevel = VehicleSupportLevel.FULL
            )
        )

        assertEquals(listOf(HvacMode.COOLING), repo.sentModes)
        assertTrue(sessionRepo.alerts.value.any { it.type == AlertType.SESSION_ENDING_SOON })
    }

    @Test
    fun `휴식 연장은 종료 시각을 늦추고 기상 알림을 다시 받을 수 있게 한다`() {
        val sessionRepo = SessionRepository()
        val now = System.currentTimeMillis()
        sessionRepo.startSession(session(now - hour, now + minute))
        sessionRepo.markWakeNotified()

        sessionRepo.extendSession(10 * minute)

        assertTrue(sessionRepo.currentSession.value!!.endTime >= now + 11 * minute - 1000)
        assertEquals(false, sessionRepo.wakeNotified)
    }
}
