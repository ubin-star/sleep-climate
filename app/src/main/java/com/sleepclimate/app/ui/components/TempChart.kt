package com.sleepclimate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sleepclimate.app.domain.model.TempSample
import com.sleepclimate.app.ui.theme.SC

/**
 * 세션 중 실내 온도 그래프
 * - 초록 띠: 허용 범위
 * - 하단 파란 막대: 공조 가동 구간
 */
@Composable
fun TempChart(
    samples: List<TempSample>,
    rangeLow: Float,
    rangeHigh: Float,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val labelW = 64f
        val hvacStripH = 14f
        val chartLeft = labelW
        val chartW = size.width - labelW
        val chartH = size.height - hvacStripH - 12f

        val temps = samples.map { it.tempCelsius }
        val minT = (listOf(rangeLow) + temps).min() - 1f
        val maxT = (listOf(rangeHigh) + temps).max() + 1f
        fun yOf(t: Float) = chartH * (1f - (t - minT) / (maxT - minT))

        // 허용 범위 띠
        drawRect(
            SC.Ok.copy(alpha = 0.08f),
            topLeft = Offset(chartLeft, yOf(rangeHigh)),
            size = Size(chartW, yOf(rangeLow) - yOf(rangeHigh))
        )
        listOf(rangeLow, rangeHigh).forEach { t ->
            drawLine(SC.Ok.copy(alpha = 0.5f), Offset(chartLeft, yOf(t)), Offset(size.width, yOf(t)), 2f)
            drawText(
                textMeasurer, "%.1f°".format(t),
                topLeft = Offset(0f, yOf(t) - 14f),
                style = TextStyle(color = SC.TextSecondary, fontSize = 13.sp)
            )
        }

        if (samples.size < 2) return@Canvas

        val t0 = samples.first().timestampMs
        val span = (samples.last().timestampMs - t0).coerceAtLeast(1L).toFloat()
        fun xOf(ms: Long) = chartLeft + chartW * ((ms - t0) / span)

        // 공조 가동 구간
        samples.zipWithNext().forEach { (a, b) ->
            if (a.hvacOn) {
                drawRect(
                    SC.Cool.copy(alpha = 0.7f),
                    topLeft = Offset(xOf(a.timestampMs), size.height - hvacStripH),
                    size = Size(xOf(b.timestampMs) - xOf(a.timestampMs), hvacStripH)
                )
            }
        }

        // 온도 선 + 아래 채움
        val line = Path()
        samples.forEachIndexed { i, s ->
            val p = Offset(xOf(s.timestampMs), yOf(s.tempCelsius))
            if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(xOf(samples.last().timestampMs), chartH)
            lineTo(xOf(t0), chartH)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(SC.TextPrimary.copy(alpha = 0.10f), SC.TextPrimary.copy(alpha = 0f)), 0f, chartH))
        drawPath(line, SC.TextPrimary, style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
