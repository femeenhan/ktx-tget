package dev.ktxtget.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import dev.ktxtget.domain.PixelRect
import dev.ktxtget.util.NodeFinder

/**
 * 열차 목록에서 각 행의 화면 좌표와 클릭 대상을 찾는다.
 *
 * 행에 텍스트가 없어도 **좌표와 클릭 가능 여부는 정상적으로 노출된다.** 목록 구조는 이렇다:
 * ```
 * View [scrollable]                     <- 목록 컨테이너
 *   View                                <- 행 래퍼
 *     Button [clickable] text='' cd=''  <- 실제 행 (이 bounds로 픽셀을 읽는다)
 * ```
 * 화면 밖으로 잘린 행은 픽셀 판정이 틀어지므로 컨테이너 안에 온전히 들어온 행만 남긴다.
 */
object TrainRowLocator {

    /** 화면에 온전히 보이는 행들. 위에서 아래 순서. */
    fun locateRows(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val container: AccessibilityNodeInfo = NodeFinder.findScrollable(root) ?: run {
            Log.d(TAG, "no scrollable container yet")
            return emptyList()
        }
        val containerBounds: PixelRect = NodeFinder.boundsOf(container)
        Log.d(TAG, "container=$containerBounds children=${container.childCount}")
        val rows: MutableList<AccessibilityNodeInfo> = mutableListOf()
        for (i: Int in 0 until container.childCount) {
            val wrapper: AccessibilityNodeInfo = container.getChild(i) ?: run {
                Log.d(TAG, "child $i is null")
                return@run null
            } ?: continue
            val row: AccessibilityNodeInfo = firstClickableDescendant(wrapper) ?: run {
                Log.d(TAG, "child $i (${NodeFinder.boundsOf(wrapper)}) has no clickable descendant")
                return@run null
            } ?: continue
            val bounds: PixelRect = NodeFinder.boundsOf(row)
            if (isFullyVisible(bounds, containerBounds)) {
                rows.add(row)
            } else {
                Log.d(TAG, "child $i rejected: row=$bounds container=$containerBounds")
            }
        }
        return rows
    }

    fun rectsOf(rows: List<AccessibilityNodeInfo>): List<PixelRect> {
        return rows.map { node: AccessibilityNodeInfo -> NodeFinder.boundsOf(node) }
    }

    private fun isFullyVisible(row: PixelRect, container: PixelRect): Boolean {
        if (row.height < KorailUiSnapshotNotes.MIN_ROW_HEIGHT_PX) {
            return false
        }
        return row.top >= container.top && row.bottom <= container.bottom
    }

    private fun firstClickableDescendant(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) {
            return node
        }
        for (i: Int in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = firstClickableDescendant(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private const val TAG: String = "KtxRows"
}
