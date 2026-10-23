package dev.ktxtget.domain

/**
 * 코레일톡 7.x 화면 문구. 실기(SM-F731N / 코레일톡 7.0.2) 접근성 덤프에서 확인한 값이다.
 *
 * 이 앱이 붙는 화면 전부에서 **라벨은 자식 TextView가, 클릭 가능 속성은 부모 View가** 들고 있다.
 * 그래서 라벨로 노드를 찾은 뒤 클릭 가능한 조상으로 올라가야 한다 — [dev.ktxtget.util.NodeFinder] 참고.
 */
object KorailLabels {
    /** 목록 상단 앱바의 새로고침. 아이콘 버튼이고 contentDescription으로만 노출된다. */
    const val REFRESH: String = "새로고침"

    /** 바텀시트 기본 예매 버튼. 구 버전의 "예매"에서 바뀌었다. */
    const val RESERVE_NOW: String = "바로 예매"

    /** 입석+좌석만 남은 열차의 예매 버튼. 「바로 예매」 대신 이 하나만 뜬다 (7.0.10 실기). */
    const val RESERVE_STANDING_PLUS_SEAT: String = "입석+좌석 예매"

    /** 누르면 바로 예매로 이어지는 버튼들. 우선순위 순. */
    val RESERVE_LABELS: List<String> = listOf(RESERVE_NOW, RESERVE_STANDING_PLUS_SEAT)

    /**
     * 매진 열차 시트의 안내 문구. 7.0.10부터 매진 행도 시트가 열리고,
     * 「좌석 선택」「바로 예매」가 비활성(enabled=false)인 채로 함께 보인다.
     */
    const val SHEET_SOLD_OUT_NOTICE: String = "모든 좌석이 판매되어"

    /** 좌석을 직접 고르는 경로. 단계가 늘어 기본으로는 쓰지 않는다. */
    const val CHOOSE_SEAT: String = "좌석 선택"

    /**
     * 예약 대기 신청. 결제가 아니라 대기 등록이라 **자동으로 누르지 않는다.**
     * 목록에 이것만 있는 열차는 시트에서 걸러 뒤로 나온다.
     */
    const val REQUEST_WAITLIST: String = "예약 대기 신청"

    const val CLASS_GENERAL: String = "일반실"
    const val CLASS_FIRST: String = "특실"

    /** KTX-이음 등은 특실 대신 우등실로 표기된다. */
    const val CLASS_SUPERIOR: String = "우등실"

    /** 시트 우상단 링크. 좌석 옵션 텍스트에서 제외하려고 둔다. */
    const val TRAIN_INFO: String = "운행 정보"

    /** 좌석 상태 문구 (시트 기준). */
    const val FREE_SEAT: String = "자유석"
    const val STANDING_PLUS_SEAT: String = "입석+좌석"
    const val STANDING: String = "입석"

    fun classLabelFor(column: SeatColumn, tabs: List<String>): String {
        return when (column) {
            SeatColumn.GENERAL -> CLASS_GENERAL
            SeatColumn.FIRST_CLASS ->
                if (tabs.contains(CLASS_SUPERIOR)) CLASS_SUPERIOR else CLASS_FIRST
        }
    }
}
