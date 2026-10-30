package dev.ktxtget.domain

/**
 * 매크로를 켠 직후 첫 판독에서 **이미 매진이 아닌 칸**이 있으면 그중 하나를 한 번만 확인한다.
 *
 * 전이 추적([SoldOutTransition])은 「매진 → 매진 아님」만 보므로, 켜는 순간 이미 풀려 있던 좌석은
 * 다시 매진됐다 풀리기 전까지 탭하지 않는다. 그런 상황은 대개 사람이 직접 사면 되는 상태라
 * 최소한만 메운다: 우선순위([preference] 등급 순, 그 안에서 위 행부터)로 첫 칸 하나만, 시작 시
 * 한 번만. 픽셀 판정은 가격·예약대기·입석을 구분하지 못하므로 실제 판단은 시트 필터가 한다.
 */
object InitialOpenSweep {

    fun firstCandidate(
        signals: List<SoldOutSignal>,
        preference: SeatClickPreference,
    ): SeatOpening? {
        val columns: List<SeatColumn> = when (preference) {
            SeatClickPreference.GENERAL_ONLY -> listOf(SeatColumn.GENERAL)
            SeatClickPreference.FIRST_CLASS_ONLY -> listOf(SeatColumn.FIRST_CLASS)
            SeatClickPreference.BOTH_PREFER_GENERAL ->
                listOf(SeatColumn.GENERAL, SeatColumn.FIRST_CLASS)
        }
        for (column: SeatColumn in columns) {
            for (index: Int in signals.indices) {
                if (!signals[index].soldOutAt(column)) {
                    return SeatOpening(index, column)
                }
            }
        }
        return null
    }
}
