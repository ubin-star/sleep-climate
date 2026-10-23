package com.sleepclimate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionPhase
import com.sleepclimate.app.domain.model.fallAsleepWindowMs
import com.sleepclimate.app.domain.model.phaseAt
import com.sleepclimate.app.domain.model.progressAt
import com.sleepclimate.app.domain.model.wakeWindowMs
import com.sleepclimate.app.ui.theme.SC

/** 잠들기 · 수면 · 기상 준비 — 구간 길이에 비례한 진행 막대 */
@Composable
fun PhaseTimeline(session: RestSession, nowMs: Long, modifier: Modifier = Modifier) {
    val total = (session.endTime - session.startTime).coerceAtLeast(1L).toFloat()
    val fallFrac = session.fallAsleepWindowMs / total
    val wakeFrac = session.wakeWindowMs / total
    val sleepFrac = (1f - fallFrac - wakeFrac).coerceAtLeast(0f)
    val progress = session.progressAt(nowMs)
    val phase = session.phaseAt(nowMs)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            val barH = 6.dp.toPx()
            val gap = 4.dp.toPx()
            val y = (size.height - barH) / 2
            val usable = size.width - gap * 2
            val segments = listOf(fallFrac, sleepFrac, wakeFrac)
            var x = 0f
            segments.forEachIndexed { i, frac ->
                val w = usable * frac
                val segStart = segments.take(i).sum()
                val filled = ((progress - segStart) / frac.coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
                drawRoundRect(SC.SurfaceHigh, Offset(x, y), Size(w, barH), CornerRadius(barH / 2))
                if (filled > 0f) {
                    drawRoundRect(
                        if (i == 2) SC.Heat.copy(alpha = 0.85f) else SC.TextPrimary.copy(alpha = 0.7f),
                        Offset(x, y), Size(w * filled, barH), CornerRadius(barH / 2)
                    )
                }
                x += w + gap
            }
            val markerX = (progress * usable + gap * when (phase) {
                SessionPhase.FALLING_ASLEEP -> 0
                SessionPhase.SLEEPING -> 1
                SessionPhase.WAKING -> 2
            }).coerceIn(0f, size.width)
            drawCircle(SC.TextPrimary, size.height / 2, Offset(markerX, size.height / 2))
        }
        Row(Modifier.fillMaxWidth()) {
            PhaseLabel(stringResource(R.string.phase_falling_asleep), phase == SessionPhase.FALLING_ASLEEP, Modifier.weight(1f), TextAlign.Start)
            PhaseLabel(stringResource(R.string.phase_sleeping), phase == SessionPhase.SLEEPING, Modifier.weight(1f), TextAlign.Center)
            PhaseLabel(stringResource(R.string.phase_waking), phase == SessionPhase.WAKING, Modifier.weight(1f), TextAlign.End)
        }
    }
}

@Composable
private fun PhaseLabel(text: String, active: Boolean, modifier: Modifier, align: TextAlign) {
    Text(
        text,
        modifier = modifier,
        textAlign = align,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
        color = if (active) SC.TextPrimary else SC.TextTertiary
    )
}
