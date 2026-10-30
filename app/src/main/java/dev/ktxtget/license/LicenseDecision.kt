package dev.ktxtget.license

object LicenseDecision {
    /** 서버 확인 없이 마지막 ACTIVE 응답을 믿어 주는 기간. */
    const val GRACE_MS: Long = 72L * 3600 * 1000

    /**
     * 만료는 서버 시각 기준으로 비교한다. 확인 시점의 (서버 − 기기) 시차를 지금 기기 시각에 더해
     * 서버 기준 현재 시각을 추정한다. 확인 뒤 기기 시계를 뒤로 돌려 경과가 음수면 0으로 본다.
     */
    fun isLicensed(s: LicenseSnapshot?, nowWallMs: Long): Boolean {
        if (s == null || s.status != LicenseStatus.ACTIVE) return false
        val sinceCheck: Long = (nowWallMs - s.checkedAtWallMs).coerceAtLeast(0L)
        if (sinceCheck >= GRACE_MS) return false
        val expires: Long = s.expiresAtEpochMs ?: return true
        val serverNow: Long = nowWallMs + (s.serverTimeEpochMs - s.checkedAtWallMs)
        return expires > serverNow
    }
}
