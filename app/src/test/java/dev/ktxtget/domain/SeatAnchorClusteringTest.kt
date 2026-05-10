package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SeatAnchorClusteringTest {
    @Test
    fun groups_same_row_by_y() {
        val anchors: List<SeatAnchorMetric> = listOf(
            SeatAnchorMetric(100, 500, "매진"),
            SeatAnchorMetric(300, 505, "매진"),
            SeatAnchorMetric(100, 700, "매진"),
            SeatAnchorMetric(300, 698, "23,700원"),
        )
        val rows: List<List<SeatAnchorMetric>> =
            SeatAnchorClustering.clusterByYAxis(anchors, thresholdY = 48)
        assertEquals(2, rows.size)
        assertEquals(2, rows[0].size)
        assertEquals(2, rows[1].size)
        assertEquals("매진", rows[0][0].text)
        assertEquals("매진", rows[0][1].text)
    }
}
