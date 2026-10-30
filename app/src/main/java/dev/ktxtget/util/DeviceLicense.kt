package dev.ktxtget.util

import android.content.Context
import android.provider.Settings
import dev.ktxtget.BuildConfig
import dev.ktxtget.license.LicenseDecision
import dev.ktxtget.license.LicenseStore

object DeviceLicense {
    fun readDeviceId(context: Context): String {
        val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return (rawId ?: "").trim().lowercase()
    }

    fun isLicensed(context: Context): Boolean =
        BuildConfig.DEBUG || LicenseDecision.isLicensed(LicenseStore(context).load(), System.currentTimeMillis())
}
