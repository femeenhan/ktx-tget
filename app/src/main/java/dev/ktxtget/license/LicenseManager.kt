package dev.ktxtget.license

import android.content.Context
import dev.ktxtget.BuildConfig
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.util.DeviceLicense
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LicenseManager {
    private val api: LicenseApi = LicenseApi(BuildConfig.API_BASE_URL)
    private val _snapshot: MutableStateFlow<LicenseSnapshot?> = MutableStateFlow(null)
    val snapshot: StateFlow<LicenseSnapshot?> = _snapshot.asStateFlow()

    @Volatile
    private var initialized: Boolean = false

    private val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshing: AtomicBoolean = AtomicBoolean(false)

    /** 마지막 refresh 시도 시각(SystemClock.elapsedRealtime). 서비스가 다시 붙어도 주기를 유지하려고 여기 둔다. */
    @Volatile
    private var lastAttemptElapsedMs: Long? = null

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
        lastAttemptElapsedMs = SystemClock.elapsedRealtime()
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

    /** 기다리지 않고 refresh를 띄운다. 이미 진행 중이면 건너뛴다. */
    fun refreshInBackground(context: Context) {
        val app: Context = context.applicationContext
        if (!refreshing.compareAndSet(false, true)) return
        backgroundScope.launch {
            try {
                refresh(app)
            } finally {
                refreshing.set(false)
            }
        }
    }

    /** 마지막 시도 뒤 [intervalMs]가 지났으면(또는 한 번도 없으면) 백그라운드 refresh를 띄운다. */
    fun refreshInBackgroundIfDue(context: Context, intervalMs: Long) {
        val last: Long? = lastAttemptElapsedMs
        if (last != null && SystemClock.elapsedRealtime() - last < intervalMs) return
        refreshInBackground(context)
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
