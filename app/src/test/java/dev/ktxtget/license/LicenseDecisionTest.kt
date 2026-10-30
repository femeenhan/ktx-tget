package dev.ktxtget.license

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseDecisionTest {
    private val hour: Long = 3600L * 1000
    private val day: Long = 24 * hour
    private val now: Long = 1_800_000_000_000L

    private fun snap(
        status: LicenseStatus = LicenseStatus.ACTIVE,
        expiresAt: Long? = null,
        checkedAtWall: Long = now - hour,
        serverTime: Long = checkedAtWall,
    ): LicenseSnapshot = LicenseSnapshot(
        status = status,
        expiresAtEpochMs = expiresAt,
        serverTimeEpochMs = serverTime,
        checkedAtElapsedMs = 0L,
        checkedAtWallMs = checkedAtWall,
        renewalPending = false,
        notice = null,
    )

    @Test
    fun activeNoExpiryCheckedOneHourAgo_isLicensed() {
        assertTrue(LicenseDecision.isLicensed(snap(), now))
    }

    @Test
    fun checked73HoursAgo_isNotLicensed() {
        assertFalse(LicenseDecision.isLicensed(snap(checkedAtWall = now - 73 * hour), now))
    }

    @Test
    fun expiresInOneHour_isLicensed() {
        assertTrue(LicenseDecision.isLicensed(snap(expiresAt = now + hour), now))
    }

    @Test
    fun expiredOneHourAgo_isNotLicensed() {
        assertFalse(LicenseDecision.isLicensed(snap(expiresAt = now - hour), now))
    }

    @Test
    fun deviceClockTwoDaysAhead_expiresOneDayLaterServerTime_isLicensed() {
        // 기기 시계가 서버보다 2일 빠름: serverTime − checkedAtWall = −2일.
        // 서버 기준 현재는 now − 2일 − 1h(확인 후 경과)… 만료는 서버 기준 확인 시점 + 1일.
        val checkedAtWall: Long = now - hour
        val serverTime: Long = checkedAtWall - 2 * day
        val expiresAt: Long = serverTime + day
        assertTrue(
            LicenseDecision.isLicensed(
                snap(expiresAt = expiresAt, checkedAtWall = checkedAtWall, serverTime = serverTime),
                now,
            ),
        )
    }

    @Test
    fun pending_isNotLicensed() {
        assertFalse(LicenseDecision.isLicensed(snap(status = LicenseStatus.PENDING), now))
    }

    @Test
    fun nullSnapshot_isNotLicensed() {
        assertFalse(LicenseDecision.isLicensed(null, now))
    }

    @Test
    fun deviceClockSetBackAfterCheck_isStillLicensed() {
        assertTrue(LicenseDecision.isLicensed(snap(checkedAtWall = now + hour), now))
    }

    @Test
    fun checkedExactly72HoursAgo_isNotLicensed() {
        assertFalse(LicenseDecision.isLicensed(snap(checkedAtWall = now - 72 * hour), now))
    }

    @Test
    fun checked71h59mAgo_isLicensed() {
        val minute: Long = 60L * 1000
        assertTrue(LicenseDecision.isLicensed(snap(checkedAtWall = now - 72 * hour + minute), now))
    }

    @Test
    fun expiresExactlyAtServerNow_isNotLicensed() {
        // serverTime == checkedAtWall이므로 서버 기준 현재 == now.
        assertFalse(LicenseDecision.isLicensed(snap(expiresAt = now), now))
    }
}
