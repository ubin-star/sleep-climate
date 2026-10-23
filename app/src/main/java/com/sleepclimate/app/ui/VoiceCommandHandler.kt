package com.sleepclimate.app.ui

import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.domain.model.SessionStatus
import com.sleepclimate.app.ui.home.HomeViewModel
import com.sleepclimate.app.ui.home.StartAvailability
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.voice.VoiceAssistant
import com.sleepclimate.app.voice.VoiceCommandParser
import com.sleepclimate.app.voice.VoiceIntent
import com.sleepclimate.app.voice.spoken
import com.sleepclimate.app.voice.spokenDuration

/**
 * 음성 명령 실행 + 음성 응답
 *
 * 차량 안에서 손을 쓰지 않고: 휴식 시작·종료, 목표 온도 변경, 상태 질문, 10분 연장, 수면 화면
 */
class VoiceCommandHandler(
    private val voice: VoiceAssistant,
    private val home: HomeViewModel,
    private val session: SessionViewModel,
    private val onSessionStarted: () -> Unit
) {

    private val isResting: Boolean
        get() = session.currentSession.value?.status == SessionStatus.RUNNING

    fun handle(text: String) {
        when (val intent = VoiceCommandParser.parse(text)) {
            is VoiceIntent.StartRest -> startRest(intent)
            VoiceIntent.EndRest -> requireRest {
                session.endSession()
                voice.speak("휴식을 종료합니다. 리포트를 보여드릴게요.")
            }
            is VoiceIntent.SetTarget -> requireRest {
                session.setTarget(intent.celsius)
                voice.speak("목표 온도를 ${intent.celsius.spoken()}도로 바꿨습니다.")
            }
            is VoiceIntent.AdjustTarget -> requireRest {
                session.adjustTarget(intent.delta)
                val target = session.currentSession.value?.targetTempCelsius ?: return@requireRest
                val direction = if (intent.delta > 0) "올려" else "낮춰"
                voice.speak("목표 온도를 ${target.spoken()}도로 $direction 드릴게요.")
            }
            VoiceIntent.QueryStatus -> voice.speak(statusSentence())
            VoiceIntent.QueryRemaining -> requireRest {
                val remaining = (session.currentSession.value!!.endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                voice.speak("기상까지 ${spokenDuration(remaining)} 남았습니다.")
            }
            VoiceIntent.QueryBattery -> voice.speak(energySentence())
            VoiceIntent.Snooze -> requireRest { session.extendSession(10) }
            VoiceIntent.SleepScreen -> requireRest {
                session.requestSleepScreen()
                voice.speak("화면을 어둡게 할게요. 편히 쉬세요.")
            }
            is VoiceIntent.Unknown -> voice.speak("다시 말씀해 주세요. 예를 들어 '여름 차박 시작', '온도 올려줘'라고 말씀해 보세요.")
        }
    }

    private fun startRest(intent: VoiceIntent.StartRest) {
        if (isResting) {
            voice.speak("이미 휴식 중입니다.")
            return
        }
        val reason = when (home.startAvailability.value) {
            StartAvailability.READY -> null
            StartAvailability.DRIVING -> "안전한 곳에 주차한 뒤 시작할 수 있어요."
            StartAvailability.DISCONNECTED -> "차량 연결을 확인한 뒤 다시 말씀해 주세요."
            StartAvailability.NO_SUPPORT -> "이 차량에서는 필요한 데이터를 확인할 수 없어요."
            StartAvailability.CHECKING -> "차량 상태를 확인하고 있어요. 잠시 후 다시 말씀해 주세요."
        }
        if (reason != null) {
            voice.speak(reason)
            return
        }
        session.startSession(intent.preset.toSession())
        onSessionStarted()
    }

    private inline fun requireRest(action: () -> Unit) {
        if (isResting) action() else voice.speak("진행 중인 휴식이 없어요. '여름 차박 시작'처럼 말씀해 주세요.")
    }

    private fun statusSentence(): String {
        val state = session.vehicleState.value.takeIf { it.lastUpdatedMs > 0 } ?: home.vehicleState.value
        val temp = state.interiorTempCelsius ?: return "실내 온도 정보를 확인할 수 없어요."
        val hvac = when {
            state.isHvacOn != true -> "공조는 꺼져 있어요."
            state.hvacMode == HvacMode.COOLING -> "냉방 중이에요."
            state.hvacMode == HvacMode.HEATING -> "난방 중이에요."
            else -> "공조가 켜져 있어요."
        }
        val target = session.currentSession.value?.takeIf { isResting }?.targetTempCelsius
        return "실내 온도는 ${temp.spoken()}도" + (target?.let { ", 목표는 ${it.spoken()}도" } ?: "") + "예요. $hvac"
    }

    private fun energySentence(): String {
        val state = session.vehicleState.value.takeIf { it.lastUpdatedMs > 0 } ?: home.vehicleState.value
        return when {
            state.batteryPercent != null -> "배터리는 ${state.batteryPercent}퍼센트 남았어요."
            state.fuelPercent != null -> "연료는 ${state.fuelPercent}퍼센트 남았어요."
            else -> "배터리 정보를 확인할 수 없어요."
        }
    }
}
