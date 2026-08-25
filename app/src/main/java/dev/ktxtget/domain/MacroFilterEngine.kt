package dev.ktxtget.domain

/**
 * 2차 필터. 목록 행이 접근성 트리에 텍스트를 노출하지 않게 되면서, 판정 시점이
 * "목록에서 거른다"에서 "행을 탭해 시트를 열고 거른다"로 옮겨졌다.
 * 걸러진 경우 호출부는 예매하지 않고 뒤로 나온다.
 */
object MacroFilterEngine {

    /** 통과하면 null, 막히면 사용자에게 보여줄 이유. */
    fun rejectionReason(sheet: BookingSheet, settings: MacroSettings): String? {
        if (!sheet.actionLabels.contains(KorailLabels.RESERVE_NOW)) {
            return "예매 버튼 없음 (${sheet.actionLabels.joinToString("/")})"
        }
        if (isExcludedTrain(sheet.trainNumber, settings.excludedTrainNumbers)) {
            return "제외 열차번호 (${sheet.trainLabel})"
        }
        val seats: String = sheet.seatOptionBlob
        if (settings.excludeFreeseatRows && seats.contains(KorailLabels.FREE_SEAT)) {
            return "자유석 제외"
        }
        if (settings.excludeGeneralStandingComboRows &&
            GeneralSeatCellKind.isStandingAndSeatedCombo(seats)
        ) {
            return "입석+좌석 제외"
        }
        if (settings.excludeGeneralStandingOnlyRows &&
            GeneralSeatCellKind.isStandingOnly(seats)
        ) {
            return "입석 제외"
        }
        return null
    }

    fun passes(sheet: BookingSheet, settings: MacroSettings): Boolean {
        return rejectionReason(sheet, settings) == null
    }

    private fun isExcludedTrain(trainNumber: String, excluded: Set<String>): Boolean {
        if (trainNumber.isEmpty()) {
            return false
        }
        val normalized: String = TrainExcludeNumbersParser.normalizeToken(trainNumber) ?: return false
        return excluded.contains(normalized)
    }
}
