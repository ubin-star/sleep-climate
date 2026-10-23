package com.sleepclimate.app.data.vehicle

import android.content.Context
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 차량 데이터 접근 Repository
 *
 * ═══════════════════════════════════════════════════════════
 *  Pleos Connect Vehicle SDK v2.3.2 연동 구조
 *  프로젝트 ID  : 314de939-581c-4f72-9a20-3b3deb887307
 *  클라이언트 ID: HjGgahiwRkOVR-DTI2AE5w
 * ═══════════════════════════════════════════════════════════
 *
 *  현재 사용 가능 (Vehicle State):
 *    - HVAC 상태 조회 (온도, 에어컨, 공기 순환)  → vehicle.getHvac()
 *    - EV 배터리 / 연료 상태 조회               → vehicle.getEvBattery(), vehicle.getFuel()
 *    - 주행 정보 조회 (주차 여부 등)             → vehicle.getDrivingMode()
 *
 *  Coming Soon (미지원 — 자동 제어 불가):
 *    - Vehicle Control (공조 켜기/끄기, 목표 온도 설정)
 *
 *  ∴ SLEEP CLIMATE는 "알림 모드(READ_ONLY)"로 동작
 *
 * ═══════════════════════════════════════════════════════════
 *  SDK 연동 방법 (gradle 좌표 확인 후):
 *
 *  1. settings.gradle.kts repositories 블록에 maven 추가:
 *     maven { url = uri("https://???") }   ← Docs > Vehicle SDK > Setup 확인
 *
 *  2. app/build.gradle.kts에 의존성 추가:
 *     implementation("ai.pleos.connect:vehicle-sdk:2.3.2")
 *
 *  3. AndroidManifest.xml에 클라이언트 ID 메타데이터 추가:
 *     <meta-data
 *         android:name="ai.pleos.connect.CLIENT_ID"
 *         android:value="HjGgahiwRkOVR-DTI2AE5w" />
 *
 *  4. 아래 TODO 주석 부분을 실제 SDK 코드로 교체
 * ═══════════════════════════════════════════════════════════
 */
open class VehicleRepository(private val context: Context) {

    // TODO: SDK 연동 후 실제 Vehicle 인스턴스로 교체
    // private lateinit var vehicle: Vehicle

    /**
     * SDK 초기화
     * 실제 코드:
     *   vehicle = Vehicle.getInstance(context)
     *   vehicle.initialize(
     *       clientId = "HjGgahiwRkOVR-DTI2AE5w",
     *       onComplete = { /* 연결 성공 */ },
     *       onFailed   = { errorCode -> /* 연결 실패 처리 */ }
     *   )
     */
    open fun initialize() {
        // TODO: vehicle.initialize(...)
    }

    /**
     * SDK 해제 — Activity/Fragment onDestroy에서 반드시 호출
     * 실제 코드: vehicle.release()
     */
    open fun release() {
        // TODO: vehicle.release()
    }

    /**
     * 기능 지원 수준 확인
     *
     * Vehicle Control이 Coming Soon이므로 현재는 항상 READ_ONLY
     * Vehicle Control 지원 시작되면 FULL로 변경
     *
     * 실제 코드:
     *   vehicle.getHvac().checkHvacCapabilities(
     *       onComplete = { capabilities ->
     *           // capabilities로 지원 여부 판단
     *       },
     *       onFailed = { /* NONE */ }
     *   )
     */
    open suspend fun checkSupportLevel(): VehicleSupportLevel {
        // Vehicle Control = Coming Soon → 읽기 전용만 가능
        delay(300)
        return VehicleSupportLevel.READ_ONLY
    }

    /**
     * 차량 상태 주기적 구독 (5초마다)
     *
     * 실제 SDK 구조:
     *   // HVAC 온도 콜백 등록
     *   vehicle.getHvac().registerHvacCallback(object : HvacCallback {
     *       override fun onHvacTemperatureChanged(zone: Int, temp: Float) {
     *           // temp → interiorTempCelsius
     *       }
     *   })
     *
     *   // EV 배터리 조회
     *   vehicle.getEvBattery().getBatteryLevel(
     *       onComplete = { level -> /* batteryPercent */ },
     *       onFailed   = { /* null 처리 */ }
     *   )
     *
     *   // 주차 여부 — DrivingMode로 확인
     *   vehicle.getDrivingMode().getGearSelection(
     *       onComplete = { gear ->
     *           val isParked = (gear == GearSelection.PARK)
     *       },
     *       onFailed = { }
     *   )
     */
    open fun observeVehicleState(): Flow<VehicleState> = flow {
        var tick = 0
        while (true) {
            // ─── TODO: 아래 Mock을 실제 SDK 호출로 교체 ──────────────────────
            val mockState = VehicleState(
                interiorTempCelsius    = 22.5f + (tick % 5) * 0.3f,  // HVAC 실내 온도
                exteriorTempCelsius    = 15.0f,                        // HVAC 외부 온도
                isHvacOn               = tick % 10 < 7,               // HVAC 작동 여부
                hvacTargetTempCelsius  = 22.0f,                       // HVAC 설정 온도
                batteryPercent         = (80 - tick).coerceAtLeast(5), // EV 배터리
                fuelPercent            = null,                          // ICE 연료 (해당 없으면 null)
                isParked               = true,                          // 기어 = PARK 여부
                isConnected            = true,
                lastUpdatedMs          = System.currentTimeMillis(),
                supportLevel           = VehicleSupportLevel.READ_ONLY // Control Coming Soon
            )
            // ──────────────────────────────────────────────────────────────────
            emit(mockState)
            delay(5_000L)
            tick++
        }
    }

    /**
     * 공조 목표 온도 설정 — Vehicle Control Coming Soon이므로 현재 항상 NOT_SUPPORTED
     *
     * Vehicle Control 지원 시작되면:
     *   vehicle.getHvac().setHvacTemperature(
     *       zone    = HvacZone.DRIVER,
     *       tempC   = tempCelsius,
     *       onComplete = { CommandResult.SENT },
     *       onFailed   = { CommandResult.REJECTED }
     *   )
     */
    open suspend fun setHvacTargetTemp(tempCelsius: Float): CommandResult {
        return CommandResult.NOT_SUPPORTED // Vehicle Control = Coming Soon
    }

    /**
     * 공조 켜기/끄기 — Vehicle Control Coming Soon이므로 현재 항상 NOT_SUPPORTED
     */
    open suspend fun setHvacOnOff(on: Boolean): CommandResult {
        return CommandResult.NOT_SUPPORTED // Vehicle Control = Coming Soon
    }

    /**
     * 공조 모드 설정 (냉방/난방/끄기/자동) — Vehicle Control Coming Soon이므로 현재 NOT_SUPPORTED.
     *
     * ── SDK 연동 시 교체 지점 ──
     * Vehicle Control API 출시 후 아래 TODO를 실제 SDK 호출로 교체:
     *   TODO: vehicle.getHvac().setHvacMode(
     *       mode      = when (mode) {
     *           HvacMode.COOLING -> HvacSdkMode.COOLING
     *           HvacMode.HEATING -> HvacSdkMode.HEATING
     *           HvacMode.OFF     -> HvacSdkMode.OFF
     *           HvacMode.AUTO    -> HvacSdkMode.AUTO
     *       },
     *       onComplete = { /* CommandResult.SENT */ },
     *       onFailed   = { /* CommandResult.REJECTED */ }
     *   )
     */
    open suspend fun setHvacMode(mode: HvacMode): CommandResult {
        return CommandResult.NOT_SUPPORTED // Vehicle Control = Coming Soon
    }

    /**
     * 팬 속도 설정 (1=약 ~ 3=강) — Vehicle Control Coming Soon이므로 현재 NOT_SUPPORTED.
     *
     * ── SDK 연동 시 교체 지점 ──
     * Vehicle Control API 출시 후 아래 TODO를 실제 SDK 호출로 교체:
     *   TODO: vehicle.getHvac().setFanSpeed(
     *       level      = level,      // 1(Low) ~ 3(High)
     *       onComplete = { /* CommandResult.SENT */ },
     *       onFailed   = { /* CommandResult.REJECTED */ }
     *   )
     */
    open suspend fun setFanSpeed(level: Int): CommandResult {
        return CommandResult.NOT_SUPPORTED // Vehicle Control = Coming Soon
    }
}

enum class CommandResult {
    SENT,           // 명령 전송됨 (결과는 상태 변화로 확인)
    REJECTED,       // 차량이 명령 거부
    NOT_SUPPORTED,  // 해당 차량 또는 SDK에서 지원 안 함 (Coming Soon 포함)
    ERROR           // 전송 실패
}

/**
 * 공조 동작 모드
 * Vehicle Control API 출시 후 실제 SDK 열거형으로 매핑 예정
 */
enum class HvacMode {
    COOLING,  // 냉방 (에어컨)
    HEATING,  // 난방 (히터)
    OFF,      // 공조 끄기
    AUTO      // 자동 (온도 기반 냉/난방 자동 전환)
}

