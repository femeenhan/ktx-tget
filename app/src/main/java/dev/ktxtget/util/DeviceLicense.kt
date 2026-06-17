package dev.ktxtget.util

import android.content.Context
import android.provider.Settings
import dev.ktxtget.BuildConfig
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class DeviceRegistrationState {
    LICENSED,
    UNLICENSED,
    DEV_OPEN,
}

object DeviceLicense {
    private const val PREFS_NAME = "ktxtget_license"
    private const val KEY_LICENSE = "license_key"

    fun readDeviceId(context: Context): String {
        val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return (rawId ?: "").trim().lowercase()
    }

    internal fun generateLicenseKey(deviceId: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(deviceId.trim().lowercase().toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02X".format(it) }
            .chunked(4).joinToString("-")
    }

    fun generateLicenseKey(deviceId: String): String =
        generateLicenseKey(deviceId, BuildConfig.LICENSE_SECRET)

    fun verifyLicenseKey(deviceId: String, inputKey: String): Boolean {
        if (deviceId.isEmpty() || inputKey.isEmpty()) return false
        val expected = generateLicenseKey(deviceId).replace("-", "")
        val normalized = inputKey.trim().uppercase().replace("-", "")
        return expected == normalized
    }

    fun saveLicenseKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LICENSE, key.trim().uppercase()).apply()
    }

    fun getSavedLicenseKey(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LICENSE, null)

    fun getRegistrationState(context: Context): DeviceRegistrationState {
        if (BuildConfig.DEBUG) return DeviceRegistrationState.DEV_OPEN
        val deviceId = readDeviceId(context)
        val savedKey = getSavedLicenseKey(context) ?: return DeviceRegistrationState.UNLICENSED
        return if (verifyLicenseKey(deviceId, savedKey)) {
            DeviceRegistrationState.LICENSED
        } else {
            DeviceRegistrationState.UNLICENSED
        }
    }

    fun isLicensed(context: Context): Boolean {
        val state = getRegistrationState(context)
        return state == DeviceRegistrationState.LICENSED || state == DeviceRegistrationState.DEV_OPEN
    }
}
