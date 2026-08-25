package dev.ktxtget.sensing

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresApi
import dev.ktxtget.domain.PixelRect
import dev.ktxtget.domain.SoldOutPixelClassifier
import dev.ktxtget.domain.SoldOutSignal
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 좌석 상태를 읽는 방법. 지금은 화면 픽셀뿐이지만, 코레일톡이 목록 접근성 라벨을 되살리면
 * 트리 기반 구현으로 갈아끼울 수 있도록 분리해 둔다.
 */
interface TrainListSensor {
    /** [rows] 각각의 매진 여부. 읽지 못하면 null(이번 주기는 건너뛴다). */
    suspend fun readSoldOut(rows: List<PixelRect>): List<SoldOutSignal>?
}

/**
 * [AccessibilityService.takeScreenshot]으로 화면을 떠서 매진 라벨을 픽셀로 읽는다.
 *
 * 목록 행이 접근성 트리에 텍스트를 노출하지 않아 선택한 방법이다. 행의 **좌표**는 트리에서
 * 그대로 얻으므로([dev.ktxtget.accessibility.TrainRowLocator]) 픽셀은 좌석 칸만 보면 된다.
 *
 * ## 메모리
 * 화면 전체를 `IntArray`로 뜨면 1080x2640 기준 한 번에 11MB고, 한 주기에 두 번(합의 판독)
 * 3초마다 반복하면 자바 힙 256MB 한도를 금방 넘긴다 — 실기에서 실제로 OOM 크래시가 났다.
 * 그래서 **행마다 값 컬럼 좁은 띠만** `getPixels`로 읽고, 그 버퍼도 재사용한다(행당 약 220KB).
 * 비트맵 픽셀은 네이티브 영역이라 자바 힙과 무관하고, 다 쓰는 즉시 recycle 한다.
 *
 * API 30 미만에서는 캡처 API가 없어 항상 null을 돌려준다.
 */
class ScreenshotTrainListSensor(
    private val service: AccessibilityService,
) : TrainListSensor {

    /**
     * 캡처 콜백과 픽셀 읽기를 메인 스레드에서 하지 않기 위한 실행기.
     * 메인에서 돌리면 화면이 끊기고, 여기서 난 예외가 앱 전체를 죽인다.
     */
    private val callbackExecutor: Executor = Dispatchers.Default.asExecutor()

    /** 행 띠를 읽을 때 재사용하는 버퍼. 매크로 루프에서 순차적으로만 쓰인다. */
    private var stripBuffer: IntArray = IntArray(0)

    override suspend fun readSoldOut(rows: List<PixelRect>): List<SoldOutSignal>? {
        if (rows.isEmpty()) {
            return emptyList()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }
        val bitmap: Bitmap = capture() ?: return null
        try {
            if (!coversAllRows(bitmap, rows)) {
                return null
            }
            return rows.map { row: PixelRect -> classifyRow(bitmap, row) }
        } catch (err: Throwable) {
            Log.e(TAG, "pixel read failed", err)
            return null
        } finally {
            bitmap.recycle()
        }
    }

    private fun coversAllRows(bitmap: Bitmap, rows: List<PixelRect>): Boolean {
        val bottom: Int = rows.maxOf { r: PixelRect -> r.bottom }
        val right: Int = rows.maxOf { r: PixelRect -> r.right }
        if (bottom <= bitmap.height && right <= bitmap.width) {
            return true
        }
        // 캡처한 디스플레이와 노드 좌표계가 다르면 엉뚱한 곳을 읽게 되므로 이번 주기는 버린다.
        Log.w(
            TAG,
            "screenshot ${bitmap.width}x${bitmap.height} does not cover rows " +
                "(right=$right bottom=$bottom)",
        )
        return false
    }

    private fun classifyRow(bitmap: Bitmap, row: PixelRect): SoldOutSignal {
        val strip: PixelRect = SoldOutPixelClassifier.stripRect(row, bitmap.width, bitmap.height)
        if (strip.width == 0 || strip.height == 0) {
            return SoldOutSignal(generalSoldOut = false, premiumSoldOut = false)
        }
        val needed: Int = strip.width * strip.height
        if (stripBuffer.size < needed) {
            stripBuffer = IntArray(needed)
        }
        bitmap.getPixels(stripBuffer, 0, strip.width, strip.left, strip.top, strip.width, strip.height)
        return SoldOutPixelClassifier.classifyStrip(
            stripBuffer,
            strip.width,
            strip.height,
            SoldOutPixelClassifier.splitYInStrip(row, strip),
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun capture(): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            try {
                service.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    callbackExecutor,
                    object : AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                            continuation.resume(toReadableBitmap(result))
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "takeScreenshot failed: $errorCode")
                            continuation.resume(null)
                        }
                    },
                )
            } catch (err: Throwable) {
                Log.e(TAG, "takeScreenshot threw", err)
                continuation.resume(null)
            }
        }

    /**
     * 하드웨어 버퍼 비트맵은 [Bitmap.getPixels]를 못 쓰므로 ARGB_8888로 한 번 복사한다.
     * 이 픽셀은 네이티브 영역이라 자바 힙을 쓰지 않지만, 호출부가 반드시 recycle 해야 한다.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun toReadableBitmap(result: AccessibilityService.ScreenshotResult): Bitmap? {
        val buffer: HardwareBuffer = result.hardwareBuffer
        var wrapped: Bitmap? = null
        try {
            wrapped = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) ?: return null
            return wrapped.copy(Bitmap.Config.ARGB_8888, false)
        } catch (err: Throwable) {
            Log.e(TAG, "screenshot conversion failed", err)
            return null
        } finally {
            wrapped?.recycle()
            buffer.close()
        }
    }

    private companion object {
        const val TAG: String = "KtxSensor"
    }
}
