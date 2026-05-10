package dev.ktxtget

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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository: MacroPreferencesRepository by lazy {
        MacroPreferencesRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KtxtgetTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KtxtgetApp(repository = repository)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            if (repository.readMacroEnabled()) {
                MacroForegroundService.start(this@MainActivity)
            }
        }
    }
}
