package com.sleepclimate.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.sleepclimate.app.R
import com.sleepclimate.app.domain.model.AlertType
import com.sleepclimate.app.domain.model.SessionAlert
import com.sleepclimate.app.ui.MainActivity

/** 세션 진행 표시(조용한 채널)와 깨워야 하는 경고(중요 채널) */
object NotificationHelper {

    const val SESSION_CHANNEL_ID = "sleep_climate_session"
    const val ALERT_CHANNEL_ID   = "sleep_climate_alert"

    private const val NOTIFICATION_ID_TEMP       = 2001
    private const val NOTIFICATION_ID_ENERGY     = 2002
    private const val NOTIFICATION_ID_CONNECTION = 2003

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val sessionChannel = NotificationChannel(
            SESSION_CHANNEL_ID,
            context.getString(R.string.notification_channel_session),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_session_desc)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.notification_channel_alert),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alert_desc)
        }

        nm.createNotificationChannel(sessionChannel)
        nm.createNotificationChannel(alertChannel)
    }

    /**
     * 온도 이탈 알림 (TEMP_OUT_OF_RANGE, COMMAND_REJECTED, COMMAND_FAILED, NOT_PARKED)
     */
    fun sendTempAlert(context: Context, alert: SessionAlert) {
        send(context, NOTIFICATION_ID_TEMP, R.string.notification_title_temp, alert)
    }

    /**
     * 배터리·연료 부족 알림 (BATTERY_LOW, FUEL_LOW)
     */
    fun sendEnergyAlert(context: Context, alert: SessionAlert) {
        val title = if (alert.type == AlertType.BATTERY_LOW) {
            R.string.notification_title_battery
        } else {
            R.string.notification_title_fuel
        }
        send(context, NOTIFICATION_ID_ENERGY, title, alert)
    }

    /**
     * 차량 연결 이상 알림 (VEHICLE_DISCONNECTED, DATA_STALE)
     */
    fun sendConnectionAlert(context: Context, alert: SessionAlert) {
        send(context, NOTIFICATION_ID_CONNECTION, R.string.notification_title_connection, alert)
    }

    private fun send(context: Context, id: Int, @StringRes title: Int, alert: SessionAlert) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(context.getString(title))
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentIntent(buildMainActivityIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(id, notification)
    }

    private fun buildMainActivityIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
