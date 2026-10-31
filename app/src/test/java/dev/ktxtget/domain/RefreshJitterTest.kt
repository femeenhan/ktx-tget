package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RefreshJitterTest {

    @Test
    fun `설정값의 0_85배에서 1_35배 사이에 머문다`() {
        val random = Random(7)
        repeat(2000) {
            val v: Long = RefreshJitter.jittered(2500L, random)
            assertTrue("$v", v in 2125L..3375L)
        }
    }

    @Test
    fun `양끝 값이 실제로 나온다`() {
        val random = Random(11)
        val samples: List<Long> = List(5000) { RefreshJitter.jittered(2000L, random) }
        assertTrue(samples.min() < 1750L)
        assertTrue(samples.max() > 2650L)
    }

    @Test
    fun `평균은 설정값보다 약간 길다`() {
        val random = Random(3)
        val avg: Double = List(10_000) { RefreshJitter.jittered(2500L, random) }.average()
        assertTrue("$avg", avg in 2650.0..2850.0)
    }

    @Test
    fun `0 이하는 0`() {
        assertEquals(0L, RefreshJitter.jittered(0L))
    }
}
