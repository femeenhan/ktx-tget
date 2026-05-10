package dev.ktxtget.util

import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.accessibility.KorailUiSnapshotNotes

/**
 * Read-only tree walks and click-target resolution. Caller recycles the active window root.
 */
object NodeFinder {

    /**
     * First preorder node whose [AccessibilityNodeInfo.getText] or [AccessibilityNodeInfo.getContentDescription]
     * contains [substring]. The returned node must be recycled by the caller when no longer the active window root.
     */
    fun findFirstPreorderContainingText(
        root: AccessibilityNodeInfo,
        substring: String,
    ): AccessibilityNodeInfo? {
        val t: String = root.text?.toString() ?: ""
        val d: String = root.contentDescription?.toString() ?: ""
        if (t.contains(substring) || d.contains(substring)) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findFirstPreorderContainingText(child, substring)
            if (found != null) {
                if (found !== child) {
                    child.recycle()
                }
                return found
            }
            child.recycle()
        }
        return null
    }

    fun findClickableWithTextOrId(
        root: AccessibilityNodeInfo,
        textHint: String,
        viewIdSuffixOrFull: String,
    ): AccessibilityNodeInfo? {
        if (viewIdSuffixOrFull.isNotEmpty()) {
            val byId: AccessibilityNodeInfo? = findClickableByViewIdSuffix(root, viewIdSuffixOrFull)
            if (byId != null) {
                return byId
            }
        }
        return findClickableContainingText(root, textHint)
    }

    fun findClickableContainingText(root: AccessibilityNodeInfo, substring: String): AccessibilityNodeInfo? {
        return findClickablePreorder(root) { node: AccessibilityNodeInfo ->
            val t: String = node.text?.toString() ?: ""
            val d: String = node.contentDescription?.toString() ?: ""
            t.contains(substring) || d.contains(substring)
        }
    }

    fun findClickableByLabel(root: AccessibilityNodeInfo, exact: String): AccessibilityNodeInfo? {
        return findClickablePreorder(root) { node: AccessibilityNodeInfo ->
            val t: String = node.text?.toString()?.trim() ?: ""
            val d: String = node.contentDescription?.toString()?.trim() ?: ""
            t == exact || d == exact
        }
    }

    /**
     * Prefers [KorailUiSnapshotNotes.VIEW_ID_RESERVE_BUTTON], then label [KorailUiSnapshotNotes.RESERVE_BUTTON_LABEL].
     * Optionally narrows search to subtree [KorailUiSnapshotNotes.VIEW_ID_BOTTOM_SHEET_ROOT] when set.
     */
    fun findReserveButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val sheetSuffix: String = KorailUiSnapshotNotes.VIEW_ID_BOTTOM_SHEET_ROOT
        val reserveId: String = KorailUiSnapshotNotes.VIEW_ID_RESERVE_BUTTON
        val searchRoot: AccessibilityNodeInfo =
            if (sheetSuffix.isNotEmpty()) {
                findFirstNodeMatchingViewId(root, sheetSuffix) ?: root
            } else {
                root
            }
        if (reserveId.isNotEmpty()) {
            val byId: AccessibilityNodeInfo? = findClickableByViewIdSuffix(searchRoot, reserveId)
            if (byId != null) {
                return byId
            }
        }
        return findClickableByLabel(searchRoot, KorailUiSnapshotNotes.RESERVE_BUTTON_LABEL)
    }

    private fun findFirstNodeMatchingViewId(
        node: AccessibilityNodeInfo,
        suffixOrFull: String,
    ): AccessibilityNodeInfo? {
        val id: String? = node.viewIdResourceName
        if (id != null && viewIdMatches(id, suffixOrFull)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findFirstNodeMatchingViewId(child, suffixOrFull)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun viewIdMatches(id: String, suffixOrFull: String): Boolean {
        return if (suffixOrFull.contains("/")) {
            id == suffixOrFull
        } else {
            id.endsWith(suffixOrFull)
        }
    }

    private fun findClickableByViewIdSuffix(
        root: AccessibilityNodeInfo,
        suffixOrFull: String,
    ): AccessibilityNodeInfo? {
        return findClickablePreorder(root) { node: AccessibilityNodeInfo ->
            val id: String = node.viewIdResourceName ?: return@findClickablePreorder false
            if (suffixOrFull.contains("/")) {
                id == suffixOrFull
            } else {
                id.endsWith(suffixOrFull)
            }
        }
    }

    private fun findClickablePreorder(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (node.isClickable && predicate(node)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findClickablePreorder(child, predicate)
            if (found != null) {
                return found
            }
        }
        return null
    }

    /**
     * If [start] is not clickable, walks up [maxHops] parents (each must be recycled except return).
     */
    @Suppress("DEPRECATION")
    fun clickTargetForLabel(start: AccessibilityNodeInfo, maxHops: Int): AccessibilityNodeInfo? {
        if (start.isClickable) {
            return start
        }
        var p: AccessibilityNodeInfo? = start.parent
        var hops: Int = 0
        while (p != null && hops < maxHops) {
            if (p.isClickable) {
                return p
            }
            val next: AccessibilityNodeInfo? = p.parent
            p.recycle()
            p = next
            hops++
        }
        if (p != null) {
            p.recycle()
        }
        return null
    }
}
