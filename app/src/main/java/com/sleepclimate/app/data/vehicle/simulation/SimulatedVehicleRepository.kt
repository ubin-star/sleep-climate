package com.sleepclimate.app.data.vehicle.simulation

import android.content.Context
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.data.vehicle.VehicleRepository
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlin.math.roundToInt

/**
 * 자동 제어 시연용 가상 차량
 *
 * Pleos Vehicle SDK의 공조 제어(Vehicle Control)가 공개되기 전까지,
 * 자동 제어 로직이 실제로 어떻게 동작하는지 보여주기 위한 시뮬레이션이다.
 * 명령은 [CabinThermalSimulator]에 반영되고, 다음 상태 업데이트에서 반영 여부가 확인된다.
 */
class SimulatedVehicleRepository(
    context: Context,
    private val simulator: CabinThermalSimulator = CabinThermalSimulator()
) : VehicleRepository(context) {

    companion object {
        const val TICK_MS = 5_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 구독자가 여럿이어도 시뮬레이터는 한 번만 진행되도록 공유 */
    private val sharedState: Flow<VehicleState> = flow {
        while (true) {
            emit(snapshot())
            delay(TICK_MS)
            simulator.step()
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    val scenario: SimulationScenario get() = simulator.scenario

    fun reset(scenario: SimulationScenario) {
        simulator.reset(scenario)
    }

    override fun initialize() = Unit
    override fun release() = Unit

    override suspend fun checkSupportLevel(): VehicleSupportLevel {
        delay(300)
        return VehicleSupportLevel.FULL
    }

    override fun observeVehicleState(): Flow<VehicleState> = sharedState

    override suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult {
        simulator.setTarget(tempCelsius)
        return CommandResult.SENT
    }

    override suspend fun setHvacOnOff(on: Boolean): CommandResult {
        simulator.setMode(if (on) HvacMode.AUTO else HvacMode.OFF)
        return CommandResult.SENT
    }

    override suspend fun setHvacMode(mode: HvacMode): CommandResult {
        simulator.setMode(mode)
        return CommandResult.SENT
    }

    private fun Float.oneDecimal(): Float = (this * 10).roundToInt() / 10f

    private fun snapshot(): VehicleState = VehicleState(
        interiorTempCelsius   = simulator.interiorTemp.oneDecimal(),
        exteriorTempCelsius   = simulator.exteriorTemp,
        isHvacOn              = simulator.isHvacOn,
        hvacMode              = simulator.hvacMode,
        hvacTargetTempCelsius = simulator.hvacTargetTemp,
        batteryPercent        = simulator.batteryPercent.toInt(),
        fuelPercent           = null,
        isParked              = true,
        isConnected           = true,
        lastUpdatedMs         = System.currentTimeMillis(),
        supportLevel          = VehicleSupportLevel.FULL
    )
}
