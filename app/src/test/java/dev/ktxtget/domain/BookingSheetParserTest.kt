package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingSheetParserTest {

    /** 실기 덤프(코레일톡 7.0.2, KTX 007 행 탭)에서 나온 시트 텍스트 그대로. */
    private val realSheetTexts: List<String> = listOf(
        "KTX 007",
        "운행 정보",
        "일반실",
        "특실",
        "자유석",
        "1량",
        "좌석 선택",
        "바로 예매",
    )

    @Test
    fun `실기 시트를 파싱한다`() {
        val sheet: BookingSheet = BookingSheetParser.parse(realSheetTexts)!!
        assertEquals("KTX 007", sheet.trainLabel)
        assertEquals("KTX", sheet.trainType)
        assertEquals("7", sheet.trainNumber)
        assertEquals(listOf("일반실", "특실"), sheet.classTabs)
        assertEquals(listOf("자유석", "1량"), sheet.seatOptionTexts)
        assertTrue(sheet.actionLabels.contains("바로 예매"))
        assertTrue(sheet.actionLabels.contains("좌석 선택"))
    }

    @Test
    fun `액션 버튼이 없으면 시트가 아니다`() {
        assertNull(BookingSheetParser.parse(listOf("서울 출발 경주 도착", "새로고침", "장바구니")))
        assertNull(BookingSheetParser.parse(emptyList()))
    }

    @Test
    fun `네자리 열차번호와 하이픈 종별을 읽는다`() {
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf("KTX-산천 4135", "일반실", "특실", "입석+좌석", "바로 예매"),
        )!!
        assertEquals("KTX-산천", sheet.trainType)
        assertEquals("4135", sheet.trainNumber)
    }

    @Test
    fun `우등실 표기를 등급 탭으로 인식한다`() {
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf("KTX-이음 709", "일반실", "우등실", "바로 예매"),
        )!!
        assertEquals(listOf("일반실", "우등실"), sheet.classTabs)
        assertEquals(
            "우등실",
            KorailLabels.classLabelFor(SeatColumn.FIRST_CLASS, sheet.classTabs),
        )
    }

    @Test
    fun `특실 표기면 특실 라벨을 쓴다`() {
        assertEquals(
            "특실",
            KorailLabels.classLabelFor(SeatColumn.FIRST_CLASS, listOf("일반실", "특실")),
        )
        assertEquals(
            "일반실",
            KorailLabels.classLabelFor(SeatColumn.GENERAL, listOf("일반실", "우등실")),
        )
    }

    @Test
    fun `예약대기만 가능한 시트도 파싱은 되지만 예매 버튼은 없다`() {
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf("KTX-산천 237", "일반실", "특실", "입석+좌석", "예약 대기 신청"),
        )!!
        assertEquals(listOf("예약 대기 신청"), sheet.actionLabels)
        assertTrue(!sheet.actionLabels.contains(KorailLabels.RESERVE_NOW))
    }

    @Test
    fun `앞자리 0은 떼고 열차번호를 맞춘다`() {
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf("KTX 001", "일반실", "바로 예매"),
        )!!
        assertEquals("1", sheet.trainNumber)
    }

    @Test
    fun `매진 시트의 비활성 버튼은 액션에서 뺀다`() {
        // 실기 덤프(코레일톡 7.0.10, 매진 KTX-이음 711 행 탭). 두 버튼 모두 enabled=false.
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf(
                "KTX-이음 711",
                "운행 정보",
                "이 열차는 모든 좌석이 판매되어 예매가 불가해요.",
                "좌석 선택",
                "바로 예매",
            ),
            disabledActions = setOf("좌석 선택", "바로 예매"),
        )!!
        assertEquals(emptyList<String>(), sheet.actionLabels)
        assertNull(sheet.reserveLabel)
    }

    @Test
    fun `입석 더하기 좌석 예매 버튼을 예매 버튼으로 인식한다`() {
        // 실기 덤프(코레일톡 7.0.10, KTX-산천 149). 좌석 옵션 줄 없이 버튼 하나만 있다.
        val sheet: BookingSheet = BookingSheetParser.parse(
            listOf("KTX-산천 149", "운행 정보", "일반실", "특실", "입석+좌석 예매"),
        )!!
        assertEquals("149", sheet.trainNumber)
        assertEquals("입석+좌석 예매", sheet.reserveLabel)
    }
}
