package com.sleepclimate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 위에서 내려다본 차량 실내 (앞쪽이 오른쪽)
 *
 * - 실내 공기 색: 현재 실내 온도에 따라 파랑(추움) ~ 초록(쾌적) ~ 주황(더움)
 * - 공조 가동 시 송풍구에서 침대 쪽으로 흐르는 바람 입자 (냉방 파랑 / 난방 주황)
 * - 뒷좌석을 펼친 차박 매트 위에서 잠든 사용자, 반려동물 동반 시 조수석에 반려동물
 */
@Composable
fun CabinView(
    interiorTemp: Float?,
    hvacOn: Boolean,
    hvacMode: HvacMode?,
    sleeping: Boolean,
    hasPet: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "cabin")
    val flow by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "flow"
    )
    val breath by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "breath"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "twinkle"
    )
    val zzz by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "zzz"
    )

    val airColor by animateColorAsState(temperatureColor(interiorTemp), tween(1500), label = "air")
    val flowColor = when {
        !hvacOn -> Color.Transparent
        hvacMode == HvacMode.HEATING -> SC.Heat
        hvacMode == HvacMode.COOLING -> SC.Cool
        else -> SC.Accent
    }
    val textMeasurer = rememberTextMeasurer()
    val stars = remember { List(40) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }

    Canvas(modifier) {
        drawNightSky(stars, twinkle)

        val carW = size.width * 0.86f
        val carH = min(size.height * 0.70f, carW * 0.48f)
        val left = (size.width - carW) / 2f
        val top = (size.height - carH) / 2f + size.height * 0.04f
        fun x(f: Float) = left + carW * f
        fun y(f: Float) = top + carH * f

        // 바퀴
        val wheelW = carW * 0.13f
        val wheelH = carH * 0.11f
        listOf(0.17f, 0.73f).forEach { wx ->
            drawRoundRect(Color(0xFF05070A), Offset(x(wx), top - wheelH * 0.5f), Size(wheelW, wheelH), CornerRadius(wheelH / 2))
            drawRoundRect(Color(0xFF05070A), Offset(x(wx), top + carH - wheelH * 0.5f), Size(wheelW, wheelH), CornerRadius(wheelH / 2))
        }

        // 차체
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF2A3441), Color(0xFF1A212B), Color(0xFF2A3441)), top, top + carH),
            topLeft = Offset(left, top), size = Size(carW, carH), cornerRadius = CornerRadius(carH * 0.3f)
        )
        drawRoundRect(SC.Border, Offset(left, top), Size(carW, carH), CornerRadius(carH * 0.3f), style = Stroke(2.5f))

        // 앞유리 / 뒷유리
        drawRoundRect(SC.Accent.copy(alpha = 0.22f), Offset(x(0.735f), y(0.12f)), Size(carW * 0.07f, carH * 0.76f), CornerRadius(carH * 0.08f))
        drawRoundRect(SC.Accent.copy(alpha = 0.14f), Offset(x(0.045f), y(0.18f)), Size(carW * 0.05f, carH * 0.64f), CornerRadius(carH * 0.08f))

        // 실내
        val cabinLeft = x(0.11f)
        val cabinTop = y(0.09f)
        val cabinW = carW * 0.615f
        val cabinH = carH * 0.82f
        drawRoundRect(SC.Night, Offset(cabinLeft, cabinTop), Size(cabinW, cabinH), CornerRadius(carH * 0.12f))
        // 실내 공기 온도 글로우
        drawRoundRect(
            brush = Brush.radialGradient(
                listOf(airColor.copy(alpha = 0.30f + breath * 0.10f), airColor.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(cabinLeft + cabinW * 0.45f, cabinTop + cabinH / 2),
                radius = cabinW * 0.62f
            ),
            topLeft = Offset(cabinLeft, cabinTop), size = Size(cabinW, cabinH), cornerRadius = CornerRadius(carH * 0.12f)
        )

        // 대시보드와 송풍구
        val dashX = x(0.685f)
        drawRoundRect(SC.SurfaceHigh, Offset(dashX, cabinTop + cabinH * 0.04f), Size(carW * 0.035f, cabinH * 0.92f), CornerRadius(8f))
        val ventYs = listOf(0.22f, 0.5f, 0.78f)
        ventYs.forEach { vy ->
            drawRoundRect(
                if (hvacOn) flowColor else SC.Border,
                Offset(dashX - 2f, cabinTop + cabinH * vy - carH * 0.04f),
                Size(carW * 0.012f, carH * 0.08f), CornerRadius(4f)
            )
        }

        // 앞좌석 (운전석 위, 조수석 아래) + 핸들
        listOf(0.14f, 0.56f).forEach { sy ->
            drawRoundRect(Color(0xFF323C49), Offset(x(0.535f), y(sy)), Size(carW * 0.10f, carH * 0.30f), CornerRadius(14f))
            drawRoundRect(Color(0xFF3C4756), Offset(x(0.505f), y(sy) - 2f), Size(carW * 0.035f, carH * 0.30f + 4f), CornerRadius(12f))
        }
        drawRoundRect(Color(0xFF4A5666), Offset(x(0.655f), y(0.17f)), Size(carW * 0.012f, carH * 0.24f), CornerRadius(6f))

        // 뒷좌석을 펼친 차박 매트
        val bedLeft = x(0.125f)
        val bedRight = x(0.48f)
        drawRoundRect(Color(0xFF283240), Offset(bedLeft, y(0.13f)), Size(bedRight - bedLeft, carH * 0.74f), CornerRadius(18f))
        drawRoundRect(Color(0xFF3A4555), Offset(x(0.14f), y(0.31f)), Size(carW * 0.055f, carH * 0.38f), CornerRadius(14f))

        // 바람 입자 — 송풍구 → 침대
        if (hvacOn) drawAirflow(flow, flowColor, dashX, bedLeft + carW * 0.08f, cabinTop, cabinH)

        if (sleeping) drawSleeper(textMeasurer, breath, zzz, carW, carH, ::x, ::y)

        if (hasPet) {
            drawText(
                textMeasurer, "🐕",
                topLeft = Offset(x(0.55f), y(0.62f)),
                style = TextStyle(fontSize = (carH * 0.10f / density).sp)
            )
        }
    }
}

private fun DrawScope.drawNightSky(stars: List<Triple<Float, Float, Float>>, twinkle: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1220), SC.Background)))
    stars.forEach { (sx, sy, phase) ->
        val alpha = 0.25f + 0.55f * ((sin(twinkle + phase * 6f) + 1f) / 2f)
        drawCircle(Color.White.copy(alpha = alpha), radius = 1.5f + phase * 1.8f, center = Offset(sx * size.width, sy * size.height))
    }
    // 초승달
    val moonCenter = Offset(size.width * 0.9f, size.height * 0.12f)
    val r = min(size.width, size.height) * 0.045f
    drawCircle(SC.Warn.copy(alpha = 0.9f), r, moonCenter)
    drawCircle(Color(0xFF0A1220), r * 0.85f, moonCenter + Offset(r * 0.45f, -r * 0.25f))
}

private fun DrawScope.drawAirflow(
    progress: Float,
    color: Color,
    startX: Float,
    endX: Float,
    cabinTop: Float,
    cabinH: Float
) {
    val count = 21
    repeat(count) { i ->
        val phase = (progress + i.toFloat() / count) % 1f
        val lane = (i % 3) - 1
        val px = startX - phase * (startX - endX)
        val py = cabinTop + cabinH / 2 + lane * cabinH * 0.27f +
                sin(phase * 2 * PI.toFloat() * 1.5f + i) * cabinH * 0.05f
        val alpha = sin(phase * PI.toFloat()) * 0.9f
        drawLine(
            color.copy(alpha = alpha * 0.5f),
            start = Offset(px + 26f, py), end = Offset(px, py),
            strokeWidth = 6f, cap = StrokeCap.Round
        )
        drawCircle(color.copy(alpha = alpha), radius = 5f, center = Offset(px, py))
    }
}

private fun DrawScope.drawSleeper(
    textMeasurer: TextMeasurer,
    breath: Float,
    zzz: Float,
    carW: Float,
    carH: Float,
    x: (Float) -> Float,
    y: (Float) -> Float
) {
    // 머리
    val headCenter = Offset(x(0.205f), y(0.5f))
    val headR = carH * 0.075f
    drawCircle(Color(0xFFD9B99B), headR, headCenter)
    // 이불 — 숨쉬기에 맞춰 살짝 부풀었다 가라앉음
    val blanketH = carH * (0.40f + breath * 0.025f)
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(SC.Accent.copy(alpha = 0.85f), Color(0xFF1F5FBF)), x(0.23f), x(0.46f)),
        topLeft = Offset(x(0.235f), y(0.5f) - blanketH / 2),
        size = Size(carW * 0.225f, blanketH),
        cornerRadius = CornerRadius(carH * 0.1f)
    )
    // Zzz
    repeat(3) { i ->
        val p = (zzz + i / 3f) % 1f
        val alpha = sin(p * PI.toFloat())
        drawText(
            textMeasurer, "Z",
            topLeft = Offset(headCenter.x + headR * (0.6f + p * 1.6f), headCenter.y - headR * (1.2f + p * 3.2f)),
            style = TextStyle(
                color = SC.TextPrimary.copy(alpha = alpha * 0.85f),
                fontSize = ((carH * (0.05f + p * 0.04f)) / density).sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
