package com.sleepclimate.app.data.vehicle

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Pleos Connect Vehicle SDK 2.3.2 연동
 *
 * 사용 API
 * - HVAC : getHvacTemperatureCurrent(실내 온도), getHvacTemperature(설정 온도), getHvacPowerState, getHvacAcState,
 *          setHvacPower, setHvacAc, setHvacAuto, setHvacTemperature, setHvacFanSpeed
 * - EvBattery : getEvBatteryStateOfCharge / Fuel : getFuelPercent
 * - Gear : getCurrentGearSelectionState (GEAR_PARK 여부로 주차 판단)
 *
 * 필요 권한 (Pleos App Market 승인 범위): CAR_CLIMATE, CONTROL_CAR_CLIMATE, CAR_ENERGY, CAR_DRIVE
 *
 * Pleos 차량 서비스가 없는 기기(일반 폰·AVD)에서는 SDK 호출이 실패하거나 응답이 없으므로,
 * 모든 호출에 제한 시간을 두고 실패 시 '데이터 없음'으로 처리한다.
 */
class PleosVehicleRepository(context: Context) : VehicleRepository(context) {

    companion object {
        private const val TAG = "PleosVehicle"
        private const val CALL_TIMEOUT_MS = 2_000L
        private const val POLL_INTERVAL_MS = 5_000L
    }

    /** 운전석 공조 존 기준으로 조회·제어 */
    private val driverZone = SeatArea(SeatValue.ROW_1_LEFT)

    private var vehicle: Vehicle? = null

    /** checkSupportLevel() 이후 결정 — null이면 아직 확인 전 */
    @Volatile var isAvailable: Boolean? = null
        private set

    @Volatile private var controlSupported = false

    override fun initialize() {
        if (vehicle != null) return
        vehicle = runCatching { Vehicle(context).also { it.initialize() } }
            .onFailure { Log.w(TAG, "Vehicle SDK 초기화 실패: ${it.message}") }
            .getOrNull()
    }

    override fun release() {
        runCatching { vehicle?.release() }
        vehicle = null
    }

    override suspend fun checkSupportLevel(): VehicleSupportLevel {
        val hvac = runCatching { vehicle?.getHVAC() }.getOrNull() ?: return unavailable()

        val first = invoke<List<SeatArea>> { ok, fail ->
            hvac.checkHvacTemperatureCurrentCapabilities({ _, areas -> ok(areas) }, fail)
        }
        // 차량 서비스가 아예 응답하지 않으면 나머지 확인을 생략
        if (first is CallResult.TimedOut) return unavailable()
        val readTemp = (first as? CallResult.Ok)?.value
        val power = call<List<SeatArea>> { ok, fail ->
            hvac.checkHvacPowerCapabilities({ areas: List<SeatArea> -> ok(areas) }, fail)
        }
        val setTemp = call<Map<SeatArea, AreaBounds<Float>>> { ok, fail ->
            hvac.checkHvacTemperatureCapabilities({ bounds: Map<SeatArea, AreaBounds<Float>> -> ok(bounds) }, fail)
        }

        if (readTemp == null && power == null && setTemp == null) return unavailable()
        isAvailable = true
        controlSupported = !power.isNullOrEmpty() && !setTemp.isNullOrEmpty()
        return when {
            readTemp.isNullOrEmpty() -> VehicleSupportLevel.NONE
            controlSupported -> VehicleSupportLevel.FULL
            else -> VehicleSupportLevel.READ_ONLY
        }
    }

    private fun unavailable(): VehicleSupportLevel {
        isAvailable = false
        controlSupported = false
        return VehicleSupportLevel.NONE
    }

    override fun observeVehicleState(): Flow<VehicleState> = flow {
        if (isAvailable == null) checkSupportLevel()
        while (true) {
            emit(readState())
            delay(POLL_INTERVAL_MS)
        }
    }

    private suspend fun readState(): VehicleState {
        val v = vehicle
        if (v == null || isAvailable != true) {
            return VehicleState(isConnected = false, lastUpdatedMs = System.currentTimeMillis(), supportLevel = VehicleSupportLevel.NONE)
        }
        val hvac = v.getHVAC()
        val interior = call<Float?> { ok, fail -> hvac.getHvacTemperatureCurrent(driverZone, TemperatureUnit.CELSIUS, { ok(it) }, fail) }
        val setPoint = call<Float?> { ok, fail -> hvac.getHvacTemperature(driverZone, TemperatureUnit.CELSIUS, { ok(it) }, fail) }
        val power = call<Boolean?> { ok, fail -> hvac.getHvacPowerState(driverZone, { ok(it) }, fail) }
        val ac = call<Boolean?> { ok, fail -> hvac.getHvacAcState(driverZone, { ok(it) }, fail) }
        val soc = call<Int?> { ok, fail -> v.getEvBattery().getEvBatteryStateOfCharge({ ok(it) }, fail) }
        val fuel = if (soc == null) call<Int?> { ok, fail -> v.getFuel().getFuelPercent({ ok(it) }, fail) } else null
        val gear = call<VehicleGear?> { ok, fail -> v.getGear().getCurrentGearSelectionState({ ok(it) }, fail) }

        val mode = when {
            power == false -> HvacMode.OFF
            power == true && ac == true -> HvacMode.COOLING
            power == true && setPoint != null && interior != null && setPoint > interior -> HvacMode.HEATING
            power == true -> HvacMode.AUTO
            else -> null
        }
        return VehicleState(
            interiorTempCelsius = interior,
            exteriorTempCelsius = null,
            isHvacOn = power,
            hvacMode = mode,
            hvacTargetTempCelsius = setPoint,
            batteryPercent = soc,
            fuelPercent = fuel,
            isParked = gear?.let { it == VehicleGear.GEAR_PARK },
            isConnected = listOf(interior, power, soc, fuel, gear).any { it != null },
            lastUpdatedMs = System.currentTimeMillis(),
            supportLevel = if (controlSupported) VehicleSupportLevel.FULL else VehicleSupportLevel.READ_ONLY
        )
    }

    override suspend fun setHvacMode(mode: HvacMode): CommandResult {
        val hvac = controllableHvac() ?: return CommandResult.NOT_SUPPORTED
        return when (mode) {
            HvacMode.OFF -> command { ok, fail -> hvac.setHvacPower(driverZone, false, { _, _ -> ok(true) }, fail) }
            HvacMode.COOLING -> command { ok, fail -> hvac.setHvacPower(driverZone, true, { _, _ -> ok(true) }, fail) }
                .then { command { ok, fail -> hvac.setHvacAc(driverZone, true, { _, _ -> ok(true) }, fail) } }
            HvacMode.HEATING -> command { ok, fail -> hvac.setHvacPower(driverZone, true, { _, _ -> ok(true) }, fail) }
                .then { command { ok, fail -> hvac.setHvacAc(driverZone, false, { _, _ -> ok(true) }, fail) } }
            HvacMode.AUTO -> command { ok, fail -> hvac.setHvacPower(driverZone, true, { _, _ -> ok(true) }, fail) }
                .then { command { ok, fail -> hvac.setHvacAuto(driverZone, true, { _, _ -> ok(true) }, fail) } }
        }
    }

    override suspend fun setHvacOnOff(on: Boolean): CommandResult =
        setHvacMode(if (on) HvacMode.AUTO else HvacMode.OFF)

    override suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult {
        val hvac = controllableHvac() ?: return CommandResult.NOT_SUPPORTED
        return command { ok, fail ->
            hvac.setHvacTemperature(driverZone, tempCelsius, TemperatureUnit.CELSIUS, { _, _ -> ok(true) }, fail)
        }
    }

    override suspend fun setFanSpeed(level: Int): CommandResult {
        val hvac = controllableHvac() ?: return CommandResult.NOT_SUPPORTED
        return command { ok, fail -> hvac.setHvacFanSpeed(driverZone, level, { _, _ -> ok(true) }, fail) }
    }

    private fun controllableHvac() = if (controlSupported) runCatching { vehicle?.getHVAC() }.getOrNull() else null

    private suspend inline fun CommandResult.then(next: () -> CommandResult): CommandResult =
        if (this == CommandResult.SENT) next() else this

    /** 콜백형 SDK 호출 → 결과값, 실패·시간 초과 시 null */
    private suspend fun <T> call(block: (ok: (T) -> Unit, fail: (Exception) -> Unit) -> Unit): T? =
        when (val r = invoke(block)) {
            is CallResult.Ok -> r.value
            else -> null
        }

    /** 제어 명령 — 거부는 REJECTED, 응답 없음은 ERROR */
    private suspend fun command(block: (ok: (Boolean) -> Unit, fail: (Exception) -> Unit) -> Unit): CommandResult =
        when (invoke(block)) {
            is CallResult.Ok -> CommandResult.SENT
            CallResult.Failed -> CommandResult.REJECTED
            CallResult.TimedOut -> CommandResult.ERROR
        }

    private sealed interface CallResult<out T> {
        data class Ok<T>(val value: T) : CallResult<T>
        data object Failed : CallResult<Nothing>
        data object TimedOut : CallResult<Nothing>
    }

    private suspend fun <T> invoke(block: (ok: (T) -> Unit, fail: (Exception) -> Unit) -> Unit): CallResult<T> =
        withTimeoutOrNull(CALL_TIMEOUT_MS) {
            suspendCancellableCoroutine<CallResult<T>> { cont ->
                try {
                    block(
                        { value -> if (cont.isActive) cont.resume(CallResult.Ok(value)) },
                        { e ->
                            Log.d(TAG, "SDK 호출 실패: ${e.message}")
                            if (cont.isActive) cont.resume(CallResult.Failed)
                        }
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "SDK 호출 예외: ${e.message}")
                    if (cont.isActive) cont.resume(CallResult.Failed)
                }
            }
        } ?: CallResult.TimedOut
}
