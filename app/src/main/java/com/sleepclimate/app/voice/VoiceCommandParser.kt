package com.sleepclimate.app.voice

import com.sleepclimate.app.domain.model.SessionPreset

/** 음성 명령 의도 */
sealed interface VoiceIntent {
    data class StartRest(val preset: SessionPreset) : VoiceIntent
    data object EndRest : VoiceIntent
    data class SetTarget(val celsius: Float) : VoiceIntent
    data class AdjustTarget(val delta: Float) : VoiceIntent
    data object QueryStatus : VoiceIntent
    data object QueryRemaining : VoiceIntent
    data object QueryBattery : VoiceIntent
    data object Snooze : VoiceIntent
    data object SleepScreen : VoiceIntent
    data class Unknown(val text: String) : VoiceIntent
}

/**
 * 인식된 문장 → [VoiceIntent]
 *
 * 예) "여름 차박 시작해줘", "추워서 잘래", "22도로 맞춰줘", "온도 좀 올려줘", "지금 몇 도야",
 *     "남은 시간 알려줘", "배터리 얼마나 남았어", "10분만 더 잘게", "휴식 끝내줘", "화면 어둡게 해줘"
 */
object VoiceCommandParser {

    private const val MIN_TARGET = 16f
    private const val MAX_TARGET = 30f
    private const val ADJUST_STEP = 0.5f
    private val numberBeforeDegree = Regex("""(\d{2}(?:\.\d)?)\s*도""")

    fun parse(raw: String): VoiceIntent {
        val t = raw.replace(" ", "").lowercase()
        return when {
            t.containsAny("분더", "더쉬", "더잘", "더자", "조금만더") -> VoiceIntent.Snooze
            t.containsAny("종료", "끝내", "끝낼", "그만", "일어날게") -> VoiceIntent.EndRest
            t.containsAny("시작", "잘게", "잘래", "잘거", "자고싶", "잠잘", "쉴게", "쉴래") -> VoiceIntent.StartRest(presetOf(t))
            numberBeforeDegree.containsMatchIn(t) -> targetOf(raw) ?: VoiceIntent.Unknown(raw)
            t.containsAny("올려", "높여", "따뜻하게", "춥", "추워") -> VoiceIntent.AdjustTarget(ADJUST_STEP)
            t.containsAny("내려", "낮춰", "시원하게", "덥", "더워") -> VoiceIntent.AdjustTarget(-ADJUST_STEP)
            t.containsAny("배터리", "연료", "충전") -> VoiceIntent.QueryBattery
            t.containsAny("남은시간", "얼마나남", "몇시간", "언제깨", "몇분남") -> VoiceIntent.QueryRemaining
            t.containsAny("몇도", "온도", "상태", "어때") -> VoiceIntent.QueryStatus
            t.containsAny("수면화면", "화면꺼", "화면어둡", "어둡게") -> VoiceIntent.SleepScreen
            else -> VoiceIntent.Unknown(raw)
        }
    }

    private fun presetOf(t: String): SessionPreset = when {
        t.containsAny("겨울", "추워", "추운", "춥", "난방") -> SessionPreset.WINTER_CAMPING
        t.containsAny("짧", "낮잠", "잠깐", "잠시", "한시간") -> SessionPreset.SHORT_REST
        else -> SessionPreset.SUMMER_CAMPING
    }

    private fun targetOf(raw: String): VoiceIntent? {
        val value = numberBeforeDegree.find(raw.replace(" ", ""))?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        if (value !in MIN_TARGET..MAX_TARGET) return null
        return VoiceIntent.SetTarget((value * 2).toInt() / 2f)
    }

    private fun String.containsAny(vararg words: String) = words.any { contains(it) }
}
