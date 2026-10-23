package com.sleepclimate.app

import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.mockito.Mockito

/**
 * 단위 테스트용 VehicleRepository 가짜(Fake) 구현.
 *
 * VehicleRepository의 생성자는 Context를 받지만 순수 JVM 테스트에서
 * 실제 Android Context는 필요하지 않다. Mockito.mock()으로 Context 자리만 채우고
 * 모든 동작을 오버라이드하여 실제 Android API를 호출하지 않는다.
 */
class FakeVehicleRepository : VehicleRepository(
    Mockito.mock(android.content.Context::class.java)
) {
    var fakeState: VehicleState = VehicleState(
        isConnected   = true,
        lastUpdatedMs = System.currentTimeMillis(),
        supportLevel  = VehicleSupportLevel.READ_ONLY
    )

    var hvacCommandResult: CommandResult = CommandResult.NOT_SUPPORTED

    /** setHvacMode()로 전달된 명령 기록 */
    val sentModes = mutableListOf<HvacMode>()

    override fun initialize() { /* no-op */ }
    override fun release()    { /* no-op */ }

    override fun observeVehicleState(): Flow<VehicleState> = flowOf(fakeState)

    override suspend fun checkSupportLevel(): VehicleSupportLevel = fakeState.supportLevel

    override suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult = hvacCommandResult
    override suspend fun setHvacOnOff(on: Boolean): CommandResult = hvacCommandResult

    override suspend fun setHvacMode(mode: HvacMode): CommandResult {
        sentModes += mode
        return hvacCommandResult
    }
}
