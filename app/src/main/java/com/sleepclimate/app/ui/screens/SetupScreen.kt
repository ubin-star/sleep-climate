package com.sleepclimate.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.RestSession
import com.sleepclimate.app.domain.model.SessionPreset
import com.sleepclimate.app.ui.components.AdaptiveSplit
import com.sleepclimate.app.ui.components.BigButton
import com.sleepclimate.app.ui.components.ChoiceRow
import com.sleepclimate.app.ui.components.InfoValue
import com.sleepclimate.app.ui.components.LineIcon
import com.sleepclimate.app.ui.components.LineIcons
import com.sleepclimate.app.ui.components.Section
import com.sleepclimate.app.ui.components.TempChevronControl
import com.sleepclimate.app.ui.components.TemperatureDial
import com.sleepclimate.app.ui.components.VehicleStatusBar
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.ui.theme.SC
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MIN_TARGET = 16f
private const val MAX_TARGET = 30f

/**
 * 세션 설정 — 프리셋 값으로 채워진 상태에서 시작 버튼만 누르면 된다
 */
@Composable
fun SetupScreen(
    viewModel: SessionViewModel,
    preset: SessionPreset,
    onStarted: () -> Unit,
    onBack: () -> Unit
) {
    val vehicle by viewModel.vehicleState.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationEnabled.collectAsStateWithLifecycle()
    var target by remember { mutableFloatStateOf(preset.targetTempCelsius) }
    var range by remember { mutableFloatStateOf(preset.rangeCelsius) }
    var hours by remember { mutableIntStateOf(preset.durationHours) }
    var energy by remember { mutableIntStateOf(preset.energyAlertPercent) }
    var hasPet by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    val endTime = timeFormat.format(Date(System.currentTimeMillis() + hours * 3_600_000L))

    Column(Modifier.fillMaxSize().background(SC.Background).padding(horizontal = 40.dp, vertical = 28.dp)) {
        VehicleStatusBar(vehicle, stringResource(R.string.session_setup_title), simulation, onBack)
        Spacer(Modifier.height(24.dp))
        AdaptiveSplit(
            visual = { modifier ->
                Box(
                    modifier.padding(end = 32.dp, bottom = 12.dp).clip(RoundedCornerShape(28.dp)).background(SC.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        TemperatureDial(
                            currentTemp = vehicle.interiorTempCelsius,
                            targetTemp = target,
                            rangeLow = target - range,
                            rangeHigh = target + range,
                            centerLabel = stringResource(R.string.dial_label_target),
                            centerValue = "%.1f°".format(target),
                            centerValueIsCurrent = false,
                            caption = stringResource(R.string.setup_dial_caption_format, target - range, target + range),
                            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(0.78f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                            InfoValue(
                                stringResource(R.string.label_interior),
                                vehicle.interiorTempCelsius?.let { "%.1f°".format(it) } ?: "—"
                            )
                            InfoValue(stringResource(R.string.label_end_time), endTime)
                        }
                    }
                }
            },
            controls = { modifier, wide ->
                Column(
                    modifier.then(if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Section(stringResource(R.string.label_target_temp)) {
                        TempChevronControl(
                            value = "%.1f°".format(target),
                            onMinus = { target = (target - 0.5f).coerceAtLeast(MIN_TARGET) },
                            onPlus = { target = (target + 0.5f).coerceAtMost(MAX_TARGET) }
                        )
                    }
                    Section(stringResource(R.string.label_temp_range)) {
                        ChoiceRow(
                            options = listOf(1.0f, 1.5f, 2.0f, 3.0f).map { it to stringResource(R.string.range_option_format, it) },
                            selected = range, onSelect = { range = it }
                        )
                    }
                    Section(stringResource(R.string.label_duration), trailing = stringResource(R.string.end_time_preview_format, endTime)) {
                        ChoiceRow(
                            options = listOf(1, 2, 4, 6, 8, 10).map { it to stringResource(R.string.duration_option_format, it) },
                            selected = hours, onSelect = { hours = it }
                        )
                    }
                    Section(stringResource(R.string.label_energy_alert), trailing = stringResource(R.string.energy_preview_format, energy)) {
                        ChoiceRow(
                            options = listOf(15, 20, 30, 40).map { it to stringResource(R.string.energy_option_format, it) },
                            selected = energy, onSelect = { energy = it }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LineIcon(LineIcons.Paw, tint = if (hasPet) SC.TextPrimary else SC.TextSecondary, size = 26.dp)
                            Text(stringResource(R.string.label_pet), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Switch(
                                checked = hasPet,
                                onCheckedChange = { hasPet = it },
                                colors = SwitchDefaults.colors(
                                    uncheckedTrackColor = SC.SurfaceHigh,
                                    uncheckedBorderColor = SC.SurfaceHigh,
                                    uncheckedThumbColor = SC.TextSecondary
                                )
                            )
                        }
                        if (hasPet) {
                            Text(stringResource(R.string.pet_safety_notice), style = MaterialTheme.typography.bodyMedium, color = SC.Warn)
                        }
                    }
                    BigButton(
                        text = stringResource(R.string.btn_confirm_session),
                        enabled = vehicle.isParked != false,
                        onClick = {
                            viewModel.startSession(
                                RestSession(
                                    targetTempCelsius = target,
                                    tempRangeLow = target - range,
                                    tempRangeHigh = target + range,
                                    endTime = System.currentTimeMillis() + hours * 3_600_000L,
                                    energyAlertPercent = energy,
                                    hasPet = hasPet
                                )
                            )
                            onStarted()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
