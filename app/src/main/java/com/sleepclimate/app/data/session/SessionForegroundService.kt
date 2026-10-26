package com.sleepclimate.app.data.session

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.sleepclimate.app.R
import com.sleepclimate.app.SleepClimateApp
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.ui.MainActivity

/**
 * 휴식 세션 동안 앱이 백그라운드로 가도 감시가 계속되도록 유지
 * (다른 앱으로 화면을 전환하거나 분할 화면에서 가려져도 공조 관리가 끊기지 않게)
 */
class SessionForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.sleepclimate.ACTION_STOP_SESSION"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, SessionForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionForegroundService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            (application as SleepClimateApp).sessionRepository.endSession()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        NotificationHelper.createChannels(this)
        val notification = NotificationCompat.Builder(this, NotificationHelper.SESSION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_session_title))
            .setContentText(getString(R.string.notification_session_text))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0, getString(R.string.notification_action_end_session),
                PendingIntent.getService(
                    this, 0, Intent(this, SessionForegroundService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setOngoing(true)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
