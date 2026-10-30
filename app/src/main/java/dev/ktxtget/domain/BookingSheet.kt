package dev.ktxtget.domain

/**
 * 행을 탭하면 열리는 예매 바텀시트의 텍스트 스냅샷.
 *
 * 목록 행과 달리 시트는 접근성 트리에 텍스트를 온전히 노출한다(실기 검증). 그래서 이 앱은
 * "무엇이 풀렸는지"를 픽셀로 거칠게 감지하고, "정말 살 것인지"는 전부 여기서 판단한다.
 */
data class BookingSheet(
    /** 시트 제목 원문. 예: "KTX 007" */
    val trainLabel: String,
    /** 열차 종별. 예: "KTX", "KTX-산천", "KTX-이음" */
    val trainType: String,
    /** 앞의 0을 뗀 열차번호. 예: "7", "4135" */
    val trainNumber: String,
    /** 시트에 있는 좌석 등급 탭. 예: ["일반실", "특실"] */
    val classTabs: List<String>,
    /** 좌석 옵션 줄의 텍스트. 예: ["자유석", "1량"] */
    val seatOptionTexts: List<String>,
    /** 누를 수 있는(활성) 하단 액션 버튼 라벨. 예: ["좌석 선택", "바로 예매"] */
    val actionLabels: List<String>,
) {
    /** 필터 판정용으로 좌석 옵션을 한 덩어리로 본 문자열. */
    val seatOptionBlob: String get() = seatOptionTexts.joinToString(" ")

    /** 누를 예매 버튼 라벨. 활성 예매 버튼이 없으면 null. */
    val reserveLabel: String?
        get() = KorailLabels.RESERVE_LABELS.firstOrNull { label: String -> actionLabels.contains(label) }

    /**
     * 시트가 좌석 조회를 끝내고 결론이 난 상태인지.
     *
     * 시트는 열리자마자 제목과 버튼 틀이 먼저 그려지고, 좌석 조회가 끝날 때까지 버튼이 전부
     * 비활성이다. 그 순간을 읽으면 활성 버튼이 하나도 없어 "예매 버튼 없음"으로 잘못 거절한다
     * (실기: 좌석이 풀린 행을 열고도 1초 안에 건너뛴 뒤 새로고침으로 돌아감). 활성 버튼이
     * 하나라도 생기거나 매진 안내가 뜨면 결론이 난 것이다.
     */
    val isSettled: Boolean
        get() = actionLabels.isNotEmpty() || seatOptionBlob.contains(KorailLabels.SHEET_SOLD_OUT_NOTICE)
}

object BookingSheetParser {

    /** "KTX 007", "KTX-산천 4135" 형태의 시트 제목. */
    private val TRAIN_LABEL: Regex = Regex("""^(\S+)\s+(\d{1,4})$""")

    /** 시트가 떠 있는지 판별하는 근거이자, 탭 대상 후보. */
    val ACTION_LABELS: List<String> = listOf(
        KorailLabels.RESERVE_NOW,
        KorailLabels.RESERVE_STANDING_PLUS_SEAT,
        KorailLabels.CHOOSE_SEAT,
        KorailLabels.REQUEST_WAITLIST,
    )

    val CLASS_TAB_LABELS: List<String> = listOf(
        KorailLabels.CLASS_GENERAL,
        KorailLabels.CLASS_FIRST,
        KorailLabels.CLASS_SUPERIOR,
    )

    /**
     * [texts]는 시트 서브트리의 text·contentDescription을 트리 순서대로 모은 것이다.
     * 액션 버튼이 하나도 없으면 시트가 아니라고 보고 null을 돌려준다.
     * [disabledActions]는 화면에 있지만 비활성인 버튼 라벨이다 — 시트 판별에는 쓰되
     * [BookingSheet.actionLabels]에서는 뺀다.
     */
    fun parse(texts: List<String>, disabledActions: Set<String> = emptySet()): BookingSheet? {
        val trimmed: List<String> = texts.map { t: String -> t.trim() }.filter { it.isNotEmpty() }
        val present: List<String> = ACTION_LABELS.filter { label: String -> trimmed.contains(label) }
        if (present.isEmpty()) {
            return null
        }
        val actions: List<String> = present.filter { label: String -> label !in disabledActions }
        var label: String = ""
        var type: String = ""
        var number: String = ""
        for (text: String in trimmed) {
            val match: MatchResult = TRAIN_LABEL.find(text) ?: continue
            label = text
            type = match.groupValues[1]
            number = match.groupValues[2].trimStart('0').ifEmpty { "0" }
            break
        }
        val tabs: List<String> = CLASS_TAB_LABELS.filter { t: String -> trimmed.contains(t) }
        val seatTexts: List<String> = trimmed.filter { text: String ->
            text != label &&
                !ACTION_LABELS.contains(text) &&
                !CLASS_TAB_LABELS.contains(text) &&
                text != KorailLabels.TRAIN_INFO
        }
        return BookingSheet(
            trainLabel = label,
            trainType = type,
            trainNumber = number,
            classTabs = tabs,
            seatOptionTexts = seatTexts,
            actionLabels = actions,
        )
    }
}
