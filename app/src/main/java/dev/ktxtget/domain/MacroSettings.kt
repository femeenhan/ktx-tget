package dev.ktxtget.domain

/**
 * Secondary filter options (DataStore in later phase; defaults for Phase 2).
 */
enum class SeatClickPreference {
    GENERAL_ONLY,
    FIRST_CLASS_ONLY,
    BOTH_PREFER_GENERAL,
}

data class MacroSettings(
    val excludedTrainNumbers: Set<String>,
    val excludeFreeseatRows: Boolean,
    val excludeGeneralStandingComboRows: Boolean = false,
    val excludeGeneralStandingOnlyRows: Boolean = false,
    val seatClickPreference: SeatClickPreference,
    val userAlertsEnabled: Boolean = true,
    /** When true, taps 「확인」 on intermediate-stop notice dialogs (코레일 문구 기준). Default off for safety. */
    val autoConfirmIntermediateStopDialog: Boolean = false,
) {
    companion object {
        val DEFAULT: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = false,
            excludeGeneralStandingComboRows = false,
            excludeGeneralStandingOnlyRows = false,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
            userAlertsEnabled = true,
            autoConfirmIntermediateStopDialog = false,
        )
    }
}
