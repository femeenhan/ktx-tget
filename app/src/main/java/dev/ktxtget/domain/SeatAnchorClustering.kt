package dev.ktxtget.domain

/**
 * Screen-coordinate hints for grouping seat cells (매진/가격) into one train row.
 * Used by [dev.ktxtget.accessibility.TrainRowTreeParser] when RecyclerView heuristics fail (e.g. 코레일톡).
 */
data class SeatAnchorMetric(
    val centerX: Int,
    val centerY: Int,
    val text: String,
)

object SeatAnchorClustering {

    fun clusterByYAxis(
        anchors: List<SeatAnchorMetric>,
        thresholdY: Int,
    ): List<List<SeatAnchorMetric>> {
        if (anchors.isEmpty()) {
            return emptyList()
        }
        val sorted: List<SeatAnchorMetric> =
            anchors.sortedWith(compareBy({ it.centerY }, { it.centerX }))
        val rows: MutableList<MutableList<SeatAnchorMetric>> = mutableListOf()
        for (a: SeatAnchorMetric in sorted) {
            val lastRow: MutableList<SeatAnchorMetric>? = rows.lastOrNull()
            if (lastRow != null &&
                kotlin.math.abs(a.centerY - lastRow.first().centerY) <= thresholdY
            ) {
                lastRow.add(a)
            } else {
                rows.add(mutableListOf(a))
            }
        }
        for (row: MutableList<SeatAnchorMetric> in rows) {
            row.sortBy { anchor: SeatAnchorMetric -> anchor.centerX }
        }
        return rows
    }
}
