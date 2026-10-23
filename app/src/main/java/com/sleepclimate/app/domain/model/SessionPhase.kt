package com.sleepclimate.app.domain.model

import kotlin.math.max

/**
 * 휴식 세션 단계 — 잠들기 → 수면 → 기상
 *
 * 기상 단계에서는 허용 범위를 좁혀, 깨어날 때 실내가 목표 온도에 가깝도록 미리 맞춘다.
 */
enum class SessionPhase { FALLING_ASLEEP, SLEEPING, WAKING }

private const val MAX_FALL_ASLEEP_MS = 15 * 60_000L
private const val MAX_WAKE_MS = 10 * 60_000L
private const val MIN_WAKE_RANGE = 0.5f

private val RestSession.durationMs: Long get() = (endTime - startTime).coerceAtLeast(1L)

/** 잠들기 구간 길이 — 최대 15분, 짧은 세션은 전체의 1/4 */
val RestSession.fallAsleepWindowMs: Long get() = minOf(MAX_FALL_ASLEEP_MS, durationMs / 4)

/** 기상 구간 길이 — 최대 10분, 짧은 세션은 전체의 1/4 */
val RestSession.wakeWindowMs: Long get() = minOf(MAX_WAKE_MS, durationMs / 4)

fun RestSession.phaseAt(nowMs: Long): SessionPhase = when {
    nowMs - startTime < fallAsleepWindowMs -> SessionPhase.FALLING_ASLEEP
    endTime - nowMs <= wakeWindowMs -> SessionPhase.WAKING
    else -> SessionPhase.SLEEPING
}

/** 0.0(세션 시작) ~ 1.0(종료) */
fun RestSession.progressAt(nowMs: Long): Float =
    ((nowMs - startTime).toFloat() / durationMs).coerceIn(0f, 1f)

/** 기상 구간 안에서의 진행도 0.0 ~ 1.0 (기상 구간 밖이면 0) */
fun RestSession.wakeProgressAt(nowMs: Long): Float {
    if (phaseAt(nowMs) != SessionPhase.WAKING) return 0f
    return (1f - (endTime - nowMs).toFloat() / max(wakeWindowMs, 1L)).coerceIn(0f, 1f)
}

/** 현재 단계에 적용할 허용 범위 — 기상 단계에서는 절반으로 좁힘 (최소 ±0.5°C) */
fun RestSession.effectiveRangeAt(nowMs: Long): ClosedFloatingPointRange<Float> {
    if (phaseAt(nowMs) != SessionPhase.WAKING) return tempRangeLow..tempRangeHigh
    val half = max((tempRangeHigh - tempRangeLow) / 4f, MIN_WAKE_RANGE)
    return (targetTempCelsius - half)..(targetTempCelsius + half)
}
