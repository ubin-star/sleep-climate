package com.sleepclimate.app

import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mockito

/** 테스트용 차량 — 상태를 직접 지정하고 보낸 명령을 기록 */
class FakeVehicleRepository : VehicleRepository(
    Mockito.mock(android.content.Context::class.java)
) {
    var fakeState: VehicleState = VehicleState(
        isConnected   = true,
        lastUpdatedMs = System.currentTimeMillis(),
        supportLevel  = VehicleSupportLevel.READ_ONLY
    )

    var hvacCommandResult: CommandResult = CommandResult.NOT_SUPPORTED

    val sentModes = mutableListOf<HvacMode>()

    override fun observeVehicleState(): Flow<VehicleState> = flowOf(fakeState)

    override suspend fun checkSupportLevel(): VehicleSupportLevel = fakeState.supportLevel

    override suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult = hvacCommandResult
    override suspend fun setHvacOnOff(on: Boolean): CommandResult = hvacCommandResult

    override suspend fun setHvacMode(mode: HvacMode): CommandResult {
        sentModes += mode
        return hvacCommandResult
    }
}
