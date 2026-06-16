package dev.ktxtget.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ktxtget.R
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.SeatClickPreference
import dev.ktxtget.domain.TicketAlertMode
import dev.ktxtget.domain.TrainExcludeNumbersParser
import dev.ktxtget.service.MacroForegroundService
import dev.ktxtget.util.DeviceLicense
import dev.ktxtget.util.DeviceRegistrationState
import kotlinx.coroutines.launch

@Composable
fun MainScreen(repository: MacroPreferencesRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    var isExcludedTrainsFieldFocused by remember { mutableStateOf(false) }
    fun dismissKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    LaunchedEffect(Unit) {
        dismissKeyboard()
    }
    LaunchedEffect(settings.excludedTrainNumbers) {
        if (!isExcludedTrainsFieldFocused) {
            excludedTrainsText = settings.excludedTrainNumbers.sorted().joinToString(", ")
        }
    }
    BackHandler(enabled = isExcludedTrainsFieldFocused) {
        dismissKeyboard()
    }
    val registrationState: DeviceRegistrationState =
        remember { DeviceLicense.getRegistrationState(context) }
    val isMacroAllowed: Boolean = remember(registrationState) {
        DeviceLicense.isLicensed(context)
    }
    val deviceId: String = remember { DeviceLicense.readDeviceId(context) }
    LaunchedEffect(isMacroAllowed, macroEnabled) {
        if (!isMacroAllowed && macroEnabled) {
            repository.setMacroEnabled(false)
        }
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted && isMacroAllowed) {
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
    val isMacroRunning: Boolean = macroEnabled && isMacroAllowed
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(focusManager, keyboardController) {
                detectTapGestures(onTap = { dismissKeyboard() })
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HomeHeader()
        if (registrationState == DeviceRegistrationState.PREVIEW ||
            registrationState == DeviceRegistrationState.WRONG_DEVICE
        ) {
            DeviceRegistrationCard(
                registrationState = registrationState,
                deviceId = deviceId,
                onCopyDeviceId = {
                    copyDeviceIdToClipboard(context, deviceId)
                },
            )
        }
        MacroStatusCard(
            isRunning = isMacroRunning,
            isMacroAllowed = isMacroAllowed,
            onToggle = { checked: Boolean ->
                if (!isMacroAllowed) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.device_registration_macro_blocked),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@MacroStatusCard
                }
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
        QuickSetupCard(
            onOpenAccessibilitySettings = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
        )
        TrainListScopeTipCard()
        SettingsSectionCard(
            title = stringResource(R.string.section_refresh),
            description = stringResource(R.string.section_refresh_desc),
            icon = Icons.Default.Schedule,
        ) {
            Text(
                text = stringResource(
                    R.string.refresh_interval_human_label,
                    sliderValue / 1000f,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.refresh_interval_range_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }
        SettingsSectionCard(
            title = stringResource(R.string.section_seat_filters),
            description = stringResource(R.string.section_seat_filters_desc),
            icon = Icons.Default.FilterList,
        ) {
            SettingSwitchRow(
                label = stringResource(R.string.exclude_freeseat_label),
                checked = settings.excludeFreeseatRows,
                onCheckedChange = { checked: Boolean ->
                    scope.launch {
                        repository.setExcludeFreeseatRows(checked)
                    }
                },
            )
            SectionDivider()
            SettingSwitchRow(
                label = stringResource(R.string.exclude_general_standing_combo_label),
                checked = settings.excludeGeneralStandingComboRows,
                onCheckedChange = { checked: Boolean ->
                    scope.launch {
                        repository.setExcludeGeneralStandingComboRows(checked)
                    }
                },
            )
            SectionDivider()
            SettingSwitchRow(
                label = stringResource(R.string.exclude_general_standing_only_label),
                checked = settings.excludeGeneralStandingOnlyRows,
                onCheckedChange = { checked: Boolean ->
                    scope.launch {
                        repository.setExcludeGeneralStandingOnlyRows(checked)
                    }
                },
            )
        }
        SettingsSectionCard(
            title = stringResource(R.string.section_train_exclude),
            description = stringResource(R.string.section_train_exclude_desc),
            icon = Icons.Default.Train,
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        isExcludedTrainsFieldFocused = focusState.isFocused
                    },
                singleLine = false,
                minLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { dismissKeyboard() }),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    dismissKeyboard()
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
            SectionDivider()
            SettingSwitchRow(
                label = stringResource(R.string.auto_confirm_intermediate_stop_label),
                supportingText = stringResource(R.string.auto_confirm_intermediate_stop_support),
                checked = settings.autoConfirmIntermediateStopDialog,
                onCheckedChange = { checked: Boolean ->
                    scope.launch {
                        repository.setAutoConfirmIntermediateStopDialog(checked)
                    }
                },
            )
        }
        SettingsSectionCard(
            title = stringResource(R.string.section_booking_options),
            description = stringResource(R.string.section_booking_options_desc),
            icon = Icons.Default.Notifications,
        ) {
            Text(
                text = stringResource(R.string.seat_preference_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            SeatClickPreference.entries.forEach { opt: SeatClickPreference ->
                SettingRadioRow(
                    label = seatPreferenceLabel(opt),
                    selected = settings.seatClickPreference == opt,
                    onClick = {
                        scope.launch {
                            repository.setSeatClickPreference(opt)
                        }
                    },
                )
            }
            SectionDivider()
            Text(
                text = stringResource(R.string.ticket_alert_mode_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            TicketAlertMode.entries.forEach { mode: TicketAlertMode ->
                SettingRadioRow(
                    label = ticketAlertModeLabel(mode),
                    selected = settings.ticketAlertMode == mode,
                    onClick = {
                        scope.launch {
                            repository.setTicketAlertMode(mode)
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ticket_alert_mode_support),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HomeHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.main_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.main_screen_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroStatusCard(
    isRunning: Boolean,
    isMacroAllowed: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val containerColor = if (isRunning) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isRunning) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRunning) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(
                            if (isRunning) {
                                R.string.macro_status_running
                            } else {
                                R.string.macro_status_stopped
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (isRunning) {
                                R.string.macro_status_running_desc
                            } else {
                                R.string.macro_status_stopped_desc
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                    )
                }
            }
            Switch(
                checked = isRunning,
                onCheckedChange = onToggle,
                enabled = isMacroAllowed,
            )
        }
    }
}

@Composable
private fun QuickSetupCard(onOpenAccessibilitySettings: () -> Unit) {
    SettingsSectionCard(
        title = stringResource(R.string.section_quick_setup),
        description = stringResource(R.string.section_quick_setup_desc),
        icon = Icons.Default.Accessibility,
    ) {
        Text(
            text = stringResource(R.string.quick_setup_steps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenAccessibilitySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Accessibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.open_accessibility_settings))
        }
    }
}

@Composable
private fun TrainListScopeTipCard() {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.train_list_scope_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.train_list_scope_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.train_list_scope_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun DeviceRegistrationCard(
    registrationState: DeviceRegistrationState,
    deviceId: String,
    onCopyDeviceId: () -> Unit,
) {
    val titleResId: Int = when (registrationState) {
        DeviceRegistrationState.PREVIEW -> R.string.device_registration_preview_title
        DeviceRegistrationState.WRONG_DEVICE -> R.string.device_registration_wrong_device_title
        DeviceRegistrationState.LICENSED,
        DeviceRegistrationState.DEV_OPEN,
        -> R.string.device_registration_preview_title
    }
    val bodyResId: Int = when (registrationState) {
        DeviceRegistrationState.PREVIEW -> R.string.device_registration_preview_body
        DeviceRegistrationState.WRONG_DEVICE -> R.string.device_registration_wrong_device_body
        DeviceRegistrationState.LICENSED,
        DeviceRegistrationState.DEV_OPEN,
        -> R.string.device_registration_preview_body
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(titleResId),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(bodyResId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.device_registration_device_id_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = deviceId.ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCopyDeviceId,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.device_registration_copy_button))
            }
        }
    }
}

private fun copyDeviceIdToClipboard(context: Context, deviceId: String) {
    if (deviceId.isEmpty()) {
        return
    }
    val clipboard: ClipboardManager? =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    clipboard?.setPrimaryClip(ClipData.newPlainText("device_id", deviceId))
    Toast.makeText(
        context,
        context.getString(R.string.device_registration_copied_toast),
        Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun ticketAlertModeLabel(mode: TicketAlertMode): String {
    return when (mode) {
        TicketAlertMode.OFF -> stringResource(R.string.ticket_alert_mode_off)
        TicketAlertMode.NORMAL -> stringResource(R.string.ticket_alert_mode_normal)
        TicketAlertMode.STRONG -> stringResource(R.string.ticket_alert_mode_strong)
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
