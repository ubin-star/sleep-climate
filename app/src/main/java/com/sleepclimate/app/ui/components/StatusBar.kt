package com.sleepclimate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.ui.theme.SC
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 화면 상단 차량 상태줄 — 왼쪽: 뒤로·화면 제목, 오른쪽: 기어 · 배터리 · 외기 · 시계
 */
@Composable
fun VehicleStatusBar(
    state: VehicleState,
    title: String,
    simulation: Boolean,
    onBack: (() -> Unit)? = null
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            now = System.currentTimeMillis()
        }
    }
    val clock = remember(now) { SimpleDateFormat("HH:mm", Locale.KOREAN).format(Date(now)) }

    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            BackButton(onBack)
            Box(Modifier.width(20.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            if (simulation) StatusItem(LineIcons.Flask, stringResource(R.string.badge_simulation), SC.Warn)
            if (state.lastUpdatedMs > 0 && !state.isConnected) {
                StatusItem(LineIcons.Link, stringResource(R.string.status_disconnected), SC.Danger)
            }
            GearIndicator(state.isParked)
            val energy = state.batteryPercent ?: state.fuelPercent
            StatusItem(LineIcons.Battery, energy?.let { "$it%" } ?: "—", if ((energy ?: 100) <= 20) SC.Warn else SC.TextSecondary)
            state.exteriorTempCelsius?.let {
                Text(stringResource(R.string.exterior_short_format, it), style = MaterialTheme.typography.bodyLarge, color = SC.TextSecondary)
            }
            Text(clock, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
        }
    }
}

/** P R N D 중 현재 상태만 밝게 — 차량 계기판의 기어 표시 방식 */
@Composable
private fun GearIndicator(isParked: Boolean?) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf("P", "D").forEach { gear ->
            val active = (gear == "P" && isParked == true) || (gear == "D" && isParked == false)
            Text(
                gear,
                fontSize = 20.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    !active -> SC.TextTertiary
                    gear == "D" -> SC.Danger
                    else -> SC.TextPrimary
                }
            )
        }
    }
}

/** ‹ 22.0° › — 차량 공조 온도 조절과 같은 방식 */
@Composable
fun TempChevronControl(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ChevronButton(LineIcons.Back, onMinus)
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.displayMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        ChevronButton(LineIcons.Forward, onPlus)
    }
}

@Composable
private fun ChevronButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(80.dp).clip(CircleShape).background(SC.SurfaceHigh).clickable(onClick = onClick).padding(4.dp),
        contentAlignment = Alignment.Center
    ) { LineIcon(icon, size = 34.dp) }
}
