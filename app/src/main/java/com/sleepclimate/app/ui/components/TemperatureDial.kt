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
 * 게이지형 온도 다이얼
 * - 바깥 눈금: 1°C 간격 (2°C마다 긴 눈금)
 * - 얇은 트랙 위 밝은 구간: 허용 범위, 짧은 흰 선: 목표 온도
 * - 빛나는 점: 현재 실내 온도 (범위 안이면 민트, 벗어나면 온도색)
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
            val track = size.minDimension * 0.022f
            val tickZone = size.minDimension * 0.05f
            val diameter = size.minDimension - tickZone * 2 - track * 2
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            val center = Offset(size.width / 2, size.height / 2)
            val radius = diameter / 2
            fun angleOf(t: Float) = START_ANGLE + SWEEP * ((t.coerceIn(DIAL_MIN, DIAL_MAX) - DIAL_MIN) / (DIAL_MAX - DIAL_MIN))
            fun pointAt(t: Float, r: Float): Offset {
                val rad = Math.toRadians(angleOf(t).toDouble())
                return Offset(center.x + (r * cos(rad)).toFloat(), center.y + (r * sin(rad)).toFloat())
            }

            // 눈금
            for (t in DIAL_MIN.toInt()..DIAL_MAX.toInt()) {
                val major = t % 2 == 0
                val inBand = t.toFloat() in animatedLow..animatedHigh
                drawLine(
                    if (inBand) SC.Ok.copy(alpha = 0.7f) else SC.TextTertiary.copy(alpha = if (major) 0.9f else 0.5f),
                    pointAt(t.toFloat(), radius + track * 1.6f),
                    pointAt(t.toFloat(), radius + track * 1.6f + if (major) tickZone * 0.75f else tickZone * 0.4f),
                    strokeWidth = if (major) 2.5f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            drawArc(SC.SurfaceHigh, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(track, cap = StrokeCap.Round))
            drawArc(
                SC.Ok.copy(alpha = 0.85f), angleOf(animatedLow), angleOf(animatedHigh) - angleOf(animatedLow),
                false, topLeft, arcSize, style = Stroke(track, cap = StrokeCap.Round)
            )
            // 목표 온도
            drawLine(
                SC.TextPrimary, pointAt(animatedTarget, radius - track * 2.2f), pointAt(animatedTarget, radius + track * 1.2f),
                strokeWidth = 4f, cap = StrokeCap.Round
            )
            // 현재 온도
            if (currentTemp != null) {
                val p = pointAt(animatedCurrent, radius)
                drawCircle(indicatorColor.copy(alpha = 0.18f), track * 3.2f, p)
                drawCircle(indicatorColor.copy(alpha = 0.35f), track * 2.0f, p)
                drawCircle(indicatorColor, track * 1.1f, p)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Caption(centerLabel)
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
