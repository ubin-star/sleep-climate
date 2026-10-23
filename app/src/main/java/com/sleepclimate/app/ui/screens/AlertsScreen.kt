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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepclimate.app.R
import com.sleepclimate.app.ui.components.BigOutlinedButton
import com.sleepclimate.app.ui.components.Panel
import com.sleepclimate.app.ui.components.alertColor
import com.sleepclimate.app.ui.components.alertIcon
import com.sleepclimate.app.ui.components.alertLabel
import com.sleepclimate.app.ui.session.SessionViewModel
import com.sleepclimate.app.ui.theme.SC
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 알림 내역 — 최신 알림이 위로 */
@Composable
fun AlertsScreen(viewModel: SessionViewModel, onBack: () -> Unit) {
    val alerts by viewModel.vehicleAlerts.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.KOREAN) }

    Column(Modifier.fillMaxSize().background(SC.Background).padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigOutlinedButton(stringResource(R.string.btn_back), onBack)
            Spacer(Modifier.padding(horizontal = 12.dp))
            Text(stringResource(R.string.alert_title), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(24.dp))
        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_alerts), style = MaterialTheme.typography.titleLarge, color = SC.TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(alerts.reversed(), key = { it.id }) { alert ->
                    Panel(Modifier.fillMaxWidth(), borderColor = alertColor(alert.type).copy(alpha = 0.5f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(alertIcon(alert.type), fontSize = 36.sp)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(alertLabel(alert.type)), style = MaterialTheme.typography.titleMedium, color = alertColor(alert.type))
                                Text(alert.message, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(timeFormat.format(Date(alert.timestampMs)), style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
