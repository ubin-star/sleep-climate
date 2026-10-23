package com.sleepclimate.app.voice

import com.sleepclimate.app.domain.model.SessionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    private fun parse(s: String) = VoiceCommandParser.parse(s)

    @Test
    fun `휴식 시작 명령은 상황에 맞는 프리셋을 고른다`() {
        assertEquals(VoiceIntent.StartRest(SessionPreset.SUMMER_CAMPING), parse("여름 차박 시작해줘"))
        assertEquals(VoiceIntent.StartRest(SessionPreset.WINTER_CAMPING), parse("추워서 이제 잘게"))
        assertEquals(VoiceIntent.StartRest(SessionPreset.SHORT_REST), parse("낮잠 좀 잘래"))
        assertEquals(VoiceIntent.StartRest(SessionPreset.SUMMER_CAMPING), parse("휴식 시작"))
    }

    @Test
    fun `숫자와 도가 있으면 목표 온도 설정`() {
        assertEquals(VoiceIntent.SetTarget(22f), parse("22도로 맞춰줘"))
        assertEquals(VoiceIntent.SetTarget(20.5f), parse("목표 온도 20.5도"))
        assertTrue(parse("40도로 해줘") is VoiceIntent.Unknown)
    }

    @Test
    fun `올려 낮춰 춥다 덥다는 목표 온도 조정`() {
        assertEquals(VoiceIntent.AdjustTarget(0.5f), parse("온도 좀 올려줘"))
        assertEquals(VoiceIntent.AdjustTarget(0.5f), parse("좀 추워"))
        assertEquals(VoiceIntent.AdjustTarget(-0.5f), parse("너무 더워"))
        assertEquals(VoiceIntent.AdjustTarget(-0.5f), parse("시원하게 해줘"))
    }

    @Test
    fun `연장과 종료를 구분한다`() {
        assertEquals(VoiceIntent.Snooze, parse("10분만 더 잘게"))
        assertEquals(VoiceIntent.EndRest, parse("휴식 끝내줘"))
        assertEquals(VoiceIntent.EndRest, parse("이제 일어날게"))
    }

    @Test
    fun `음성 안내용 숫자·시간 표현`() {
        assertEquals("22", 22.0f.spoken())
        assertEquals("22.5", 22.5f.spoken())
        assertEquals("8시간", spokenDuration(8 * 3_600_000L))
        assertEquals("1시간 30분", spokenDuration(90 * 60_000L))
        assertEquals("3분", spokenDuration(3 * 60_000L))
    }

    @Test
    fun `상태 질문`() {
        assertEquals(VoiceIntent.QueryStatus, parse("지금 몇 도야"))
        assertEquals(VoiceIntent.QueryRemaining, parse("남은 시간 알려줘"))
        assertEquals(VoiceIntent.QueryBattery, parse("배터리 얼마나 남았어"))
        assertEquals(VoiceIntent.SleepScreen, parse("화면 어둡게 해줘"))
    }
}
