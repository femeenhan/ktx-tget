package dev.ktxtget.domain

/**
 * 한 열차 행의 좌석 칸별 「매진」 여부. 픽셀 판정 결과라 매진/비매진 두 값만 구분한다.
 * (가격·예약대기·자유석·입석+좌석은 전부 흰 글씨라 서로 구별하지 않는다 —
 *  정확한 좌석 상태는 행을 탭한 뒤 바텀시트 텍스트로 읽는다.)
 */
data class SoldOutSignal(
    val generalSoldOut: Boolean,
    val premiumSoldOut: Boolean,
) {
    fun soldOutAt(column: SeatColumn): Boolean {
        return when (column) {
            SeatColumn.GENERAL -> generalSoldOut
            SeatColumn.FIRST_CLASS -> premiumSoldOut
        }
    }
}
