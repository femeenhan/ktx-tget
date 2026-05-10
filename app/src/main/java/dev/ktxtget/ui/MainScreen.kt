package dev.ktxtget.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ktxtget.R
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.SeatClickPreference
import dev.ktxtget.domain.TrainExcludeNumbersParser
import dev.ktxtget.service.MacroForegroundService
import kotlinx.coroutines.launch

@Composable
fun MainScreen(repository: MacroPreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val macroEnabled: Boolean by repository.macroEnabled.collectAsStateWithLifecycle(initialValue = false)
    val settings by repository.macroSettings.collectAsStateWithLifecycle(
        initialValue = MacroSettings.DEFAULT,
    )
    val refreshMs: Long by repository.refreshIntervalMs.collectAsStateWithLifecycle(
        initialValue = MacroPreferencesRepository.MIN_REFRESH_MS,
    )
    var sliderValue by remember(refreshMs) { mutableFloatStateOf(refreshMs.toFloat()) }
    LaunchedEffect(refreshMs) {
        sliderValue = refreshMs.toFloat()
    }
    var excludedTrainsText by remember { mutableStateOf("") }
    LaunchedEffect(settings.excludedTrainNumbers) {
        excludedTrainsText = settings.excludedTrainNumbers.sorted().joinToString(", ")
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted) {
                scope.launch {
                    repository.setMacroEnabled(true)
                    MacroForegroundService.start(context)
                }
            }
        }
    val minMs: Float = MacroPreferencesRepository.MIN_REFRESH_MS.toFloat()
    val maxMs: Float = MacroPreferencesRepository.MAX_REFRESH_MS.toFloat()
    val step: Float = MacroPreferencesRepository.REFRESH_STEP_MS.toFloat()
    val stepCount: Int = ((maxMs - minMs) / step).toInt().coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.main_screen_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        RowSwitch(
            label = stringResource(R.string.macro_enabled_label),
            checked = macroEnabled,
            onCheckedChange = { checked: Boolean ->
                if (checked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        scope.launch {
                            repository.setMacroEnabled(true)
                            MacroForegroundService.start(context)
                        }
                    }
                } else {
                    scope.launch {
                        repository.setMacroEnabled(false)
                    }
                }
            },
        )
        Text(
            text = stringResource(R.string.refresh_interval_slider_label, sliderValue.toLong()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                scope.launch {
                    repository.setRefreshIntervalMs(sliderValue.toLong())
                }
            },
            valueRange = minMs..maxMs,
            steps = (stepCount - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.general_seat_filters_heading),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        RowSwitch(
            label = stringResource(R.string.exclude_freeseat_label),
            checked = settings.excludeFreeseatRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeFreeseatRows(checked)
                }
            },
        )
        RowSwitch(
            label = stringResource(R.string.exclude_general_standing_combo_label),
            checked = settings.excludeGeneralStandingComboRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeGeneralStandingComboRows(checked)
                }
            },
        )
        RowSwitch(
            label = stringResource(R.string.exclude_general_standing_only_label),
            checked = settings.excludeGeneralStandingOnlyRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeGeneralStandingOnlyRows(checked)
                }
            },
        )
        OutlinedTextField(
            value = excludedTrainsText,
            onValueChange = { excludedTrainsText = it },
            label = { Text(stringResource(R.string.excluded_trains_hint)) },
            supportingText = {
                Text(stringResource(R.string.excluded_trains_support))
            },
            placeholder = {
                Text(text = stringResource(R.string.excluded_trains_placeholder))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
        )
        RowSwitch(
            label = stringResource(R.string.auto_confirm_intermediate_stop_label),
            checked = settings.autoConfirmIntermediateStopDialog,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setAutoConfirmIntermediateStopDialog(checked)
                }
            },
        )
        Text(
            text = stringResource(R.string.auto_confirm_intermediate_stop_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = stringResource(R.string.seat_preference_label))
        SeatClickPreference.entries.forEach { opt: SeatClickPreference ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = settings.seatClickPreference == opt,
                        role = Role.RadioButton,
                        onClick = {
                            scope.launch {
                                repository.setSeatClickPreference(opt)
                            }
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = settings.seatClickPreference == opt,
                    onClick = null,
                )
                Text(
                    text = seatPreferenceLabel(opt),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        RowSwitch(
            label = stringResource(R.string.user_alerts_label),
            checked = settings.userAlertsEnabled,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setUserAlertsEnabled(checked)
                }
            },
        )
        Button(
            onClick = {
                scope.launch {
                    val trains: Set<String> =
                        TrainExcludeNumbersParser.parseCommaSeparated(excludedTrainsText)
                    repository.setExcludedTrainNumbers(trains)
                    excludedTrainsText = trains.sorted().joinToString(", ")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.apply_filters_button))
        }
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.open_accessibility_settings))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun seatPreferenceLabel(pref: SeatClickPreference): String {
    return when (pref) {
        SeatClickPreference.GENERAL_ONLY -> stringResource(R.string.seat_pref_general_only)
        SeatClickPreference.FIRST_CLASS_ONLY -> stringResource(R.string.seat_pref_first_only)
        SeatClickPreference.BOTH_PREFER_GENERAL -> stringResource(R.string.seat_pref_both_general)
    }
}
