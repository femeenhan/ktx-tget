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
    val excludeGeneralStandingOnlyRows: Boolean = true,
    val seatClickPreference: SeatClickPreference,
    val ticketAlertMode: TicketAlertMode = TicketAlertMode.NORMAL,
    /** When true, taps 「확인」 on intermediate-stop notice dialogs (코레일 문구 기준). */
    val autoConfirmIntermediateStopDialog: Boolean = true,
) {
    companion object {
        /**
         * 처음 설치했을 때의 값. 일반 이용자가 가장 흔히 원하는 쪽으로 둔다:
         * 일반실만, 자유석·입석은 넘김, 정차 안내는 자동 확인, 알림은 기본.
         */
        val DEFAULT: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = true,
            excludeGeneralStandingComboRows = false,
            excludeGeneralStandingOnlyRows = true,
            seatClickPreference = SeatClickPreference.GENERAL_ONLY,
            ticketAlertMode = TicketAlertMode.NORMAL,
            autoConfirmIntermediateStopDialog = true,
        )
    }
}
