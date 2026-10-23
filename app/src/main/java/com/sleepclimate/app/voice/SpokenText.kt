package com.sleepclimate.app.voice

import java.util.Locale
import kotlin.math.roundToInt

/** 22.0 → "22", 22.5 → "22.5" */
fun Float.spoken(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else String.format(Locale.US, "%.1f", tenths / 10f)
}

/** 8시간 → "8시간", 1시간 30분 → "1시간 30분", 3분 → "3분" */
fun spokenDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000L).coerceAtLeast(1L)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0L -> "${m}분"
        m == 0L -> "${h}시간"
        else -> "${h}시간 ${m}분"
    }
}
