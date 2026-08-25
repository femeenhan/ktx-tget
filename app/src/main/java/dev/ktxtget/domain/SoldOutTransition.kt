package dev.ktxtget.domain

/** 매진이 풀린 자리. [rowIndex]는 화면에 보이는 순서(0부터). */
data class SeatOpening(
    val rowIndex: Int,
    val column: SeatColumn,
)

/**
 * 새로고침 사이의 「매진 → 매진 아님」 전이를 찾는다.
 *
 * 픽셀 판정은 매진/비매진만 구분하므로 여기서 나온 전이는 "뭔가 풀렸다"까지만 뜻한다.
 * 실제로 살 수 있는 좌석인지(예약대기·입석+좌석·자유석 여부)는 행을 탭한 뒤
 * 바텀시트 텍스트로 판단한다 — [BookingSheet] 참고.
 */
object SoldOutTransition {

    /**
     * 행 개수·위치가 달라지면(스크롤, 열차 출발로 목록 변동) 직전 상태와 행이 어긋나므로
     * 비교 자체를 버려야 한다. 그 판단에 쓰는 지문.
     */
    fun fingerprint(rows: List<PixelRect>): String {
        return rows.joinToString(",") { r: PixelRect -> "${r.top}:${r.bottom}" }
    }

    /**
     * [preference] 우선순위대로 훑어 처음 발견한 전이를 돌려준다.
     * [previous]가 비었거나 길이가 다르면 비교하지 않는다(첫 관측 취급).
     */
    fun findOpening(
        previous: List<SoldOutSignal>,
        current: List<SoldOutSignal>,
        preference: SeatClickPreference,
    ): SeatOpening? {
        if (previous.isEmpty() || previous.size != current.size) {
            return null
        }
        val columns: List<SeatColumn> = when (preference) {
            SeatClickPreference.GENERAL_ONLY -> listOf(SeatColumn.GENERAL)
            SeatClickPreference.FIRST_CLASS_ONLY -> listOf(SeatColumn.FIRST_CLASS)
            SeatClickPreference.BOTH_PREFER_GENERAL ->
                listOf(SeatColumn.GENERAL, SeatColumn.FIRST_CLASS)
        }
        for (column: SeatColumn in columns) {
            for (index: Int in current.indices) {
                val before: Boolean = previous[index].soldOutAt(column)
                val after: Boolean = current[index].soldOutAt(column)
                if (before && !after) {
                    return SeatOpening(index, column)
                }
            }
        }
        return null
    }
}

/**
 * 새로고침 주기마다 [observe]를 호출하면 전이가 생긴 순간에만 [SeatOpening]을 돌려준다.
 * 목록 구조가 바뀌면 기준선을 조용히 새로 잡는다.
 */
class SoldOutTransitionTracker {
    private var fingerprint: String = ""
    private var previous: List<SoldOutSignal> = emptyList()

    fun observe(
        rows: List<PixelRect>,
        signals: List<SoldOutSignal>,
        preference: SeatClickPreference,
    ): SeatOpening? {
        val nowFingerprint: String = SoldOutTransition.fingerprint(rows)
        if (nowFingerprint != fingerprint) {
            fingerprint = nowFingerprint
            previous = signals
            return null
        }
        val opening: SeatOpening? =
            SoldOutTransition.findOpening(previous, signals, preference)
        previous = signals
        return opening
    }

    fun reset() {
        fingerprint = ""
        previous = emptyList()
    }
}
