package com.sleepclimate.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.data.vehicle.HvacMode
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.domain.model.SessionPhase
import com.sleepclimate.app.domain.model.SessionStatus
import com.sleepclimate.app.domain.model.effectiveRangeAt
import com.sleepclimate.app.domain.model.phaseAt
import com.sleepclimate.app.domain.model.wakeProgressAt
import com.sleepclimate.app.ui.components.PhaseTimeline
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.components.AdaptiveSplit
import com.sleepclimate.app.ui.components.BigButton
import com.sleepclimate.app.ui.components.BigOutlinedButton
import com.sleepclimate.app.ui.components.CabinView
import com.sleepclimate.app.ui.components.Caption
import com.sleepclimate.app.ui.components.InfoValue
import com.sleepclimate.app.ui.components.LineIcon
import com.sleepclimate.app.ui.components.LineIcons
import com.sleepclimate.app.ui.components.TemperatureDial
import com.sleepclimate.app.ui.components.VehicleStatusBar
import com.sleepclimate.app.ui.components.alertColor
import com.sleepclimate.app.ui.components.alertIcon
import com.sleepclimate.app.ui.components.controlActionLabel
import com.sleepclimate.app.ui.components.controlStatusLabel
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

/** 종료 시각 이후 기상 화면을 유지하는 최대 시간 */
private const val WAKE_GRACE_MS = 5 * 60_000L
private const val SNOOZE_MINUTES = 10

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

    val phase = current.phaseAt(now)
    val range = current.effectiveRangeAt(now)
    val wakeTime = remainingMs == 0L && current.status == SessionStatus.RUNNING

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            // 기상 화면에서 응답이 없으면 일정 시간 뒤 자동 종료 — 공조를 무기한 관리하지 않는다
            val s = viewModel.currentSession.value
            if (s != null && s.status == SessionStatus.RUNNING && now - s.endTime > WAKE_GRACE_MS) viewModel.endSession()
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

    val ambient by animateColorAsState(
        when {
            vehicle.isHvacOn != true -> SC.TextTertiary.copy(alpha = 0.4f)
            vehicle.hvacMode == HvacMode.HEATING -> SC.Heat
            vehicle.hvacMode == HvacMode.COOLING -> SC.Cool
            else -> SC.TextSecondary
        },
        tween(1200), label = "ambient"
    )

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
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
            VehicleStatusBar(vehicle, stringResource(R.string.running_title), simulation)
            Spacer(Modifier.height(24.dp))
            AdaptiveSplit(
                visual = { modifier ->
                    Box(modifier.padding(end = 32.dp, bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(SC.Night)) {
                        CabinView(
                            interiorTemp = vehicle.interiorTempCelsius,
                            hvacOn = vehicle.isHvacOn == true,
                            hvacMode = vehicle.hvacMode,
                            sleeping = true,
                            hasPet = current.hasPet,
                            modifier = Modifier.fillMaxSize()
                        )
                        AmbientStrip(ambient, Modifier.align(Alignment.BottomCenter))
                        Row(
                            Modifier.align(Alignment.BottomStart).padding(36.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LineIcon(LineIcons.Fan, tint = if (vehicle.isHvacOn == true) ambient else SC.TextTertiary, size = 26.dp)
                            Text(
                                stringResource(R.string.info_hvac) + "  " + stringResource(hvacLabel(vehicle)),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (vehicle.isHvacOn == true) SC.TextPrimary else SC.TextSecondary
                            )
                        }
                    }
                },
                controls = { modifier, wide ->
                    Column(
                        modifier.then(if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        TemperatureDial(
                            currentTemp = vehicle.interiorTempCelsius,
                            targetTemp = current.targetTempCelsius,
                            rangeLow = range.start,
                            rangeHigh = range.endInclusive,
                            centerLabel = stringResource(R.string.dial_label_current),
                            caption = stringResource(
                                R.string.running_dial_caption_format,
                                current.targetTempCelsius, range.start, range.endInclusive
                            ),
                            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(0.72f)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            InfoValue(stringResource(R.string.remaining_label), formatRemaining(remainingMs))
                            InfoValue(
                                stringResource(R.string.info_control),
                                stringResource(
                                    when (vehicle.supportLevel) {
                                        VehicleSupportLevel.FULL -> R.string.mode_full
                                        VehicleSupportLevel.READ_ONLY -> R.string.mode_read_only
                                        VehicleSupportLevel.NONE -> R.string.mode_none
                                        VehicleSupportLevel.UNKNOWN -> R.string.status_checking
                                    }
                                )
                            )
                        }
                        PhaseTimeline(current, now)
                        if (phase == SessionPhase.WAKING) {
                            Caption(stringResource(R.string.phase_waking_detail), color = SC.Heat)
                        }
                        controls.lastOrNull()?.let { last ->
                            Caption(
                                stringResource(
                                    R.string.last_control_format,
                                    stringResource(controlActionLabel(last.mode)),
                                    stringResource(controlStatusLabel(last.status)),
                                    SimpleDateFormat("HH:mm:ss", Locale.KOREAN).format(Date(last.timestampMs))
                                ),
                                color = SC.TextSecondary
                            )
                        }
                        alerts.lastOrNull()?.let { AlertBanner(it, onShowAlerts) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BigOutlinedButton(stringResource(R.string.btn_sleep_screen), { dimmed = true }, Modifier.weight(1f), icon = LineIcons.Moon)
                            BigOutlinedButton(
                                if (alerts.isEmpty()) stringResource(R.string.btn_view_alerts_empty)
                                else stringResource(R.string.btn_view_alerts_format, alerts.size),
                                onShowAlerts, Modifier.weight(1f),
                                color = if (alerts.isEmpty()) SC.TextSecondary else SC.Warn,
                                icon = LineIcons.Bell
                            )
                        }
                        BigOutlinedButton(stringResource(R.string.btn_end_session), { confirmEnd = true }, Modifier.fillMaxWidth(), color = SC.Danger)
                    }
                }
            )
        }

        AnimatedVisibility(dimmed && !wakeTime, enter = fadeIn(), exit = fadeOut()) {
            SleepOverlay(vehicle, remainingMs, alerts.lastOrNull(), ambient, current.wakeProgressAt(now)) { dimmed = false }
        }
        AnimatedVisibility(wakeTime, enter = fadeIn(tween(1500)), exit = fadeOut()) {
            WakeOverlay(
                vehicle = vehicle,
                targetTemp = current.targetTempCelsius,
                minutesUntilAutoEnd = ((WAKE_GRACE_MS - (now - current.endTime)) / 60_000L + 1).toInt().coerceAtLeast(1),
                onSnooze = { viewModel.extendSession(SNOOZE_MINUTES); dimmed = true },
                onReport = { viewModel.endSession() }
            )
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            containerColor = SC.Surface,
            title = { Text(stringResource(R.string.end_confirm_title), style = MaterialTheme.typography.headlineMedium) },
            text = { Text(stringResource(R.string.end_confirm_message), style = MaterialTheme.typography.bodyLarge, color = SC.TextSecondary) },
            confirmButton = {
                BigButton(stringResource(R.string.end_confirm_yes), { confirmEnd = false; viewModel.endSession() }, color = SC.Danger)
            },
            dismissButton = {
                BigOutlinedButton(stringResource(R.string.end_confirm_no), { confirmEnd = false })
            }
        )
    }
}

/** 실내 앰비언트 라이트처럼 공조 상태 색으로 은은하게 빛나는 하단 라인 */
@Composable
private fun AmbientStrip(color: Color, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth(0.7f)
            .height(3.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent)))
    )
}

@Composable
private fun AlertBanner(alert: SessionAlert, onClick: () -> Unit) {
    val color = alertColor(alert.type)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SC.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LineIcon(alertIcon(alert.type), tint = color, size = 26.dp)
        Text(alert.message, style = MaterialTheme.typography.bodyLarge, color = SC.TextPrimary, modifier = Modifier.weight(1f))
        LineIcon(LineIcons.Forward, tint = SC.TextTertiary, size = 20.dp)
    }
}

/** 수면 화면 — 큰 숫자 하나만 남기고 눈부심 없이, 터치하면 복귀 */
@Composable
private fun SleepOverlay(
    vehicle: VehicleState,
    remainingMs: Long,
    latestAlert: SessionAlert?,
    ambient: Color,
    wakeProgress: Float,
    onWake: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SC.Night)
            // 기상 단계: 화면 아래에서부터 일출처럼 따뜻한 빛이 서서히 차오른다
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to SC.Heat.copy(alpha = 0.06f * wakeProgress),
                    1f to SC.Heat.copy(alpha = 0.42f * wakeProgress)
                )
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onWake),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                vehicle.interiorTempCelsius?.let { "%.1f°".format(it) } ?: "—",
                style = MaterialTheme.typography.displayLarge,
                fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.4f,
                color = temperatureColor(vehicle.interiorTempCelsius).copy(alpha = 0.6f)
            )
            Box(Modifier.fillMaxWidth(0.18f).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, ambient, Color.Transparent))))
            Text(
                stringResource(hvacLabel(vehicle)) + "   ·   " + formatRemaining(remainingMs),
                style = MaterialTheme.typography.titleLarge, color = SC.TextTertiary
            )
            latestAlert?.takeIf { System.currentTimeMillis() - it.timestampMs < 10 * 60_000L }?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(alertIcon(it.type), tint = alertColor(it.type), size = 22.dp)
                    Text(it.message, style = MaterialTheme.typography.bodyLarge, color = alertColor(it.type))
                }
            }
        }
        Caption(
            stringResource(R.string.sleep_screen_hint),
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            color = SC.TextTertiary.copy(alpha = 0.6f)
        )
    }
}

/** 기상 화면 — 휴식 종료 시각. 더 쉬거나 리포트로 이동 */
@Composable
private fun WakeOverlay(
    vehicle: VehicleState,
    targetTemp: Float,
    minutesUntilAutoEnd: Int,
    onSnooze: () -> Unit,
    onReport: () -> Unit
) {
    val clock = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    Box(
        Modifier
            .fillMaxSize()
            .background(SC.Night)
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.5f to SC.Heat.copy(alpha = 0.06f), 1f to SC.Heat.copy(alpha = 0.32f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(clock.format(Date()), style = MaterialTheme.typography.displayLarge, color = SC.TextPrimary)
            Text(stringResource(R.string.wake_title), style = MaterialTheme.typography.headlineLarge)
            Text(
                vehicle.interiorTempCelsius?.let { stringResource(R.string.wake_message_format, it, targetTemp) } ?: "",
                style = MaterialTheme.typography.titleLarge, color = SC.TextSecondary
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigOutlinedButton(stringResource(R.string.btn_snooze), onSnooze, Modifier.widthIn(min = 260.dp), icon = LineIcons.Moon)
                BigButton(stringResource(R.string.btn_view_report), onReport, Modifier.widthIn(min = 260.dp))
            }
            Caption(stringResource(R.string.wake_auto_end_format, minutesUntilAutoEnd), color = SC.TextTertiary)
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d:%02d".format(h, m, s)
}
