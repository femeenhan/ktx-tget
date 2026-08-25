package dev.ktxtget.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.domain.BookingSheet
import dev.ktxtget.domain.BookingSheetParser
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

    /**
     * 시트가 떠 있으면 그 내용을, 아니면 null.
     * 열차 제목을 못 읽으면 제외 열차번호 필터를 걸 수 없으므로 실패로 본다(예매하면 안 된다).
     */
    fun read(root: AccessibilityNodeInfo): BookingSheet? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        val texts: MutableList<String> = mutableListOf()
        NodeFinder.collectTexts(sheetRoot, texts)
        val sheet: BookingSheet = BookingSheetParser.parse(texts) ?: return null
        if (sheet.trainNumber.isEmpty()) {
            return null
        }
        return sheet
    }

    /** 시트가 화면에 있는지만 빠르게 본다. */
    fun isOpen(root: AccessibilityNodeInfo): Boolean {
        return findActionNode(root) != null
    }

    fun findClassTab(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        return NodeFinder.findClickableByLabel(sheetRoot, label)
    }

    fun findActionButton(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val sheetRoot: AccessibilityNodeInfo = findSheetRoot(root) ?: return null
        return NodeFinder.findClickableByLabel(sheetRoot, label)
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
