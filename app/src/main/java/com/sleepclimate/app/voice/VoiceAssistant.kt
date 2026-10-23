package com.sleepclimate.app.voice

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 음성 비서 화면 상태 */
sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val partial: String) : VoiceState
    data class Heard(val text: String) : VoiceState
    data class Speaking(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
}

/**
 * 음성 명령(STT)·음성 안내(TTS)
 *
 * Pleos Connect TextToSpeech / SpeechToText SDK를 먼저 사용하고,
 * Pleos 음성 서비스가 없는 기기(일반 폰·AVD)에서는 Android 기본 음성 엔진으로 자동 전환한다.
 */
class VoiceAssistant(private val context: Context, private val clientId: String) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var output: SpeechOutputEngine? = null
    private var input: SpeechInputEngine? = null
    private var clearJob: Job? = null

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _engines = MutableStateFlow("")
    /** 사용 중인 엔진 표시용 — 예: "Pleos TTS · Pleos STT" */
    val engines: StateFlow<String> = _engines.asStateFlow()

    val canListen: Boolean get() = input != null

    private var initJob: Job? = null

    fun initialize() {
        initJob = scope.launch {
            input = PleosStt.create(context, clientId) ?: AndroidStt.create(context)
            output = PleosTts.create(context, clientId) ?: AndroidTts.create(context)
            updateEngineLabel()
        }
    }

    private fun updateEngineLabel() {
        _engines.value = listOfNotNull(output?.engineName, input?.engineName).joinToString(" · ")
    }

    /** 음성 안내 — 안내 문구는 화면 하단에도 잠시 표시 */
    fun speak(text: String) {
        clearJob?.cancel()
        _state.value = VoiceState.Speaking(text)
        scope.launch {
            // 앱 시작 직후라면 엔진 준비를 기다리고, 준비에 실패했으면 한 번 더 시도
            initJob?.join()
            if (output == null) {
                output = AndroidTts.create(context)
                updateEngineLabel()
            }
            val engine = output
            if (engine == null) {
                clearLater()
            } else {
                engine.speak(text) { clearLater(1_500L) }
            }
        }
    }

    /** 음성 명령 듣기 — 최종 인식 문장을 [onCommand]로 전달 */
    fun listen(onCommand: (String) -> Unit) {
        val engine = input
        if (engine == null) {
            _state.value = VoiceState.Error("이 기기에서는 음성 인식을 사용할 수 없습니다.")
            clearLater()
            return
        }
        clearJob?.cancel()
        output?.stop()
        _state.value = VoiceState.Listening("")
        engine.start(object : SpeechInputListener {
            override fun onPartial(text: String) {
                _state.value = VoiceState.Listening(text)
            }

            override fun onFinal(text: String) {
                _state.value = VoiceState.Heard(text)
                onCommand(text)
            }

            override fun onError(message: String) {
                _state.value = VoiceState.Error("잘 듣지 못했어요. 다시 말씀해 주세요.")
                clearLater()
            }
        })
    }

    fun stopListening() {
        input?.stop()
        _state.value = VoiceState.Idle
    }

    fun release() {
        output?.release()
        input?.release()
    }

    private fun clearLater(delayMs: Long = 2_500L) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delayMs)
            _state.value = VoiceState.Idle
        }
    }
}
