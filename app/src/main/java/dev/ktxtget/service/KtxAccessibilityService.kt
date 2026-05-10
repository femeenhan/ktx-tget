package dev.ktxtget.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.accessibility.KorailUiSnapshotNotes
import dev.ktxtget.accessibility.ParsedTrainRow
import dev.ktxtget.accessibility.TrainRowTreeParser
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.domain.MacroFilterEngine
import dev.ktxtget.domain.PreviousRowCells
import dev.ktxtget.domain.PriceTransitionDetector
import dev.ktxtget.domain.SeatColumn
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.TrainRow
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.util.NodeFinder
import dev.ktxtget.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class KtxAccessibilityService : AccessibilityService() {
    private val repository: MacroPreferencesRepository by lazy {
        MacroPreferencesRepository(applicationContext)
    }
    private val parser: TrainRowTreeParser = TrainRowTreeParser()
    private val serviceJob: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private var macroLoopJob: Job? = null
    private val previousCells: MutableMap<String, PreviousRowCells> = mutableMapOf()

    @Volatile
    private var waitingForReserve: Boolean = false
    private val ticketConfirmationHandled: AtomicBoolean = AtomicBoolean(false)
    private val reserveInteractionMutex: Mutex = Mutex()
    @Volatile
    private var lastReserveAssistUptimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "KtxAccessibilityService connected")
        runtimeLog("service", "접근성 서비스 연결됨")
        macroLoopJob = scope.launch {
            var prevMacroEnabled: Boolean = false
            while (isActive) {
                try {
                    val enabledNow: Boolean = repository.readMacroEnabled()
                    if (enabledNow && !prevMacroEnabled) {
                        ticketConfirmationHandled.set(false)
                    }
                    prevMacroEnabled = enabledNow
                    if (!enabledNow) {
                        delay(POLL_WHEN_DISABLED_MS)
                        continue
                    }
                    runMacroCycle()
                } catch (err: CancellationException) {
                    throw err
                } catch (err: Exception) {
                    Log.e(TAG, "macro cycle failed", err)
                    runtimeLog("error", err.message ?: err.javaClass.simpleName)
                }
                val intervalMs: Long =
                    repository.readRefreshIntervalMs().coerceIn(
                        MacroPreferencesRepository.MIN_REFRESH_MS,
                        MacroPreferencesRepository.MAX_REFRESH_MS,
                    )
                delay(intervalMs)
            }
        }
    }

    override fun onDestroy() {
        macroLoopJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName: CharSequence? = event.packageName
        if (!KorailUiSnapshotNotes.isTargetPackage(packageName)) {
            return
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(
                TAG,
                "eventType=${eventTypeToString(event.eventType)} package=$packageName",
            )
        }
        scope.launch {
            val root: AccessibilityNodeInfo = rootInActiveWindow ?: return@launch
            try {
                if (!isKorailActiveWindow(root)) {
                    return@launch
                }
                if (hasTicketConfirmation(root)) {
                    val macroOn: Boolean = repository.readMacroEnabled()
                    val reservePending: Boolean = waitingForReserve
                    if (!macroOn && !reservePending) {
                        return@launch
                    }
                    handleTicketConfirmationDetected("event")
                    return@launch
                }
                if (!repository.readMacroEnabled()) {
                    return@launch
                }
                val eventSettings: MacroSettings = repository.readMacroSettings()
                if (eventSettings.autoConfirmIntermediateStopDialog) {
                    tryAutoConfirmIntermediateStopDialog(root)
                }
                if (waitingForReserve && repository.readMacroEnabled()) {
                    val eventType: Int = event.eventType
                    if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                        eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    ) {
                        val nowMs: Long = SystemClock.uptimeMillis()
                        if (nowMs - lastReserveAssistUptimeMs >= RESERVE_EVENT_ASSIST_MIN_GAP_MS) {
                            lastReserveAssistUptimeMs = nowMs
                            scope.launch {
                                tryPerformReserveTap(eventSettings)
                            }
                        }
                    }
                }
            } finally {
                recycleAccessibilityNode(root)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt")
    }

    private suspend fun runMacroCycle() {
        if (!repository.readMacroEnabled()) {
            return
        }
        val settings = repository.readMacroSettings()
        var root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            Log.w(TAG, "macro tick skipped: rootInActiveWindow is null")
            runtimeLog("tick", "root 없음 — 스킵")
            return
        }
        val windowPackage: String =
            if (Build.VERSION.SDK_INT >= 30) {
                root.packageName?.toString() ?: "unknown"
            } else {
                "n/a"
            }
        Log.i(
            TAG,
            "macro tick window=$windowPackage (expect ${KorailUiSnapshotNotes.PACKAGE_KORAIL_TALK} or " +
                "${KorailUiSnapshotNotes.PACKAGE_KORAIL_MOBILE} on train list)",
        )
        val onKorail: Boolean = isKorailActiveWindow(root)
        if (!onKorail) {
            if (waitingForReserve) {
                Log.d(TAG, "skip: foreground=$windowPackage but waitingForReserve (return to 코레일톡 for 예매)")
                runtimeLog("skip", "코레일 포그라운드 아님 ($windowPackage), 예매 대기 중")
            } else {
                Log.d(TAG, "skip: foreground=$windowPackage — 매크로는 코레일 앱 포그라운드에서만 동작")
                runtimeLog("skip", "코레일 포그라운드 아님 ($windowPackage)")
            }
            recycleAccessibilityNode(root)
            return
        }
        try {
            if (hasTicketConfirmation(root)) {
                handleTicketConfirmationDetected("cycle_pre_refresh")
                return
            }
            if (dismissIntermediateStopDialogIfRequested(settings, root)) {
                return
            }
            if (waitingForReserve) {
                tryPerformReserveTap(settings)
                return
            }
            val refreshed: Boolean = performRefreshClick(root)
            if (!refreshed) {
                Log.d(TAG, "Refresh control not found or click failed")
                runtimeLog("refresh", "새로고침 실패 또는 컨트롤 없음")
            } else {
                runtimeLog("refresh", "새로고침 탭")
            }
        } finally {
            recycleAccessibilityNode(root)
        }
        delay(MIN_AFTER_REFRESH_MS)
        val postRefreshGateRoot: AccessibilityNodeInfo = rootInActiveWindow ?: return
        try {
            if (!isKorailActiveWindow(postRefreshGateRoot)) {
                Log.d(TAG, "parse phase skip: left Korail")
                runtimeLog("parse", "새로고침 후 코레일 아님 — 파싱 스킵")
                return
            }
            if (hasTicketConfirmation(postRefreshGateRoot)) {
                handleTicketConfirmationDetected("cycle_post_refresh")
                return
            }
            if (dismissIntermediateStopDialogIfRequested(settings, postRefreshGateRoot)) {
                return
            }
        } finally {
            recycleAccessibilityNode(postRefreshGateRoot)
        }
        val session: ParseSession = parseTrainRowsWithAdaptiveRetries()
        if (session.rows.isEmpty()) {
            Log.d(TAG, "Parsed zero train rows after retries")
            runtimeLog("parse", "열차 행 0개 (재시도 후)")
        } else {
            Log.i(TAG, "Parsed ${session.rows.size} train row(s) keys=${session.rows.map { pr -> pr.row.stableKey }}")
            runtimeLog("parse", "열차 행 ${session.rows.size}개 keys=${session.rows.map { pr -> pr.row.stableKey }}")
        }
        val snapshotForClicks: AccessibilityNodeInfo = session.snapshotRoot ?: return
        try {
            if (!isKorailActiveWindow(snapshotForClicks)) {
                return
            }
            if (hasTicketConfirmation(snapshotForClicks)) {
                handleTicketConfirmationDetected("cycle_snapshot")
                return
            }
            if (dismissIntermediateStopDialogIfRequested(settings, snapshotForClicks)) {
                return
            }
            val filteredRows: List<TrainRow> = MacroFilterEngine.filter(
                session.rows.map { pr: ParsedTrainRow -> pr.row },
                settings,
            )
            for (row: TrainRow in filteredRows) {
                val prev: PreviousRowCells? = previousCells[row.stableKey]
                val col: SeatColumn? = PriceTransitionDetector.findSoldOutToPriceColumn(
                    prev,
                    row,
                    settings.seatClickPreference,
                )
                if (col == null) {
                    continue
                }
                val freshClickRoot: AccessibilityNodeInfo = rootInActiveWindow ?: break
                try {
                    if (!isKorailActiveWindow(freshClickRoot)) {
                        break
                    }
                    if (hasTicketConfirmation(freshClickRoot)) {
                        handleTicketConfirmationDetected("cycle_price_click")
                        return
                    }
                    if (dismissIntermediateStopDialogIfRequested(settings, freshClickRoot)) {
                        return
                    }
                    val resolved: AccessibilityNodeInfo? =
                        parser.findPriceClickTarget(freshClickRoot, row.stableKey, col)
                    if (resolved == null) {
                        Log.w(TAG, "No fresh click target for transition key=${row.stableKey} col=$col")
                        continue
                    }
                    if (performClick(resolved)) {
                        Log.i(TAG, "Clicked price cell key=${row.stableKey} col=$col")
                        runtimeLog("price", "가격 탭 key=${row.stableKey} col=$col")
                        waitingForReserve = true
                        break
                    }
                    Log.w(TAG, "performAction failed for price key=${row.stableKey}")
                } finally {
                    recycleAccessibilityNode(freshClickRoot)
                }
            }
            for (pr: ParsedTrainRow in session.rows) {
                val r: TrainRow = pr.row
                previousCells[r.stableKey] = PreviousRowCells(
                    r.generalSeatCellText,
                    r.firstClassSeatCellText,
                )
            }
        } finally {
            recycleAccessibilityNode(snapshotForClicks)
        }
        if (!waitingForReserve) {
            return
        }
        pollReserveButtonWithRetries(settings)
    }

    private suspend fun handleTicketConfirmationDetected(source: String): Boolean {
        if (!ticketConfirmationHandled.compareAndSet(false, true)) {
            return false
        }
        val settings: MacroSettings = repository.readMacroSettings()
        NotificationHelper.triggerUserAlert(applicationContext, settings.userAlertsEnabled)
        waitingForReserve = false
        repository.setMacroEnabled(false)
        runtimeLog("ticket", "승차권 정보 확인 ($source) — 매크로 중지")
        Log.i(TAG, "Ticket confirmation handled ($source)")
        return true
    }

    /**
     * 코레일 예매 바텀시트에서 「예매」 탭. 이벤트 보조와 매크로 루프가 동시에 돌지 않도록 [reserveInteractionMutex] 사용.
     */
    private suspend fun tryPerformReserveTap(settings: MacroSettings): Boolean {
        return reserveInteractionMutex.withLock {
            if (!repository.readMacroEnabled() || !waitingForReserve) {
                return@withLock false
            }
            val tapRoot: AccessibilityNodeInfo = rootInActiveWindow ?: return@withLock false
            try {
                if (!isKorailActiveWindow(tapRoot)) {
                    return@withLock false
                }
                if (hasTicketConfirmation(tapRoot)) {
                    handleTicketConfirmationDetected("reserve_tap")
                    return@withLock true
                }
                if (dismissIntermediateStopDialogIfRequested(settings, tapRoot)) {
                    return@withLock false
                }
                val target: AccessibilityNodeInfo? = NodeFinder.findReserveButton(tapRoot)
                if (target == null) {
                    return@withLock false
                }
                val ok: Boolean = performClick(target)
                if (ok) {
                    waitingForReserve = false
                    Log.i(TAG, "Clicked reserve")
                    runtimeLog("reserve", "예매 버튼 탭 (바텀시트)")
                }
                ok
            } finally {
                recycleAccessibilityNode(tapRoot)
            }
        }
    }

    private suspend fun pollReserveButtonWithRetries(settings: MacroSettings) {
        val deadlineMs: Long = SystemClock.elapsedRealtime() + RESERVE_POLL_BUDGET_MS
        var attempt: Int = 0
        while (waitingForReserve && SystemClock.elapsedRealtime() < deadlineMs) {
            if (!repository.readMacroEnabled()) {
                break
            }
            val finished: Boolean = tryPerformReserveTap(settings)
            if (finished) {
                return
            }
            attempt++
            val gapMs: Long =
                if (attempt <= 8) {
                    RESERVE_POLL_GAP_QUICK_MS
                } else {
                    RESERVE_POLL_GAP_SLOW_MS
                }
            delay(gapMs)
        }
        if (waitingForReserve) {
            runtimeLog("reserve", "예매 버튼 미발견 (바텀시트 타임아웃)")
        }
    }

    private suspend fun parseTrainRowsWithAdaptiveRetries(): ParseSession {
        val deadlineMs: Long = SystemClock.elapsedRealtime() + POST_REFRESH_PARSE_BUDGET_MS
        var attemptIndex: Int = 0
        while (SystemClock.elapsedRealtime() < deadlineMs) {
            val snapshot: AccessibilityNodeInfo = rootInActiveWindow ?: return ParseSession(emptyList(), null)
            if (!isKorailActiveWindow(snapshot)) {
                recycleAccessibilityNode(snapshot)
                return ParseSession(emptyList(), null)
            }
            val parsed: List<ParsedTrainRow> = parser.parse(snapshot)
            if (parsed.isNotEmpty()) {
                return ParseSession(parsed, snapshot)
            }
            recycleAccessibilityNode(snapshot)
            attemptIndex++
            val gapMs: Long =
                if (attemptIndex <= 4) {
                    PARSE_GAP_QUICK_MS
                } else {
                    PARSE_GAP_SLOW_MS
                }
            delay(gapMs)
        }
        return ParseSession(emptyList(), null)
    }

    /**
     * [rows] correspond to [snapshotRoot]; caller recycles [snapshotRoot] after use.
     */
    private data class ParseSession(
        val rows: List<ParsedTrainRow>,
        val snapshotRoot: AccessibilityNodeInfo?,
    )

    private fun isKorailActiveWindow(root: AccessibilityNodeInfo): Boolean {
        if (Build.VERSION.SDK_INT < 30) {
            return true
        }
        return KorailUiSnapshotNotes.isTargetPackage(root.packageName)
    }

    private suspend fun performRefreshClick(root: AccessibilityNodeInfo): Boolean {
        val target: AccessibilityNodeInfo? = NodeFinder.findClickableWithTextOrId(
            root,
            KorailUiSnapshotNotes.REFRESH_TEXT_HINT,
            KorailUiSnapshotNotes.VIEW_ID_REFRESH,
        )
        if (target == null) {
            return false
        }
        return performClick(target)
    }

    private suspend fun performClick(node: AccessibilityNodeInfo): Boolean {
        return withContext(Dispatchers.Main) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun hasTicketConfirmation(root: AccessibilityNodeInfo): Boolean {
        val nodes: List<AccessibilityNodeInfo> =
            root.findAccessibilityNodeInfosByText(KorailUiSnapshotNotes.TICKET_CONFIRMATION_TITLE)
        try {
            return nodes.isNotEmpty()
        } finally {
            recycleNodes(nodes)
        }
    }

    private suspend fun dismissIntermediateStopDialogIfRequested(
        settings: MacroSettings,
        root: AccessibilityNodeInfo,
    ): Boolean {
        if (!settings.autoConfirmIntermediateStopDialog) {
            return false
        }
        return tryAutoConfirmIntermediateStopDialog(root)
    }

    /**
     * Locates 「확인」 near body text matching [KorailUiSnapshotNotes.INTERMEDIATE_STOP_DIALOG_TEXT_MARKERS].
     * Does not run when [hasTicketConfirmation] is true.
     */
    private fun findIntermediateStopConfirmTarget(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (hasTicketConfirmation(root)) {
            return null
        }
        for (marker: String in KorailUiSnapshotNotes.INTERMEDIATE_STOP_DIALOG_TEXT_MARKERS) {
            val anchor: AccessibilityNodeInfo? =
                NodeFinder.findFirstPreorderContainingText(root, marker)
            if (anchor == null) {
                continue
            }
            try {
                val target: AccessibilityNodeInfo? =
                    findConfirmClickableNearAnchor(anchor, KorailUiSnapshotNotes.INTERMEDIATE_STOP_CONFIRM_LABEL)
                if (target != null) {
                    return target
                }
            } finally {
                if (anchor !== root) {
                    recycleAccessibilityNode(anchor)
                }
            }
        }
        return null
    }

    private fun findConfirmClickableNearAnchor(
        anchor: AccessibilityNodeInfo,
        label: String,
    ): AccessibilityNodeInfo? {
        var hops: Int = 0
        var node: AccessibilityNodeInfo? = anchor.parent
        while (node != null && hops < INTERMEDIATE_STOP_DIALOG_MAX_PARENT_HOPS) {
            val confirm: AccessibilityNodeInfo? = NodeFinder.findClickableByLabel(node, label)
            if (confirm != null) {
                node.recycle()
                return confirm
            }
            val parent: AccessibilityNodeInfo? = node.parent
            node.recycle()
            node = parent
            hops++
        }
        node?.recycle()
        return null
    }

    private suspend fun tryAutoConfirmIntermediateStopDialog(root: AccessibilityNodeInfo): Boolean {
        val target: AccessibilityNodeInfo? = findIntermediateStopConfirmTarget(root)
        if (target == null) {
            return false
        }
        try {
            val ok: Boolean = performClick(target)
            if (ok) {
                runtimeLog("dialog", "정차 안내 확인 자동 탭")
                Log.i(TAG, "Auto-confirmed intermediate stop dialog")
            }
            return ok
        } finally {
            recycleAccessibilityNode(target)
        }
    }

    private fun recycleNodes(nodes: List<AccessibilityNodeInfo>) {
        val size: Int = nodes.size
        for (i in 0 until size) {
            recycleAccessibilityNode(nodes[i])
        }
    }

    @Suppress("DEPRECATION")
    private fun recycleAccessibilityNode(node: AccessibilityNodeInfo) {
        node.recycle()
    }

    private fun eventTypeToString(eventType: Int): String {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            else -> eventType.toString()
        }
    }

    private fun runtimeLog(kind: String, detail: String) {
        try {
            MacroRuntimeLog.emit(kind, detail)
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val TAG: String = "KtxA11y"
        private const val POLL_WHEN_DISABLED_MS: Long = 400L
        private const val MIN_AFTER_REFRESH_MS: Long = 200L
        private const val POST_REFRESH_PARSE_BUDGET_MS: Long = 1300L
        private const val PARSE_GAP_QUICK_MS: Long = 90L
        private const val PARSE_GAP_SLOW_MS: Long = 220L
        private const val RESERVE_POLL_BUDGET_MS: Long = 2800L
        private const val RESERVE_POLL_GAP_QUICK_MS: Long = 75L
        private const val RESERVE_POLL_GAP_SLOW_MS: Long = 160L
        private const val RESERVE_EVENT_ASSIST_MIN_GAP_MS: Long = 85L
        private const val INTERMEDIATE_STOP_DIALOG_MAX_PARENT_HOPS: Int = 12
    }
}
