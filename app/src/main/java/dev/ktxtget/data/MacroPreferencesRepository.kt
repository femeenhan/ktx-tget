package dev.ktxtget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.SeatClickPreference
import dev.ktxtget.domain.TicketAlertMode
import dev.ktxtget.domain.TrainExcludeNumbersParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.macroDataStore: DataStore<Preferences> by preferencesDataStore(name = "macro_prefs")

/**
 * Persists macro toggle, refresh cadence, and secondary filters.
 */
class MacroPreferencesRepository(
    private val context: Context,
) {
    private object PrefsKeys {
        val MACRO_ENABLED = booleanPreferencesKey("macro_enabled")
        val REFRESH_INTERVAL_MS = longPreferencesKey("refresh_interval_ms")
        val EXCLUDED_TRAINS_CSV = stringPreferencesKey("excluded_trains_csv")
        val EXCLUDE_FREESEAT = booleanPreferencesKey("exclude_freeseat")
        val EXCLUDE_GENERAL_STANDING_COMBO = booleanPreferencesKey("exclude_general_standing_combo")
        val EXCLUDE_GENERAL_STANDING_ONLY = booleanPreferencesKey("exclude_general_standing_only")
        val SEAT_PREF = stringPreferencesKey("seat_click_pref")
        val TICKET_ALERT_MODE = stringPreferencesKey("ticket_alert_mode")
        val USER_ALERTS_ENABLED = booleanPreferencesKey("user_alerts_enabled")
        val AUTO_CONFIRM_INTERMEDIATE_STOP = booleanPreferencesKey("auto_confirm_intermediate_stop_dialog")
        val PAUSED_UNTIL_WALL_MS = longPreferencesKey("paused_until_wall_ms")
    }

    val macroEnabled: Flow<Boolean> = context.macroDataStore.data.map { p: Preferences ->
        p[PrefsKeys.MACRO_ENABLED] ?: false
    }
    val refreshIntervalMs: Flow<Long> = context.macroDataStore.data.map { p: Preferences ->
        (p[PrefsKeys.REFRESH_INTERVAL_MS] ?: REFRESH_DEFAULT_MS).coerceIn(
            MIN_REFRESH_MS,
            MAX_REFRESH_MS,
        )
    }
    val macroSettings: Flow<MacroSettings> = context.macroDataStore.data.map { p: Preferences ->
        preferencesToMacroSettings(p)
    }

    /** 코레일톡 경고 뒤 쉬는 기한(벽시계 ms). 0이면 쉬지 않는 중. */
    val pausedUntilWallMs: Flow<Long> = context.macroDataStore.data.map { p: Preferences ->
        p[PrefsKeys.PAUSED_UNTIL_WALL_MS] ?: 0L
    }

    suspend fun readMacroEnabled(): Boolean {
        return macroEnabled.first()
    }

    suspend fun readPausedUntilWallMs(): Long {
        return pausedUntilWallMs.first()
    }

    suspend fun setPausedUntilWallMs(value: Long) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.PAUSED_UNTIL_WALL_MS] = value
        }
    }

    suspend fun readRefreshIntervalMs(): Long {
        return refreshIntervalMs.first()
    }

    suspend fun readMacroSettings(): MacroSettings {
        val p: Preferences = context.macroDataStore.data.first()
        return preferencesToMacroSettings(p)
    }

    suspend fun setMacroEnabled(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.MACRO_ENABLED] = value
        }
    }

    suspend fun setRefreshIntervalMs(value: Long) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.REFRESH_INTERVAL_MS] =
                value.coerceIn(MIN_REFRESH_MS, MAX_REFRESH_MS)
        }
    }

    suspend fun setExcludedTrainNumbers(trainNumbers: Set<String>) {
        context.macroDataStore.edit { preferences ->
            val csv: String =
                trainNumbers.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")
            if (csv.isEmpty()) {
                preferences.remove(PrefsKeys.EXCLUDED_TRAINS_CSV)
            } else {
                preferences[PrefsKeys.EXCLUDED_TRAINS_CSV] = csv
            }
        }
    }

    suspend fun setExcludeFreeseatRows(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.EXCLUDE_FREESEAT] = value
        }
    }

    suspend fun setExcludeGeneralStandingComboRows(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.EXCLUDE_GENERAL_STANDING_COMBO] = value
        }
    }

    suspend fun setExcludeGeneralStandingOnlyRows(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.EXCLUDE_GENERAL_STANDING_ONLY] = value
        }
    }

    suspend fun setSeatClickPreference(value: SeatClickPreference) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.SEAT_PREF] = value.name
        }
    }

    suspend fun setTicketAlertMode(value: TicketAlertMode) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.TICKET_ALERT_MODE] = value.name
        }
    }

    suspend fun setAutoConfirmIntermediateStopDialog(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.AUTO_CONFIRM_INTERMEDIATE_STOP] = value
        }
    }

    private fun preferencesToMacroSettings(p: Preferences): MacroSettings {
        val excludedCsv: String? = p[PrefsKeys.EXCLUDED_TRAINS_CSV]
        val excludedTrainNumbers: Set<String> =
            excludedCsv?.split(",")?.mapNotNull { segment: String ->
                TrainExcludeNumbersParser.normalizeToken(segment.trim())
            }?.toSet() ?: emptySet()
        val defaults: MacroSettings = MacroSettings.DEFAULT
        val excludeFreeseatRows: Boolean = p[PrefsKeys.EXCLUDE_FREESEAT] ?: defaults.excludeFreeseatRows
        val excludeGeneralStandingComboRows: Boolean =
            p[PrefsKeys.EXCLUDE_GENERAL_STANDING_COMBO] ?: defaults.excludeGeneralStandingComboRows
        val excludeGeneralStandingOnlyRows: Boolean =
            p[PrefsKeys.EXCLUDE_GENERAL_STANDING_ONLY] ?: defaults.excludeGeneralStandingOnlyRows
        val prefName: String = p[PrefsKeys.SEAT_PREF] ?: defaults.seatClickPreference.name
        val seatClickPreference: SeatClickPreference = try {
            SeatClickPreference.valueOf(prefName)
        } catch (_: IllegalArgumentException) {
            defaults.seatClickPreference
        }
        val ticketAlertMode: TicketAlertMode = resolveTicketAlertMode(p)
        val autoConfirmIntermediateStopDialog: Boolean =
            p[PrefsKeys.AUTO_CONFIRM_INTERMEDIATE_STOP] ?: defaults.autoConfirmIntermediateStopDialog
        return MacroSettings(
            excludedTrainNumbers = excludedTrainNumbers,
            excludeFreeseatRows = excludeFreeseatRows,
            excludeGeneralStandingComboRows = excludeGeneralStandingComboRows,
            excludeGeneralStandingOnlyRows = excludeGeneralStandingOnlyRows,
            seatClickPreference = seatClickPreference,
            ticketAlertMode = ticketAlertMode,
            autoConfirmIntermediateStopDialog = autoConfirmIntermediateStopDialog,
        )
    }

    private fun resolveTicketAlertMode(p: Preferences): TicketAlertMode {
        val modeName: String? = p[PrefsKeys.TICKET_ALERT_MODE]
        if (modeName != null) {
            return try {
                TicketAlertMode.valueOf(modeName)
            } catch (_: IllegalArgumentException) {
                MacroSettings.DEFAULT.ticketAlertMode
            }
        }
        // 예전 버전의 켜짐/꺼짐 값만 있으면 그 뜻을 지킨다. 아무 값도 없으면 기본값.
        val legacyEnabled: Boolean? = p[PrefsKeys.USER_ALERTS_ENABLED]
        return when (legacyEnabled) {
            null -> MacroSettings.DEFAULT.ticketAlertMode
            true -> TicketAlertMode.STRONG
            false -> TicketAlertMode.OFF
        }
    }

    companion object {
        private const val REFRESH_DEFAULT_MS: Long = 2500L
        const val MIN_REFRESH_MS: Long = 1500L
        const val MAX_REFRESH_MS: Long = 3000L
        const val REFRESH_STEP_MS: Long = 250L
    }
}
