package dev.ktxtget.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import dev.ktxtget.R
import dev.ktxtget.service.KtxAccessibilityService

/**
 * 접근성 권한을 켜는 설정 화면으로 보내고, 켜져 있는지 읽는다.
 *
 * 설정 앱의 접근성 목록은 어느 항목을 눌러야 하는지 일반 사용자가 알기 어려워,
 * 설정 앱이 알아듣는 숨은 extra(`:settings:fragment_args_key`)로 우리 서비스의 토글 화면을 바로 연다.
 * AOSP·픽셀은 이 extra를 읽어 바로 토글 화면을 연다. 삼성 One UI(8 기준)는 무시하고 접근성 목록만 열고,
 * 토글 화면용 공개 액션(`ACCESSIBILITY_DETAILS_SETTINGS`)은 시스템 권한이 있어야 해서 쓸 수 없다.
 * 그래서 어느 기기든 목록이 열렸을 때 보이도록 「설치된 앱 → ktx-tget」 토스트를 같이 띄운다.
 */
object AccessibilitySettings {
    private const val FRAGMENT_ARGS_KEY = ":settings:fragment_args_key"
    private const val SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

    fun serviceComponent(context: Context): ComponentName =
        ComponentName(context, KtxAccessibilityService::class.java)

    fun isServiceEnabled(context: Context): Boolean {
        val enabled: String? = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        val component: ComponentName = serviceComponent(context)
        return isEnabledIn(enabled, component.flattenToString(), component.flattenToShortString())
    }

    /**
     * `enabled_accessibility_services` 값(콜론 구분)에 우리 서비스가 들어 있는지.
     * [long]은 `pkg/pkg.Cls`, [short]는 `pkg/.Cls` 표기. 기기마다 둘 중 하나로 저장한다.
     */
    fun isEnabledIn(enabledList: String?, long: String, short: String): Boolean {
        if (enabledList.isNullOrBlank()) return false
        return enabledList.split(':').any { entry: String ->
            entry.equals(long, ignoreCase = true) || entry.equals(short, ignoreCase = true)
        }
    }

    /** 우리 서비스의 켜기/끄기 화면을 바로 연다. 안 되면 접근성 목록으로. 어느 쪽이든 다음 단계를 토스트로 알린다. */
    fun openServiceSettings(context: Context) {
        Toast.makeText(context, R.string.accessibility_toast, Toast.LENGTH_LONG).show()
        val flat: String = serviceComponent(context).flattenToString()
        val direct = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(FRAGMENT_ARGS_KEY, flat)
            putExtra(SHOW_FRAGMENT_ARGS, Bundle().apply { putString(FRAGMENT_ARGS_KEY, flat) })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(direct) }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    /**
     * 이 앱의 「앱 정보」 화면. Android 13+에서 브라우저로 설치한 앱은 여기서 ⋮ → 「제한된 설정 허용」을
     * 먼저 눌러야 접근성 스위치가 눌린다.
     */
    fun openAppInfo(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
