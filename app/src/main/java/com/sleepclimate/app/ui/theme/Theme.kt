package com.sleepclimate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 차량 디스플레이용 다크 팔레트 — 야간 차박 중 눈부심을 줄이는 어두운 배경 */
object SC {
    val Background = Color(0xFF0B0F14)
    val Surface = Color(0xFF151B23)
    val SurfaceHigh = Color(0xFF1E2630)
    val Border = Color(0xFF2A3441)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val Accent = Color(0xFF2F81F7)
    val Cool = Color(0xFF4FC3F7)
    val Heat = Color(0xFFFF8A50)
    val Ok = Color(0xFF3FB950)
    val Warn = Color(0xFFD29922)
    val Danger = Color(0xFFF85149)
    val Night = Color(0xFF070A0E)
}

/** 실내 온도를 색으로 — 추움(파랑) → 쾌적(초록) → 더움(주황·빨강) */
fun temperatureColor(tempCelsius: Float?): Color {
    if (tempCelsius == null) return SC.TextSecondary
    return when {
        tempCelsius <= 18f -> SC.Cool
        tempCelsius <= 22f -> lerp(SC.Cool, SC.Ok, (tempCelsius - 18f) / 4f)
        tempCelsius <= 26f -> lerp(SC.Ok, SC.Heat, (tempCelsius - 22f) / 4f)
        tempCelsius <= 30f -> lerp(SC.Heat, SC.Danger, (tempCelsius - 26f) / 4f)
        else -> SC.Danger
    }
}

private val colorScheme = darkColorScheme(
    primary = SC.Accent,
    onPrimary = Color.White,
    secondary = SC.Cool,
    background = SC.Background,
    onBackground = SC.TextPrimary,
    surface = SC.Surface,
    onSurface = SC.TextPrimary,
    surfaceVariant = SC.SurfaceHigh,
    onSurfaceVariant = SC.TextSecondary,
    outline = SC.Border,
    error = SC.Danger
)

/** 운전석·뒷좌석 거리에서도 읽히도록 큰 글자 위주 */
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 88.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp),
    bodyMedium = TextStyle(fontSize = 17.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun SleepClimateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        Surface(color = SC.Background, contentColor = SC.TextPrimary, content = content)
    }
}
