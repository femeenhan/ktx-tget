package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MacroFilterEngineTest {
    @Test
    fun excluded_train_number_removes_row() {
        val row: TrainRow = TrainRow(
            stableKey = "001",
            rawLineTexts = listOf("KTX 001"),
            generalSeatCellText = "매진",
            firstClassSeatCellText = "매진",
        )
        val settings: MacroSettings = MacroSettings(
            excludedTrainNumbers = setOf("001"),
            excludeFreeseatRows = false,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val actual: List<TrainRow> = MacroFilterEngine.filter(listOf(row), settings)
        assertEquals(0, actual.size)
    }

    @Test
    fun freeseat_filter_removes_row() {
        val row: TrainRow = TrainRow(
            stableKey = "002",
            rawLineTexts = listOf("자유석 포함"),
            generalSeatCellText = "매진",
            firstClassSeatCellText = "매진",
        )
        val settings: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = true,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val actual: List<TrainRow> = MacroFilterEngine.filter(listOf(row), settings)
        assertEquals(0, actual.size)
    }

    @Test
    fun standing_combo_in_general_cell_removed_when_enabled() {
        val row: TrainRow = TrainRow(
            stableKey = "004",
            rawLineTexts = listOf("KTX 004"),
            generalSeatCellText = "입석+좌석",
            firstClassSeatCellText = "매진",
        )
        val settings: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = false,
            excludeGeneralStandingComboRows = true,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val actual: List<TrainRow> = MacroFilterEngine.filter(listOf(row), settings)
        assertEquals(0, actual.size)
    }

    @Test
    fun standing_only_in_general_cell_removed_when_enabled() {
        val row: TrainRow = TrainRow(
            stableKey = "005",
            rawLineTexts = listOf("KTX 005"),
            generalSeatCellText = "입석",
            firstClassSeatCellText = "매진",
        )
        val settings: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = false,
            excludeGeneralStandingOnlyRows = true,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val actual: List<TrainRow> = MacroFilterEngine.filter(listOf(row), settings)
        assertEquals(0, actual.size)
    }

    @Test
    fun standing_combo_not_removed_by_standing_only_flag() {
        val row: TrainRow = TrainRow(
            stableKey = "006",
            rawLineTexts = listOf("KTX 006"),
            generalSeatCellText = "입석+좌석",
            firstClassSeatCellText = "매진",
        )
        val settings: MacroSettings = MacroSettings(
            excludedTrainNumbers = emptySet(),
            excludeFreeseatRows = false,
            excludeGeneralStandingOnlyRows = true,
            seatClickPreference = SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val actual: List<TrainRow> = MacroFilterEngine.filter(listOf(row), settings)
        assertEquals(1, actual.size)
    }
}
