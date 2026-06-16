package dev.ktxtget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.service.MacroForegroundService
import dev.ktxtget.ui.KtxtgetApp
import dev.ktxtget.ui.theme.KtxtgetTheme
import dev.ktxtget.util.DeviceLicense
import dev.ktxtget.util.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository: MacroPreferencesRepository by lazy {
        MacroPreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stopTicketAlertIfRequested()
        enableEdgeToEdge()
        setContent {
            KtxtgetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KtxtgetApp(repository = repository)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        stopTicketAlertIfRequested()
    }

    override fun onStart() {
        super.onStart()
        NotificationHelper.stopUserAlert(this)
        lifecycleScope.launch {
            if (!DeviceLicense.isLicensed(this@MainActivity)) {
                if (repository.readMacroEnabled()) {
                    repository.setMacroEnabled(false)
                }
                return@launch
            }
            if (repository.readMacroEnabled()) {
                MacroForegroundService.start(this@MainActivity)
            }
        }
    }

    private fun stopTicketAlertIfRequested() {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_STOP_TICKET_ALERT, false) == true) {
            NotificationHelper.stopUserAlert(this)
        }
    }
}
