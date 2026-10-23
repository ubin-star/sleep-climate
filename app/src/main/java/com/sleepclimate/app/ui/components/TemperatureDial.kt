package com.sleepclimate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import kotlin.math.cos
import kotlin.math.sin

private const val DIAL_MIN = 14f
private const val DIAL_MAX = 32f
private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * 원형 온도 다이얼
 * - 회색 트랙: 14~32°C
 * - 초록 띠: 허용 범위, 흰 눈금: 목표 온도
 * - 색 점: 현재 실내 온도 (범위 안이면 초록, 벗어나면 경고색)
 */
@Composable
fun TemperatureDial(
    currentTemp: Float?,
    targetTemp: Float,
    rangeLow: Float,
    rangeHigh: Float,
    centerLabel: String,
    modifier: Modifier = Modifier,
    centerValue: String = currentTemp?.let { "%.1f°".format(it) } ?: "—",
    caption: String? = null,
    centerValueIsCurrent: Boolean = true
) {
    val animatedCurrent by animateFloatAsState(currentTemp ?: targetTemp, tween(900), label = "current")
    val animatedLow by animateFloatAsState(rangeLow, tween(400), label = "low")
    val animatedHigh by animateFloatAsState(rangeHigh, tween(400), label = "high")
    val animatedTarget by animateFloatAsState(targetTemp, tween(400), label = "target")
    val inRange = currentTemp != null && currentTemp in rangeLow..rangeHigh
    val indicatorColor by animateColorAsState(
        if (currentTemp == null) SC.TextSecondary else if (inRange) SC.Ok else temperatureColor(currentTemp),
        tween(600), label = "indicator"
    )

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.07f
            val diameter = size.minDimension - stroke * 2
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            fun angleOf(t: Float) = START_ANGLE + SWEEP * ((t.coerceIn(DIAL_MIN, DIAL_MAX) - DIAL_MIN) / (DIAL_MAX - DIAL_MIN))

            drawArc(SC.SurfaceHigh, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                SC.Ok.copy(alpha = 0.55f), angleOf(animatedLow), angleOf(animatedHigh) - angleOf(animatedLow),
                false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )

            val center = Offset(size.width / 2, size.height / 2)
            val radius = diameter / 2
            fun pointAt(t: Float, r: Float): Offset {
                val rad = Math.toRadians(angleOf(t).toDouble())
                return Offset(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat())
            }
            // 목표 온도 눈금
            drawLine(
                SC.TextPrimary, pointAt(animatedTarget, radius - stroke * 0.9f), pointAt(animatedTarget, radius + stroke * 0.9f),
                strokeWidth = stroke * 0.18f, cap = StrokeCap.Round
            )
            // 현재 온도 점
            if (currentTemp != null) {
                val p = pointAt(animatedCurrent, radius)
                drawCircle(indicatorColor.copy(alpha = 0.35f), stroke * 0.95f, p)
                drawCircle(indicatorColor, stroke * 0.6f, p)
                drawCircle(SC.Background, stroke * 0.22f, p)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerLabel, style = MaterialTheme.typography.labelMedium, color = SC.TextSecondary)
            Text(
                centerValue,
                style = MaterialTheme.typography.displayMedium,
                color = if (currentTemp == null || !centerValueIsCurrent) SC.TextPrimary else indicatorColor
            )
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}