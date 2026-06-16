package dev.ktxtget.util

import android.content.Context
import android.provider.Settings
import dev.ktxtget.BuildConfig

enum class DeviceRegistrationState {
    LICENSED,
    PREVIEW,
    DEV_OPEN,
    WRONG_DEVICE,
}

object DeviceLicense {
    fun readDeviceId(context: Context): String {
        val rawId: String? =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return normalizeDeviceId(rawId ?: "")
    }

    fun buildAllowedDeviceId(): String {
        return normalizeDeviceId(BuildConfig.ALLOWED_DEVICE_ID)
    }

    fun getRegistrationState(context: Context): DeviceRegistrationState {
        val allowedDeviceId: String = buildAllowedDeviceId()
        if (allowedDeviceId.isEmpty()) {
            return if (BuildConfig.DEBUG) {
                DeviceRegistrationState.DEV_OPEN
            } else {
                DeviceRegistrationState.PREVIEW
            }
        }
        val currentDeviceId: String = readDeviceId(context)
        if (currentDeviceId.isEmpty()) {
            return DeviceRegistrationState.WRONG_DEVICE
        }
        return if (deviceIdsMatch(allowedDeviceId, currentDeviceId)) {
            DeviceRegistrationState.LICENSED
        } else {
            DeviceRegistrationState.WRONG_DEVICE
        }
    }

    fun isLicensed(context: Context): Boolean {
        return getRegistrationState(context) == DeviceRegistrationState.LICENSED ||
            getRegistrationState(context) == DeviceRegistrationState.DEV_OPEN
    }

    fun normalizeDeviceId(rawId: String): String {
        return rawId.trim().lowercase()
    }

    fun deviceIdsMatch(allowedDeviceId: String, currentDeviceId: String): Boolean {
        val allowed: String = normalizeDeviceId(allowedDeviceId)
        val current: String = normalizeDeviceId(currentDeviceId)
        if (allowed.isEmpty() || current.isEmpty()) {
            return false
        }
        return allowed == current
    }
}
