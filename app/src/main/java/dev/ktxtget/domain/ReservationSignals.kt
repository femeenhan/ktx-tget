package dev.ktxtget.domain

/**
 * 「바로 예매」 뒤 코레일톡이 예약 성공을 알리는 문구. 7.1.x 실기(2026-10-31)에서
 * 「이용 안내」 창 본문 「승차권 예약이 완료되었습니다. 즉시 결제하시거나, 장바구니에 담아 …」가
 * 「승차권 정보 확인」 화면을 덮고 버튼은 「닫기」뿐이었다. 이 문구가 보이면 예매 화면 도달로 본다.
 */
object ReservationSignals {
    private val DONE_MARKERS: List<String> = listOf(
        "승차권 예약이 완료",
        "예약이 완료되었습니다",
    )

    fun isReservationDone(texts: List<String>): Boolean {
        return texts.any { raw: String ->
            val line: String = raw.replace('\n', ' ').replace(" ", "")
            DONE_MARKERS.any { marker: String -> line.contains(marker.replace(" ", "")) }
        }
    }
}
