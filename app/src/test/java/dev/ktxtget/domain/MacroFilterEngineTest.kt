package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MacroFilterEngineTest {

    private fun sheet(
        label: String = "KTX 007",
        number: String = "7",
        seats: List<String> = listOf("1량"),
        actions: List<String> = listOf("좌석 선택", "바로 예매"),
    ): BookingSheet = BookingSheet(
        trainLabel = label,
        trainType = label.substringBefore(' '),
        trainNumber = number,
        classTabs = listOf("일반실", "특실"),
        seatOptionTexts = seats,
        actionLabels = actions,
    )

    @Test
    fun `기본 설정이면 통과한다`() {
        assertNull(MacroFilterEngine.rejectionReason(sheet(), MacroSettings.DEFAULT))
    }

    @Test
    fun `예약대기만 있는 시트는 막는다`() {
        val reason: String? = MacroFilterEngine.rejectionReason(
            sheet(actions = listOf("예약 대기 신청")),
            MacroSettings.DEFAULT,
        )
        assertNotNull(reason)
    }

    @Test
    fun `제외 열차번호는 앞자리 0과 무관하게 막는다`() {
        val settings: MacroSettings =
            MacroSettings.DEFAULT.copy(excludedTrainNumbers = setOf("7"))
        assertNotNull(MacroFilterEngine.rejectionReason(sheet(number = "7"), settings))
        assertNull(MacroFilterEngine.rejectionReason(sheet(number = "121"), settings))
    }

    @Test
    fun `네자리 열차번호도 제외할 수 있다`() {
        val settings: MacroSettings = MacroSettings.DEFAULT.copy(
            excludedTrainNumbers = TrainExcludeNumbersParser.parseCommaSeparated("4135"),
        )
        assertNotNull(
            MacroFilterEngine.rejectionReason(sheet(label = "KTX-산천 4135", number = "4135"), settings),
        )
    }

    @Test
    fun `자유석 제외 설정이 켜지면 막는다`() {
        val settings: MacroSettings = MacroSettings.DEFAULT.copy(excludeFreeseatRows = true)
        assertNotNull(MacroFilterEngine.rejectionReason(sheet(seats = listOf("자유석", "1량")), settings))
        assertNull(MacroFilterEngine.rejectionReason(sheet(seats = listOf("1량")), settings))
    }

    @Test
    fun `입석 더하기 좌석 제외가 켜지면 막는다`() {
        val settings: MacroSettings =
            MacroSettings.DEFAULT.copy(excludeGeneralStandingComboRows = true)
        assertNotNull(MacroFilterEngine.rejectionReason(sheet(seats = listOf("입석+좌석")), settings))
    }

    @Test
    fun `입석만 제외 설정은 입석 더하기 좌석을 막지 않는다`() {
        val settings: MacroSettings =
            MacroSettings.DEFAULT.copy(excludeGeneralStandingOnlyRows = true)
        assertNull(MacroFilterEngine.rejectionReason(sheet(seats = listOf("입석+좌석")), settings))
        assertNotNull(MacroFilterEngine.rejectionReason(sheet(seats = listOf("입석")), settings))
    }

    @Test
    fun `막힌 이유는 사람이 읽을 수 있는 문구다`() {
        val reason: String? = MacroFilterEngine.rejectionReason(
            sheet(seats = listOf("자유석")),
            MacroSettings.DEFAULT.copy(excludeFreeseatRows = true),
        )
        assertEquals("자유석 제외", reason)
    }

    @Test
    fun `시트에 매진 안내가 있으면 막는다`() {
        val reason: String? = MacroFilterEngine.rejectionReason(
            sheet(seats = listOf("이 열차는 모든 좌석이 판매되어 예매가 불가해요."), actions = emptyList()),
            MacroSettings.DEFAULT,
        )
        assertNotNull(reason)
    }

    @Test
    fun `입석 더하기 좌석 예매 버튼만 있는 시트는 기본 설정이면 통과한다`() {
        assertNull(
            MacroFilterEngine.rejectionReason(
                sheet(seats = emptyList(), actions = listOf("입석+좌석 예매")),
                MacroSettings.DEFAULT,
            ),
        )
    }

    @Test
    fun `입석 더하기 좌석 제외는 버튼 라벨로도 막는다`() {
        val settings: MacroSettings =
            MacroSettings.DEFAULT.copy(excludeGeneralStandingComboRows = true)
        val combo: BookingSheet = sheet(seats = emptyList(), actions = listOf("입석+좌석 예매"))
        assertNotNull(MacroFilterEngine.rejectionReason(combo, settings))
        val standingOnly: MacroSettings =
            MacroSettings.DEFAULT.copy(excludeGeneralStandingOnlyRows = true)
        assertNull(MacroFilterEngine.rejectionReason(combo, standingOnly))
    }
}
