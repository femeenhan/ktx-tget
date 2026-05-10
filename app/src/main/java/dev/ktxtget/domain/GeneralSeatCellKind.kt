package dev.ktxtget.domain

/**
 * Classifies the Korail **general-class** seat cell label for row filtering.
 * Order: 입석+좌석 before 단일 입석 substring checks.
 */
object GeneralSeatCellKind {
    fun isStandingAndSeatedCombo(generalSeatCellText: String): Boolean {
        return generalSeatCellText.contains("입석+좌석")
    }

    fun isStandingOnly(generalSeatCellText: String): Boolean {
        return generalSeatCellText.contains("입석") && !generalSeatCellText.contains("입석+좌석")
    }
}
