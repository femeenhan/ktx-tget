package dev.ktxtget.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseApiParseTest {
    @Test
    fun `임시 이용 응답은 ACTIVE에 trial=true`() {
        val s = LicenseApi.parseSnapshot(
            checkedAtElapsedMs = 0L, checkedAtWallMs = 0L, text = """{"status":"active","expiresAt":"2026-11-01T03:30:00.000Z","renewalPending":false,
               "trial":true,"serverTime":"2026-11-01T02:30:00.000Z","notice":null}""",
        )
        assertEquals(LicenseStatus.ACTIVE, s.status)
        assertTrue(s.trial)
        assertEquals(s.serverTimeEpochMs + 3600_000L, s.expiresAtEpochMs)
    }

    @Test
    fun `trial 필드가 없는 예전 서버 응답도 읽는다`() {
        val s = LicenseApi.parseSnapshot(
            checkedAtElapsedMs = 0L, checkedAtWallMs = 0L, text = """{"status":"pending","expiresAt":null,"renewalPending":false,"serverTime":"2026-11-01T02:30:00.000Z","notice":"안내"}""",
        )
        assertEquals(LicenseStatus.PENDING, s.status)
        assertFalse(s.trial)
        assertEquals("안내", s.notice)
    }
}
