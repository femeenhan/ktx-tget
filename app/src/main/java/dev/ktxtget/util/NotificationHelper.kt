package dev.ktxtget.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.ktxtget.R
import android.os.Vibrator

/**
 * Registers notification channels and shows foreground / ticket alerts.
 */
object NotificationHelper {
    const val NOTIFICATION_ID_RUNNING: Int = 1001
    const val NOTIFICATION_ID_TICKET_ALERT: Int = 1002
    private const val CHANNEL_MACRO_RUNNING: String = "macro_running"
    private const val CHANNEL_TICKET_ALERT: String = "ticket_alert"
    private const val TAG: String = "KtxNotify"
    private var alertRingtone: Ringtone? = null

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val nm: NotificationManager =
            context.getSystemService(NotificationManager::class.java) ?: return
        val running: NotificationChannel = NotificationChannel(
            CHANNEL_MACRO_RUNNING,
            context.getString(R.string.notification_channel_macro_running_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_macro_running_desc)
            setShowBadge(false)
        }
        val alert: NotificationChannel = NotificationChannel(
            CHANNEL_TICKET_ALERT,
            context.getString(R.string.notification_channel_ticket_alert_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_ticket_alert_desc)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(running)
        nm.createNotificationChannel(alert)
    }

    fun buildRunningNotification(context: Context): Notification {
        ensureChannels(context)
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_MACRO_RUNNING)
                .setContentTitle(context.getString(R.string.notification_macro_running_title))
                .setContentText(context.getString(R.string.notification_macro_running_text))
                .setSmallIcon(context.applicationInfo.icon)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        return builder.build()
    }

    fun triggerUserAlert(context: Context, alertsEnabled: Boolean) {
        if (!alertsEnabled) {
            return
        }
        ensureChannels(context)
        val compat: NotificationManagerCompat =
            NotificationManagerCompat.from(context.applicationContext)
        if (!compat.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled; skipping user alert")
            return
        }
        val alertNotification: Notification =
            NotificationCompat.Builder(context.applicationContext, CHANNEL_TICKET_ALERT)
                .setContentTitle(context.getString(R.string.notification_ticket_alert_title))
                .setContentText(context.getString(R.string.notification_ticket_alert_text))
                .setSmallIcon(context.applicationInfo.icon)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build()
        try {
            compat.notify(NOTIFICATION_ID_TICKET_ALERT, alertNotification)
        } catch (err: SecurityException) {
            Log.e(TAG, "notify denied", err)
        }
        playAlarmSound(context.applicationContext)
        vibrateAlert(context.applicationContext)
    }

    private fun vibrateAlert(context: Context) {
        val vibrator: Vibrator? = ContextCompat.getSystemService(context, Vibrator::class.java)
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }
        val pattern: LongArray = longArrayOf(0L, 500L, 200L, 500L, 200L, 500L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun playAlarmSound(context: Context) {
        try {
            alertRingtone?.stop()
        } catch (_: Exception) {
        }
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone: Ringtone? = RingtoneManager.getRingtone(context, alarmUri)
        alertRingtone = ringtone
        try {
            ringtone?.play()
        } catch (err: Exception) {
            Log.e(TAG, "alarm play failed", err)
        }
    }
}
