package dev.ktxtget.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.accessibility.BookingSheetReader
import dev.ktxtget.accessibility.KorailUiSnapshotNotes
import dev.ktxtget.accessibility.TrainRowLocator
import dev.ktxtget.data.MacroPreferencesRepository
import dev.ktxtget.domain.BookingSheet
import dev.ktxtget.domain.InitialOpenSweep
import dev.ktxtget.domain.KorailLabels
import dev.ktxtget.domain.MacroFilterEngine
import dev.ktxtget.domain.MacroSettings
import dev.ktxtget.domain.PixelRect
import dev.ktxtget.domain.SeatOpening
import dev.ktxtget.domain.SoldOutSignal
import dev.ktxtget.domain.SoldOutTransitionTracker
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.sensing.ScreenshotTrainListSensor
import dev.ktxtget.sensing.TrainListSensor
import dev.ktxtget.util.AccessibilityTreeDump
import dev.ktxtget.util.DeviceLicense
import dev.ktxtget.util.NodeFinder
import dev.ktxtget.util.NotificationHelper
import java.util.concurrent.atomic.AtomicBoolean
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

/**
 * 매크로 본체.
 *
 * ## 동작 흐름 (코레일톡 7.x 기준)
 * 1. 앱바의 **새로고침**을 주기적으로 탭한다.
 * 2. 화면을 캡처해 각 행의 좌석 칸이 **매진인지**만 픽셀로 판정한다.
 *    목록 행이 접근성 트리에 텍스트를 전혀 노출하지 않아 이 방법을 쓴다 —
 *    [KorailUiSnapshotNotes] 주석 참고.
 * 3. 「매진 → 매진 아님」 전이가 잡히면 그 행을 탭해 **바텀시트**를 연다.
 * 4. 시트는 텍스트가 정상 노출되므로, 여기서 열차번호·좌석 종류를 읽어 2차 필터를 적용한다.
 *    걸리면 시트를 닫고(손잡이 탭 — BACK은 코레일 홈으로 튕긴다), 통과하면 등급 탭을 맞춘 뒤 **「바로 예매」** 를 탭한다.
 * 5. 「승차권 정보 확인」 화면에 닿으면 알림을 띄우고 매크로를 멈춘다. 결제는 사용자가 한다.
 *
 * 픽셀 판정은 매진 여부까지만 구분하므로 3번은 "뭔가 풀렸다"는 신호일 뿐이고,
 * **살지 말지는 전부 4번에서 시트 텍스트로 결정한다.**
 *
 * AccessibilityNodeInfo.recycle()은 더 이상 필요하지 않아(API 33부터 no-op, 이하에서도 GC 대상)
 * 이 클래스는 노드를 재활용하지 않는다.
 */
class KtxAccessibilityService : AccessibilityService() {

    private val repository: MacroPreferencesRepository by lazy {
        MacroPreferencesRepository(applicationContext)
    }
    private val sensor: TrainListSensor by lazy { ScreenshotTrainListSensor(this) }
    private val tracker: SoldOutTransitionTracker = SoldOutTransitionTracker()

    /** 켠 직후 첫 판독에서 이미 매진이 아닌 칸을 한 번 확인할지. 시작할 때 켜고 첫 판독에서 끈다. */
    private var initialCheckPending: Boolean = false
    private val serviceJob: Job = SupervisorJob()
    private val scope: CoroutineScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private var macroLoopJob: Job? = null

    private val ticketConfirmationHandled: AtomicBoolean = AtomicBoolean(false)

    /** 트리 덤프는 진단용이라 세션당 한 번만 남긴다. */
    private val dumpedTreeOnce: AtomicBoolean = AtomicBoolean(false)

    /** 새로고침조차 못 한 연속 주기 수. 코레일 팝업이 화면을 덮고 있을 때 늘어난다. */
    private var blockedCycles: Int = 0
    private val bookingMutex: Mutex = Mutex()

    /**
     * 예매 탭이 성공한 뒤 이 시각까지는, 「승차권 정보 확인」을 가리는 안내 팝업의 「확인」을
     * 본문과 무관하게 눌러 준다. 이미 예매가 진행된 뒤라 남은 팝업은 안내성으로 본다. 0이면 비활성.
     */
    @Volatile
    private var postReserveConfirmDeadlineUptimeMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "KtxAccessibilityService connected")
        runtimeLog("service", "접근성 서비스 연결됨")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            runtimeLog("error", "안드로이드 11 미만에서는 화면 판독을 쓸 수 없습니다")
        }
        // onServiceConnected는 같은 인스턴스에서 여러 번 불릴 수 있다(실기에서 3분에 5회 관측).
        // 그때마다 루프를 새로 띄우면 루프가 겹쳐 새로고침과 화면 캡처가 배수로 늘어난다.
        macroLoopJob?.cancel()
        macroLoopJob = scope.launch {
            var previouslyEnabled = false
            while (isActive) {
                try {
                    val enabled: Boolean = repository.readMacroEnabled()
                    if (enabled && !previouslyEnabled) {
                        ticketConfirmationHandled.set(false)
                        tracker.reset()
                        initialCheckPending = true
                        dumpedTreeOnce.set(false)
                        blockedCycles = 0
                    }
                    previouslyEnabled = enabled
                    if (!DeviceLicense.isLicensed(applicationContext)) {
                        if (enabled) {
                            repository.setMacroEnabled(false)
                        }
                        delay(POLL_WHEN_DISABLED_MS)
                        continue
                    }
                    if (!enabled) {
                        delay(POLL_WHEN_DISABLED_MS)
                        continue
                    }
                    val cycleStart: Long = SystemClock.elapsedRealtime()
                    runMacroCycle()
                    delay(remainingInterval(cycleStart))
                } catch (err: CancellationException) {
                    throw err
                } catch (err: Exception) {
                    Log.e(TAG, "macro cycle failed", err)
                    runtimeLog("error", err.message ?: err.javaClass.simpleName)
                    delay(POLL_WHEN_DISABLED_MS)
                }
            }
        }
    }

    /**
     * 새로고침 간격은 **주기 시작 시점 기준**이다. 한 주기(새로고침→목록 복귀→판독)에 이미
     * 1초 남짓 걸리므로, 그 뒤에 간격을 통째로 더 쉬면 실제 간격이 설정값보다 1초 이상 길어진다.
     * 주기에 쓴 시간을 빼고 남은 만큼만 쉰다. 다만 코레일에 연타하지 않도록 최소 쉼은 둔다.
     */
    private suspend fun remainingInterval(cycleStartElapsedMs: Long): Long {
        val interval: Long = repository.readRefreshIntervalMs().coerceIn(
            MacroPreferencesRepository.MIN_REFRESH_MS,
            MacroPreferencesRepository.MAX_REFRESH_MS,
        )
        val spent: Long = SystemClock.elapsedRealtime() - cycleStartElapsedMs
        return (interval - spent).coerceAtLeast(MIN_CYCLE_GAP_MS)
    }

    override fun onDestroy() {
        macroLoopJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!KorailUiSnapshotNotes.isTargetPackage(event.packageName)) {
            return
        }
        scope.launch {
            val root: AccessibilityNodeInfo = rootInActiveWindow ?: return@launch
            if (!isKorailActiveWindow(root)) {
                return@launch
            }
            if (hasTicketConfirmation(root)) {
                if (repository.readMacroEnabled() || isAwaitingPostReserveConfirmation()) {
                    handleTicketConfirmationDetected("event")
                }
                return@launch
            }
            tryAutoConfirmPostReserveNotice(root)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt")
    }

    private suspend fun runMacroCycle() {
        val settings: MacroSettings = repository.readMacroSettings()
        if (!passGates(settings, "cycle_start")) {
            return
        }
        // 시트가 이미 열려 있으면 왜 열렸는지 알 수 없다(직전 주기의 잔여이거나 사용자가 직접 연 것).
        // 어느 쪽이든 어떤 등급을 노리는지 모르는 상태라 예매하지 않고 목록으로 물러난다.
        val pending: AccessibilityNodeInfo? = rootInActiveWindow
        if (pending != null && BookingSheetReader.isOpen(pending)) {
            runtimeLog("sheet", "예매 시트가 열려 있어 목록으로 되돌립니다")
            leaveSheet()
            return
        }
        if (!performRefresh()) {
            handleRefreshUnavailable()
            return
        }
        blockedCycles = 0
        runtimeLog("refresh", "새로고침 탭")
        val rowNodes: List<AccessibilityNodeInfo> = awaitRows() ?: return
        val rects: List<PixelRect> = TrainRowLocator.rectsOf(rowNodes)
        val signals: List<SoldOutSignal> = sensor.readSoldOut(rects) ?: run {
            runtimeLog("sense", "화면 판독 실패 — 이번 주기 건너뜀")
            return
        }
        val soldOutCount: Int = signals.count { s: SoldOutSignal -> s.generalSoldOut || s.premiumSoldOut }
        runtimeLog("parse", "행 ${rects.size}개 (매진 포함 ${soldOutCount}개)")
        Log.i(TAG, "signals=" + signals.joinToString(",") { s: SoldOutSignal ->
            "${if (s.generalSoldOut) 'X' else 'o'}${if (s.premiumSoldOut) 'X' else 'o'}"
        })

        // 전이가 보이면 확인 판독 없이 바로 탭한다. 덜 그려진 화면을 찍어 생기는 오판은
        // 시트에서 「매진」으로 걸러지므로 잘못 사지는 않고, 그 대신 취소표를 잡는 순간의
        // 0.4초를 아낀다 — 사용자 관측상 글자 지연은 드물어 이쪽이 이득이다.
        val transition: SeatOpening? = tracker.observe(rects, signals, settings.seatClickPreference)
        val opening: SeatOpening = transition ?: initialCandidate(signals, settings) ?: return
        val target: AccessibilityNodeInfo = rowNodes.getOrNull(opening.rowIndex) ?: return
        val why: String = if (transition != null) "매진 해제" else "시작 시 비매진"
        runtimeLog("open", "${opening.rowIndex + 1}번째 행 ${columnLabel(opening)} $why — 시트 열기")
        if (!performClick(target)) {
            runtimeLog("open", "행 탭 실패")
            return
        }
        completeBooking(settings, opening)
    }

    /** 켠 직후 첫 판독 한 번만: 이미 매진이 아닌 칸이 있으면 그중 하나를 시트로 확인한다. */
    private fun initialCandidate(signals: List<SoldOutSignal>, settings: MacroSettings): SeatOpening? {
        if (!initialCheckPending) {
            return null
        }
        initialCheckPending = false
        return InitialOpenSweep.firstCandidate(signals, settings.seatClickPreference)
    }

    /**
     * 시트가 뜨기를 기다렸다가 필터를 적용하고, 통과하면 「바로 예매」까지 누른다.
     * 어느 단계든 막히면 뒤로 나와 목록으로 돌아간다.
     */
    private suspend fun completeBooking(settings: MacroSettings, opening: SeatOpening?) {
        bookingMutex.withLock {
            val sheet: BookingSheet? = awaitSheet()
            if (sheet == null) {
                runtimeLog("sheet", "시트를 읽지 못했습니다 — 시트 닫기")
                leaveSheet()
                return@withLock
            }
            if (opening != null && !selectClassTab(sheet, opening)) {
                runtimeLog("sheet", "${sheet.trainLabel} 등급 탭 전환 실패 — 시트 닫기")
                leaveSheet()
                return@withLock
            }
            // 등급 탭을 바꾸면 좌석 옵션이 달라지므로 필터 직전에 다시 읽는다.
            val current: BookingSheet = awaitSheet() ?: sheet
            val rejection: String? = MacroFilterEngine.rejectionReason(current, settings)
            if (rejection != null) {
                runtimeLog("filter", "${current.trainLabel} 건너뜀 — $rejection")
                leaveSheet()
                return@withLock
            }
            // 필터를 통과했다면 활성 예매 버튼이 있다.
            val reserveLabel: String = current.reserveLabel ?: KorailLabels.RESERVE_NOW
            val reserve: AccessibilityNodeInfo? = rootInActiveWindow?.let { root ->
                BookingSheetReader.findActionButton(root, reserveLabel)
            }
            if (reserve == null || !performClick(reserve)) {
                runtimeLog("reserve", "${current.trainLabel} 「$reserveLabel」 탭 실패 — 시트 닫기")
                leaveSheet()
                return@withLock
            }
            postReserveConfirmDeadlineUptimeMs =
                SystemClock.uptimeMillis() + POST_RESERVE_CONFIRM_BUDGET_MS
            runtimeLog("reserve", "${current.trainLabel} 「$reserveLabel」 탭")
        }
    }

    /**
     * 새로고침 버튼이 없을 때. 대개 코레일이 모달 팝업을 띄운 상태다.
     *
     * 실기에서 「인터넷 연결이 원활하지 않습니다」 안내창이 목록을 덮은 채로 남아, 매크로가
     * 같은 실패만 계속 찍는 상황을 관측했다. 그 팝업은 매크로가 치울 수 없고(선택지가
     * 「앱 종료」·「티켓 목록 보기」뿐이라 함부로 누르면 안 된다) 저절로 사라지지도 않는다.
     * 그래서 화면에 무엇이 떠 있는지 로그로 알려 주고, 계속 막혀 있으면 멈춘다.
     */
    private suspend fun handleRefreshUnavailable() {
        blockedCycles++
        val blocker: String = describeForegroundText()
        val suffix: String = if (blocker.isEmpty()) "" else " — 화면: $blocker"
        runtimeLog("refresh", "새로고침 버튼이 없습니다$suffix")
        if (blockedCycles >= MAX_BLOCKED_CYCLES) {
            repository.setMacroEnabled(false)
            blockedCycles = 0
            runtimeLog("error", "코레일 화면이 막혀 있어 매크로를 멈춥니다. 화면을 확인해 주세요.")
        }
    }

    /** 지금 화면의 짧은 문구 몇 개. 무엇이 막고 있는지 사용자에게 알려 주려고 쓴다. */
    private fun describeForegroundText(): String {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return ""
        val texts: MutableList<String> = mutableListOf()
        NodeFinder.collectTexts(root, texts)
        return texts
            .map { line: String -> line.replace('\n', ' ').trim() }
            .filter { line: String -> line.isNotEmpty() && line.length <= 60 }
            .distinct()
            .take(3)
            .joinToString(" / ")
    }

    /**
     * 새로고침 직후 「목록을 불러오고 있어요」 자리표시자가 뜨는지 잠깐 지켜본다.
     * 뜨면 true. 캐시 등으로 끝내 안 뜨면 false를 돌려주고 호출부는 그대로 진행한다.
     */
    private suspend fun awaitLoadingPlaceholder(): Boolean {
        val deadline: Long = SystemClock.elapsedRealtime() + LOADING_APPEAR_BUDGET_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val root: AccessibilityNodeInfo? = rootInActiveWindow
            if (root != null &&
                NodeFinder.containsText(root, KorailUiSnapshotNotes.LIST_LOADING_MARKER)
            ) {
                return true
            }
            delay(ROWS_POLL_GAP_MS)
        }
        return false
    }

    /**
     * 새로고침 뒤 목록이 돌아올 때까지 기다린다.
     *
     * 코레일톡은 새로고침을 누르면 목록 전체를 「목록을 불러오고 있어요」 자리표시자로 바꾼다.
     * 고정 시간을 기다리면 네트워크가 느린 날 매번 빈 화면을 읽게 되므로 행이 실제로 돌아올
     * 때까지 폴링한다. 행이 보인 뒤에도 글자가 다 그려지도록 잠깐 두고 좌표를 다시 잡는다 —
     * 화면 캡처로 매진을 읽기 때문에 그리는 도중을 찍으면 안 된다.
     */
    private suspend fun awaitRows(): List<AccessibilityNodeInfo>? {
        val start: Long = SystemClock.elapsedRealtime()
        // 새로고침을 눌러도 목록이 자리표시자로 바뀌기까지 잠깐 걸린다. 그 사이에 읽으면
        // 갱신 전 화면을 새 결과로 착각하므로, 자리표시자가 뜨는지 잠깐 지켜본다.
        var sawLoading: Boolean = awaitLoadingPlaceholder()
        val deadline: Long = SystemClock.elapsedRealtime() + ROWS_WAIT_BUDGET_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val root: AccessibilityNodeInfo? = rootInActiveWindow
            if (root != null && isKorailActiveWindow(root)) {
                if (hasTicketConfirmation(root)) {
                    handleTicketConfirmationDetected("await_rows")
                    return null
                }
                if (TrainRowLocator.locateRows(root).isNotEmpty()) {
                    delay(ROWS_SETTLE_MS)
                    val settled: AccessibilityNodeInfo = rootInActiveWindow ?: return null
                    val rows: List<AccessibilityNodeInfo> = TrainRowLocator.locateRows(settled)
                    if (rows.isNotEmpty()) {
                        Log.i(TAG, "rows=${rows.size} after ${SystemClock.elapsedRealtime() - start}ms")
                        return rows
                    }
                }
                if (NodeFinder.containsText(root, KorailUiSnapshotNotes.LIST_LOADING_MARKER)) {
                    sawLoading = true
                }
            }
            delay(ROWS_POLL_GAP_MS)
        }
        if (sawLoading) {
            runtimeLog("parse", "목록 로딩이 ${ROWS_WAIT_BUDGET_MS}ms 안에 끝나지 않았습니다")
        } else {
            runtimeLog("parse", "열차 행 0개 — 열차 목록 화면인지 확인하세요")
            rootInActiveWindow?.let { root ->
                if (dumpedTreeOnce.compareAndSet(false, true)) {
                    AccessibilityTreeDump.log(root, "no rows")
                }
            }
        }
        return null
    }

    /**
     * 시트가 뜨고 **좌석 조회까지 끝나기를** 기다린다.
     *
     * 시트는 제목과 버튼 틀이 먼저 그려지고 좌석 조회가 끝나야 버튼이 활성화된다. 그 사이를
     * 읽으면 활성 버튼이 없어 필터가 "예매 버튼 없음"으로 거절해 버린다 — 실기에서 좌석이 풀린
     * 행을 열고도 그대로 닫고 새로고침으로 돌아간 원인. 그래서 [BookingSheet.isSettled]가 될
     * 때까지 폴링하고, 시간 안에 결론이 안 나면 마지막으로 읽은 시트를 그대로 돌려준다
     * (필터가 이유를 기록한다).
     */
    private suspend fun awaitSheet(): BookingSheet? {
        val deadline: Long = SystemClock.elapsedRealtime() + SHEET_WAIT_BUDGET_MS
        var lastSheet: BookingSheet? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            val root: AccessibilityNodeInfo? = rootInActiveWindow
            if (root != null && isKorailActiveWindow(root)) {
                if (hasTicketConfirmation(root)) {
                    handleTicketConfirmationDetected("await_sheet")
                    return null
                }
                if (tryConfirmRowNotice(root)) {
                    delay(SHEET_POLL_GAP_MS)
                    continue
                }
                val sheet: BookingSheet? = BookingSheetReader.read(root)
                if (sheet != null) {
                    if (sheet.isSettled) {
                        return sheet
                    }
                    lastSheet = sheet
                }
            }
            delay(SHEET_POLL_GAP_MS)
        }
        if (lastSheet != null) {
            runtimeLog("sheet", "${lastSheet.trainLabel} 좌석 조회가 ${SHEET_WAIT_BUDGET_MS}ms 안에 끝나지 않았습니다")
        }
        return lastSheet
    }

    /** 시트가 목표 등급 탭을 보고 있게 만든다. 이미 맞으면 아무것도 하지 않는다. */
    private suspend fun selectClassTab(sheet: BookingSheet, opening: SeatOpening): Boolean {
        val label: String = KorailLabels.classLabelFor(opening.column, sheet.classTabs)
        if (!sheet.classTabs.contains(label)) {
            // 등급 탭이 아예 없는 시트(단일 등급)면 그대로 진행한다.
            return sheet.classTabs.isEmpty()
        }
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        val tab: AccessibilityNodeInfo = BookingSheetReader.findClassTab(root, label) ?: return false
        // 등급 탭은 선택 상태를 isSelected가 아니라 isChecked로 노출한다 (실기 확인).
        if (tab.isChecked) {
            return true
        }
        val clicked: Boolean = performClick(tab)
        if (clicked) {
            delay(CLASS_TAB_SETTLE_MS)
        }
        return clicked
    }

    /**
     * 시트를 닫고 열차 목록으로 돌아간다.
     *
     * BACK은 쓰지 않는다. 코레일톡 7.0.10은 시트가 떠 있을 때 BACK을 받으면 시트를 닫지 않고
     * 목록까지 건너뛰어 코레일 홈으로 가 버린다(실기 재현) — 「취소표를 잡는 순간 홈으로 튕긴다」의
     * 원인이었다. 대신 시트 손잡이를 누르고, 목록으로 돌아왔는지 확인한다. 확인하지 못하면
     * 엉뚱한 화면에서 계속 탭하지 않도록 매크로를 멈춘다.
     */
    private suspend fun leaveSheet() {
        tracker.reset()
        // 열차를 탭하면 시트 위에 「이용 안내」가 먼저 뜨기도 한다. 그 창이 앞에 있으면 손잡이가 안 보인다.
        rootInActiveWindow?.let { root ->
            if (tryConfirmRowNotice(root)) {
                delay(ROW_NOTICE_SETTLE_MS)
            }
        }
        val handle: AccessibilityNodeInfo? =
            rootInActiveWindow?.let { root -> BookingSheetReader.findDragHandle(root) }
        if (handle == null || !performClick(handle)) {
            stopMacro("시트를 닫지 못해 매크로를 멈춥니다. 화면을 확인해 주세요.")
            return
        }
        if (!awaitBackOnList()) {
            stopMacro("시트를 닫은 뒤 열차 목록으로 돌아오지 못해 매크로를 멈춥니다. 화면을 확인해 주세요.")
        }
    }

    /** 시트가 사라지고 목록의 새로고침 버튼이 다시 보이면 true. */
    private suspend fun awaitBackOnList(): Boolean {
        val deadline: Long = SystemClock.elapsedRealtime() + SHEET_CLOSE_BUDGET_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val root: AccessibilityNodeInfo? = rootInActiveWindow
            if (root != null &&
                isKorailActiveWindow(root) &&
                !BookingSheetReader.isOpen(root) &&
                NodeFinder.findClickableContainingText(root, KorailLabels.REFRESH) != null
            ) {
                return true
            }
            delay(SHEET_POLL_GAP_MS)
        }
        return false
    }

    private suspend fun stopMacro(reason: String) {
        repository.setMacroEnabled(false)
        runtimeLog("error", reason)
    }

    /**
     * 열차 행을 탭하면 시트 위에 뜨는 「이용 안내」(산천 연결 운행, 경유 노선 안내 등)의 「확인」.
     * 안내성 팝업이라 본문은 따지지 않는다. 제목 주변에서만 「확인」을 찾는다.
     */
    private suspend fun tryConfirmRowNotice(root: AccessibilityNodeInfo): Boolean {
        val title: AccessibilityNodeInfo =
            NodeFinder.findNodeByLabel(root, KorailUiSnapshotNotes.ROW_NOTICE_DIALOG_TITLE) ?: return false
        val target: AccessibilityNodeInfo = NodeFinder.findClickableByLabelNearAnchor(
            title,
            KorailUiSnapshotNotes.CONFIRM_LABEL,
        ) ?: return false
        val clicked: Boolean = performClick(target)
        if (clicked) {
            runtimeLog("dialog", "열차 이용 안내 확인")
        }
        return clicked
    }

    /**
     * 매 단계 앞에 두는 공통 확인. 코레일이 아니거나 예매가 이미 끝난 상태면 false.
     */
    private suspend fun passGates(settings: MacroSettings, source: String): Boolean {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: run {
            runtimeLog("tick", "화면을 읽지 못했습니다 — 스킵")
            return false
        }
        if (!isKorailActiveWindow(root)) {
            runtimeLog("skip", "코레일 앱이 앞에 있지 않습니다")
            return false
        }
        if (hasTicketConfirmation(root)) {
            handleTicketConfirmationDetected(source)
            return false
        }
        if (tryAutoConfirmPostReserveNotice(root)) {
            return false
        }
        if (tryConfirmRowNotice(root)) {
            return false
        }
        if (settings.autoConfirmIntermediateStopDialog && tryAutoConfirmIntermediateStop(root)) {
            return false
        }
        return true
    }

    private suspend fun handleTicketConfirmationDetected(source: String): Boolean {
        if (!ticketConfirmationHandled.compareAndSet(false, true)) {
            return false
        }
        val settings: MacroSettings = repository.readMacroSettings()
        NotificationHelper.triggerUserAlert(applicationContext, settings.ticketAlertMode)
        postReserveConfirmDeadlineUptimeMs = 0L
        repository.setMacroEnabled(false)
        tracker.reset()
        runtimeLog("ticket", "승차권 정보 확인 ($source) — 매크로 중지")
        Log.i(TAG, "Ticket confirmation handled ($source)")
        return true
    }

    private suspend fun performRefresh(): Boolean {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return false
        val target: AccessibilityNodeInfo =
            NodeFinder.findClickableContainingText(root, KorailLabels.REFRESH) ?: return false
        return performClick(target)
    }

    private suspend fun performClick(node: AccessibilityNodeInfo): Boolean {
        return withContext(Dispatchers.Main) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun isKorailActiveWindow(root: AccessibilityNodeInfo): Boolean {
        return KorailUiSnapshotNotes.isTargetPackage(root.packageName)
    }

    private fun hasTicketConfirmation(root: AccessibilityNodeInfo): Boolean {
        return root
            .findAccessibilityNodeInfosByText(KorailUiSnapshotNotes.TICKET_CONFIRMATION_TITLE)
            .isNotEmpty()
    }

    private fun isAwaitingPostReserveConfirmation(): Boolean {
        val deadline: Long = postReserveConfirmDeadlineUptimeMs
        if (deadline == 0L) {
            return false
        }
        if (SystemClock.uptimeMillis() >= deadline) {
            postReserveConfirmDeadlineUptimeMs = 0L
            return false
        }
        return true
    }

    /**
     * 예매 직후 뜨는 안내 팝업(이용안내·연접 좌석 재배정 등)의 「확인」을 눌러 준다.
     * 이 창은 「승차권 정보 확인」 위에 겹쳐 제목을 가리므로 본문 문구를 따지지 않는다 —
     * 이미 예매가 진행된 뒤라 남은 팝업은 안내성이라고 본다.
     */
    private suspend fun tryAutoConfirmPostReserveNotice(root: AccessibilityNodeInfo): Boolean {
        if (!isAwaitingPostReserveConfirmation()) {
            return false
        }
        val target: AccessibilityNodeInfo =
            NodeFinder.findClickableByLabel(root, KorailUiSnapshotNotes.CONFIRM_LABEL) ?: return false
        val clicked: Boolean = performClick(target)
        if (clicked) {
            runtimeLog("dialog", "예매 후 안내 팝업 확인")
        }
        return clicked
    }

    /** 중간 정차역 안내 팝업. 사용자가 켠 경우에만 누른다. */
    private suspend fun tryAutoConfirmIntermediateStop(root: AccessibilityNodeInfo): Boolean {
        for (marker: String in KorailUiSnapshotNotes.INTERMEDIATE_STOP_DIALOG_TEXT_MARKERS) {
            val anchor: AccessibilityNodeInfo =
                NodeFinder.findNodeContainingText(root, marker) ?: continue
            // 팝업 본문 주변으로만 「확인」을 찾는다. 트리 전체에서 찾으면 화면 다른 곳의
            // 같은 라벨을 누를 수 있다.
            val target: AccessibilityNodeInfo = NodeFinder.findClickableByLabelNearAnchor(
                anchor,
                KorailUiSnapshotNotes.CONFIRM_LABEL,
            ) ?: continue
            val clicked: Boolean = performClick(target)
            if (clicked) {
                runtimeLog("dialog", "정차 안내 확인")
                return true
            }
        }
        return false
    }

    private fun columnLabel(opening: SeatOpening): String {
        return KorailLabels.classLabelFor(opening.column, emptyList())
    }

    private fun runtimeLog(kind: String, detail: String) {
        Log.i(TAG, "[$kind] $detail")
        try {
            MacroRuntimeLog.emit(kind, detail)
        } catch (_: Throwable) {
        }
    }

    private companion object {
        const val TAG: String = "KtxA11y"
        const val POLL_WHEN_DISABLED_MS: Long = 400L

        /** 새로고침 후 목록이 돌아올 때까지 기다리는 최대 시간. */
        const val ROWS_WAIT_BUDGET_MS: Long = 4000L
        const val ROWS_POLL_GAP_MS: Long = 120L

        /** 행이 보인 뒤 글자가 다 그려지도록 두는 시간. 캡처가 그리는 도중을 찍지 않게 한다. */
        const val ROWS_SETTLE_MS: Long = 220L

        /** 새로고침 직후 로딩 자리표시자가 나타나기를 지켜보는 시간. */
        const val LOADING_APPEAR_BUDGET_MS: Long = 700L

        /** 주기가 간격보다 오래 걸렸어도 다음 새로고침 전에 최소한 이만큼은 쉰다. */
        const val MIN_CYCLE_GAP_MS: Long = 150L
        const val SHEET_WAIT_BUDGET_MS: Long = 2500L
        const val SHEET_POLL_GAP_MS: Long = 100L
        const val CLASS_TAB_SETTLE_MS: Long = 250L

        /** 손잡이를 누른 뒤 시트가 닫히고 목록이 돌아오기를 기다리는 최대 시간. */
        const val SHEET_CLOSE_BUDGET_MS: Long = 2000L
        const val ROW_NOTICE_SETTLE_MS: Long = 300L
        const val POST_RESERVE_CONFIRM_BUDGET_MS: Long = 15_000L

        /** 이만큼 연속으로 새로고침조차 못 하면 매크로를 멈추고 사용자에게 알린다. */
        const val MAX_BLOCKED_CYCLES: Int = 5
    }
}
