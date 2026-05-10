package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrainExcludeNumbersParserTest {
    @Test
    fun parse_comma_and_pad() {
        val actual: Set<String> = TrainExcludeNumbersParser.parseCommaSeparated("116, 82 ,082")
        assertEquals(setOf("116", "082"), actual)
    }

    @Test
    fun rejects_non_digit_and_long_tokens() {
        val actual: Set<String> = TrainExcludeNumbersParser.parseCommaSeparated("116, abc, 1234")
        assertEquals(setOf("116"), actual)
    }

    @Test
    fun normalize_three_digit_unchanged() {
        assertEquals("082", TrainExcludeNumbersParser.normalizeToken("082"))
    }

    @Test
    fun normalize_four_digits_null() {
        assertNull(TrainExcludeNumbersParser.normalizeToken("1234"))
    }
}
