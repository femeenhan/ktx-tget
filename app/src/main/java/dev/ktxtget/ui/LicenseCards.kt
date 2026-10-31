package dev.ktxtget.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ktxtget.BuildConfig
import dev.ktxtget.R
import dev.ktxtget.license.AppConfig
import dev.ktxtget.license.LicenseSnapshot
import dev.ktxtget.license.LicenseStatus
import dev.ktxtget.ui.theme.Ink
import dev.ktxtget.ui.theme.InkMuted
import dev.ktxtget.ui.theme.InkSecondary
import dev.ktxtget.ui.theme.MonoFamily
import dev.ktxtget.ui.theme.Sage
import java.util.Locale

private const val DEPOSITOR_MAX_LENGTH: Int = 40
private const val HOUR_MS: Long = 3600L * 1000
private const val DAY_MS: Long = 24L * HOUR_MS
private const val EXPIRY_WARN_MS: Long = 7L * DAY_MS
private val NoticeShape = RoundedCornerShape(16.dp)

private enum class Plan(val wire: String, val labelRes: Int) {
    DAY("day", R.string.license_plan_day),
    MONTH("month", R.string.license_plan_month),
    YEAR("year", R.string.license_plan_year),
    LIFETIME("lifetime", R.string.license_plan_lifetime),
}

private fun AppConfig.priceOf(plan: Plan): Long = when (plan) {
    Plan.DAY -> priceDay
    Plan.MONTH -> priceMonth
    Plan.YEAR -> priceYear
    Plan.LIFETIME -> priceLifetime
}

/** 서버 기준 현재 시각. 확인 시점의 (서버 - 기기) 시차를 지금 기기 시각에 더한다. */
private fun serverNow(s: LicenseSnapshot, nowWallMs: Long): Long =
    nowWallMs + (s.serverTimeEpochMs - s.checkedAtWallMs)

/** 만료까지 남은 일수(올림, 최소 0). 만료가 없으면 null. */
fun daysLeft(s: LicenseSnapshot, nowWallMs: Long): Long? {
    val expires: Long = s.expiresAtEpochMs ?: return null
    val left: Long = expires - serverNow(s, nowWallMs)
    if (left <= 0) return 0
    return (left + DAY_MS - 1) / DAY_MS
}

/** 만료까지 남은 시간(올림, 최소 0). 하루 미만일 때 일수 대신 보여 준다. */
fun hoursLeft(s: LicenseSnapshot, nowWallMs: Long): Long? {
    val expires: Long = s.expiresAtEpochMs ?: return null
    val left: Long = expires - serverNow(s, nowWallMs)
    if (left <= 0) return 0
    return (left + HOUR_MS - 1) / HOUR_MS
}

fun shouldShowExpiryLine(s: LicenseSnapshot, nowWallMs: Long): Boolean {
    if (s.status != LicenseStatus.ACTIVE) return false
    if (s.renewalPending) return true
    val expires: Long = s.expiresAtEpochMs ?: return false
    return expires - serverNow(s, nowWallMs) <= EXPIRY_WARN_MS
}

@Composable
fun NoticeRow(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        color = Ink,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .border(BorderStroke(LineWidth, InkMuted), NoticeShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun UpdateBanner(versionName: String, onDownload: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(BorderStroke(LineWidth, Ink), NoticeShape)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.update_banner_text, versionName),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        InkButton(text = stringResource(R.string.update_banner_button), onClick = onDownload)
    }
}

/**
 * 접근성 권한이 꺼져 있을 때 홈 맨 위에 두는 안내. 권한이 없으면 아래 모든 설정이 무의미하므로
 * 업데이트 배너와 같은 줄 모양으로 가장 먼저 보이게 한다.
 */
@Composable
fun AccessibilityBanner(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(BorderStroke(LineWidth, Ink), NoticeShape)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.accessibility_banner_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Ink,
            )
            Text(text = stringResource(R.string.accessibility_banner_desc), style = CaptionStyle)
        }
        InkButton(text = stringResource(R.string.accessibility_open), onClick = onOpen)
    }
}

/** 활성화(또는 연장) 요청 카드. [title]이 null이면 [status]로 제목을 고른다. */
@Composable
fun ActivationRequestCard(
    config: AppConfig?,
    status: LicenseStatus,
    onSubmit: (depositor: String, plan: String) -> Unit,
    submitting: Boolean,
    error: String?,
    onReloadConfig: () -> Unit,
    deviceId: String,
    title: String? = null,
) {
    val context: Context = LocalContext.current
    var depositor: String by rememberSaveable { mutableStateOf("") }
    var plan: Plan by rememberSaveable { mutableStateOf(Plan.MONTH) }
    var agreed: Boolean by rememberSaveable { mutableStateOf(false) }
    val heading: String = title ?: when (status) {
        LicenseStatus.EXPIRED -> stringResource(R.string.license_request_title_expired)
        LicenseStatus.REVOKED -> stringResource(R.string.license_request_title_revoked)
        else -> stringResource(R.string.license_request_title)
    }
    OutlineCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = heading, style = LabelStyle.copy(fontSize = 16.sp))
                val sub: Int = if (status == LicenseStatus.REVOKED) {
                    R.string.license_revoked_desc
                } else {
                    R.string.license_request_desc
                }
                Text(text = stringResource(sub), style = CaptionStyle)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = stringResource(R.string.license_depositor_label), style = CaptionStyle)
                OutlinedTextField(
                    value = depositor,
                    onValueChange = { v: String -> depositor = v.replace("\n", "").take(DEPOSITOR_MAX_LENGTH) },
                    singleLine = true,
                    enabled = !submitting,
                    placeholder = { Text(text = stringResource(R.string.license_depositor_hint), color = InkMuted) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ink,
                        unfocusedBorderColor = Ink,
                        cursorColor = Ink,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column {
                Text(text = stringResource(R.string.license_plan_label), style = CaptionStyle)
                Column(modifier = Modifier.selectableGroup()) {
                    Plan.entries.forEach { p: Plan ->
                        PlanRow(
                            label = stringResource(p.labelRes),
                            price = priceText(config?.priceOf(p)),
                            selected = plan == p,
                            onSelect = { plan = p },
                        )
                        RowDivider()
                    }
                }
            }
            if (config != null) {
                AccountRow(bankAccount = config.bankAccount)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.license_config_failed),
                        style = CaptionStyle,
                        modifier = Modifier.weight(1f),
                    )
                    LineButton(text = stringResource(R.string.license_config_reload), onClick = onReloadConfig)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Ink,
                            uncheckedColor = Ink,
                            checkmarkColor = Sage,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.license_consent),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Ink,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                LineButton(
                    text = stringResource(R.string.license_terms_open),
                    onClick = {
                        openUrl(context, BuildConfig.API_BASE_URL.trimEnd('/') + (config?.termsUrl ?: "/terms"))
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InkButton(
                    text = stringResource(
                        if (submitting) R.string.license_request_sending else R.string.license_request_send,
                    ),
                    onClick = { onSubmit(depositor.trim(), plan.wire) },
                    enabled = depositor.isNotBlank() && agreed && !submitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        text = stringResource(R.string.license_request_failed, error),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                    )
                }
            }
            DeviceIdLine(deviceId = deviceId)
        }
    }
}

@Composable
private fun PlanRow(label: String, price: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Ink, unselectedColor = Ink),
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = Ink,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        )
        Text(text = price, fontFamily = MonoFamily, fontSize = 14.sp, color = Ink)
    }
}

@Composable
private fun priceText(price: Long?): String = when {
    price == null -> "—"
    price <= 0L -> stringResource(R.string.license_price_ask)
    else -> stringResource(R.string.license_price_won, String.format(Locale.KOREA, "%,d", price))
}

@Composable
private fun AccountRow(bankAccount: String) {
    val context: Context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.license_account_label), style = CaptionStyle)
            Text(
                text = bankAccount.ifBlank { "—" },
                fontFamily = MonoFamily,
                fontSize = 14.sp,
                color = Ink,
            )
        }
        if (bankAccount.isNotBlank()) {
            LineButton(
                text = stringResource(R.string.license_copy_short),
                onClick = {
                    copyToClipboard(context, "bank_account", bankAccount, R.string.license_account_copied_toast)
                },
            )
        }
    }
}

@Composable
private fun DeviceIdLine(deviceId: String) {
    val context: Context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.license_device_id_line, deviceId.take(6).ifEmpty { "—" }),
            fontFamily = MonoFamily,
            fontSize = 12.sp,
            color = InkSecondary,
            modifier = Modifier.weight(1f),
        )
        if (deviceId.isNotEmpty()) {
            LineButton(
                text = stringResource(R.string.license_copy_short),
                onClick = {
                    copyToClipboard(context, "device_id", deviceId, R.string.license_device_id_copied_toast)
                },
            )
        }
    }
}

@Composable
fun PendingCard(config: AppConfig?, onCheck: () -> Unit, checking: Boolean) {
    val context: Context = LocalContext.current
    OutlineCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.license_pending_title), style = LabelStyle.copy(fontSize = 16.sp))
                Text(text = stringResource(R.string.license_pending_desc), style = CaptionStyle)
            }
            if (config != null) AccountRow(bankAccount = config.bankAccount)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config != null && config.openchatUrl.isNotBlank()) {
                    LineButton(
                        text = stringResource(R.string.license_openchat),
                        onClick = { openUrl(context, config.openchatUrl) },
                    )
                }
                LineButton(
                    text = stringResource(
                        if (checking) R.string.license_checking else R.string.license_check_again,
                    ),
                    onClick = { if (!checking) onCheck() },
                )
            }
        }
    }
}

@Composable
fun ExpiryLine(snapshot: LicenseSnapshot, nowWallMs: Long, onRenew: () -> Unit, renewOpen: Boolean) {
    val days: Long? = daysLeft(snapshot, nowWallMs)
    val hours: Long? = hoursLeft(snapshot, nowWallMs)
    val leftText: String = when {
        days == null || hours == null -> ""
        hours <= 24 -> stringResource(R.string.license_hours_left, hours)
        else -> stringResource(R.string.license_days_left, days)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = leftText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier.weight(1f),
        )
        if (snapshot.renewalPending) {
            Text(text = stringResource(R.string.license_renewal_pending), fontSize = 13.sp, color = InkSecondary)
        } else {
            LineButton(
                text = stringResource(if (renewOpen) R.string.license_renew_close else R.string.license_renew),
                onClick = onRenew,
            )
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.open_url_failed_toast), Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, label: String, text: String, toastRes: Int) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, context.getString(toastRes), Toast.LENGTH_SHORT).show()
}
