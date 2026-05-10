package dev.ktxtget.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MacroForegroundService : Service() {
    private val serviceJob: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(serviceJob + Dispatchers.Main)
    private lateinit var repository: MacroPreferencesRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        repository = MacroPreferencesRepository(applicationContext)
        NotificationHelper.ensureChannels(applicationContext)
        startForeground(
            NotificationHelper.NOTIFICATION_ID_RUNNING,
            NotificationHelper.buildRunningNotification(applicationContext),
        )
        scope.launch {
            repository.macroEnabled.collect { enabled: Boolean ->
                if (!enabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    companion object {
        fun start(context: android.content.Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MacroForegroundService::class.java),
            )
        }
    }
}
