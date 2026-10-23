package com.sleepclimate.app.data.session

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sleepclimate.app.R
import com.sleepclimate.app.data.notification.NotificationHelper
import com.sleepclimate.app.ui.MainActivity

/**
 * 세션 진행 중 앱이 백그라운드로 전환돼도 세션을 유지하는 포그라운드 서비스
 *
 * NOTE: Pleos Connect 차량 앱 정책에 따라 백그라운드 실행 가능 여부가 달라질 수 있음
 * 실제 SDK 확인 후 제약이 있으면 세션 제한 및 종료 알림으로 대체
 */
class SessionForegroundService : Service() {

    companion object {
        // 세션 채널 ID — NotificationHelper에서 관리
        const val CHANNEL_ID      = NotificationHelper.SESSION_CHANNEL_ID
        // 경고 채널 ID — 고우선순위 알림용
        const val ALERT_CHANNEL_ID = NotificationHelper.ALERT_CHANNEL_ID
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.sleepclimate.ACTION_STOP_SESSION"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // 채널은 Application.onCreate()에서 이미 생성됨
        // 여기서는 중복 생성을 방지하기 위해 createChannels()를 재호출해도 안전함
        NotificationHelper.createChannels(this)

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, SessionForegroundService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_session_title))
            .setContentText(getString(R.string.notification_session_text))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_action_end_session), stopIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
