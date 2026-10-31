package dev.ktxtget.ui

import dev.ktxtget.license.LicenseSnapshot
import dev.ktxtget.license.LicenseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryLineTest {
    private val hour: Long = 3600L * 1000
    private val day: Long = 24 * hour
    private val now: Long = 1_800_000_000_000L

    private fun snap(
        expiresAt: Long?,
        status: LicenseStatus = LicenseStatus.ACTIVE,
        renewalPending: Boolean = false,
        serverSkew: Long = 0L,
    ): LicenseSnapshot = LicenseSnapshot(
        status = status,
        expiresAtEpochMs = expiresAt,
        serverTimeEpochMs = now + serverSkew,
        checkedAtElapsedMs = 0L,
        checkedAtWallMs = now,
        renewalPending = renewalPending,
        notice = null,
    )

    @Test
    fun daysLeftRoundsUpAndClampsAtZero() {
        assertEquals(3L, daysLeft(snap(now + 2 * day + hour), now))
        assertEquals(1L, daysLeft(snap(now + hour), now))
        assertEquals(0L, daysLeft(snap(now - hour), now))
        assertNull(daysLeft(snap(null), now))
    }

    @Test
    fun daysLeftUsesServerClock() {
        // 기기 시계가 서버보다 하루 늦다: 서버 기준으로는 하루가 이미 지났다.
        assertEquals(2L, daysLeft(snap(now + 3 * day, serverSkew = day), now))
    }

    @Test
    fun lineShowsWheneverActive() {
        assertTrue(shouldShowExpiryLine(snap(now + 7 * day), now))
        assertTrue(shouldShowExpiryLine(snap(now + 300 * day), now))
        assertTrue(shouldShowExpiryLine(snap(null), now))
        assertTrue(shouldShowExpiryLine(snap(now + 300 * day, renewalPending = true), now))
        assertFalse(shouldShowExpiryLine(snap(now + day, status = LicenseStatus.PENDING), now))
    }

    @Test
    fun nearExpiryWithinSevenDays() {
        assertTrue(isNearExpiry(snap(now + 7 * day), now))
        assertFalse(isNearExpiry(snap(now + 8 * day), now))
        assertFalse(isNearExpiry(snap(null), now))
    }

    @Test
    fun hoursLeftRoundsUpAndClampsAtZero() {
        assertEquals(3L, hoursLeft(snap(now + 2 * hour + 1), now))
        assertEquals(24L, hoursLeft(snap(now + day), now))
        assertEquals(0L, hoursLeft(snap(now - hour), now))
        assertNull(hoursLeft(snap(null), now))
    }
}
