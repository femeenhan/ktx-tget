package dev.ktxtget.license

import android.os.SystemClock
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LicenseApi(baseUrl: String) {
    private val base: String = baseUrl.trimEnd('/')

    suspend fun request(deviceId: String, depositor: String, plan: String, versionCode: Int): LicenseSnapshot {
        val body: JSONObject = JSONObject()
            .put("deviceId", deviceId)
            .put("depositor", depositor)
            .put("plan", plan)
            .put("versionCode", versionCode)
            .put("agreed", true)
        val (code, text) = call("POST", "/api/device/request", body.toString())
        if (code !in 200..299) throw LicenseApiException(code, errorMessage(code, text))
        return parsing { parseSnapshot(text) }
    }

    suspend fun status(deviceId: String, versionCode: Int): LicenseSnapshot {
        val query = "deviceId=${enc(deviceId)}&versionCode=$versionCode"
        val (code, text) = call("GET", "/api/device/status?$query", null)
        if (code !in 200..299) throw LicenseApiException(code, errorMessage(code, text))
        return parsing { parseSnapshot(text) }
    }

    suspend fun config(): AppConfig {
        val (code, text) = call("GET", "/api/config", null)
        if (code !in 200..299) throw LicenseApiException(code, errorMessage(code, text))
        return parsing {
            val json = JSONObject(text)
            val prices: JSONObject = json.getJSONObject("prices")
            AppConfig(
                priceLifetime = prices.getLong("lifetime"),
                priceMonth = prices.getLong("month"),
                priceYear = prices.getLong("year"),
                bankAccount = json.getString("bankAccount"),
                openchatUrl = json.getString("openchatUrl"),
                termsUrl = json.getString("termsUrl"),
            )
        }
    }

    suspend fun version(): VersionInfo? {
        val (code, text) = call("GET", "/api/version", null)
        if (code == 404) return null
        if (code !in 200..299) throw LicenseApiException(code, errorMessage(code, text))
        return parsing {
            val json = JSONObject(text)
            VersionInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                url = json.getString("url"),
            )
        }
    }

    private suspend fun call(method: String, path: String, jsonBody: String?): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val conn = URL(base + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = TIMEOUT_MS
                conn.readTimeout = TIMEOUT_MS
                conn.setRequestProperty("Accept", "application/json")
                if (jsonBody != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                }
                val code: Int = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text: String = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                code to text
            } finally {
                conn.disconnect()
            }
        }

    private fun errorMessage(code: Int, text: String): String =
        try {
            val json = JSONObject(text)
            if (json.isNull("error")) "HTTP $code" else json.optString("error").ifEmpty { "HTTP $code" }
        } catch (_: Exception) {
            "HTTP $code"
        }

    /** 응답 형식이 어긋나면(JSONException, DateTimeParseException 등) IOException으로 바꿔 던진다. */
    private inline fun <T> parsing(block: () -> T): T =
        try {
            block()
        } catch (err: IOException) {
            throw err
        } catch (err: Exception) {
            throw IOException("invalid response: ${err.message ?: err.javaClass.simpleName}", err)
        }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    companion object {
        private const val TIMEOUT_MS: Int = 8000

        internal fun parseSnapshot(text: String): LicenseSnapshot {
            val json = JSONObject(text)
            val status: LicenseStatus = when (val raw = json.getString("status")) {
                "none" -> LicenseStatus.NONE
                "pending" -> LicenseStatus.PENDING
                "active" -> LicenseStatus.ACTIVE
                "expired" -> LicenseStatus.EXPIRED
                "revoked" -> LicenseStatus.REVOKED
                else -> throw IOException("unknown status: $raw")
            }
            val expiresAt: Long? =
                if (json.isNull("expiresAt")) null else Instant.parse(json.getString("expiresAt")).toEpochMilli()
            return LicenseSnapshot(
                status = status,
                expiresAtEpochMs = expiresAt,
                serverTimeEpochMs = Instant.parse(json.getString("serverTime")).toEpochMilli(),
                checkedAtElapsedMs = SystemClock.elapsedRealtime(),
                checkedAtWallMs = System.currentTimeMillis(),
                renewalPending = json.optBoolean("renewalPending", false),
                notice = if (json.isNull("notice")) null else json.getString("notice"),
            )
        }
    }
}
