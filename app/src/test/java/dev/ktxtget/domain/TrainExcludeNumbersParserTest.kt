package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainExcludeNumbersParserTest {

    @Test
    fun `앞자리 0을 뗀 형태로 정규화한다`() {
        assertEquals("7", TrainExcludeNumbersParser.normalizeToken("007"))
        assertEquals("7", TrainExcludeNumbersParser.normalizeToken("7"))
        assertEquals("121", TrainExcludeNumbersParser.normalizeToken("121"))
    }

    @Test
    fun `네자리 열차번호를 받는다`() {
        assertEquals("4135", TrainExcludeNumbersParser.normalizeToken("4135"))
        assertEquals("4031", TrainExcludeNumbersParser.normalizeToken("4031"))
    }

    @Test
    fun `다섯자리 이상과 숫자 아닌 값은 버린다`() {
        assertNull(TrainExcludeNumbersParser.normalizeToken("41350"))
        assertNull(TrainExcludeNumbersParser.normalizeToken("KTX"))
        assertNull(TrainExcludeNumbersParser.normalizeToken("12a"))
        assertNull(TrainExcludeNumbersParser.normalizeToken(""))
    }

    @Test
    fun `쉼표 목록을 파싱하고 표기가 달라도 같은 값으로 묶는다`() {
        assertEquals(
            setOf("7", "121", "4135"),
            TrainExcludeNumbersParser.parseCommaSeparated("007, 7, 121, 4135"),
        )
    }

    @Test
    fun `빈 조각과 공백은 무시한다`() {
        assertEquals(setOf("1"), TrainExcludeNumbersParser.parseCommaSeparated(" 001 , , "))
    }
}
