package com.sleepclimate.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.domain.model.SessionStatus
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.components.AdaptiveSplit
import com.sleepclimate.app.ui.components.BigButton
import com.sleepclimate.app.ui.components.BigOutlinedButton
import com.sleepclimate.app.ui.components.CabinView
import com.sleepclimate.app.ui.components.StatusPill
import com.sleepclimate.app.ui.components.TemperatureDial
import com.sleepclimate.app.ui.components.alertColor
import com.sleepclimate.app.ui.components.alertIcon
import com.sleepclimate.app.ui.components.controlActionLabel
import com.sleepclimate.app.ui.components.controlStatusLabel
import com.sleepclimate.app.ui.components.energyText
import com.sleepclimate.app.ui.components.hvacLabel
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 마지막 터치 후 이 시간이 지나면 수면 화면으로 어둡게 전환 */
private const val AUTO_DIM_MS = 30_000L

/**
 * 세션 진행 — 자는 동안 앱이 공조를 관리하는 모습을 보여주고, 필요할 때만 깨운다
 */
@Composable
fun RunningScreen(
    viewModel: SessionViewModel,
    onShowAlerts: () -> Unit,
    onFinished: () -> Unit
) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicleState.collectAsStateWithLifecycle()
    val alerts by viewModel.vehicleAlerts.collectAsStateWithLifecycle()
    val controls by viewModel.controlEvents.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationEnabled.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastTouch by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dimmed by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }

    val current = session ?: return
    val remainingMs = (current.endTime - now).coerceAtLeast(0L)

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            if (current.endTime - now <= 0) viewModel.endSession()
            delay(1_000L)
        }
    }
    LaunchedEffect(current.status) {
        if (current.status == SessionStatus.ENDED) onFinished()
    }
    LaunchedEffect(lastTouch, dimmed) {
        if (!dimmed) {
            delay(AUTO_DIM_MS)
            dimmed = true
        }
    }
    BackHandler { confirmEnd = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(SC.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastTouch = System.currentTimeMillis()
                    }
                }
            }
    ) {
        Column(Modifier.fillMaxSize().padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.session_running_label),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.weight(1f)
                )
                if (simulation) StatusPill(stringResource(R.string.badge_simulation), SC.Warn)
                StatusPill(
                    stringResource(
                        when (vehicle.supportLevel) {
                            VehicleSupportLevel.FULL -> R.string.mode_full
                            VehicleSupportLevel.READ_ONLY -> R.string.mode_read_only
                            VehicleSupportLevel.NONE -> R.string.mode_none
                            VehicleSupportLevel.UNKNOWN -> R.string.status_checking
                        }
                    ),
                    if (vehicle.supportLevel == VehicleSupportLevel.FULL) SC.Accent else SC.TextSecondary
                )
                StatusPill(energyText(vehicle), SC.TextPrimary)
            }
            Spacer(Modifier.height(24.dp))
            AdaptiveSplit(
                visual = { modifier ->
                    Box(modifier.padding(end = 24.dp, bottom = 16.dp).clip(RoundedCornerShape(28.dp)).background(SC.Night)) {
                        CabinView(
                            interiorTemp = vehicle.interiorTempCelsius,
                            hvacOn = vehicle.isHvacOn == true,
                            hvacMode = vehicle.hvacMode,
                            sleeping = true,
                            hasPet = current.hasPet,
                            modifier = Modifier.fillMaxSize()
                        )
                        HvacCaption(vehicle, Modifier.align(Alignment.BottomStart).padding(28.dp))
                    }
                },
                controls = { modifier, wide ->
                    Column(
                        modifier.then(if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        TemperatureDial(
                            currentTemp = vehicle.interiorTempCelsius,
                            targetTemp = current.targetTempCelsius,
                            rangeLow = current.tempRangeLow,
                            rangeHigh = current.tempRangeHigh,
                            centerLabel = stringResource(R.string.dial_label_current),
                            caption = stringResource(
                                R.string.running_dial_caption_format,
                                current.targetTempCelsius, current.tempRangeLow, current.tempRangeHigh
                            ),
                            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(0.75f)
                        )
                        controls.lastOrNull()?.let { last ->
                            Text(
                                stringResource(
                                    R.string.last_control_format,
                                    stringResource(controlActionLabel(last.mode)),
                                    stringResource(controlStatusLabel(last.status)),
                                    SimpleDateFormat("HH:mm:ss", Locale.KOREAN).format(Date(last.timestampMs))
                                ),
                                style = MaterialTheme.typography.bodyLarge, color = SC.Accent, textAlign = TextAlign.Center
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.remaining_label), style = MaterialTheme.typography.labelMedium, color = SC.TextSecondary)
                            Text(formatRemaining(remainingMs), style = MaterialTheme.typography.headlineLarge, fontSize = 48.sp)
                        }
                        alerts.lastOrNull()?.let { AlertBanner(it, onShowAlerts) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            BigOutlinedButton(stringResource(R.string.btn_sleep_screen), { dimmed = true }, Modifier.weight(1f))
                            BigOutlinedButton(
                                if (alerts.isEmpty()) stringResource(R.string.btn_view_alerts_empty)
                                else stringResource(R.string.btn_view_alerts_format, alerts.size),
                                onShowAlerts, Modifier.weight(1f),
                                color = if (alerts.isEmpty()) SC.TextSecondary else SC.Warn
                            )
                        }
                        BigButton(stringResource(R.string.btn_end_session), { confirmEnd = true }, Modifier.fillMaxWidth(), color = SC.Danger)
                    }
                }
            )
        }

        AnimatedVisibility(dimmed, enter = fadeIn(), exit = fadeOut()) {
            SleepOverlay(vehicle, remainingMs, alerts.lastOrNull()) { dimmed = false }
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = SC.Surface,
            title = { Text(stringResource(R.string.end_confirm_title), style = MaterialTheme.typography.headlineMedium) },
            text = { Text(stringResource(R.string.end_confirm_message), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                BigButton(stringResource(R.string.end_confirm_yes), { confirmEnd = false; viewModel.endSession() }, color = SC.Danger)
            },
            dismissButton = {
                BigOutlinedButton(stringResource(R.string.end_confirm_no), { confirmEnd = false })
            }
        )
    }
}

@Composable
private fun HvacCaption(vehicle: VehicleState, modifier: Modifier) {
    val hvac = stringResource(hvacLabel(vehicle))
    Text(
        vehicle.exteriorTempCelsius?.let { stringResource(R.string.hvac_status_format, hvac, it) }
            ?: stringResource(R.string.hvac_status_no_exterior, hvac),
        style = MaterialTheme.typography.titleLarge,
        color = if (vehicle.isHvacOn == true) SC.Cool else SC.TextSecondary,
        modifier = modifier
    )
}

@Composable
private fun AlertBanner(alert: SessionAlert, onClick: () -> Unit) {
    val color = alertColor(alert.type)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(alertIcon(alert.type), fontSize = 28.sp)
        Text(alert.message, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** 수면 화면 — 눈부심 없이 최소 정보만, 터치하면 복귀 */
@Composable
private fun SleepOverlay(vehicle: VehicleState, remainingMs: Long, latestAlert: SessionAlert?, onWake: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SC.Night)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onWake),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                vehicle.interiorTempCelsius?.let { "%.1f°".format(it) } ?: "—",
                style = MaterialTheme.typography.displayLarge,
                color = temperatureColor(vehicle.interiorTempCelsius).copy(alpha = 0.55f)
            )
            Text(
                stringResource(hvacLabel(vehicle)) + "  ·  " + formatRemaining(remainingMs),
                style = MaterialTheme.typography.titleLarge, color = SC.TextSecondary.copy(alpha = 0.7f)
            )
            latestAlert?.takeIf { System.currentTimeMillis() - it.timestampMs < 10 * 60_000L }?.let {
                Text("${alertIcon(it.type)} ${it.message}", style = MaterialTheme.typography.bodyLarge, color = alertColor(it.type))
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.sleep_screen_hint), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary.copy(alpha = 0.4f))
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
