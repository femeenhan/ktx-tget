package dev.ktxtget.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import dev.ktxtget.domain.PriceTransitionDetector
import dev.ktxtget.domain.SeatColumn
import dev.ktxtget.domain.TrainRow
import dev.ktxtget.domain.TrainRowStableKey
import dev.ktxtget.util.NodeFinder

/**
 * Parsed row with optional click targets (nodes valid until active-window root is recycled).
 */
data class ParsedTrainRow(
    val row: TrainRow,
    val generalClickTarget: AccessibilityNodeInfo?,
    val firstClassClickTarget: AccessibilityNodeInfo?,
)

/**
 * Builds [ParsedTrainRow] from the Korail accessibility tree. Prefer filling [KorailUiSnapshotNotes] ids from Layout Inspector.
 */
class TrainRowTreeParser {

    fun parse(root: AccessibilityNodeInfo): List<ParsedTrainRow> {
        if (KorailUiSnapshotNotes.VIEW_ID_TRAIN_ROW_ROOT.isNotEmpty()) {
            val fromIds: List<ParsedTrainRow> =
                parseRowsByViewId(root, KorailUiSnapshotNotes.VIEW_ID_TRAIN_ROW_ROOT)
            if (fromIds.isNotEmpty()) {
                return fromIds
            }
        }
        val recycler: AccessibilityNodeInfo? = findRecyclerView(root)
        val rowRoots: List<AccessibilityNodeInfo> =
            if (recycler != null && recycler.childCount > 0) {
                (0 until recycler.childCount).mapNotNull { i: Int -> recycler.getChild(i) }
            } else {
                listOf(root)
            }
        val out: MutableList<ParsedTrainRow> = mutableListOf()
        for (rowRoot: AccessibilityNodeInfo in rowRoots) {
            val parsed: ParsedTrainRow? = harvestRow(rowRoot)
            if (parsed != null) {
                out.add(parsed)
            }
        }
        if (out.isNotEmpty()) {
            return out
        }
        return parseBySeatAnchorClustering(root)
    }

    /**
     * Re-parses [root] so the returned node is from the same snapshot as [performClick].
     */
    fun findPriceClickTarget(
        root: AccessibilityNodeInfo,
        stableKey: String,
        col: SeatColumn,
    ): AccessibilityNodeInfo? {
        val rows: List<ParsedTrainRow> = parse(root)
        val match: ParsedTrainRow? =
            rows.firstOrNull { pr: ParsedTrainRow -> pr.row.stableKey == stableKey }
        if (match == null) {
            return null
        }
        return when (col) {
            SeatColumn.GENERAL -> match.generalClickTarget
            SeatColumn.FIRST_CLASS -> match.firstClassClickTarget
        }
    }

    private fun parseRowsByViewId(root: AccessibilityNodeInfo, viewIdSpec: String): List<ParsedTrainRow> {
        val matches: MutableList<AccessibilityNodeInfo> = mutableListOf()
        collectNodesWithViewId(root, viewIdSpec, matches)
        val out: MutableList<ParsedTrainRow> = mutableListOf()
        for (rowRoot: AccessibilityNodeInfo in matches) {
            val parsed: ParsedTrainRow? = harvestRow(rowRoot)
            if (parsed != null) {
                out.add(parsed)
            }
        }
        return out
    }

    private fun collectNodesWithViewId(
        node: AccessibilityNodeInfo,
        viewIdSpec: String,
        out: MutableList<AccessibilityNodeInfo>,
    ) {
        val id: String? = node.viewIdResourceName
        if (id != null && viewIdMatches(id, viewIdSpec)) {
            out.add(node)
        }
        for (i in 0 until node.childCount) {
            val c: AccessibilityNodeInfo = node.getChild(i) ?: continue
            collectNodesWithViewId(c, viewIdSpec, out)
        }
    }

    private fun viewIdMatches(id: String, viewIdSpec: String): Boolean {
        return if (viewIdSpec.contains("/")) {
            id == viewIdSpec
        } else {
            id.endsWith(viewIdSpec)
        }
    }

    private fun findRecyclerView(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className: String = node.className?.toString() ?: ""
        if (className.contains("RecyclerView")) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findRecyclerView(child)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun harvestRow(rowRoot: AccessibilityNodeInfo): ParsedTrainRow? {
        val rawTexts: MutableList<String> = mutableListOf()
        collectTextsDepthFirst(rowRoot, rawTexts)
        val seatPairs: MutableList<Pair<String, AccessibilityNodeInfo?>> = mutableListOf()
        collectSeatLikeOrdered(rowRoot, seatPairs)
        if (rawTexts.isEmpty() && seatPairs.isEmpty()) {
            return null
        }
        val generalText: String = seatPairs.getOrNull(0)?.first ?: ""
        val firstClassText: String = seatPairs.getOrNull(1)?.first ?: ""
        val gClick: AccessibilityNodeInfo? = resolvePriceClickTarget(
            seatPairs.getOrNull(0),
            KorailUiSnapshotNotes.VIEW_ID_PRICE_GENERAL,
            rowRoot,
            generalText,
        )
        val fClick: AccessibilityNodeInfo? = resolvePriceClickTarget(
            seatPairs.getOrNull(1),
            KorailUiSnapshotNotes.VIEW_ID_PRICE_FIRST,
            rowRoot,
            firstClassText,
        )
        val key: String = TrainRowStableKey.fromRawTexts(rawTexts)
        val row: TrainRow = TrainRow(
            stableKey = key,
            rawLineTexts = rawTexts.toList(),
            generalSeatCellText = generalText,
            firstClassSeatCellText = firstClassText,
        )
        return ParsedTrainRow(row, gClick, fClick)
    }

    private fun resolvePriceClickTarget(
        pair: Pair<String, AccessibilityNodeInfo?>?,
        viewId: String,
        rowRoot: AccessibilityNodeInfo,
        cellText: String,
    ): AccessibilityNodeInfo? {
        if (viewId.isNotEmpty()) {
            val byId: AccessibilityNodeInfo? =
                findClickableByViewIdInSubtree(rowRoot, viewId)
            if (byId != null) {
                return byId
            }
        }
        if (pair == null) {
            return null
        }
        val resolved: AccessibilityNodeInfo? = pair.second
        if (resolved != null) {
            return resolved
        }
        if (cellText.isEmpty()) {
            return null
        }
        return findClickableWithExactText(rowRoot, cellText)
    }

    private fun findClickableByViewIdInSubtree(
        root: AccessibilityNodeInfo,
        viewIdSpec: String,
    ): AccessibilityNodeInfo? {
        val id: String? = root.viewIdResourceName
        if (id != null && viewIdMatches(id, viewIdSpec) && root.isClickable) {
            return root
        }
        for (i in 0 until root.childCount) {
            val child: AccessibilityNodeInfo = root.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findClickableByViewIdInSubtree(child, viewIdSpec)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun findClickableWithExactText(
        root: AccessibilityNodeInfo,
        text: String,
    ): AccessibilityNodeInfo? {
        val target: String = text.trim()
        if (target.isEmpty()) {
            return null
        }
        return findNodePreorder(root) { node: AccessibilityNodeInfo ->
            if (!node.isClickable) {
                return@findNodePreorder false
            }
            val t: String = node.text?.toString()?.trim() ?: ""
            t == target
        }
    }

    private fun findNodePreorder(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (predicate(node)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child: AccessibilityNodeInfo = node.getChild(i) ?: continue
            val found: AccessibilityNodeInfo? = findNodePreorder(child, predicate)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun collectTextsDepthFirst(node: AccessibilityNodeInfo, out: MutableList<String>) {
        val t: String = node.text?.toString()?.trim() ?: ""
        if (t.isNotEmpty()) {
            out.add(t)
        }
        val d: String = node.contentDescription?.toString()?.trim() ?: ""
        if (d.isNotEmpty() && d != t) {
            out.add(d)
        }
        for (i in 0 until node.childCount) {
            val c: AccessibilityNodeInfo = node.getChild(i) ?: continue
            collectTextsDepthFirst(c, out)
        }
    }

    private fun collectSeatLikeOrdered(
        node: AccessibilityNodeInfo,
        out: MutableList<Pair<String, AccessibilityNodeInfo?>>,
    ) {
        val t: String = node.text?.toString()?.trim() ?: ""
        if (t.isNotEmpty() && isSeatLike(t)) {
            val click: AccessibilityNodeInfo? = NodeFinder.clickTargetForLabel(node, 14)
            out.add(Pair(t, click))
        }
        for (i in 0 until node.childCount) {
            val c: AccessibilityNodeInfo = node.getChild(i) ?: continue
            collectSeatLikeOrdered(c, out)
        }
    }

    private fun isSeatLike(s: String): Boolean {
        return PriceTransitionDetector.isSoldOutLabel(s) ||
            PriceTransitionDetector.isPriceLabel(s)
    }

    private data class AnchorWithBounds(
        val node: AccessibilityNodeInfo,
        val text: String,
        val centerX: Int,
        val centerY: Int,
    )

    /**
     * 코레일톡·Compose 등: RecyclerView 직계 행이 없을 때 매진/가격 노드의 화면 Y로 행 묶기.
     */
    private fun parseBySeatAnchorClustering(root: AccessibilityNodeInfo): List<ParsedTrainRow> {
        val anchors: MutableList<AnchorWithBounds> = mutableListOf()
        collectAnchorsWithBounds(root, anchors)
        if (anchors.isEmpty()) {
            return emptyList()
        }
        val clusters: List<List<AnchorWithBounds>> =
            clusterAnchorsByYThreshold(
                anchors,
                KorailUiSnapshotNotes.SEAT_ROW_CLUSTER_THRESHOLD_PX,
            )
        val out: MutableList<ParsedTrainRow> = mutableListOf()
        for (cluster: List<AnchorWithBounds> in clusters) {
            val parsed: ParsedTrainRow? = harvestAnchorClusterToParsed(cluster)
            if (parsed != null) {
                out.add(parsed)
            }
        }
        return out
    }

    private fun collectAnchorsWithBounds(
        node: AccessibilityNodeInfo,
        out: MutableList<AnchorWithBounds>,
    ) {
        val t: String = node.text?.toString()?.trim() ?: ""
        val cd: String = node.contentDescription?.toString()?.trim() ?: ""
        val label: String =
            when {
                t.isNotEmpty() && isSeatLike(t) -> t
                cd.isNotEmpty() && isSeatLike(cd) -> cd
                else -> ""
            }
        if (label.isNotEmpty()) {
            val r: Rect = Rect()
            if (Build.VERSION.SDK_INT >= 30) {
                node.getBoundsInScreen(r)
            } else {
                AccessibilityNodeInfoCompat.wrap(node).getBoundsInScreen(r)
            }
            val cx: Int = (r.left + r.right) / 2
            val cy: Int = (r.top + r.bottom) / 2
            out.add(AnchorWithBounds(node, label, cx, cy))
        }
        for (i in 0 until node.childCount) {
            val c: AccessibilityNodeInfo = node.getChild(i) ?: continue
            collectAnchorsWithBounds(c, out)
        }
    }

    private fun clusterAnchorsByYThreshold(
        anchors: List<AnchorWithBounds>,
        thresholdY: Int,
    ): List<List<AnchorWithBounds>> {
        val sorted: List<AnchorWithBounds> =
            anchors.sortedWith(compareBy({ it.centerY }, { it.centerX }))
        val rows: MutableList<MutableList<AnchorWithBounds>> = mutableListOf()
        for (a: AnchorWithBounds in sorted) {
            val lastRow: MutableList<AnchorWithBounds>? = rows.lastOrNull()
            if (lastRow != null &&
                kotlin.math.abs(a.centerY - lastRow.first().centerY) <= thresholdY
            ) {
                lastRow.add(a)
            } else {
                rows.add(mutableListOf(a))
            }
        }
        for (row: MutableList<AnchorWithBounds> in rows) {
            row.sortBy { anchor: AnchorWithBounds -> anchor.centerX }
        }
        return rows
    }

    private fun harvestAnchorClusterToParsed(
        cluster: List<AnchorWithBounds>,
    ): ParsedTrainRow? {
        if (cluster.isEmpty()) {
            return null
        }
        val leftToRight: List<AnchorWithBounds> = cluster.sortedBy { it.centerX }
        val generalAnchor: AnchorWithBounds = leftToRight.first()
        val firstClassAnchor: AnchorWithBounds? = leftToRight.getOrNull(1)
        val gText: String = generalAnchor.text
        val fText: String = firstClassAnchor?.text ?: ""
        val rawTexts: MutableList<String> = mutableListOf()
        for (a: AnchorWithBounds in leftToRight) {
            rawTexts.add(a.text)
        }
        collectSiblingLineTexts(generalAnchor.node, rawTexts)
        val distinctTexts: List<String> = rawTexts.distinct()
        val key: String = TrainRowStableKey.fromRawTexts(distinctTexts)
        val gClickPrimary: AccessibilityNodeInfo? =
            NodeFinder.clickTargetForLabel(generalAnchor.node, 14)
        val fClickPrimary: AccessibilityNodeInfo? =
            firstClassAnchor?.let { anchor: AnchorWithBounds ->
                NodeFinder.clickTargetForLabel(anchor.node, 14)
            }
        val gClick: AccessibilityNodeInfo? = gClickPrimary
        val fClick: AccessibilityNodeInfo? = fClickPrimary
        val row: TrainRow = TrainRow(
            stableKey = key,
            rawLineTexts = distinctTexts,
            generalSeatCellText = gText,
            firstClassSeatCellText = fText,
        )
        return ParsedTrainRow(row, gClick, fClick)
    }

    private fun collectSiblingLineTexts(anchorNode: AccessibilityNodeInfo, out: MutableList<String>) {
        val parent: AccessibilityNodeInfo? = anchorNode.parent
        if (parent == null) {
            return
        }
        try {
            for (i in 0 until parent.childCount) {
                val child: AccessibilityNodeInfo = parent.getChild(i) ?: continue
                val tt: String = child.text?.toString()?.trim() ?: ""
                val cd: String = child.contentDescription?.toString()?.trim() ?: ""
                if (tt.isNotEmpty() && !isSeatLike(tt) && !out.contains(tt)) {
                    out.add(tt)
                }
                if (cd.isNotEmpty() && cd != tt && !isSeatLike(cd) && !out.contains(cd)) {
                    out.add(cd)
                }
            }
        } finally {
            parent.recycle()
        }
    }
}
