package dev.ktxtget.domain

object MacroFilterEngine {
    fun filter(rows: List<TrainRow>, settings: MacroSettings): List<TrainRow> {
        return rows.filter { row: TrainRow -> passes(row, settings) }
    }

    private fun passes(row: TrainRow, settings: MacroSettings): Boolean {
        if (settings.excludedTrainNumbers.contains(row.stableKey)) {
            return false
        }
        val blob: String = row.rawLineTexts.joinToString(" ") + " " +
            row.generalSeatCellText + " " + row.firstClassSeatCellText
        if (settings.excludeFreeseatRows && blob.contains("자유석")) {
            return false
        }
        val general: String = row.generalSeatCellText
        if (settings.excludeGeneralStandingComboRows &&
            GeneralSeatCellKind.isStandingAndSeatedCombo(general)
        ) {
            return false
        }
        if (settings.excludeGeneralStandingOnlyRows &&
            GeneralSeatCellKind.isStandingOnly(general)
        ) {
            return false
        }
        return true
    }
}
