package com.sleepclimate.app.data.vehicle

import android.content.Context
import com.sleepclimate.app.data.vehicle.simulation.SimulatedVehicleRepository
import com.sleepclimate.app.data.vehicle.simulation.SimulationScenario
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * 실제 차량(Pleos Vehicle SDK) 경로와 자동 제어 시뮬레이션 경로를 런타임에 전환하는 Repository
 *
 * 앱의 나머지 부분은 이 클래스만 바라보므로, 시뮬레이션을 켜고 꺼도 ViewModel을 다시 만들 필요가 없다.
 */
class SwitchableVehicleRepository(
    context: Context,
    private val real: PleosVehicleRepository,
    private val simulated: SimulatedVehicleRepository
) : VehicleRepository(context) {

    private val _simulationEnabled = MutableStateFlow(false)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    val simulationScenario: SimulationScenario get() = simulated.scenario

    private val active: VehicleRepository
        get() = if (_simulationEnabled.value) simulated else real

    fun setSimulation(enabled: Boolean, scenario: SimulationScenario = simulated.scenario) {
        if (enabled) simulated.reset(scenario)
        _simulationEnabled.value = enabled
    }

    /** 이 기기에서 Pleos 차량 서비스에 연결되는지 확인 (일반 폰·AVD에서는 false) */
    suspend fun isRealVehicleAvailable(): Boolean {
        if (real.isAvailable == null) real.checkSupportLevel()
        return real.isAvailable == true
    }

    override fun initialize() = real.initialize()
    override fun release() = real.release()

    override suspend fun checkSupportLevel(): VehicleSupportLevel = active.checkSupportLevel()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeVehicleState(): Flow<VehicleState> =
        _simulationEnabled.flatMapLatest { enabled ->
            if (enabled) simulated.observeVehicleState() else real.observeVehicleState()
        }

    override suspend fun setHvacTargetTemp(tempCelsius: Float) = active.setHvacTargetTemp(tempCelsius)
    override suspend fun setHvacOnOff(on: Boolean) = active.setHvacOnOff(on)
    override suspend fun setHvacMode(mode: HvacMode) = active.setHvacMode(mode)
    override suspend fun setFanSpeed(level: Int) = active.setFanSpeed(level)
}
