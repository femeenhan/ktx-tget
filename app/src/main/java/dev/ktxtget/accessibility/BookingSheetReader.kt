package dev.ktxtget.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.domain.BookingSheet
import dev.ktxtget.domain.BookingSheetParser
import dev.ktxtget.domain.KorailLabels
import dev.ktxtget.domain.PixelRect
import dev.ktxtget.util.NodeFinder

/**
 * 행을 탭하면 열리는 예매 바텀시트를 읽는다.
 *
 * 시트만 따로 떼어 읽는 게 중요하다. 화면 루트에서 통째로 텍스트를 모으면 목록 헤더
 * (`수정`, `새로고침`, `KTX`, `직통` …)까지 섞여 들어와 좌석 옵션 판정이 오염된다.
 *
 * 실기 트리 기준, 액션 버튼에서 위로 올라가다 **열차 제목(`KTX 007`)을 포함하는 첫 조상**이
 * 정확히 시트 루트였다. 그 위 단계부터는 목록 헤더가 함께 들어온다.
 */
object BookingSheetReader {

    /** "KTX 007", "KTX-산천 4135" 같은 시트 제목. */
    private val TRAIN_LABEL: Regex = Regex("""^\S+\s+\d{1,4}$""")

    private const val MAX_HOPS: Int = 6

    /** 액션 버튼에서 손잡이를 품은 시트 컨테이너까지 (실기 기준 4단계). */
    private const val HANDLE_MAX_HOPS: Int = 8

    /**
     * 시트가 떠 있으면 그 내용을, 아니면 null.
     * 열차 제목을 못 읽으면 제외 열차번호 필터를 걸 수 없으므로 실패로 본다(예매하면 안 된다).
     */
    fun read(root: AccessibilityNodeInfo): BookingSheet? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        val texts: MutableList<String> = mutableListOf()
        NodeFinder.collectTexts(sheetRoot, texts)
        val sheet: BookingSheet =
            BookingSheetParser.parse(texts, disabledActionLabels(sheetRoot)) ?: return null
        if (sheet.trainNumber.isEmpty()) {
            return null
        }
        return sheet
    }

    /** 시트가 화면에 있는지만 빠르게 본다. */
    fun isOpen(root: AccessibilityNodeInfo): Boolean {
        return findActionNode(root) != null ||
            findFirstMatching(root) { text: String -> text == KorailLabels.TRAIN_INFO } != null
    }

    fun findClassTab(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        return NodeFinder.findClickableByLabel(sheetRoot, label)
    }

    /** 누를 수 있는 액션 버튼만 돌려준다. 매진 시트의 비활성 버튼은 눌러도 실패한다. */
    fun findActionButton(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        return NodeFinder.findClickableByLabel(sheetRoot, label)?.takeIf { it.isEnabled }
    }

    /**
     * 시트 상단 손잡이. 탭하면 시트만 닫히고 목록은 그대로 남는다 (7.0.10 실기).
     *
     * 시트가 떠 있을 때 BACK은 시트를 닫지 않고 목록까지 건너뛰어 코레일 홈으로 가 버리므로
     * 시트는 반드시 이 손잡이로 닫는다. 바깥(어두운 영역) 탭도 쓰지 않는다 — 터치가 아래
     * 열차 행까지 전달돼 다른 열차의 시트가 열리는 것을 실기에서 봤다.
     *
     * 실기 트리: 액션 버튼에서 위로 올라가면 시트 컨테이너가 나오고, 그 첫 자식이
     * 텍스트 없는 클릭 가능 View(폭 전체)다.
     */
    fun findDragHandle(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 버튼 라벨을 모르는 시트(새 문구)라도 닫을 수 있도록 「운행 정보」 링크도 기준으로 쓴다.
        val anchor: AccessibilityNodeInfo = findActionNode(root)
            ?: findFirstMatching(root) { text: String -> text == KorailLabels.TRAIN_INFO }
            ?: return null
        val anchorTop: Int = NodeFinder.boundsOf(anchor).top
        var current: AccessibilityNodeInfo? = anchor.parent
        var hops: Int = 0
        while (current != null && hops < HANDLE_MAX_HOPS) {
            val width: Int = NodeFinder.boundsOf(current).width
            for (i: Int in 0 until current.childCount) {
                val child: AccessibilityNodeInfo = current.getChild(i) ?: continue
                val bounds: PixelRect = NodeFinder.boundsOf(child)
                if (child.isClickable &&
                    bounds.bottom <= anchorTop &&
                    width > 0 && bounds.width * 10 >= width * 9 &&
                    !hasAnyLabel(child)
                ) {
                    return child
                }
            }
            current = current.parent
            hops++
        }
        return null
    }

    /** 시트에 있지만 비활성인 액션 버튼 라벨. */
    private fun disabledActionLabels(sheetRoot: AccessibilityNodeInfo): Set<String> {
        return BookingSheetParser.ACTION_LABELS.filter { label: String ->
            val button: AccessibilityNodeInfo? = NodeFinder.findClickableByLabel(sheetRoot, label)
            button != null && !button.isEnabled
        }.toSet()
    }

    private fun hasAnyLabel(node: AccessibilityNodeInfo): Boolean {
        return findFirstMatching(node) { true } != null
    }

    private fun findSheetRoot(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val action: AccessibilityNodeInfo = findActionNode(root) ?: return null
        var current: AccessibilityNodeInfo? = action.parent
        var hops: Int = 0
        while (current != null && hops < MAX_HOPS) {
            if (hasTrainLabel(current)) {
                return current
            }
            current = current.parent
            hops++
        }
        return null
    }

    private fun findActionNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFirstMatching(root) { text: String ->
            BookingSheetParser.ACTION_LABELS.contains(text)
        }
    }

    private fun hasTrainLabel(node: AccessibilityNodeInfo): Boolean {
        return findFirstMatching(node) { text: String -> TRAIN_LABEL.matches(text) } != null
    }

    private fun findFirstMatching(
        node: AccessibilityNodeInfo,
        matches: (String) -> Boolean,
    ): AccessibilityNodeInfo? {
        val text: String = node.text?.toString()?.trim() ?: ""
        val description: String = node.contentDescription?.toString()?.trim() ?: ""
        if ((text.isNotEmpty() && matches(text)) || (description.isNotEmpty() && matches(description))) {
            return node
        }
        for (i: Int in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findFirstMatching(child, matches)
            if (found != null) {
                return found
            }
        }
        return null
    }
}
