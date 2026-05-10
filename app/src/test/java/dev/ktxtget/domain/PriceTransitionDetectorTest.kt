package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceTransitionDetectorTest {
    @Test
    fun general_column_transition() {
        val prev: PreviousRowCells = PreviousRowCells(
            generalText = "매진",
            firstClassText = "매진",
        )
        val curr: TrainRow = TrainRow(
            stableKey = "1",
            rawLineTexts = emptyList(),
            generalSeatCellText = "23,700원",
            firstClassSeatCellText = "매진",
        )
        val col: SeatColumn? = PriceTransitionDetector.findSoldOutToPriceColumn(
            prev,
            curr,
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertEquals(SeatColumn.GENERAL, col)
    }

    @Test
    fun prefers_general_when_both_transition() {
        val prev: PreviousRowCells = PreviousRowCells(
            generalText = "매진",
            firstClassText = "매진",
        )
        val curr: TrainRow = TrainRow(
            stableKey = "1",
            rawLineTexts = emptyList(),
            generalSeatCellText = "23,700원",
            firstClassSeatCellText = "33,700원",
        )
        val col: SeatColumn? = PriceTransitionDetector.findSoldOutToPriceColumn(
            prev,
            curr,
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertEquals(SeatColumn.GENERAL, col)
    }

    @Test
    fun first_only_skips_general() {
        val prev: PreviousRowCells = PreviousRowCells(
            generalText = "매진",
            firstClassText = "매진",
        )
        val curr: TrainRow = TrainRow(
            stableKey = "1",
            rawLineTexts = emptyList(),
            generalSeatCellText = "매진",
            firstClassSeatCellText = "15,000원",
        )
        val col: SeatColumn? = PriceTransitionDetector.findSoldOutToPriceColumn(
            prev,
            curr,
            SeatClickPreference.FIRST_CLASS_ONLY,
        )
        assertEquals(SeatColumn.FIRST_CLASS, col)
    }

    @Test
    fun no_previous_no_transition() {
        val curr: TrainRow = TrainRow(
            stableKey = "1",
            rawLineTexts = emptyList(),
            generalSeatCellText = "23,700원",
            firstClassSeatCellText = "매진",
        )
        val col: SeatColumn? = PriceTransitionDetector.findSoldOutToPriceColumn(
            null,
            curr,
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertNull(col)
    }
}
