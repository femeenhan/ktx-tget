package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoldOutTransitionTest {

    private fun rows(count: Int): List<PixelRect> =
        (0 until count).map { i: Int -> PixelRect(45, 809 + i * 361, 1035, 1091 + i * 361) }

    @Test
    fun `첫 관측은 기준선만 잡고 아무것도 돌려주지 않는다`() {
        val tracker = SoldOutTransitionTracker()
        val opening: SeatOpening? = tracker.observe(
            rows(2),
            listOf(SoldOutSignal(true, true), SoldOutSignal(true, true)),
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertNull(opening)
    }

    @Test
    fun `매진이 풀리면 해당 행과 칸을 알려준다`() {
        val tracker = SoldOutTransitionTracker()
        val r = rows(2)
        tracker.observe(
            r,
            listOf(SoldOutSignal(true, true), SoldOutSignal(true, true)),
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        val opening: SeatOpening? = tracker.observe(
            r,
            listOf(SoldOutSignal(true, true), SoldOutSignal(false, true)),
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertEquals(SeatOpening(1, SeatColumn.GENERAL), opening)
    }

    @Test
    fun `매진으로 다시 막히는 방향은 전이가 아니다`() {
        val tracker = SoldOutTransitionTracker()
        val r = rows(1)
        tracker.observe(r, listOf(SoldOutSignal(false, false)), SeatClickPreference.BOTH_PREFER_GENERAL)
        val opening: SeatOpening? =
            tracker.observe(r, listOf(SoldOutSignal(true, true)), SeatClickPreference.BOTH_PREFER_GENERAL)
        assertNull(opening)
    }

    @Test
    fun `일반실만 설정이면 특실이 풀려도 무시한다`() {
        val tracker = SoldOutTransitionTracker()
        val r = rows(1)
        tracker.observe(r, listOf(SoldOutSignal(true, true)), SeatClickPreference.GENERAL_ONLY)
        val opening: SeatOpening? =
            tracker.observe(r, listOf(SoldOutSignal(true, false)), SeatClickPreference.GENERAL_ONLY)
        assertNull(opening)
    }

    @Test
    fun `둘 다 풀리면 일반실을 먼저 고른다`() {
        val tracker = SoldOutTransitionTracker()
        val r = rows(1)
        tracker.observe(r, listOf(SoldOutSignal(true, true)), SeatClickPreference.BOTH_PREFER_GENERAL)
        val opening: SeatOpening? =
            tracker.observe(r, listOf(SoldOutSignal(false, false)), SeatClickPreference.BOTH_PREFER_GENERAL)
        assertEquals(SeatOpening(0, SeatColumn.GENERAL), opening)
    }

    @Test
    fun `목록 구조가 바뀌면 비교하지 않고 기준선을 새로 잡는다`() {
        val tracker = SoldOutTransitionTracker()
        tracker.observe(rows(3), List(3) { SoldOutSignal(true, true) }, SeatClickPreference.BOTH_PREFER_GENERAL)
        // 스크롤·열차 출발 등으로 행 구성이 달라진 경우
        val opening: SeatOpening? = tracker.observe(
            rows(2),
            List(2) { SoldOutSignal(false, false) },
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertNull(opening)
    }

    @Test
    fun `기준선을 새로 잡은 다음 주기부터 다시 전이를 잡는다`() {
        val tracker = SoldOutTransitionTracker()
        val r = rows(2)
        tracker.observe(rows(3), List(3) { SoldOutSignal(true, true) }, SeatClickPreference.BOTH_PREFER_GENERAL)
        tracker.observe(r, List(2) { SoldOutSignal(true, true) }, SeatClickPreference.BOTH_PREFER_GENERAL)
        val opening: SeatOpening? = tracker.observe(
            r,
            listOf(SoldOutSignal(true, true), SoldOutSignal(true, false)),
            SeatClickPreference.BOTH_PREFER_GENERAL,
        )
        assertEquals(SeatOpening(1, SeatColumn.FIRST_CLASS), opening)
    }
}
