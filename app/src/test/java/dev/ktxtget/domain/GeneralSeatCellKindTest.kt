package dev.ktxtget.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneralSeatCellKindTest {
    @Test
    fun detects_standing_combo() {
        assertTrue(GeneralSeatCellKind.isStandingAndSeatedCombo("입석+좌석 예약대기"))
    }

    @Test
    fun detects_standing_only_not_combo() {
        assertTrue(GeneralSeatCellKind.isStandingOnly("입석"))
        assertFalse(GeneralSeatCellKind.isStandingOnly("입석+좌석"))
    }
}
