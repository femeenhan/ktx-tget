package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InitialOpenSweepTest {

    @Test
    fun `일반실 먼저, 위 행부터 하나만 고른다`() {
        val signals = listOf(
            SoldOutSignal(generalSoldOut = true, premiumSoldOut = false),
            SoldOutSignal(generalSoldOut = false, premiumSoldOut = false),
        )
        assertEquals(
            SeatOpening(1, SeatColumn.GENERAL),
            InitialOpenSweep.firstCandidate(signals, SeatClickPreference.BOTH_PREFER_GENERAL),
        )
        assertNull(InitialOpenSweep.firstCandidate(signals, SeatClickPreference.GENERAL_ONLY).takeIf { it?.rowIndex != 1 })
    }

    @Test
    fun `설정한 등급에 비매진이 없으면 null`() {
        val signals = listOf(SoldOutSignal(generalSoldOut = true, premiumSoldOut = false))
        assertNull(InitialOpenSweep.firstCandidate(signals, SeatClickPreference.GENERAL_ONLY))
        assertNull(InitialOpenSweep.firstCandidate(emptyList(), SeatClickPreference.BOTH_PREFER_GENERAL))
    }
}
