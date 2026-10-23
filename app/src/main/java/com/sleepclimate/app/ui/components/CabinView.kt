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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import kotlin.math.min

private val BodyStroke = Color(0xFF3A3F46)
private val InnerStroke = Color(0xFF2E3238)
private val SeatFill = Color(0xFF1C1F23)
private val SilhouetteFill = Color(0xFF41474F)

/**
 * 위에서 내려다본 차량 (앞쪽이 오른쪽) — 차량 상태 화면 스타일의 선 렌더
 *
 * - 실내 빛: 현재 실내 온도 색 (추움 파랑 ~ 쾌적 민트 ~ 더움 주황)
 * - 공조 가동 시 송풍구에서 뒤쪽 매트로 흐르는 유선 (냉방 파랑 / 난방 주황)
 * - 뒷좌석을 펼친 차박 매트와 잠든 사용자 실루엣, 반려동물 동반 시 조수석 실루엣
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
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "flow"
    )
    val breath by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse), label = "breath"
    )
    val airColor by animateColorAsState(temperatureColor(interiorTemp), tween(1500), label = "air")
    val flowColor by animateColorAsState(
        when {
            !hvacOn -> Color.Transparent
            hvacMode == HvacMode.HEATING -> SC.Heat
            hvacMode == HvacMode.COOLING -> SC.Cool
            else -> SC.TextSecondary
        },
        tween(800), label = "flowColor"
    )

    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xFF16191D), SC.Night),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * 0.6f
            )
        )

        val carW = size.width * 0.80f
        val carH = min(size.height * 0.62f, carW * 0.44f)
        val left = (size.width - carW) / 2f
        val top = (size.height - carH) / 2f
        fun x(f: Float) = left + carW * f
        fun y(f: Float) = top + carH * f

        // 바닥 그림자
        drawOval(
            Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)),
            topLeft = Offset(x(-0.04f), y(0.82f)), size = Size(carW * 1.08f, carH * 0.4f)
        )

        // 사이드 미러
        listOf(-0.06f, 0.98f).forEach { my ->
            drawRoundRect(BodyStroke, Offset(x(0.665f), y(my)), Size(carW * 0.04f, carH * 0.08f), CornerRadius(10f))
        }

        // 차체 — 앞(오른쪽)이 더 둥근 실루엣
        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    left = left, top = top, right = left + carW, bottom = top + carH,
                    topLeftCornerRadius = CornerRadius(carW * 0.07f, carH * 0.32f),
                    bottomLeftCornerRadius = CornerRadius(carW * 0.07f, carH * 0.32f),
                    topRightCornerRadius = CornerRadius(carW * 0.16f, carH * 0.46f),
                    bottomRightCornerRadius = CornerRadius(carW * 0.16f, carH * 0.46f)
                )
            )
        }
        drawPath(body, Brush.verticalGradient(listOf(Color(0xFF1E2125), Color(0xFF15171A), Color(0xFF1E2125)), top, top + carH))
        drawPath(body, BodyStroke, style = Stroke(3f))

        // 앞유리·뒷유리
        drawGlass(x(0.70f), x(0.79f), y(0.13f), y(0.87f), front = true)
        drawGlass(x(0.055f), x(0.115f), y(0.18f), y(0.82f), front = false)
        // 보닛 라인
        drawLine(InnerStroke, Offset(x(0.86f), y(0.22f)), Offset(x(0.86f), y(0.78f)), 2f)

        // 실내 바닥
        val cabinLeft = x(0.125f)
        val cabinTop = y(0.11f)
        val cabinW = carW * 0.57f
        val cabinH = carH * 0.78f
        drawRoundRect(Color(0xFF101215), Offset(cabinLeft, cabinTop), Size(cabinW, cabinH), CornerRadius(carH * 0.1f))
        drawRoundRect(
            Brush.radialGradient(
                listOf(airColor.copy(alpha = 0.20f + breath * 0.08f), airColor.copy(alpha = 0.04f), Color.Transparent),
                center = Offset(cabinLeft + cabinW * 0.42f, cabinTop + cabinH / 2),
                radius = cabinW * 0.6f
            ),
            Offset(cabinLeft, cabinTop), Size(cabinW, cabinH), CornerRadius(carH * 0.1f)
        )
        drawRoundRect(InnerStroke, Offset(cabinLeft, cabinTop), Size(cabinW, cabinH), CornerRadius(carH * 0.1f), style = Stroke(2f))

        // 대시보드와 송풍구
        val dashX = x(0.665f)
        drawRoundRect(Color(0xFF1A1D21), Offset(dashX, cabinTop + 6f), Size(carW * 0.028f, cabinH - 12f), CornerRadius(8f))
        val ventYs = listOf(0.24f, 0.5f, 0.76f)
        ventYs.forEach { vy ->
            drawRoundRect(
                if (hvacOn) flowColor else InnerStroke,
                Offset(dashX - 3f, cabinTop + cabinH * vy - carH * 0.035f),
                Size(5f, carH * 0.07f), CornerRadius(3f)
            )
        }

        // 앞좌석 + 스티어링
        listOf(0.15f, 0.56f).forEach { sy -> drawSeat(x(0.505f), y(sy), carW * 0.12f, carH * 0.29f) }
        drawRoundRect(BodyStroke, Offset(x(0.635f), y(0.18f)), Size(carW * 0.008f, carH * 0.22f), CornerRadius(4f))

        // 차박 매트 (뒷좌석 폴딩)
        val bedLeft = x(0.14f)
        val bedRight = x(0.485f)
        val bedTop = y(0.15f)
        val bedH = carH * 0.70f
        drawRoundRect(SeatFill, Offset(bedLeft, bedTop), Size(bedRight - bedLeft, bedH), CornerRadius(16f))
        drawRoundRect(InnerStroke, Offset(bedLeft, bedTop), Size(bedRight - bedLeft, bedH), CornerRadius(16f), style = Stroke(1.5f))
        val stitch = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
        listOf(0.36f, 0.66f).forEach { f ->
            val sx = bedLeft + (bedRight - bedLeft) * f
            drawLine(InnerStroke, Offset(sx, bedTop + 12f), Offset(sx, bedTop + bedH - 12f), 1.5f, pathEffect = stitch)
        }

        if (sleeping) drawSleeper(breath, carW, carH, ::x, ::y)
        if (hasPet) drawPet(x(0.555f), y(0.705f), carH)

        if (hvacOn) {
            drawStreamlines(flow, flowColor, dashX, x(0.30f), cabinTop, cabinH, ventYs)
        }
    }
}

private fun DrawScope.drawGlass(l: Float, r: Float, t: Float, b: Float, front: Boolean) {
    val inset = (b - t) * 0.06f
    val glass = Path().apply {
        if (front) {
            moveTo(l, t + inset); lineTo(r, t); lineTo(r, b); lineTo(l, b - inset)
        } else {
            moveTo(l, t); lineTo(r, t + inset); lineTo(r, b - inset); lineTo(l, b)
        }
        close()
    }
    drawPath(glass, Color(0xFF1A2430))
    drawPath(glass, Color(0xFF2A3644), style = Stroke(1.5f))
}

private fun DrawScope.drawSeat(sx: Float, sy: Float, w: Float, h: Float) {
    val backW = w * 0.28f
    drawRoundRect(SeatFill, Offset(sx + backW * 0.8f, sy), Size(w - backW * 0.8f, h), CornerRadius(14f))
    drawRoundRect(InnerStroke, Offset(sx + backW * 0.8f, sy), Size(w - backW * 0.8f, h), CornerRadius(14f), style = Stroke(1.5f))
    drawRoundRect(Color(0xFF23272C), Offset(sx, sy - 2f), Size(backW, h + 4f), CornerRadius(12f))
    drawRoundRect(InnerStroke, Offset(sx, sy - 2f), Size(backW, h + 4f), CornerRadius(12f), style = Stroke(1.5f))
}

private fun DrawScope.drawSleeper(
    breath: Float,
    carW: Float,
    carH: Float,
    x: (Float) -> Float,
    y: (Float) -> Float
) {
    // 베개
    drawRoundRect(Color(0xFF262A30), Offset(x(0.155f), y(0.33f)), Size(carW * 0.05f, carH * 0.34f), CornerRadius(14f))
    // 머리
    drawCircle(SilhouetteFill, carH * 0.068f, Offset(x(0.19f), y(0.5f)))
    // 이불 — 숨쉬기에 맞춰 천천히 부풀었다 가라앉음
    val blanketH = carH * (0.42f + breath * 0.022f)
    val blanketTop = y(0.5f) - blanketH / 2
    drawRoundRect(
        Brush.horizontalGradient(listOf(Color(0xFF3A4048), Color(0xFF2C3036)), x(0.22f), x(0.47f)),
        Offset(x(0.222f), blanketTop), Size(carW * 0.245f, blanketH), CornerRadius(carH * 0.08f)
    )
    // 접힌 이불 끝선
    drawLine(Color(0xFF3A3F46), Offset(x(0.245f), blanketTop + 10f), Offset(x(0.245f), blanketTop + blanketH - 10f), 2f, cap = StrokeCap.Round)
}

private fun DrawScope.drawPet(cx: Float, cy: Float, carH: Float) {
    drawOval(SilhouetteFill, Offset(cx - carH * 0.07f, cy - carH * 0.045f), Size(carH * 0.14f, carH * 0.09f))
    drawCircle(SilhouetteFill, carH * 0.04f, Offset(cx + carH * 0.08f, cy - carH * 0.02f))
}

/** 송풍구 → 매트 방향 유선. 점선 위상을 움직여 바람이 흐르는 것처럼 보이게 한다 */
private fun DrawScope.drawStreamlines(
    progress: Float,
    color: Color,
    startX: Float,
    endX: Float,
    cabinTop: Float,
    cabinH: Float,
    ventYs: List<Float>
) {
    val lanes = listOf(
        ventYs[0] to 0.30f, ventYs[0] to 0.42f,
        ventYs[1] to 0.50f,
        ventYs[2] to 0.58f, ventYs[2] to 0.70f
    )
    val dashLen = 26f
    val gapLen = 30f
    val effect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen), phase = -progress * (dashLen + gapLen))
    val span = startX - endX
    lanes.forEachIndexed { i, (from, to) ->
        val sy = cabinTop + cabinH * from
        val ey = cabinTop + cabinH * to
        val path = Path().apply {
            moveTo(startX - 8f, sy)
            cubicTo(startX - span * 0.35f, sy, endX + span * 0.30f, ey, endX, ey)
        }
        drawPath(
            path,
            Brush.horizontalGradient(listOf(Color.Transparent, color.copy(alpha = 0.55f), color.copy(alpha = 0.9f)), endX, startX),
            style = Stroke(width = if (i == 2) 3.5f else 2.5f, cap = StrokeCap.Round, pathEffect = effect)
        )
    }
}
