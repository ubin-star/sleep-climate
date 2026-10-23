package com.sleepclimate.app.voice

import android.content.Context
import com.sleepclimate.app.data.vehicle.CommandResult
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.data.vehicle.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 음성 명령 관리자
 *
 * ═══════════════════════════════════════════════════════════
 *  Pleos Connect STT/TTS SDK v3.3.1 연동
 *
 *  STT: ai.pleos.playground.stt.SpeechToText
 *  TTS: ai.pleos.playground.tts.TextToSpeech
 *
 *  TODO: gradle 의존성 추가 후 아래 주석 해제
 *  import ai.pleos.playground.stt.SpeechToText
 *  import ai.pleos.playground.stt.constant.Mode as SttMode
 *  import ai.pleos.playground.tts.TextToSpeech
 *  import ai.pleos.playground.tts.constant.Mode as TtsMode
 * ═══════════════════════════════════════════════════════════
 *
 *  지원 음성 명령어:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │ 세션 제어                                               │
 *  │  "슬립 클라이밋 시작" / "휴식 시작"                    │
 *  │  "세션 종료" / "종료해줘"                               │
 *  │                                                         │
 *  │ 공조 제어 (인식 완전 동작, 실행은 SDK Coming Soon 안내) │
 *  │  "에어컨 켜줘" / "냉방 켜줘"                           │
 *  │  "에어컨 꺼줘" / "냉방 꺼줘"                           │
 *  │  "히터 켜줘"  / "난방 켜줘"                            │
 *  │  "히터 꺼줘"  / "난방 꺼줘"                            │
 *  │  "공조 꺼줘"  / "다 꺼줘"                              │
 *  │  "바람 세게" / "바람 약하게"                           │
 *  │                                                         │
 *  │ 온도 설정                                               │
 *  │  "22도로 맞춰줘" / "목표 온도 20도"                    │
 *  │  "온도 올려줘" / "온도 낮춰줘"                         │
 *  │                                                         │
 *  │ 상태 확인                                               │
 *  │  "지금 온도 알려줘" / "현재 온도"                      │
 *  │  "배터리 얼마야"                                        │
 *  │  "남은 시간 얼마야"                                     │
 *  └─────────────────────────────────────────────────────────┘
 */
class VoiceCommandManager(
    private val context: Context,
    private val vehicleRepo: VehicleRepository? = null
) {

    // ── STT / TTS 인스턴스 ──
    // TODO: SDK 의존성 추가 후 실제 인스턴스로 교체
    // private val stt = SpeechToText(context, SttMode.HYBRID)
    // private val tts = TextToSpeech(context, TtsMode.HYBRID)

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    /** 공조 명령 비동기 실행용 스코프 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** SDK 초기화 — Activity.onCreate() 에서 호출 */
    fun initialize() {
        // TODO: SDK 연동 후 교체
        // stt.initialize()
        // stt.addListener(sttResultListener)
        // stt.setLocale(Locale.KOREAN)
        //
        // tts.initialize()
        // tts.addEventListener(ttsEventListener)
        // tts.setLocale(Locale.KOREAN)
        _state.value = VoiceState.Ready
    }

    /** 음성 인식 시작 — 마이크 버튼 누를 때 */
    fun startListening() {
        if (_state.value !is VoiceState.Ready) return
        _state.value = VoiceState.Listening

        // TODO: SDK 연동 후 교체
        // stt.request()

        // Mock: 2초 후 인식 완료 시뮬레이션 (에뮬레이터 테스트용)
        simulateRecognition()
    }

    /** 음성 인식 중단 */
    fun stopListening() {
        // TODO: stt.stop()
        _state.value = VoiceState.Ready
    }

    /** TTS로 앱 상태 읽어주기 */
    fun speak(text: String) {
        _state.value = VoiceState.Speaking(text)

        // TODO: SDK 연동 후 교체
        // tts.speak(text)
        // TTS 완료 후 Ready로 복귀는 ttsEventListener.onFinish()에서 처리

        // Mock: 콘솔 출력
        android.util.Log.d("TTS", "📢 $text")
    }

    /**
     * 인식된 텍스트를 명령으로 파싱
     *
     * STT ResultListener.onResult()에서 호출:
     *   override fun onResult(result: String, isFinal: Boolean) {
     *       if (isFinal) parseAndExecute(result)
     *   }
     */
    fun parseAndExecute(recognizedText: String): VoiceCommand {
        val text = recognizedText.trim().lowercase()
        val command = when {
            // ── 세션 시작 ──
            text.contains("시작") || text.contains("start") ->
                VoiceCommand.StartSession

            // ── 세션 종료 ──
            text.contains("종료") || text.contains("끝") || text.contains("stop") ->
                VoiceCommand.EndSession

            // ── 에어컨 / 냉방 ──
            text.contains("에어컨") || text.contains("냉방") -> when {
                text.contains("켜") -> VoiceCommand.TurnOnAc
                text.contains("꺼") -> VoiceCommand.TurnOffAc
                else                -> VoiceCommand.Unknown(recognizedText)
            }

            // ── 히터 / 난방 ──
            text.contains("히터") || text.contains("난방") -> when {
                text.contains("켜") -> VoiceCommand.TurnOnHeater
                text.contains("꺼") -> VoiceCommand.TurnOffHeater
                else                -> VoiceCommand.Unknown(recognizedText)
            }

            // ── 공조 끄기 ──
            (text.contains("공조") || text.contains("다")) && text.contains("꺼") ->
                VoiceCommand.TurnOffHvac

            // ── 팬/바람 세기 ──
            text.contains("바람") -> when {
                text.contains("세게") || text.contains("강하게") || text.contains("높여") ->
                    VoiceCommand.SetFanSpeed(3)
                text.contains("약하게") || text.contains("낮춰") || text.contains("줄여") ->
                    VoiceCommand.SetFanSpeed(1)
                else ->
                    VoiceCommand.SetFanSpeed(2)
            }

            // ── 온도 설정: "22도로" / "목표 온도 20" ──
            text.contains("도") && extractNumber(text) != null -> {
                val temp = extractNumber(text)!!
                if (temp in 16.0..30.0) VoiceCommand.SetTemperature(temp.toFloat())
                else VoiceCommand.Unknown("온도 범위는 16도에서 30도 사이입니다.")
            }

            // ── 온도 올리기 ──
            text.contains("올려") || text.contains("높여") || text.contains("더 따뜻") ->
                VoiceCommand.AdjustTemperature(+0.5f)

            // ── 온도 낮추기 ──
            text.contains("낮춰") || text.contains("내려") || text.contains("더 시원") ->
                VoiceCommand.AdjustTemperature(-0.5f)

            // ── 온도 확인 ──
            text.contains("온도") && (text.contains("알려") || text.contains("지금") || text.contains("현재")) ->
                VoiceCommand.QueryTemperature

            // ── 배터리 확인 ──
            text.contains("배터리") ->
                VoiceCommand.QueryBattery

            // ── 남은 시간 확인 ──
            text.contains("시간") && (text.contains("얼마") || text.contains("남은") || text.contains("남았")) ->
                VoiceCommand.QueryTimeRemaining

            // ── 상태 전체 확인 ──
            text.contains("상태") || text.contains("어때") ->
                VoiceCommand.QueryStatus

            else -> VoiceCommand.Unknown(recognizedText)
        }

        _lastCommand.value = command
        _state.value = VoiceState.CommandRecognized(recognizedText, command)
        return command
    }

    /**
     * 명령에 대한 TTS 응답 생성.
     *
     * 공조 제어 명령(TurnOnAc, TurnOffAc, TurnOnHeater, TurnOffHeater, TurnOffHvac, SetFanSpeed)은
     * vehicleRepo를 통해 실행 시도 후 NOT_SUPPORTED이면 "준비 중" 안내를 한다.
     * Vehicle Control API 출시 후 NOT_SUPPORTED 분기를 실제 성공 메시지로 교체.
     */
    fun respondToCommand(command: VoiceCommand, ctx: VoiceResponseContext) {
        when (command) {
            // ── 세션 제어 ──
            is VoiceCommand.StartSession ->
                speak("휴식 세션을 시작합니다. 목표 온도 ${ctx.targetTemp}도, ${ctx.duration}시간으로 설정됩니다.")

            is VoiceCommand.EndSession ->
                speak("세션을 종료합니다. 총 ${ctx.elapsed}분 동안 휴식하셨습니다.")

            // ── 온도 설정 ──
            is VoiceCommand.SetTemperature ->
                speak("목표 온도를 ${command.tempCelsius.toInt()}도로 설정했습니다. 공조가 자동 지원되면 바로 적용됩니다.")

            is VoiceCommand.AdjustTemperature -> {
                val dir = if (command.delta > 0) "올렸습니다" else "낮췄습니다"
                speak("목표 온도를 ${Math.abs(command.delta)}도 $dir.")
            }

            // ── 상태 조회 ──
            is VoiceCommand.QueryTemperature ->
                speak("현재 실내 온도는 ${ctx.currentTemp}도입니다. 목표 온도는 ${ctx.targetTemp}도입니다.")

            is VoiceCommand.QueryBattery ->
                speak("배터리는 현재 ${ctx.battery}퍼센트입니다.")

            is VoiceCommand.QueryTimeRemaining ->
                speak("세션 종료까지 ${ctx.remaining}분 남았습니다.")

            is VoiceCommand.QueryStatus ->
                speak("현재 실내 온도 ${ctx.currentTemp}도, 배터리 ${ctx.battery}퍼센트, 세션 종료까지 ${ctx.remaining}분 남았습니다.")

            // ── 공조 제어 ── Vehicle Control Coming Soon → 준비 중 안내
            // SDK 출시 후: NOT_SUPPORTED 분기를 실제 성공 응답으로 교체
            is VoiceCommand.TurnOnAc -> scope.launch {
                val result = vehicleRepo?.setHvacMode(HvacMode.COOLING) ?: CommandResult.NOT_SUPPORTED
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "에어컨을 켰습니다."
                )
            }

            is VoiceCommand.TurnOffAc -> scope.launch {
                val result = vehicleRepo?.setHvacMode(HvacMode.OFF) ?: CommandResult.NOT_SUPPORTED
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "에어컨을 껐습니다."
                )
            }

            is VoiceCommand.TurnOnHeater -> scope.launch {
                val result = vehicleRepo?.setHvacMode(HvacMode.HEATING) ?: CommandResult.NOT_SUPPORTED
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "히터를 켰습니다."
                )
            }

            is VoiceCommand.TurnOffHeater -> scope.launch {
                val result = vehicleRepo?.setHvacMode(HvacMode.OFF) ?: CommandResult.NOT_SUPPORTED
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "히터를 껐습니다."
                )
            }

            is VoiceCommand.TurnOffHvac -> scope.launch {
                val result = vehicleRepo?.setHvacMode(HvacMode.OFF) ?: CommandResult.NOT_SUPPORTED
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "공조를 껐습니다."
                )
            }

            is VoiceCommand.SetFanSpeed -> scope.launch {
                val result = vehicleRepo?.setFanSpeed(command.level) ?: CommandResult.NOT_SUPPORTED
                val levelText = when (command.level) {
                    1    -> "약하게"
                    3    -> "세게"
                    else -> "중간으로"
                }
                speak(
                    if (result == CommandResult.NOT_SUPPORTED)
                        "공조 자동 제어는 준비 중입니다. 차량 공조 버튼을 직접 조작해주세요."
                    else "팬 속도를 ${levelText} 조절했습니다."
                )
            }

            is VoiceCommand.Unknown ->
                speak("죄송합니다. 다시 말씀해 주세요. '온도 올려줘', '에어컨 켜줘' 와 같이 말씀해 주세요.")
        }
    }

    /** SDK 해제 — Activity.onDestroy()에서 호출 */
    fun release() {
        // TODO: stt.removeListener(sttResultListener); stt.release()
        // TODO: tts.removeEventListener(ttsEventListener); tts.release()
        _state.value = VoiceState.Idle
    }

    // ── 내부 유틸 ──

    private fun extractNumber(text: String): Double? {
        val regex = Regex("""(\d+(?:\.\d+)?)""")
        return regex.find(text)?.value?.toDoubleOrNull()
    }

    /** 에뮬레이터/Mock 테스트용 시뮬레이션 */
    private fun simulateRecognition() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_state.value is VoiceState.Listening) {
                val testCommands = listOf(
                    "지금 온도 알려줘",
                    "배터리 얼마야",
                    "온도 올려줘",
                    "에어컨 켜줘",
                    "히터 꺼줘",
                    "바람 세게"
                )
                val cmd = testCommands.random()
                parseAndExecute(cmd)
            }
        }, 2000)
    }
}

// ── 음성 인식 상태 ──
sealed class VoiceState {
    object Idle      : VoiceState()
    object Ready     : VoiceState()
    object Listening : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class CommandRecognized(val raw: String, val command: VoiceCommand) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

// ── 음성 명령 종류 ──
sealed class VoiceCommand {
    // 세션 제어
    object StartSession                              : VoiceCommand()
    object EndSession                                : VoiceCommand()
    // 공조 제어 — 명령 인식 완전 동작, 실행은 Vehicle Control SDK Coming Soon
    object TurnOnAc                                  : VoiceCommand() // "에어컨 켜줘" / "냉방 켜줘"
    object TurnOffAc                                 : VoiceCommand() // "에어컨 꺼줘" / "냉방 꺼줘"
    object TurnOnHeater                              : VoiceCommand() // "히터 켜줘"  / "난방 켜줘"
    object TurnOffHeater                             : VoiceCommand() // "히터 꺼줘"  / "난방 꺼줘"
    object TurnOffHvac                               : VoiceCommand() // "공조 꺼줘"  / "다 꺼줘"
    data class SetFanSpeed(val level: Int)           : VoiceCommand() // "바람 세게(3)" / "바람 약하게(1)"
    // 온도
    data class SetTemperature(val tempCelsius: Float): VoiceCommand()
    data class AdjustTemperature(val delta: Float)   : VoiceCommand()
    // 조회
    object QueryTemperature                          : VoiceCommand()
    object QueryBattery                              : VoiceCommand()
    object QueryTimeRemaining                        : VoiceCommand()
    object QueryStatus                               : VoiceCommand()
    // 알 수 없음
    data class Unknown(val raw: String)              : VoiceCommand()
}

// ── TTS 응답 생성에 필요한 컨텍스트 ──
data class VoiceResponseContext(
    val currentTemp: String = "--",
    val targetTemp: String  = "--",
    val battery: String     = "--",
    val remaining: String   = "--",
    val elapsed: String     = "--",
    val duration: String    = "--"
)
