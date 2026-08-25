package dev.ktxtget.util

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 서비스가 실제로 보는 접근성 트리를 로그로 남긴다.
 *
 * 코레일 앱이 바뀔 때마다 "화면에는 보이는데 왜 못 찾지"를 판단해야 하는데, uiautomator 덤프는
 * 우리 서비스와 fetch 플래그가 달라 그대로 믿을 수 없다. 그래서 서비스 시점의 트리를 직접 본다.
 */
object AccessibilityTreeDump {
    private const val TAG: String = "KtxTree"
    private const val MAX_NODES: Int = 300

    fun log(root: AccessibilityNodeInfo, reason: String) {
        Log.i(TAG, "---- tree dump ($reason) ----")
        var budget: Int = MAX_NODES
        budget = walk(root, 0, budget)
        Log.i(TAG, "---- end (remaining budget $budget) ----")
    }

    private fun walk(node: AccessibilityNodeInfo, depth: Int, budget: Int): Int {
        if (budget <= 0) {
            return 0
        }
        val flags: String = buildString {
            if (node.isClickable) append('C')
            if (node.isScrollable) append('S')
            if (node.isCheckable) append('K')
            if (node.isChecked) append('k')
            if (node.isVisibleToUser) append('V')
        }
        val text: String = node.text?.toString()?.trim().orEmpty()
        val desc: String = node.contentDescription?.toString()?.trim().orEmpty()
        Log.i(
            TAG,
            "${" ".repeat(depth)}${node.className?.toString()?.substringAfterLast('.')}" +
                " [$flags] kids=${node.childCount} b=${NodeFinder.boundsOf(node)}" +
                (if (text.isNotEmpty()) " t='$text'" else "") +
                (if (desc.isNotEmpty()) " cd='$desc'" else ""),
        )
        var left: Int = budget - 1
        for (i: Int in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            left = walk(child, depth + 1, left)
        }
        return left
    }
}
