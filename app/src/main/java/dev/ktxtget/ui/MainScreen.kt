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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ktxtget.R
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.SeatClickPreference
import dev.ktxtget.domain.TicketAlertMode
import dev.ktxtget.domain.TrainExcludeNumbersParser
import dev.ktxtget.runtime.MacroLogLine
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.service.MacroForegroundService
import dev.ktxtget.ui.theme.DisplayFamily
import dev.ktxtget.ui.theme.Ink
import dev.ktxtget.ui.theme.InkDisabled
import dev.ktxtget.ui.theme.InkMuted
import dev.ktxtget.ui.theme.InkSecondary
import dev.ktxtget.ui.theme.MonoFamily
import dev.ktxtget.ui.theme.Sage
import dev.ktxtget.ui.theme.TrackOnInk
import dev.ktxtget.BuildConfig
import dev.ktxtget.license.LicenseDecision
import dev.ktxtget.license.LicenseManager
import dev.ktxtget.license.AppConfig
import dev.ktxtget.license.LicenseSnapshot
import dev.ktxtget.license.LicenseStatus
import dev.ktxtget.license.VersionInfo
import dev.ktxtget.util.DeviceLicense
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

private enum class MacroState { LOCKED, IDLE, RUNNING }

private const val RECENT_LOG_COUNT: Int = 4

@Composable
fun MainScreen(repository: MacroPreferencesRepository, onOpenLog: () -> Unit) {
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
    fun dismissKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val licenseSnapshot: LicenseSnapshot? by LicenseManager.snapshot.collectAsStateWithLifecycle()
    val isMacroAllowed: Boolean =
        BuildConfig.DEBUG || LicenseDecision.isLicensed(licenseSnapshot, System.currentTimeMillis())
    val deviceId: String = remember { DeviceLicense.readDeviceId(context) }
    // 카드는 DEBUG와 무관하게 서버 상태를 따른다. 매크로 허용(isMacroAllowed)과는 따로 본다.
    val licenseUi: LicenseStatus = licenseSnapshot?.status ?: LicenseStatus.NONE
    var config: AppConfig? by remember { mutableStateOf(null) }
    var versionInfo: VersionInfo? by remember { mutableStateOf(null) }
    var submitting: Boolean by remember { mutableStateOf(false) }
    var requestError: String? by remember { mutableStateOf(null) }
    var checking: Boolean by remember { mutableStateOf(false) }
    var showRenew: Boolean by remember { mutableStateOf(false) }
    val networkError: String = stringResource(R.string.license_network_error)
    fun loadConfig() {
        scope.launch { config = runCatching { LicenseManager.fetchConfig() }.getOrNull() }
    }
    LaunchedEffect(Unit) {
        config = runCatching { LicenseManager.fetchConfig() }.getOrNull()
    }
    LaunchedEffect(Unit) {
        versionInfo = try {
            LicenseManager.fetchVersion()
        } catch (_: IOException) {
            null
        }
    }
    LaunchedEffect(licenseUi == LicenseStatus.PENDING) {
        if (licenseUi != LicenseStatus.PENDING) return@LaunchedEffect
        while (isActive) {
            delay(60_000)
            LicenseManager.refresh(context)
        }
    }
    LaunchedEffect(licenseSnapshot?.renewalPending, licenseUi) {
        if (licenseSnapshot?.renewalPending == true || licenseUi != LicenseStatus.ACTIVE) showRenew = false
    }
    LaunchedEffect(licenseUi) { requestError = null }
    fun submitRequest(depositor: String, plan: String) {
        dismissKeyboard()
        scope.launch {
            submitting = true
            try {
                LicenseManager.sendRequest(context, depositor.trim(), plan)
                requestError = null
            } catch (e: IOException) {
                requestError = e.message ?: networkError
            } finally {
                submitting = false
            }
        }
    }
    fun checkNow() {
        scope.launch {
            checking = true
            try {
                LicenseManager.refresh(context)
            } finally {
                checking = false
            }
        }
    }
    LaunchedEffect(isMacroAllowed, macroEnabled) {
        if (!isMacroAllowed && macroEnabled) {
            repository.setMacroEnabled(false)
        }
    }
    fun startMacro() {
        scope.launch {
            repository.setMacroEnabled(true)
            MacroForegroundService.start(context)
        }
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            if (granted && isMacroAllowed) {
                startMacro()
            }
        }
    val state: MacroState = when {
        !isMacroAllowed -> MacroState.LOCKED
        macroEnabled -> MacroState.RUNNING
        else -> MacroState.IDLE
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerTapToDismiss { dismissKeyboard() }
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        val notice: String? = licenseSnapshot?.notice
        if (!notice.isNullOrBlank()) NoticeRow(text = notice)
        val update: VersionInfo? = versionInfo
        if (update != null && update.versionCode > BuildConfig.VERSION_CODE) {
            UpdateBanner(versionName = update.versionName, onDownload = { openUrl(context, update.url) })
        }
        HomeHeader(onOpenLog = onOpenLog)
        StatusPill(
            state = state,
            onStart = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    startMacro()
                }
            },
            onStop = {
                scope.launch {
                    repository.setMacroEnabled(false)
                }
            },
        )
        when (licenseUi) {
            LicenseStatus.NONE, LicenseStatus.EXPIRED, LicenseStatus.REVOKED -> {
                Spacer(modifier = Modifier.height(14.dp))
                ActivationRequestCard(
                    config = config,
                    status = licenseUi,
                    onSubmit = ::submitRequest,
                    submitting = submitting,
                    error = requestError,
                    onReloadConfig = ::loadConfig,
                    deviceId = deviceId,
                )
            }
            LicenseStatus.PENDING -> {
                Spacer(modifier = Modifier.height(14.dp))
                PendingCard(config = config, onCheck = ::checkNow, checking = checking)
            }
            LicenseStatus.ACTIVE -> {
                val snap: LicenseSnapshot? = licenseSnapshot
                if (snap != null && shouldShowExpiryLine(snap, System.currentTimeMillis())) {
                    Spacer(modifier = Modifier.height(6.dp))
                    ExpiryLine(
                        snapshot = snap,
                        nowWallMs = System.currentTimeMillis(),
                        onRenew = { showRenew = !showRenew },
                        renewOpen = showRenew,
                    )
                    if (showRenew) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ActivationRequestCard(
                            config = config,
                            status = LicenseStatus.EXPIRED,
                            onSubmit = ::submitRequest,
                            submitting = submitting,
                            error = requestError,
                            onReloadConfig = ::loadConfig,
                            deviceId = deviceId,
                            title = stringResource(R.string.license_request_title_renew),
                        )
                    }
                }
            }
        }
        when (state) {
            MacroState.LOCKED -> Unit
            MacroState.RUNNING -> RecentLog(onOpenLog = onOpenLog)
            MacroState.IDLE -> Unit
        }
        Spacer(modifier = Modifier.height(if (state == MacroState.RUNNING) 12.dp else 14.dp))
        RefreshIntervalCard(
            refreshMs = refreshMs,
            onChange = { value: Long ->
                scope.launch {
                    repository.setRefreshIntervalMs(value)
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceCard(
                title = stringResource(R.string.seat_pref_title),
                description = stringResource(R.string.seat_pref_desc),
                options = SeatClickPreference.entries,
                selected = settings.seatClickPreference,
                label = { pref: SeatClickPreference -> seatPreferenceLabel(pref) },
                note = { pref: SeatClickPreference ->
                    if (pref == SeatClickPreference.BOTH_PREFER_GENERAL) {
                        stringResource(R.string.seat_pref_both_note)
                    } else {
                        null
                    }
                },
                onSelect = { pref: SeatClickPreference ->
                    scope.launch {
                        repository.setSeatClickPreference(pref)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                title = stringResource(R.string.alert_title),
                description = stringResource(R.string.alert_desc),
                options = TicketAlertMode.entries,
                selected = settings.ticketAlertMode,
                label = { mode: TicketAlertMode -> ticketAlertModeLabel(mode) },
                note = { mode: TicketAlertMode ->
                    if (mode == settings.ticketAlertMode) ticketAlertModeNote(mode) else null
                },
                onSelect = { mode: TicketAlertMode ->
                    scope.launch {
                        repository.setTicketAlertMode(mode)
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(26.dp))
        SectionHeading(
            title = stringResource(R.string.seat_filter_title),
            description = stringResource(R.string.seat_filter_desc),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SectionRule()
        SwitchRow(
            title = stringResource(R.string.seat_filter_freeseat),
            checked = settings.excludeFreeseatRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeFreeseatRows(checked)
                }
            },
        )
        SwitchRow(
            title = stringResource(R.string.seat_filter_standing_combo),
            checked = settings.excludeGeneralStandingComboRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeGeneralStandingComboRows(checked)
                }
            },
        )
        SwitchRow(
            title = stringResource(R.string.seat_filter_standing_only),
            checked = settings.excludeGeneralStandingOnlyRows,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setExcludeGeneralStandingOnlyRows(checked)
                }
            },
        )
        Spacer(modifier = Modifier.height(26.dp))
        TrainExcludeSection(
            trainNumbers = settings.excludedTrainNumbers,
            onChange = { trains: Set<String> ->
                scope.launch {
                    repository.setExcludedTrainNumbers(trains)
                }
            },
            onDone = { dismissKeyboard() },
        )
        Spacer(modifier = Modifier.height(26.dp))
        SectionHeading(title = stringResource(R.string.popup_title))
        Spacer(modifier = Modifier.height(8.dp))
        SectionRule()
        SwitchRow(
            title = stringResource(R.string.intermediate_stop_title),
            description = stringResource(R.string.intermediate_stop_desc),
            checked = settings.autoConfirmIntermediateStopDialog,
            onCheckedChange = { checked: Boolean ->
                scope.launch {
                    repository.setAutoConfirmIntermediateStopDialog(checked)
                }
            },
        )
        AccessibilityRow(
            onOpen = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
        Spacer(modifier = Modifier.height(26.dp))
        DisplayShrinkSection(
            onCopy = { command: String -> copyCommandToClipboard(context, command) },
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = stringResource(R.string.train_list_scope_note), style = CaptionStyle)
    }
}

private fun Modifier.pointerTapToDismiss(onTap: () -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onTap() })
        },
    )

@Composable
private fun HomeHeader(onOpenLog: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontFamily = DisplayFamily,
            fontSize = 38.sp,
            letterSpacing = (-1).sp,
            color = Ink,
        )
        UnderlinedLink(text = stringResource(R.string.home_log_link), onClick = onOpenLog)
    }
}

@Composable
fun UnderlinedLink(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier.drawBehind {
                val y: Float = size.height + 2.dp.toPx()
                drawLine(Ink, Offset(0f, y), Offset(size.width, y), strokeWidth = LineWidth.toPx())
            },
        )
    }
}

@Composable
private fun StatusPill(state: MacroState, onStart: () -> Unit, onStop: () -> Unit) {
    val shape = RoundedCornerShape(48.dp)
    val container: Modifier = when (state) {
        MacroState.RUNNING -> Modifier.clip(shape).background(Ink)
        MacroState.IDLE -> Modifier.border(LineWidth, Ink, shape)
        MacroState.LOCKED -> Modifier.dashedBorder(InkMuted, LineWidth, 48.dp)
    }
    val (title, description) = when (state) {
        MacroState.RUNNING -> R.string.status_running to R.string.status_running_desc
        MacroState.IDLE -> R.string.status_idle to R.string.status_idle_desc
        MacroState.LOCKED -> R.string.status_locked to R.string.status_locked_desc
    }
    val textColor = if (state == MacroState.RUNNING) Sage else Ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .then(container)
            .padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(title),
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.6).sp,
                color = textColor,
            )
            Text(
                text = stringResource(description),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (state == MacroState.RUNNING) TrackOnInk else Ink,
            )
        }
        when (state) {
            MacroState.RUNNING -> CircleAction(
                text = stringResource(R.string.action_stop),
                onClick = onStop,
                modifier = Modifier.background(Sage, CircleShape),
                textColor = Ink,
            )
            MacroState.IDLE -> CircleAction(
                text = stringResource(R.string.action_start),
                onClick = onStart,
                modifier = Modifier.background(Ink, CircleShape),
                textColor = Sage,
            )
            MacroState.LOCKED -> CircleAction(
                text = stringResource(R.string.action_start),
                onClick = null,
                modifier = Modifier.dashedBorder(InkMuted, LineWidth, 35.dp),
                textColor = InkMuted,
            )
        }
    }
}

@Composable
private fun CircleAction(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    textColor: Color,
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .then(modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
private fun RecentLog(onOpenLog: () -> Unit) {
    val lines: List<MacroLogLine> by MacroRuntimeLog.lines.collectAsStateWithLifecycle()
    Spacer(modifier = Modifier.height(22.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = stringResource(R.string.recent_log_title), style = LabelStyle)
        Text(
            text = stringResource(R.string.recent_log_all),
            fontFamily = MonoFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onOpenLog)
                .padding(vertical = 8.dp),
        )
    }
    SectionRule()
    val recent: List<MacroLogLine> = lines.takeLast(RECENT_LOG_COUNT).asReversed()
    if (recent.isEmpty()) {
        Text(
            text = stringResource(R.string.recent_log_empty),
            style = CaptionStyle,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        RowDivider()
    } else {
        val formatter = rememberLogTimeFormatter()
        recent.forEach { line: MacroLogLine ->
            LogRow(line = line, formatter = formatter)
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(text = stringResource(R.string.running_settings_note), style = CaptionStyle)
}

@Composable
private fun RefreshIntervalCard(refreshMs: Long, onChange: (Long) -> Unit) {
    val min: Long = MacroPreferencesRepository.MIN_REFRESH_MS
    val max: Long = MacroPreferencesRepository.MAX_REFRESH_MS
    val step: Long = MacroPreferencesRepository.REFRESH_STEP_MS
    OutlineCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionHeading(
                title = stringResource(R.string.refresh_interval_title),
                description = stringResource(R.string.refresh_interval_range),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton(
                    symbol = "−",
                    description = stringResource(R.string.refresh_interval_decrease),
                    enabled = refreshMs > min,
                    onClick = { onChange((refreshMs - step).coerceAtLeast(min)) },
                )
                Text(
                    text = stringResource(R.string.refresh_interval_value, formatSeconds(refreshMs)),
                    fontFamily = MonoFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.8).sp,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(78.dp),
                )
                StepButton(
                    symbol = "+",
                    description = stringResource(R.string.refresh_interval_increase),
                    enabled = refreshMs < max,
                    onClick = { onChange((refreshMs + step).coerceAtMost(max)) },
                )
            }
        }
    }
}

/** 2000 → "2.0", 2250 → "2.25". 0.25초 단위라 소수 둘째 자리까지만 나온다. */
private fun formatSeconds(ms: Long): String {
    return if (ms % 100L == 0L) {
        String.format(Locale.US, "%.1f", ms / 1000.0)
    } else {
        String.format(Locale.US, "%.2f", ms / 1000.0)
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) Ink else InkDisabled
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .border(LineWidth, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = symbol, fontSize = 18.sp, color = color)
        }
    }
}

@Composable
private fun <T> ChoiceCard(
    title: String,
    description: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    note: @Composable (T) -> String?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlineCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading(title = title, description = description)
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option: T ->
                    val isSelected: Boolean = option == selected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 3.dp),
                    ) {
                        Text(
                            text = label(option),
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Ink else InkDisabled,
                        )
                        val noteText: String? = note(option)
                        if (noteText != null) {
                            Text(
                                text = noteText,
                                fontSize = 12.sp,
                                color = if (isSelected) InkSecondary else InkDisabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrainExcludeSection(
    trainNumbers: Set<String>,
    onChange: (Set<String>) -> Unit,
    onDone: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val fieldLabel: String = stringResource(R.string.train_exclude_title)
    fun add() {
        val normalized: String? = TrainExcludeNumbersParser.normalizeToken(input.trim())
        if (normalized == null) {
            showError = input.isNotBlank()
            return
        }
        onChange(trainNumbers + normalized)
        input = ""
        showError = false
        onDone()
    }
    SectionHeading(
        title = stringResource(R.string.train_exclude_title),
        description = stringResource(R.string.train_exclude_desc),
    )
    if (trainNumbers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            trainNumbers.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { number: String ->
                TrainChip(number = number, onRemove = { onChange(trainNumbers - number) })
            }
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = input,
            onValueChange = { value: String ->
                input = value.filter { it.isDigit() }.take(4)
                showError = false
            },
            singleLine = true,
            textStyle = TextStyle(fontFamily = MonoFamily, fontSize = 15.sp, color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { add() }),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .border(LineWidth, Ink, CircleShape)
                .semantics { contentDescription = fieldLabel },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (input.isEmpty()) {
                        Text(
                            text = stringResource(R.string.train_exclude_placeholder),
                            fontSize = 15.sp,
                            color = InkMuted,
                        )
                    }
                    inner()
                }
            },
        )
        InkButton(text = stringResource(R.string.train_exclude_add), onClick = { add() })
    }
    if (showError) {
        Text(
            text = stringResource(R.string.train_exclude_invalid),
            style = CaptionStyle,
            modifier = Modifier.padding(top = 6.dp, start = 16.dp),
        )
    }
}

@Composable
private fun TrainChip(number: String, onRemove: () -> Unit) {
    val removeLabel: String = stringResource(R.string.train_exclude_remove, number)
    Row(
        modifier = Modifier
            .height(34.dp)
            .border(LineWidth, Ink, CircleShape)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = number, fontFamily = MonoFamily, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "×", fontSize = 15.sp, color = Ink)
        }
    }
}

@Composable
private fun AccessibilityRow(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.accessibility_title), fontSize = 16.sp, color = Ink)
            Text(text = stringResource(R.string.accessibility_desc), style = CaptionStyle)
        }
        LineButton(text = stringResource(R.string.accessibility_open), onClick = onOpen)
    }
    RowDivider()
}

/**
 * adb로 시스템 DPI를 낮춰 한 화면에 더 많은 열차 행을 넣는 방법. 설정 앱 최소값(420)보다
 * 더 내릴 수 있는 유일한 경로라 잊지 않도록 앱 안에 적어 둔다. 명령은 복사해서 PC에서 실행한다.
 */
@Composable
private fun DisplayShrinkSection(onCopy: (String) -> Unit) {
    SectionHeading(
        title = stringResource(R.string.display_shrink_title),
        description = stringResource(R.string.display_shrink_desc),
    )
    Spacer(modifier = Modifier.height(8.dp))
    SectionRule()
    CommandRow(
        title = stringResource(R.string.display_shrink_apply_title),
        description = stringResource(R.string.display_shrink_apply_desc),
        command = stringResource(R.string.display_shrink_apply_command),
        onCopy = onCopy,
    )
    CommandRow(
        title = stringResource(R.string.display_shrink_reset_title),
        description = stringResource(R.string.display_shrink_reset_desc),
        command = stringResource(R.string.display_shrink_reset_command),
        onCopy = onCopy,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = stringResource(R.string.display_shrink_note), style = CaptionStyle)
}

@Composable
private fun CommandRow(
    title: String,
    description: String,
    command: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, fontSize = 16.sp, color = Ink)
            Text(text = description, style = CaptionStyle)
            Text(
                text = command,
                fontFamily = MonoFamily,
                fontSize = 13.sp,
                color = Ink,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        LineButton(
            text = stringResource(R.string.display_shrink_copy),
            onClick = { onCopy(command) },
        )
    }
    RowDivider()
}

private fun copyCommandToClipboard(context: Context, command: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    clipboard?.setPrimaryClip(ClipData.newPlainText("adb_command", command))
    Toast.makeText(
        context,
        context.getString(R.string.display_shrink_copied_toast),
        Toast.LENGTH_SHORT,
    ).show()
}

@Composable
private fun ticketAlertModeLabel(mode: TicketAlertMode): String {
    return when (mode) {
        TicketAlertMode.OFF -> stringResource(R.string.alert_off)
        TicketAlertMode.NORMAL -> stringResource(R.string.alert_normal)
        TicketAlertMode.STRONG -> stringResource(R.string.alert_strong)
    }
}

@Composable
private fun ticketAlertModeNote(mode: TicketAlertMode): String {
    return when (mode) {
        TicketAlertMode.OFF -> stringResource(R.string.alert_off_note)
        TicketAlertMode.NORMAL -> stringResource(R.string.alert_normal_note)
        TicketAlertMode.STRONG -> stringResource(R.string.alert_strong_note)
    }
}

@Composable
private fun seatPreferenceLabel(pref: SeatClickPreference): String {
    return when (pref) {
        SeatClickPreference.GENERAL_ONLY -> stringResource(R.string.seat_pref_general_only)
        SeatClickPreference.FIRST_CLASS_ONLY -> stringResource(R.string.seat_pref_first_only)
        SeatClickPreference.BOTH_PREFER_GENERAL -> stringResource(R.string.seat_pref_both)
    }
}
