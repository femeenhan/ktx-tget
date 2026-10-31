package dev.ktxtget.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationSignalsTest {
    @Test
    fun `실기 7_1_x 예약 완료 안내창 문구를 잡는다`() {
        val screen = listOf(
            "이용 안내",
            "닫기",
            "승차권 예약이 완료되었습니다. 즉시 결제하시거나, 장바구니에 담아 나중에 결제하실 수 있습니다.",
        )
        assertTrue(ReservationSignals.isReservationDone(screen))
    }

    @Test
    fun `줄바꿈과 띄어쓰기가 달라도 잡는다`() {
        assertTrue(ReservationSignals.isReservationDone(listOf("승차권\n예약이 완료 되었습니다.")))
    }

    @Test
    fun `목록 화면과 마일리지 안내는 아니다`() {
        assertFalse(ReservationSignals.isReservationDone(listOf("서울", "부산", "KTX 042", "매진", "새로고침")))
        assertFalse(
            ReservationSignals.isReservationDone(
                listOf("KTX 동행자 마일리지 이용 안내", "열차별로 본인 승차권(1매)은 KTX 마일리지가 자동 적립됩니다.", "확인"),
            ),
        )
    }
}
