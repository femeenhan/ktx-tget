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
        val USER_ALERTS_ENABLED = booleanPreferencesKey("user_alerts_enabled")
        val AUTO_CONFIRM_INTERMEDIATE_STOP = booleanPreferencesKey("auto_confirm_intermediate_stop_dialog")
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

    suspend fun readMacroEnabled(): Boolean {
        return macroEnabled.first()
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

    suspend fun setUserAlertsEnabled(value: Boolean) {
        context.macroDataStore.edit { preferences ->
            preferences[PrefsKeys.USER_ALERTS_ENABLED] = value
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
        val excludeFreeseatRows: Boolean = p[PrefsKeys.EXCLUDE_FREESEAT] ?: false
        val excludeGeneralStandingComboRows: Boolean =
            p[PrefsKeys.EXCLUDE_GENERAL_STANDING_COMBO] ?: false
        val excludeGeneralStandingOnlyRows: Boolean =
            p[PrefsKeys.EXCLUDE_GENERAL_STANDING_ONLY] ?: false
        val prefName: String = p[PrefsKeys.SEAT_PREF] ?: SeatClickPreference.BOTH_PREFER_GENERAL.name
        val seatClickPreference: SeatClickPreference = try {
            SeatClickPreference.valueOf(prefName)
        } catch (_: IllegalArgumentException) {
            SeatClickPreference.BOTH_PREFER_GENERAL
        }
        val userAlertsEnabled: Boolean = p[PrefsKeys.USER_ALERTS_ENABLED] ?: true
        val autoConfirmIntermediateStopDialog: Boolean =
            p[PrefsKeys.AUTO_CONFIRM_INTERMEDIATE_STOP] ?: false
        return MacroSettings(
            excludedTrainNumbers = excludedTrainNumbers,
            excludeFreeseatRows = excludeFreeseatRows,
            excludeGeneralStandingComboRows = excludeGeneralStandingComboRows,
            excludeGeneralStandingOnlyRows = excludeGeneralStandingOnlyRows,
            seatClickPreference = seatClickPreference,
            userAlertsEnabled = userAlertsEnabled,
            autoConfirmIntermediateStopDialog = autoConfirmIntermediateStopDialog,
        )
    }

    companion object {
        private const val REFRESH_DEFAULT_MS: Long = 2500L
        const val MIN_REFRESH_MS: Long = 2000L
        const val MAX_REFRESH_MS: Long = 3000L
        const val REFRESH_STEP_MS: Long = 250L
    }
}
