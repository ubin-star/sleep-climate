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

/**
 * 차량 디스플레이 팔레트 — 따뜻한 무채색 블랙 위에 포인트 색은 의미가 있을 때만 사용
 * (냉방·난방·경고·주요 동작)
 */
object SC {
    val Background = Color(0xFF0C0D0F)
    val Surface = Color(0xFF16181B)
    val SurfaceHigh = Color(0xFF202327)
    val Divider = Color(0xFF2B2F34)
    val TextPrimary = Color(0xFFF1F1EE)
    val TextSecondary = Color(0xFF8E9298)
    val TextTertiary = Color(0xFF5D6167)
    val Accent = Color(0xFF5B8CFF)
    val Cool = Color(0xFF63C7F5)
    val Heat = Color(0xFFFF9E57)
    val Ok = Color(0xFF7CCFA4)
    val Warn = Color(0xFFE6B450)
    val Danger = Color(0xFFFF6B5E)
    val Night = Color(0xFF08090A)
}

/** 실내 온도를 색으로 — 추움(파랑) → 쾌적(민트) → 더움(주황·빨강) */
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
    outline = SC.Divider,
    error = SC.Danger
)

/** 큰 숫자는 얇게, 라벨은 작고 차분하게 — 순정 클러스터 계열의 위계 */
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 128.sp, fontWeight = FontWeight.Thin, letterSpacing = (-4).sp),
    displayMedium = TextStyle(fontSize = 84.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Normal),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)
)

@Composable
fun SleepClimateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        Surface(color = SC.Background, contentColor = SC.TextPrimary, content = content)
    }
}
