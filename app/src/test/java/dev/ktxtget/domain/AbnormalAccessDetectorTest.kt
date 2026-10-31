package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AbnormalAccessDetectorTest {
    @Test
    fun `보통 열차 목록 문구는 지나간다`() {
        val texts = listOf("서울", "부산", "KTX 101", "매진", "일반실", "특실", "새로고침", "좌석 부족")
        assertNull(AbnormalAccessDetector.match(texts))
    }

    @Test
    fun `비정상 접근 문구를 잡는다`() {
        val texts = listOf("알림", "비정상적인 접근이 감지되어 서비스 이용이 제한됩니다.", "확인")
        assertEquals("비정상적인 접근이 감지되어 서비스 이용이 제한됩니다.", AbnormalAccessDetector.match(texts))
    }

    @Test
    fun `띄어쓰기가 달라도 잡는다`() {
        assertEquals("비정상적인접근입니다", AbnormalAccessDetector.match(listOf("비정상적인접근입니다")))
        assertEquals("자동화  프로그램 사용 금지", AbnormalAccessDetector.match(listOf("자동화  프로그램 사용 금지")))
    }

    @Test
    fun `긴 문구는 60자로 자른다`() {
        val long = "매크로 " + "가".repeat(100)
        assertEquals(60, AbnormalAccessDetector.match(listOf(long))?.length)
    }

    @Test
    fun `줄바꿈은 공백으로 바꾼다`() {
        assertEquals("과도한 요청으로 잠시 후 다시 시도", AbnormalAccessDetector.match(listOf("과도한 요청으로\n잠시 후 다시 시도")))
    }
}
