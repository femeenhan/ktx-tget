package dev.ktxtget.license

import java.io.IOException

enum class LicenseStatus { NONE, PENDING, ACTIVE, EXPIRED, REVOKED }

data class LicenseSnapshot(
    val status: LicenseStatus,
    val expiresAtEpochMs: Long?,
    val serverTimeEpochMs: Long,
    /** SystemClock.elapsedRealtime() at response time. */
    val checkedAtElapsedMs: Long,
    /** System.currentTimeMillis() at response time. */
    val checkedAtWallMs: Long,
    val renewalPending: Boolean,
    val notice: String?,
    /** 입금 확인 전 임시 이용 중. status는 ACTIVE, expiresAt은 임시 이용이 끝나는 시각. 서버 0.3.4+가 보낸다. */
    val trial: Boolean = false,
)

data class AppConfig(
    val priceDay: Long,
    val priceLifetime: Long,
    val priceMonth: Long,
    val priceYear: Long,
    val bankAccount: String,
    val openchatUrl: String,
    /** Server path such as "/terms"; prefix with the base URL when opening. */
    val termsUrl: String,
)

data class VersionInfo(val versionCode: Int, val versionName: String, val url: String)

class LicenseApiException(val code: Int, message: String) : IOException(message)
