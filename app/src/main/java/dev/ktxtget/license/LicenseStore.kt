package dev.ktxtget.license

import android.content.Context
import android.content.SharedPreferences

class LicenseStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): LicenseSnapshot? {
        if (prefs.contains(KEY_LEGACY_LICENSE)) {
            prefs.edit().remove(KEY_LEGACY_LICENSE).apply()
        }
        val rawStatus: String = prefs.getString(KEY_STATUS, null) ?: return null
        val status: LicenseStatus = LicenseStatus.entries.firstOrNull { it.name == rawStatus } ?: return null
        return LicenseSnapshot(
            status = status,
            expiresAtEpochMs = if (prefs.contains(KEY_EXPIRES)) prefs.getLong(KEY_EXPIRES, 0L) else null,
            serverTimeEpochMs = prefs.getLong(KEY_SERVER_TIME, 0L),
            checkedAtElapsedMs = prefs.getLong(KEY_CHECKED_ELAPSED, 0L),
            checkedAtWallMs = prefs.getLong(KEY_CHECKED_WALL, 0L),
            renewalPending = prefs.getBoolean(KEY_RENEWAL_PENDING, false),
            notice = prefs.getString(KEY_NOTICE, null),
        )
    }

    fun save(s: LicenseSnapshot) {
        prefs.edit().apply {
            putString(KEY_STATUS, s.status.name)
            if (s.expiresAtEpochMs != null) putLong(KEY_EXPIRES, s.expiresAtEpochMs) else remove(KEY_EXPIRES)
            putLong(KEY_SERVER_TIME, s.serverTimeEpochMs)
            putLong(KEY_CHECKED_ELAPSED, s.checkedAtElapsedMs)
            putLong(KEY_CHECKED_WALL, s.checkedAtWallMs)
            putBoolean(KEY_RENEWAL_PENDING, s.renewalPending)
            if (s.notice != null) putString(KEY_NOTICE, s.notice) else remove(KEY_NOTICE)
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "ktxtget_license"
        const val KEY_LEGACY_LICENSE = "license_key"
        const val KEY_STATUS = "status"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_SERVER_TIME = "server_time"
        const val KEY_CHECKED_ELAPSED = "checked_at_elapsed"
        const val KEY_CHECKED_WALL = "checked_at_wall"
        const val KEY_RENEWAL_PENDING = "renewal_pending"
        const val KEY_NOTICE = "notice"
    }
}
