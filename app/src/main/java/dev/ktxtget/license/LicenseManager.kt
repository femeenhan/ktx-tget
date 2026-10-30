package dev.ktxtget.license

import android.content.Context
import dev.ktxtget.BuildConfig
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.util.DeviceLicense
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object LicenseManager {
    private val api: LicenseApi = LicenseApi(BuildConfig.API_BASE_URL)
    private val _snapshot: MutableStateFlow<LicenseSnapshot?> = MutableStateFlow(null)
    val snapshot: StateFlow<LicenseSnapshot?> = _snapshot.asStateFlow()

    @Volatile
    private var initialized: Boolean = false

    /** 저장된 스냅샷으로 [snapshot]을 한 번 채운다. 여러 번 불러도 된다. */
    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            if (_snapshot.value == null) _snapshot.value = LicenseStore(context).load()
            initialized = true
        }
    }

    /** 서버에서 상태를 받아 저장한다. 실패하면 저장값을 그대로 두고 null을 돌려준다. */
    suspend fun refresh(context: Context): LicenseSnapshot? {
        init(context)
        return try {
            val s: LicenseSnapshot = withContext(Dispatchers.IO) {
                api.status(DeviceLicense.readDeviceId(context), BuildConfig.VERSION_CODE)
            }
            publish(context, s)
            s
        } catch (err: CancellationException) {
            throw err
        } catch (err: Exception) {
            MacroRuntimeLog.emit("error", "라이선스 확인 실패: ${err.message ?: err.javaClass.simpleName}")
            null
        }
    }

    /** 사용 신청을 보낸다. 실패하면 예외를 그대로 던진다. */
    suspend fun sendRequest(context: Context, depositor: String, plan: String): LicenseSnapshot {
        init(context)
        val s: LicenseSnapshot = withContext(Dispatchers.IO) {
            api.request(DeviceLicense.readDeviceId(context), depositor, plan, BuildConfig.VERSION_CODE)
        }
        publish(context, s)
        return s
    }

    suspend fun fetchConfig(): AppConfig = api.config()

    suspend fun fetchVersion(): VersionInfo? = api.version()

    private suspend fun publish(context: Context, s: LicenseSnapshot) {
        withContext(Dispatchers.IO) { LicenseStore(context).save(s) }
        _snapshot.value = s
    }
}
