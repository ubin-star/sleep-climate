package com.sleepclimate.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.sleepclimate.app.ui.components.Panel
import com.sleepclimate.app.ui.components.StatTile
import com.sleepclimate.app.ui.components.StatusPill
import com.sleepclimate.app.ui.components.TempChart
import com.sleepclimate.app.ui.components.alertColor
import com.sleepclimate.app.ui.components.alertIcon
import com.sleepclimate.app.ui.components.alertLabel
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.ui.theme.SC
import com.sleepclimate.app.ui.theme.temperatureColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** 세션 결과 — 밤사이 온도가 어떻게 관리됐는지 요약 */
@Composable
fun ResultScreen(viewModel: SessionViewModel, onHome: () -> Unit) {
    val session by viewModel.currentSession.collectAsStateWithLifecycle()
    val alerts by viewModel.vehicleAlerts.collectAsStateWithLifecycle()
    val events by viewModel.controlEvents.collectAsStateWithLifecycle()
    val samples by viewModel.tempSamples.collectAsStateWithLifecycle()
    val support = viewModel.vehicleState.value.supportLevel
    val endedAt = remember { System.currentTimeMillis() }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREAN) }
    BackHandler(onBack = onHome)

    val current = session ?: return
    val elapsed = endedAt - current.startTime
    val avgTemp = samples.takeIf { it.isNotEmpty() }?.map { it.tempCelsius }?.average()?.toFloat()
    val inRangePercent = samples.takeIf { it.isNotEmpty() }
        ?.let { list -> (list.count { it.tempCelsius in current.tempRangeLow..current.tempRangeHigh } * 100f / list.size).roundToInt() }

    Column(
        Modifier.fillMaxSize().background(SC.Background).padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.session_time_range_format, timeFormat.format(Date(current.startTime)), timeFormat.format(Date(endedAt))),
                    style = MaterialTheme.typography.titleMedium, color = SC.TextSecondary
                )
            }
            BigButton(stringResource(R.string.btn_back_home), onHome, Modifier.padding(start = 16.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatTile(
                stringResource(R.string.stat_duration),
                stringResource(
                    R.string.duration_value_format,
                    TimeUnit.MILLISECONDS.toHours(elapsed).toInt(),
                    (TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60).toInt()
                ),
                Modifier.weight(1f)
            )
            StatTile(
                stringResource(R.string.stat_avg_temp),
                avgTemp?.let { stringResource(R.string.temp_value_format, it) } ?: stringResource(R.string.no_value),
                Modifier.weight(1f),
                valueColor = temperatureColor(avgTemp)
            )
            StatTile(
                stringResource(R.string.stat_in_range),
                inRangePercent?.let { stringResource(R.string.percent_value_format, it) } ?: stringResource(R.string.no_value),
                Modifier.weight(1f),
                valueColor = if ((inRangePercent ?: 0) >= 80) SC.Ok else SC.Warn
            )
            StatTile(
                stringResource(R.string.stat_auto_control),
                if (events.isEmpty() && support != VehicleSupportLevel.FULL) stringResource(R.string.no_value)
                else stringResource(R.string.auto_control_value_format, events.size, events.count { it.status == ControlStatus.CONFIRMED }),
                Modifier.weight(1f),
                valueColor = SC.Accent
            )
        }

        Panel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.chart_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.chart_legend), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
                }
                TempChart(samples, current.tempRangeLow, current.tempRangeHigh, Modifier.fillMaxWidth().height(260.dp))
                if (support != VehicleSupportLevel.FULL && events.isEmpty()) {
                    Text(stringResource(R.string.command_result_read_only), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.alert_summary_label), style = MaterialTheme.typography.titleMedium, color = SC.TextSecondary)
            if (alerts.isEmpty()) {
                Text(stringResource(R.string.alert_summary_empty), style = MaterialTheme.typography.bodyLarge)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    alerts.groupBy { it.type }.forEach { (type, list) ->
                        StatusPill(
                            "${alertIcon(type)} " + stringResource(R.string.alert_summary_item_format, stringResource(alertLabel(type)), list.size),
                            alertColor(type)
                        )
                    }
                }
            }
        }
    }
}
