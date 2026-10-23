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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.data.vehicle.simulation.SimulationScenario
import com.sleepclimate.app.domain.model.SessionPreset
import com.sleepclimate.app.domain.model.VehicleState
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.components.AdaptiveSplit
import com.sleepclimate.app.ui.components.CabinView
import com.sleepclimate.app.ui.components.Caption
import com.sleepclimate.app.ui.components.ChoiceRow
import com.sleepclimate.app.ui.components.LineIcon
import com.sleepclimate.app.ui.components.LineIcons
import com.sleepclimate.app.ui.components.Panel
import com.sleepclimate.app.ui.components.VehicleStatusBar
import com.sleepclimate.app.ui.components.dimmedIf
import com.sleepclimate.app.ui.home.HomeViewModel
import com.sleepclimate.app.ui.home.StartAvailability
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor

/**
 * 홈 — 차량 렌더와 휴식 모드 타일을 첫 화면에 바로 (하위 메뉴 없이 한 번 탭으로 진입)
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onPresetSelected: (SessionPreset) -> Unit) {
    val state by viewModel.vehicleState.collectAsStateWithLifecycle()
    val support by viewModel.supportLevel.collectAsStateWithLifecycle()
    val availability by viewModel.startAvailability.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationEnabled.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(SC.Background).padding(horizontal = 40.dp, vertical = 28.dp)) {
        VehicleStatusBar(state, stringResource(R.string.app_name), simulation)
        Spacer(Modifier.height(24.dp))
        AdaptiveSplit(
            visual = { modifier -> CabinPanel(state, support, modifier.padding(end = 32.dp, bottom = 12.dp)) },
            controls = { modifier, wide ->
                Column(
                    modifier.then(if (wide) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Caption(stringResource(R.string.home_question))
                    ModeTiles(enabled = availability == StartAvailability.READY, onPresetSelected = onPresetSelected)
                    StartHint(availability)
                    Spacer(Modifier.height(4.dp))
                    SimulationCard(simulation, viewModel.simulationScenario, viewModel::setSimulation)
                }
            }
        )
    }
}

@Composable
private fun CabinPanel(state: VehicleState, support: VehicleSupportLevel, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(28.dp)).background(SC.Night)) {
        CabinView(
            interiorTemp = state.interiorTempCelsius,
            hvacOn = state.isHvacOn == true,
            hvacMode = state.hvacMode,
            sleeping = false,
            hasPet = false,
            modifier = Modifier.fillMaxSize()
        )
        Column(Modifier.align(Alignment.TopStart).padding(36.dp)) {
            Caption(stringResource(R.string.label_interior))
            Text(
                state.interiorTempCelsius?.let { "%.1f°".format(it) } ?: "—",
                style = MaterialTheme.typography.displayMedium,
                color = temperatureColor(state.interiorTempCelsius)
            )
            Text(
                stringResource(
                    when (support) {
                        VehicleSupportLevel.FULL -> R.string.home_mode_full
                        VehicleSupportLevel.READ_ONLY -> R.string.home_mode_read_only
                        VehicleSupportLevel.NONE -> R.string.badge_no_support
                        VehicleSupportLevel.UNKNOWN -> R.string.status_checking
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = SC.TextSecondary
            )
        }
    }
}

private data class ModeTile(val preset: SessionPreset, val title: Int, val icon: ImageVector, val tint: Color)

@Composable
private fun ModeTiles(enabled: Boolean, onPresetSelected: (SessionPreset) -> Unit) {
    val tiles = listOf(
        ModeTile(SessionPreset.SUMMER_CAMPING, R.string.preset_summer, LineIcons.Snowflake, SC.Cool),
        ModeTile(SessionPreset.WINTER_CAMPING, R.string.preset_winter, LineIcons.Flame, SC.Heat),
        ModeTile(SessionPreset.SHORT_REST, R.string.preset_short, LineIcons.Cup, SC.TextPrimary),
        ModeTile(SessionPreset.CUSTOM, R.string.preset_custom, LineIcons.Sliders, SC.TextSecondary)
    )
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { tile ->
                    Panel(
                        modifier = Modifier.weight(1f).dimmedIf(!enabled),
                        onClick = if (enabled) ({ onPresetSelected(tile.preset) }) else null
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            LineIcon(tile.icon, tint = tile.tint, size = 34.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(stringResource(tile.title), style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (tile.preset == SessionPreset.CUSTOM) " "
                                    else stringResource(R.string.preset_detail_format, tile.preset.targetTempCelsius, tile.preset.durationHours),
                                    style = MaterialTheme.typography.bodyLarge, color = SC.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartHint(availability: StartAvailability) {
    val hint = when (availability) {
        StartAvailability.CHECKING     -> R.string.home_hint_checking
        StartAvailability.DISCONNECTED -> R.string.home_hint_disconnected
        StartAvailability.NO_SUPPORT   -> R.string.home_hint_no_support
        StartAvailability.DRIVING      -> R.string.home_hint_driving
        StartAvailability.READY        -> null
    } ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LineIcon(LineIcons.Warning, tint = SC.Warn, size = 22.dp)
        Text(stringResource(hint), style = MaterialTheme.typography.bodyLarge, color = SC.Warn)
    }
}

@Composable
private fun SimulationCard(
    enabled: Boolean,
    scenario: SimulationScenario,
    onChange: (Boolean, SimulationScenario) -> Unit
) {
    var selected by remember { mutableStateOf(scenario) }
    Panel(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LineIcon(LineIcons.Flask, tint = if (enabled) SC.Warn else SC.TextSecondary, size = 28.dp)
                Text(stringResource(R.string.sim_switch_label), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { onChange(it, selected) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = SC.Warn,
                        uncheckedTrackColor = SC.SurfaceHigh,
                        uncheckedBorderColor = SC.SurfaceHigh,
                        uncheckedThumbColor = SC.TextSecondary
                    )
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
