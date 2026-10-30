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
)

data class AppConfig(
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
