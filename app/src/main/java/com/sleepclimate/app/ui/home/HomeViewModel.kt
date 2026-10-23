package com.sleepclimate.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sleepclimate.app.data.vehicle.SwitchableVehicleRepository
import com.sleepclimate.app.data.vehicle.simulation.SimulationScenario
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val vehicleRepository: SwitchableVehicleRepository
) : ViewModel() {

    val simulationEnabled: StateFlow<Boolean> = vehicleRepository.simulationEnabled
    val simulationScenario: SimulationScenario get() = vehicleRepository.simulationScenario

    fun setSimulation(enabled: Boolean, scenario: SimulationScenario) {
        vehicleRepository.setSimulation(enabled, scenario)
    }

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _supportLevel = MutableStateFlow(VehicleSupportLevel.UNKNOWN)
    val supportLevel: StateFlow<VehicleSupportLevel> = _supportLevel.asStateFlow()

    /** 세션 시작 가능 여부 — 불가하면 그 사유를 함께 노출 */
    val startAvailability: StateFlow<StartAvailability> =
        combine(_vehicleState, _supportLevel) { state, level ->
            when {
                level == VehicleSupportLevel.UNKNOWN || state.lastUpdatedMs == 0L ->
                    StartAvailability.CHECKING
                level == VehicleSupportLevel.NONE -> StartAvailability.NO_SUPPORT
                !state.isConnected                -> StartAvailability.DISCONNECTED
                state.isParked == false           -> StartAvailability.DRIVING
                else                              -> StartAvailability.READY
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StartAvailability.CHECKING)

    private val _fallbackSimulation = MutableStateFlow(false)
    /** Pleos 차량 서비스가 없는 기기라서 자동으로 시뮬레이션으로 실행 중 */
    val fallbackSimulation: StateFlow<Boolean> = _fallbackSimulation.asStateFlow()

    init {
        viewModelScope.launch {
            if (!vehicleRepository.isRealVehicleAvailable() && !vehicleRepository.simulationEnabled.value) {
                vehicleRepository.setSimulation(true, SimulationScenario.SUMMER_NIGHT)
                _fallbackSimulation.value = true
            }
        }
        checkSupportLevel()
        observeVehicle()
    }

    /** 실제 차량 ↔ 시뮬레이션 전환 시마다 지원 수준을 다시 확인 */
    private fun checkSupportLevel() {
        viewModelScope.launch {
            vehicleRepository.simulationEnabled.collectLatest {
                _supportLevel.value = VehicleSupportLevel.UNKNOWN
                _supportLevel.value = vehicleRepository.checkSupportLevel()
            }
        }
    }

    private fun observeVehicle() {
        viewModelScope.launch {
            vehicleRepository.observeVehicleState().collect { state ->
                _vehicleState.value = state
            }
        }
    }

    class Factory(private val repo: SwitchableVehicleRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo) as T
        }
    }
}

enum class StartAvailability {
    CHECKING,
    READY,
    DISCONNECTED,
    NO_SUPPORT,
    DRIVING
}
