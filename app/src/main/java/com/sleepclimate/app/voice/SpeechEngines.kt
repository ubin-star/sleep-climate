package com.sleepclimate.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitListener
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitListener
import android.speech.tts.TextToSpeech as AndroidTextToSpeech

private const val TAG = "SpeechEngine"
private const val INIT_TIMEOUT_MS = 3_000L

/** 음성 안내 출력 */
interface SpeechOutputEngine {
    val engineName: String
    fun speak(text: String, onDone: () -> Unit)
    fun stop()
    fun release()
}

/** 음성 인식 입력 */
interface SpeechInputEngine {
    val engineName: String
    fun start(listener: SpeechInputListener)
    fun stop()
    fun release()
}

interface SpeechInputListener {
    fun onPartial(text: String)
    fun onFinal(text: String)
    fun onError(message: String)
}

private val mainHandler = Handler(Looper.getMainLooper())

// ─── Pleos Connect TextToSpeech / SpeechToText SDK 3.3.1 ────────────────────────────

class PleosTts private constructor(private val tts: TextToSpeech) : SpeechOutputEngine {
    override val engineName = "Pleos TTS"
    private var onDone: (() -> Unit)? = null

    init {
        tts.setLocale(Locale.KOREAN)
        tts.addEventListener(object : EventListener {
            override fun onStart() = Unit
            override fun onReady() = Unit
            override fun onDone() = finish()
            override fun onStop() = finish()
            override fun onError(error: TtsError) = finish()
            override fun onUpdatedRms(rms: Double) = Unit
        })
    }

    private fun finish() {
        val callback = onDone ?: return
        onDone = null
        mainHandler.post(callback)
    }

    override fun speak(text: String, onDone: () -> Unit) {
        this.onDone = onDone
        runCatching { tts.speak(text, false) }.onFailure { finish() }
    }

    override fun stop() { runCatching { tts.stop() } }
    override fun release() { runCatching { tts.release() } }

    companion object {
        /** Pleos 음성 서비스가 없는 기기에서는 null */
        suspend fun create(context: Context, clientId: String): PleosTts? {
            val tts = runCatching { TextToSpeech(context, TtsMode.HYBRID) }.getOrNull() ?: return null
            val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        tts.initialize(object : TtsInitListener {
                            override fun onSuccess() { if (cont.isActive) cont.resume(true) }
                            override fun onFailure(error: TtsError) { if (cont.isActive) cont.resume(false) }
                        })
                    }.onFailure { if (cont.isActive) cont.resume(false) }
                }
            } ?: false
            if (!ok) {
                runCatching { tts.release() }
                return null
            }
            runCatching { tts.registerApp(clientId, {}, { Log.w(TAG, "Pleos TTS registerApp 실패: $it") }) }
            return PleosTts(tts)
        }
    }
}

class PleosStt private constructor(private val stt: SpeechToText) : SpeechInputEngine {
    override val engineName = "Pleos STT"
    private var listener: SpeechInputListener? = null

    init {
        stt.setLocale(Locale.KOREAN)
        stt.addListener(object : ResultListener {
            override fun onUpdated(text: String, isFinal: Boolean) {
                mainHandler.post {
                    val l = listener ?: return@post
                    if (isFinal) {
                        listener = null
                        l.onFinal(text)
                    } else {
                        l.onPartial(text)
                    }
                }
            }
            override fun onUpdatedEpdData(start: Long, end: Long) = Unit
            override fun onStartedRecognition() = Unit
            override fun onEndedRecognition() = Unit
            override fun onReady() = Unit
            override fun onUpdatedRms(rms: Double) = Unit
            override fun onError(error: SttError) {
                mainHandler.post {
                    listener?.onError(error.name)
                    listener = null
                }
            }
        })
    }

    override fun start(listener: SpeechInputListener) {
        this.listener = listener
        runCatching { stt.request() }.onFailure { listener.onError(it.message ?: "STT") }
    }

    override fun stop() { runCatching { stt.stop() }; listener = null }
    override fun release() { runCatching { stt.release() } }

    companion object {
        suspend fun create(context: Context, clientId: String): PleosStt? {
            val stt = runCatching { SpeechToText(context, SttMode.HYBRID) }.getOrNull() ?: return null
            val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    runCatching {
                        stt.initialize(object : SttInitListener {
                            override fun onSuccess() { if (cont.isActive) cont.resume(true) }
                            override fun onFailure(error: SttError) { if (cont.isActive) cont.resume(false) }
                        })
                    }.onFailure { if (cont.isActive) cont.resume(false) }
                }
            } ?: false
            if (!ok) {
                runCatching { stt.release() }
                return null
            }
            runCatching { stt.registerApp(clientId, {}, { Log.w(TAG, "Pleos STT registerApp 실패: $it") }) }
            return PleosStt(stt)
        }
    }
}

// ─── Android 기본 음성 엔진 (Pleos 음성 서비스가 없는 폰·에뮬레이터용) ───────────────

class AndroidTts private constructor(private val tts: AndroidTextToSpeech) : SpeechOutputEngine {
    override val engineName = "Android TTS"
    private var onDone: (() -> Unit)? = null

    init {
        tts.language = Locale.KOREAN
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish()
        })
    }

    private fun finish() {
        val callback = onDone ?: return
        onDone = null
        mainHandler.post(callback)
    }

    override fun speak(text: String, onDone: () -> Unit) {
        this.onDone = onDone
        tts.speak(text, AndroidTextToSpeech.QUEUE_FLUSH, null, "sleep-climate")
    }

    override fun stop() { tts.stop() }
    override fun release() { tts.shutdown() }

    companion object {
        /** 기본 TTS 엔진은 처음 켤 때 수 초가 걸릴 수 있어 제한 시간을 길게 둔다 */
        private const val ANDROID_TTS_TIMEOUT_MS = 10_000L

        suspend fun create(context: Context): AndroidTts? {
            var engine: AndroidTextToSpeech? = null
            val ok = withTimeoutOrNull(ANDROID_TTS_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    engine = AndroidTextToSpeech(context.applicationContext) { status ->
                        if (cont.isActive) cont.resume(status == AndroidTextToSpeech.SUCCESS)
                    }
                }
            } ?: false
            val tts = engine ?: return null
            if (!ok) {
                tts.shutdown()
                return null
            }
            return AndroidTts(tts)
        }
    }
}

class AndroidStt private constructor(private val context: Context) : SpeechInputEngine {
    override val engineName = "Android STT"
    private var recognizer: SpeechRecognizer? = null

    override fun start(listener: SpeechInputListener) {
        recognizer?.destroy()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) = listener.onError("STT $error")
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(listener::onPartial)
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) listener.onError("STT empty") else listener.onFinal(text)
            }
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
        )
    }

    override fun stop() { recognizer?.stopListening() }
    override fun release() { recognizer?.destroy(); recognizer = null }

    companion object {
        fun create(context: Context): AndroidStt? =
            if (SpeechRecognizer.isRecognitionAvailable(context)) AndroidStt(context.applicationContext) else null
    }
}
