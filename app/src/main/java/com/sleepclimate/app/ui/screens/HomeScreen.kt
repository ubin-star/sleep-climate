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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.data.vehicle.simulation.SimulationScenario
import com.sleepclimate.app.domain.model.SessionPreset
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.components.AdaptiveSplit
import com.sleepclimate.app.ui.components.CabinView
import com.sleepclimate.app.ui.components.ChoiceRow
import com.sleepclimate.app.ui.components.Panel
import com.sleepclimate.app.ui.components.StatusPill
import com.sleepclimate.app.ui.components.energyText
import com.sleepclimate.app.ui.components.supportLabel
import com.sleepclimate.app.ui.home.HomeViewModel
import com.sleepclimate.app.ui.home.StartAvailability
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 홈 — 차량 상태를 한눈에, 프리셋 카드 한 번 탭으로 세션 설정 진입
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onPresetSelected: (SessionPreset) -> Unit) {
    val state by viewModel.vehicleState.collectAsStateWithLifecycle()
    val support by viewModel.supportLevel.collectAsStateWithLifecycle()
    val availability by viewModel.startAvailability.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationEnabled.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(SC.Background).padding(32.dp)) {
        TopStatusBar(state, support, simulation)
        Spacer(Modifier.height(24.dp))
        AdaptiveSplit(
            visual = { modifier -> CabinPanel(state, modifier.padding(end = 24.dp, bottom = 16.dp)) },
            controls = { modifier, wide ->
                Column(
                    modifier.then(if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(stringResource(R.string.home_question), style = MaterialTheme.typography.headlineMedium)
                    PresetGrid(enabled = availability == StartAvailability.READY, onPresetSelected = onPresetSelected)
                    StartHint(availability, support)
                    SimulationCard(
                        enabled = simulation,
                        scenario = viewModel.simulationScenario,
                        onChange = viewModel::setSimulation
                    )
                }
            }
        )
    }
}

@Composable
fun TopStatusBar(state: VehicleState, support: VehicleSupportLevel, simulation: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, letterSpacing = 2.sp)
            Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (simulation) StatusPill(stringResource(R.string.badge_simulation), SC.Warn)
            val connected = state.lastUpdatedMs > 0 && state.isConnected
            StatusPill(
                stringResource(
                    when {
                        state.lastUpdatedMs == 0L -> R.string.status_checking
                        state.isConnected -> R.string.status_connected
                        else -> R.string.status_disconnected
                    }
                ),
                if (connected) SC.Ok else SC.TextSecondary
            )
            StatusPill(stringResource(supportLabel(support)), if (support == VehicleSupportLevel.FULL) SC.Accent else SC.TextSecondary)
            StatusPill(
                stringResource(
                    when (state.isParked) {
                        true -> R.string.park_status_parked
                        false -> R.string.park_status_driving
                        null -> R.string.park_status_unknown
                    }
                ),
                if (state.isParked == false) SC.Danger else SC.TextPrimary
            )
            StatusPill(energyText(state), SC.TextPrimary)
        }
    }
}

@Composable
private fun CabinPanel(state: VehicleState, modifier: Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.KOREAN) }
    Box(modifier.clip(RoundedCornerShape(28.dp)).background(SC.Night)) {
        CabinView(
            interiorTemp = state.interiorTempCelsius,
            hvacOn = state.isHvacOn == true,
            hvacMode = state.hvacMode,
            sleeping = false,
            hasPet = false,
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
            Text(
                state.interiorTempCelsius?.let { stringResource(R.string.interior_short_format, it) }
                    ?: stringResource(R.string.current_vehicle_temp_unavailable),
                style = MaterialTheme.typography.displayMedium,
                color = temperatureColor(state.interiorTempCelsius)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                state.exteriorTempCelsius?.let {
                    Text(stringResource(R.string.exterior_short_format, it), style = MaterialTheme.typography.titleMedium, color = SC.TextSecondary)
                }
                if (state.lastUpdatedMs > 0) {
                    Text(
                        stringResource(R.string.last_updated_format, timeFormat.format(Date(state.lastUpdatedMs))),
                        style = MaterialTheme.typography.titleMedium, color = SC.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetGrid(enabled: Boolean, onPresetSelected: (SessionPreset) -> Unit) {
    val presets = listOf(
        SessionPreset.SUMMER_CAMPING to R.string.preset_summer,
        SessionPreset.WINTER_CAMPING to R.string.preset_winter,
        SessionPreset.SHORT_REST to R.string.preset_short,
        SessionPreset.CUSTOM to R.string.preset_custom
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        presets.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (preset, label) ->
                    PresetCard(preset, stringResource(label), enabled, Modifier.weight(1f)) { onPresetSelected(preset) }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(preset: SessionPreset, title: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Panel(
        modifier = modifier.alpha(if (enabled) 1f else 0.4f),
        onClick = if (enabled) onClick else null,
        borderColor = if (enabled && preset != SessionPreset.CUSTOM) SC.Accent.copy(alpha = 0.6f) else SC.Border
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(preset.emoji, fontSize = 40.sp)
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (preset != SessionPreset.CUSTOM) {
                Text(
                    stringResource(R.string.preset_detail_format, preset.targetTempCelsius, preset.durationHours),
                    style = MaterialTheme.typography.bodyLarge, color = SC.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StartHint(availability: StartAvailability, support: VehicleSupportLevel) {
    val hint = when (availability) {
        StartAvailability.CHECKING     -> R.string.home_hint_checking
        StartAvailability.DISCONNECTED -> R.string.home_hint_disconnected
        StartAvailability.NO_SUPPORT   -> R.string.home_hint_no_support
        StartAvailability.DRIVING      -> R.string.home_hint_driving
        StartAvailability.READY        -> if (support == VehicleSupportLevel.READ_ONLY) R.string.home_hint_read_only else null
    } ?: return
    Text(stringResource(hint), style = MaterialTheme.typography.bodyLarge, color = SC.Warn)
}

@Composable
private fun SimulationCard(
    enabled: Boolean,
    scenario: SimulationScenario,
    onChange: (Boolean, SimulationScenario) -> Unit
) {
    var selected by remember { mutableStateOf(scenario) }
    Panel(Modifier.fillMaxWidth(), borderColor = if (enabled) SC.Warn.copy(alpha = 0.6f) else SC.Border) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.badge_simulation), style = MaterialTheme.typography.labelMedium, color = SC.Warn)
                    Text(stringResource(R.string.sim_switch_label), style = MaterialTheme.typography.titleLarge)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onChange(it, selected) },
                    colors = SwitchDefaults.colors(checkedTrackColor = SC.Warn)
                )
            }
            if (enabled) {
                ChoiceRow(
                    options = listOf(
                        SimulationScenario.SUMMER_NIGHT to stringResource(R.string.sim_scenario_summer),
                        SimulationScenario.WINTER_NIGHT to stringResource(R.string.sim_scenario_winter),
                        SimulationScenario.LOW_BATTERY to stringResource(R.string.sim_scenario_low_battery)
                    ),
                    selected = selected,
                    onSelect = { selected = it; onChange(true, it) }
                )
                Text(stringResource(R.string.sim_notice), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
            }
        }
    }
}
