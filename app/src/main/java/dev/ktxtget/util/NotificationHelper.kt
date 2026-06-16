package dev.ktxtget.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.ktxtget.MainActivity
import dev.ktxtget.R
import dev.ktxtget.domain.TicketAlertMode

/**
 * Registers notification channels and shows foreground / ticket alerts.
 */
object NotificationHelper {
    const val NOTIFICATION_ID_RUNNING: Int = 1001
    const val NOTIFICATION_ID_TICKET_ALERT: Int = 1002
    const val EXTRA_STOP_TICKET_ALERT: String = "dev.ktxtget.extra.STOP_TICKET_ALERT"
    private const val CHANNEL_MACRO_RUNNING: String = "macro_running"
    private const val CHANNEL_TICKET_ALERT_STRONG: String = "ticket_alert_v2"
    private const val CHANNEL_TICKET_ALERT_NORMAL: String = "ticket_alert_normal"
    private const val LEGACY_CHANNEL_TICKET_ALERT: String = "ticket_alert"
    private const val TAG: String = "KtxNotify"
    private const val ALARM_MAX_DURATION_MS: Long = 90_000L
    private val STRONG_VIBRATION_PATTERN: LongArray = longArrayOf(
        0L,
        1_000L,
        300L,
        1_000L,
        300L,
        1_000L,
        300L,
        1_000L,
        500L,
        1_500L,
        300L,
        1_500L,
    )
    private val NORMAL_VIBRATION_PATTERN: LongArray = longArrayOf(
        0L,
        500L,
        200L,
        500L,
        200L,
        500L,
    )
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var alertPlayer: MediaPlayer? = null
    private var alertRingtone: Ringtone? = null
    private var alertWakeLock: PowerManager.WakeLock? = null
    private var stopAlarmRunnable: Runnable? = null

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
        val notificationUri: Uri = resolveNotificationSoundUri(context)
        val notificationAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val normalAlert: NotificationChannel = NotificationChannel(
            CHANNEL_TICKET_ALERT_NORMAL,
            context.getString(R.string.notification_channel_ticket_alert_normal_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_ticket_alert_normal_desc)
            setSound(notificationUri, notificationAudioAttributes)
            enableVibration(true)
            vibrationPattern = NORMAL_VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val alarmUri: Uri = resolveAlarmSoundUri(context)
        val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val strongAlert: NotificationChannel = NotificationChannel(
            CHANNEL_TICKET_ALERT_STRONG,
            context.getString(R.string.notification_channel_ticket_alert_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_ticket_alert_desc)
            setSound(alarmUri, alarmAudioAttributes)
            enableVibration(true)
            vibrationPattern = STRONG_VIBRATION_PATTERN
            enableLights(true)
            lightColor = Color.RED
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        nm.createNotificationChannel(running)
        nm.createNotificationChannel(normalAlert)
        nm.createNotificationChannel(strongAlert)
        nm.deleteNotificationChannel(LEGACY_CHANNEL_TICKET_ALERT)
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

    fun triggerUserAlert(context: Context, mode: TicketAlertMode) {
        when (mode) {
            TicketAlertMode.OFF -> return
            TicketAlertMode.NORMAL -> triggerNormalAlert(context)
            TicketAlertMode.STRONG -> triggerStrongAlert(context)
        }
    }

    fun stopUserAlert(context: Context) {
        stopAlarmSound()
        stopRingtone()
        releaseAlertWakeLock()
        cancelVibration(context.applicationContext)
        stopAlarmRunnable?.let { runnable: Runnable ->
            handler.removeCallbacks(runnable)
        }
        stopAlarmRunnable = null
    }

    private fun triggerNormalAlert(context: Context) {
        val appContext: Context = context.applicationContext
        ensureChannels(appContext)
        val compat: NotificationManagerCompat = NotificationManagerCompat.from(appContext)
        if (!compat.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled; skipping user alert")
            return
        }
        playNotificationSound(appContext)
        vibrateOnce(appContext, NORMAL_VIBRATION_PATTERN)
        postTicketNotification(
            context = appContext,
            channelId = CHANNEL_TICKET_ALERT_NORMAL,
            soundUri = resolveNotificationSoundUri(appContext),
            vibrationPattern = NORMAL_VIBRATION_PATTERN,
            useFullScreenIntent = false,
        )
    }

    private fun triggerStrongAlert(context: Context) {
        val appContext: Context = context.applicationContext
        ensureChannels(appContext)
        val compat: NotificationManagerCompat = NotificationManagerCompat.from(appContext)
        if (!compat.areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled; skipping user alert")
            return
        }
        acquireAlertWakeLock(appContext)
        playLoopingAlarmSound(appContext)
        vibrateRepeating(appContext)
        postTicketNotification(
            context = appContext,
            channelId = CHANNEL_TICKET_ALERT_STRONG,
            soundUri = resolveAlarmSoundUri(appContext),
            vibrationPattern = STRONG_VIBRATION_PATTERN,
            useFullScreenIntent = true,
        )
    }

    private fun postTicketNotification(
        context: Context,
        channelId: String,
        soundUri: Uri,
        vibrationPattern: LongArray,
        useFullScreenIntent: Boolean,
    ) {
        val compat: NotificationManagerCompat = NotificationManagerCompat.from(context)
        val launchIntent: Intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_STOP_TICKET_ALERT, true)
        }
        val pendingFlags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, launchIntent, pendingFlags)
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, channelId)
                .setContentTitle(context.getString(R.string.notification_ticket_alert_title))
                .setContentText(context.getString(R.string.notification_ticket_alert_text))
                .setSmallIcon(context.applicationInfo.icon)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(contentIntent)
                .setSound(soundUri)
                .setVibrate(vibrationPattern)
                .setDefaults(0)
        if (useFullScreenIntent) {
            val fullScreenIntent: PendingIntent =
                PendingIntent.getActivity(context, 1, launchIntent, pendingFlags)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                builder.setFullScreenIntent(fullScreenIntent, true)
            } else {
                val nm: NotificationManager? =
                    context.getSystemService(NotificationManager::class.java)
                if (nm?.canUseFullScreenIntent() == true) {
                    builder.setFullScreenIntent(fullScreenIntent, true)
                }
            }
        }
        try {
            compat.notify(NOTIFICATION_ID_TICKET_ALERT, builder.build())
        } catch (err: SecurityException) {
            Log.e(TAG, "notify denied", err)
        }
    }

    private fun acquireAlertWakeLock(context: Context) {
        releaseAlertWakeLock()
        val powerManager: PowerManager? =
            ContextCompat.getSystemService(context, PowerManager::class.java)
        if (powerManager == null) {
            return
        }
        alertWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ktxtget:TicketAlert",
        ).apply {
            acquire(ALARM_MAX_DURATION_MS)
        }
    }

    private fun releaseAlertWakeLock() {
        alertWakeLock?.let { wakeLock: PowerManager.WakeLock ->
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
        alertWakeLock = null
    }

    private fun playLoopingAlarmSound(context: Context) {
        stopAlarmSound()
        stopRingtone()
        val alarmUri: Uri = resolveAlarmSoundUri(context)
        try {
            val player: MediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            alertPlayer = player
            val runnable: Runnable = Runnable {
                stopAlarmSound()
                releaseAlertWakeLock()
            }
            stopAlarmRunnable = runnable
            handler.postDelayed(runnable, ALARM_MAX_DURATION_MS)
        } catch (err: Exception) {
            Log.e(TAG, "looping alarm play failed", err)
        }
    }

    private fun playNotificationSound(context: Context) {
        stopAlarmSound()
        stopRingtone()
        val notificationUri: Uri = resolveNotificationSoundUri(context)
        try {
            val ringtone: Ringtone? = RingtoneManager.getRingtone(context, notificationUri)
            alertRingtone = ringtone
            ringtone?.play()
        } catch (err: Exception) {
            Log.e(TAG, "notification sound play failed", err)
        }
    }

    private fun stopAlarmSound() {
        alertPlayer?.let { player: MediaPlayer ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {
            } finally {
                player.release()
            }
        }
        alertPlayer = null
    }

    private fun stopRingtone() {
        try {
            alertRingtone?.stop()
        } catch (_: Exception) {
        }
        alertRingtone = null
    }

    private fun vibrateOnce(context: Context, pattern: LongArray) {
        val vibrator: Vibrator? = ContextCompat.getSystemService(context, Vibrator::class.java)
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun vibrateRepeating(context: Context) {
        val vibrator: Vibrator? = ContextCompat.getSystemService(context, Vibrator::class.java)
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(STRONG_VIBRATION_PATTERN, 0),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(STRONG_VIBRATION_PATTERN, 0)
        }
    }

    private fun cancelVibration(context: Context) {
        val vibrator: Vibrator? = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.cancel()
    }

    private fun resolveAlarmSoundUri(context: Context): Uri {
        val alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri != null) {
            return alarmUri
        }
        return resolveNotificationSoundUri(context)
    }

    private fun resolveNotificationSoundUri(context: Context): Uri {
        val notificationUri: Uri? =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (notificationUri != null) {
            return notificationUri
        }
        val ringtoneUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (ringtoneUri != null) {
            return ringtoneUri
        }
        return Settings.System.DEFAULT_NOTIFICATION_URI
    }
}
