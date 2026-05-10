package dev.ktxtget.domain

object PriceTransitionDetector {
    private const val SOLD_OUT_LITERAL: String = "매진"
    fun isSoldOutLabel(text: String): Boolean {
        val t: String = text.trim()
        return t == SOLD_OUT_LITERAL || t.contains(SOLD_OUT_LITERAL)
    }

    fun isPriceLabel(text: String): Boolean {
        return PricePattern.KOREAN_WON.containsMatchIn(text)
    }

    /**
    * When [previous] is null, no transition from 매진 is assumed (first observation).
    */
    fun findSoldOutToPriceColumn(
        previous: PreviousRowCells?,
        current: TrainRow,
        preference: SeatClickPreference,
    ): SeatColumn? {
        if (previous == null) {
            return null
        }
        val order: List<SeatColumn> = when (preference) {
            SeatClickPreference.GENERAL_ONLY -> listOf(SeatColumn.GENERAL)
            SeatClickPreference.FIRST_CLASS_ONLY -> listOf(SeatColumn.FIRST_CLASS)
            SeatClickPreference.BOTH_PREFER_GENERAL -> listOf(
                SeatColumn.GENERAL,
                SeatColumn.FIRST_CLASS,
            )
        }
        for (col: SeatColumn in order) {
            val before: String = when (col) {
                SeatColumn.GENERAL -> previous.generalText
                SeatColumn.FIRST_CLASS -> previous.firstClassText
            }
            val after: String = when (col) {
                SeatColumn.GENERAL -> current.generalSeatCellText
                SeatColumn.FIRST_CLASS -> current.firstClassSeatCellText
            }
            if (isSoldOutLabel(before) && isPriceLabel(after)) {
                return col
            }
        }
        return null
    }
}
