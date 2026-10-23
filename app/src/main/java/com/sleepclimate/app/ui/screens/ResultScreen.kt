package com.sleepclimate.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.ControlStatus
import com.sleepclimate.app.domain.model.VehicleSupportLevel
import com.sleepclimate.app.ui.components.BigButton
import com.sleepclimate.app.ui.components.Caption
import com.sleepclimate.app.ui.components.LineIcon
import com.sleepclimate.app.ui.components.Panel
import com.sleepclimate.app.ui.components.StatTile
import com.sleepclimate.app.ui.components.TempChart
import com.sleepclimate.app.ui.components.VehicleStatusBar
import com.sleepclimate.app.ui.components.alertColor
import com.sleepclimate.app.ui.components.alertIcon
import com.sleepclimate.app.ui.components.alertLabel
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.ui.components.isCompactWidth
import com.sleepclimate.app.ui.components.screenPadding
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** 휴식 리포트 — 밤사이 온도가 어떻게 관리됐는지 요약 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(viewModel: SessionViewModel, onHome: () -> Unit) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val alerts by viewModel.vehicleAlerts.collectAsStateWithLifecycle()
    val events by viewModel.controlEvents.collectAsStateWithLifecycle()
    val samples by viewModel.tempSamples.collectAsStateWithLifecycle()
    val vehicle by viewModel.vehicleState.collectAsStateWithLifecycle()
    val simulation by viewModel.simulationEnabled.collectAsStateWithLifecycle()
    val endedAt = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    BackHandler(onBack = onHome)

    val current = session ?: return
    val elapsed = endedAt - current.startTime
    val avgTemp = samples.takeIf { it.isNotEmpty() }?.map { it.tempCelsius }?.average()?.toFloat()
    val inRangePercent = samples.takeIf { it.isNotEmpty() }
        ?.let { list -> (list.count { it.tempCelsius in current.tempRangeLow..current.tempRangeHigh } * 100f / list.size).roundToInt() }

    Column(Modifier.fillMaxSize().background(SC.Background).screenPadding()) {
        VehicleStatusBar(vehicle, stringResource(R.string.result_title), simulation, onHome)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Caption(stringResource(R.string.session_time_range_format, timeFormat.format(Date(current.startTime)), timeFormat.format(Date(endedAt))))
            val stats = listOf(
                Triple(
                    stringResource(R.string.stat_duration),
                    stringResource(
                        R.string.duration_value_format,
                        TimeUnit.MILLISECONDS.toHours(elapsed).toInt(),
                        (TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60).toInt()
                    ),
                    SC.TextPrimary
                ),
                Triple(
                    stringResource(R.string.stat_avg_temp),
                    avgTemp?.let { "%.1f°".format(it) } ?: stringResource(R.string.no_value),
                    temperatureColor(avgTemp)
                ),
                Triple(
                    stringResource(R.string.stat_in_range),
                    inRangePercent?.let { stringResource(R.string.percent_value_format, it) } ?: stringResource(R.string.no_value),
                    if ((inRangePercent ?: 0) >= 80) SC.Ok else SC.Warn
                ),
                Triple(
                    stringResource(R.string.stat_auto_control),
                    if (events.isEmpty() && vehicle.supportLevel != VehicleSupportLevel.FULL) stringResource(R.string.no_value)
                    else stringResource(R.string.auto_control_value_format, events.size, events.count { it.status == ControlStatus.CONFIRMED }),
                    SC.TextPrimary
                )
            )
            stats.chunked(if (isCompactWidth()) 2 else 4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { (label, value, color) -> StatTile(label, value, Modifier.weight(1f), valueColor = color) }
                }
            }

            Panel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Caption(stringResource(R.string.chart_title), Modifier.weight(1f))
                        Text(stringResource(R.string.chart_legend), style = MaterialTheme.typography.bodyMedium, color = SC.TextTertiary)
                    }
                    TempChart(samples, current.tempRangeLow, current.tempRangeHigh, Modifier.fillMaxWidth().height(280.dp))
                    if (vehicle.supportLevel != VehicleSupportLevel.FULL && events.isEmpty()) {
                        Text(stringResource(R.string.command_result_read_only), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
                    }
                }
            }

            Panel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Caption(stringResource(R.string.alert_summary_label))
                    if (alerts.isEmpty()) {
                        Text(stringResource(R.string.alert_summary_empty), style = MaterialTheme.typography.bodyLarge, color = SC.TextSecondary)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            alerts.groupBy { it.type }.forEach { (type, list) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LineIcon(alertIcon(type), tint = alertColor(type), size = 22.dp)
                                    Text(
                                        stringResource(R.string.alert_summary_item_format, stringResource(alertLabel(type)), list.size),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BigButton(stringResource(R.string.btn_back_home), onHome, Modifier.fillMaxWidth())
        }
    }
}
