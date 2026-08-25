package dev.ktxtget.util

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.domain.PixelRect

/**
 * 접근성 트리 탐색.
 *
 * 코레일톡 7.x(Compose)는 **라벨을 자식 TextView가 들고, 클릭 가능 속성은 부모 View가 든다.**
 * 예를 들어 앱바 새로고침은 이렇게 생겼다:
 * ```
 * View [clickable] cd=''          <- 실제로 눌러야 하는 노드
 *   View          cd='새로고침'    <- 라벨을 든 노드 (클릭 불가)
 * ```
 * 그래서 "클릭 가능하면서 라벨도 가진 노드"를 찾으면 아무것도 못 찾는다.
 * 라벨로 먼저 찾고 클릭 가능한 조상으로 올라가야 한다 — [findClickableForLabel]이 그 일을 한다.
 *
 * 반환된 노드는 활성 윈도우 루트가 유효한 동안에만 쓸 수 있다.
 * (recycle()은 API 33부터 no-op이라 여기서는 중간 노드를 recycle 하지 않는다.)
 */
object NodeFinder {

    /** 라벨 노드에서 클릭 가능한 조상까지 올라갈 최대 단계. */
    const val DEFAULT_MAX_HOPS: Int = 8

    fun boundsOf(node: AccessibilityNodeInfo): PixelRect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return PixelRect(rect.left, rect.top, rect.right, rect.bottom)
    }

    /**
     * [matches]를 만족하는 노드를 전위 순회로 찾고, 그 노드나 조상 중 클릭 가능한 첫 노드를 돌려준다.
     * 후보가 여러 개면 클릭 가능한 조상을 가진 첫 후보를 쓴다.
     */
    fun findClickableForLabel(
        root: AccessibilityNodeInfo,
        maxHops: Int = DEFAULT_MAX_HOPS,
        matches: (String) -> Boolean,
    ): AccessibilityNodeInfo? {
        val labelled: MutableList<AccessibilityNodeInfo> = mutableListOf()
        collectMatching(root, matches, labelled)
        for (node: AccessibilityNodeInfo in labelled) {
            val target: AccessibilityNodeInfo? = clickableSelfOrAncestor(node, maxHops)
            if (target != null) {
                return target
            }
        }
        return null
    }

    /** 라벨이 정확히 [label]인 노드를 눌러야 할 때. */
    fun findClickableByLabel(
        root: AccessibilityNodeInfo,
        label: String,
    ): AccessibilityNodeInfo? {
        return findClickableForLabel(root) { text: String -> text == label }
    }

    /** 라벨에 [substring]이 들어간 노드를 눌러야 할 때. */
    fun findClickableContainingText(
        root: AccessibilityNodeInfo,
        substring: String,
    ): AccessibilityNodeInfo? {
        return findClickableForLabel(root) { text: String -> text.contains(substring) }
    }

    /** [labels] 순서대로 찾아 처음 걸리는 것을 돌려준다. 우선순위 있는 버튼 후보에 쓴다. */
    fun findFirstClickableByLabels(
        root: AccessibilityNodeInfo,
        labels: List<String>,
    ): AccessibilityNodeInfo? {
        for (label: String in labels) {
            val found: AccessibilityNodeInfo? = findClickableByLabel(root, label)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /** 트리에 있는 모든 text·contentDescription을 전위 순회 순서로 모은다. */
    fun collectTexts(root: AccessibilityNodeInfo, out: MutableList<String>) {
        val text: String = root.text?.toString()?.trim() ?: ""
        if (text.isNotEmpty()) {
            out.add(text)
        }
        val description: String = root.contentDescription?.toString()?.trim() ?: ""
        if (description.isNotEmpty() && description != text) {
            out.add(description)
        }
        for (i: Int in 0 until root.childCount) {
            val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
            collectTexts(child, out)
        }
    }

    fun containsText(root: AccessibilityNodeInfo, substring: String): Boolean {
        val text: String = root.text?.toString() ?: ""
        val description: String = root.contentDescription?.toString() ?: ""
        if (text.contains(substring) || description.contains(substring)) {
            return true
        }
        for (i: Int in 0 until root.childCount) {
            val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
            if (containsText(child, substring)) {
                return true
            }
        }
        return false
    }

    /** 스크롤 가능한 첫 노드. 열차 목록 컨테이너를 잡는 데 쓴다. */
    fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) {
            return root
        }
        for (i: Int in 0 until root.childCount) {
            val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findScrollable(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /**
     * [anchor] 주변에서만 라벨이 [label]인 클릭 대상을 찾는다.
     *
     * 조상을 한 단계씩 넓혀 가며 그 서브트리 안에서만 찾으므로, 화면 다른 곳에 있는 같은 라벨의
     * 버튼을 잘못 누르지 않는다. 팝업 본문 문구를 기준으로 그 팝업의 버튼을 누를 때 쓴다.
     */
    fun findClickableByLabelNearAnchor(
        anchor: AccessibilityNodeInfo,
        label: String,
        maxHops: Int = DEFAULT_MAX_HOPS,
    ): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = anchor.parent
        var hops: Int = 0
        while (current != null && hops < maxHops) {
            val found: AccessibilityNodeInfo? = findClickableByLabel(current, label)
            if (found != null) {
                return found
            }
            current = current.parent
            hops++
        }
        return null
    }

    /** 라벨이 정확히 [label]인 첫 노드. 클릭 가능 여부는 보지 않는다. */
    fun findNodeByLabel(root: AccessibilityNodeInfo, label: String): AccessibilityNodeInfo? {
        val matched: MutableList<AccessibilityNodeInfo> = mutableListOf()
        collectMatching(root, { text: String -> text == label }, matched)
        return matched.firstOrNull()
    }

    /** 라벨에 [substring]이 들어간 첫 노드. 클릭 가능 여부는 보지 않는다. */
    fun findNodeContainingText(
        root: AccessibilityNodeInfo,
        substring: String,
    ): AccessibilityNodeInfo? {
        val matched: MutableList<AccessibilityNodeInfo> = mutableListOf()
        collectMatching(root, { text: String -> text.contains(substring) }, matched)
        return matched.firstOrNull()
    }

    /** [node]가 클릭 가능하면 그대로, 아니면 [maxHops] 안의 클릭 가능한 조상. */
    fun clickableSelfOrAncestor(
        node: AccessibilityNodeInfo,
        maxHops: Int = DEFAULT_MAX_HOPS,
    ): AccessibilityNodeInfo? {
        if (node.isClickable) {
            return node
        }
        var current: AccessibilityNodeInfo? = node.parent
        var hops: Int = 0
        while (current != null && hops < maxHops) {
            if (current.isClickable) {
                return current
            }
            current = current.parent
            hops++
        }
        return null
    }

    private fun collectMatching(
        node: AccessibilityNodeInfo,
        matches: (String) -> Boolean,
        out: MutableList<AccessibilityNodeInfo>,
    ) {
        val text: String = node.text?.toString()?.trim() ?: ""
        val description: String = node.contentDescription?.toString()?.trim() ?: ""
        if ((text.isNotEmpty() && matches(text)) || (description.isNotEmpty() && matches(description))) {
            out.add(node)
        }
        for (i: Int in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            collectMatching(child, matches, out)
        }
    }
}
