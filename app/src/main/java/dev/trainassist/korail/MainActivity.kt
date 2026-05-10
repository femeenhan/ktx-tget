package dev.trainassist.korail

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import dev.trainassist.korail.databinding.ActivityMainBinding
import dev.trainassist.korail.service.TrainAssistAccessibilityService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.switchAutomation.setOnCheckedChangeListener(::onAutomationToggled)
        binding.textFooter.text =
            "Target app package: com.korail.talk (KorailTalk). For personal use on your own phone."
    }

    override fun onResume() {
        super.onResume()
        syncSwitchFromPreferences()
        refreshUiState()
    }

    private fun syncSwitchFromPreferences() {
        val armed: Boolean = AutomationPreferences.isAutomationArmed(this)
        binding.switchAutomation.setOnCheckedChangeListener(null)
        binding.switchAutomation.isChecked = armed
        binding.switchAutomation.setOnCheckedChangeListener(::onAutomationToggled)
    }

    private fun onAutomationToggled(button: CompoundButton, isChecked: Boolean) {
        if (isChecked && !isTrainAssistAccessibilityEnabled()) {
            binding.switchAutomation.setOnCheckedChangeListener(null)
            binding.switchAutomation.isChecked = false
            binding.switchAutomation.setOnCheckedChangeListener(::onAutomationToggled)
            Snackbar.make(binding.root, R.string.accessibility_not_enabled, Snackbar.LENGTH_LONG).show()
            refreshUiState()
            return
        }
        AutomationPreferences.setAutomationArmed(this, isChecked)
        refreshUiState()
    }

    private fun refreshUiState() {
        val accessibilityEnabled: Boolean = isTrainAssistAccessibilityEnabled()
        binding.textAccessibilityStatus.text =
            if (accessibilityEnabled) {
                "Accessibility service enabled"
            } else {
                getString(R.string.accessibility_not_enabled)
            }
        binding.switchAutomation.isEnabled = accessibilityEnabled || AutomationPreferences.isAutomationArmed(this)
    }

    private fun isTrainAssistAccessibilityEnabled(): Boolean {
        val componentName: ComponentName = ComponentName(this, TrainAssistAccessibilityService::class.java)
        val enabledServices: String =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter: TextUtils.SimpleStringSplitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val serviceId: String = splitter.next()
            if (componentName.flattenToString().equals(serviceId, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
