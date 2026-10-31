package dev.ktxtget.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 실기(SM-F731N 1080x2640 / 코레일톡 7.0.2) 스크린샷에서 실측한 색과 좌표를 그대로 쓴다.
 * 값이 바뀌면 기기에서 다시 재보고 여기부터 고칠 것.
 */
class SoldOutPixelClassifierTest {

    private companion object {
        const val WIDTH: Int = 1080
        const val HEIGHT: Int = 2640

        /** 매진 글자색 (코레일톡 7.0.2 실측). */
        const val SOLD_OUT_RED: Int = 0xFFFF5359.toInt()

        /** 매진 글자색 (코레일톡 7.1.0 실측, 2026-10-30 SM-F731N). 7.0.x보다 진하고 어둡다. */
        const val SOLD_OUT_RED_710: Int = 0xFFDF1B35.toInt()

        /** 「마감임박」 모래시계 이모지 — 매진과 완전히 같은 색이라 위치로만 구분된다. */
        const val HOURGLASS_RED: Int = 0xFFFF5359.toInt()

        /** 「좌석 부족」 글자색 (코레일톡 7.1.0 실측, 2026-10-31 SM-F731N). 「일반실/특실」 라벨과 같은 회색. */
        const val SHORTAGE_GREY: Int = 0xFF707177.toInt()

        /** 「입석+좌석」 글자색 (코레일톡 7.1.0 실측). */
        const val STANDING_DARK: Int = 0xFF10131C.toInt()

        /** 「M 5%적립」 배지 주황. */
        const val REWARD_BADGE: Int = 0xFFD45F08.toInt()

        /** 「할인15%」·「운임15%」 초록. */
        const val DISCOUNT_GREEN: Int = 0xFF00A562.toInt()

        const val BACKGROUND: Int = 0xFF101216.toInt()

        /** 실측 행 하나: KTX 027 행의 Button bounds. */
        val ROW: PixelRect = PixelRect(45, 809, 1035, 1091)
    }

    private fun blankImage(): IntArray = IntArray(WIDTH * HEIGHT) { BACKGROUND }

    private fun paint(image: IntArray, color: Int, x0: Int, y0: Int, x1: Int, y1: Int) {
        for (y: Int in y0 until y1) {
            for (x: Int in x0 until x1) {
                image[y * WIDTH + x] = color
            }
        }
    }

    /**
     * 캡처 경로(ScreenshotTrainListSensor)와 같은 순서로 자른 뒤 판정한다.
     * 좌표 계산은 [SoldOutPixelClassifier.stripRect] / [SoldOutPixelClassifier.splitYInStrip]을
     * 그대로 쓰므로, 실행 경로와 어긋날 수 없다.
     */
    private fun classify(image: IntArray, row: PixelRect = ROW): SoldOutSignal {
        val strip: PixelRect = SoldOutPixelClassifier.stripRect(row, WIDTH, HEIGHT)
        if (strip.width == 0 || strip.height == 0) {
            return SoldOutSignal(generalSoldOut = false, premiumSoldOut = false)
        }
        val buffer = IntArray(strip.width * strip.height)
        for (y: Int in 0 until strip.height) {
            for (x: Int in 0 until strip.width) {
                buffer[y * strip.width + x] = image[(strip.top + y) * WIDTH + strip.left + x]
            }
        }
        return SoldOutPixelClassifier.classifyStrip(
            buffer,
            strip.width,
            strip.height,
            SoldOutPixelClassifier.splitYInStrip(row, strip),
        )
    }

    @Test
    fun `매진 글자색만 참으로 판정한다`() {
        assertTrue(SoldOutPixelClassifier.isSoldOutRed(SOLD_OUT_RED))
        assertTrue(SoldOutPixelClassifier.isSoldOutRed(SOLD_OUT_RED_710))
        assertFalse(SoldOutPixelClassifier.isSoldOutRed(REWARD_BADGE))
        assertFalse(SoldOutPixelClassifier.isSoldOutRed(DISCOUNT_GREEN))
        assertFalse(SoldOutPixelClassifier.isSoldOutRed(BACKGROUND))
        assertFalse(SoldOutPixelClassifier.isSoldOutRed(0xFFFFFFFF.toInt()))
    }

    @Test
    fun `7_1_0의 진한 빨강도 매진으로 잡는다`() {
        // 7.1.0에서 매진 색이 (223,27,53)으로 바뀌어 기존 범위(g 56~124)에 걸리지 않았고,
        // 그 탓에 모든 행이 「매진 아님」으로 읽혀 매크로가 전이를 한 번도 못 잡았다(실기).
        val image: IntArray = blankImage()
        paint(image, SOLD_OUT_RED_710, 900, 850, 1030, 890)
        paint(image, SOLD_OUT_RED_710, 900, 1000, 1030, 1040)
        assertEquals(SoldOutSignal(generalSoldOut = true, premiumSoldOut = true), classify(image))
    }

    @Test
    fun `매진이 없으면 양쪽 다 false`() {
        assertEquals(SoldOutSignal(false, false), classify(blankImage()))
    }

    @Test
    fun `상단 값컬럼의 빨강은 일반실 매진`() {
        val image: IntArray = blankImage()
        paint(image, SOLD_OUT_RED, 900, 850, 1030, 890)
        assertEquals(SoldOutSignal(generalSoldOut = true, premiumSoldOut = false), classify(image))
    }

    @Test
    fun `하단 값컬럼의 빨강은 특실 매진`() {
        val image: IntArray = blankImage()
        paint(image, SOLD_OUT_RED, 900, 1000, 1030, 1040)
        assertEquals(SoldOutSignal(generalSoldOut = false, premiumSoldOut = true), classify(image))
    }

    @Test
    fun `양쪽 매진을 모두 잡는다`() {
        val image: IntArray = blankImage()
        paint(image, SOLD_OUT_RED, 900, 850, 1030, 890)
        paint(image, SOLD_OUT_RED, 900, 1000, 1030, 1040)
        assertEquals(SoldOutSignal(generalSoldOut = true, premiumSoldOut = true), classify(image))
    }

    @Test
    fun `마감임박 이모지는 값컬럼 왼쪽이라 매진으로 오인하지 않는다`() {
        val image: IntArray = blankImage()
        // 실측 이모지 위치 x=695~719. 값컬럼 시작(837)보다 왼쪽이라 띠 밖이다.
        paint(image, HOURGLASS_RED, 695, 850, 720, 890)
        paint(image, HOURGLASS_RED, 695, 1000, 720, 1040)
        assertEquals(SoldOutSignal(false, false), classify(image))
    }

    @Test
    fun `적립 배지와 할인 초록은 매진으로 오인하지 않는다`() {
        val image: IntArray = blankImage()
        paint(image, REWARD_BADGE, 860, 900, 1030, 940)
        paint(image, DISCOUNT_GREEN, 860, 1040, 1030, 1080)
        assertEquals(SoldOutSignal(false, false), classify(image))
    }

    @Test
    fun `좌석 부족 회색은 매진으로 센다`() {
        // 실측: 좌석 부족 셀은 회색 1050~1065px. 매진↔좌석 부족이 번갈아 떠도 전이가 아니어야 한다.
        assertTrue(SoldOutPixelClassifier.isUnavailableGrey(SHORTAGE_GREY))
        assertFalse(SoldOutPixelClassifier.isUnavailableGrey(STANDING_DARK))
        assertFalse(SoldOutPixelClassifier.isUnavailableGrey(SOLD_OUT_RED_710))
        val image: IntArray = blankImage()
        paint(image, SHORTAGE_GREY, 915, 850, 1030, 860) // 1150px, 위 칸
        assertEquals(SoldOutSignal(generalSoldOut = true, premiumSoldOut = false), classify(image))
    }

    @Test
    fun `띠에 걸친 일반실 특실 라벨의 회색은 좌석 부족으로 오인하지 않는다`() {
        // 실측: 라벨 끝이 띠에 196~257px 새어 들어온다. 300px까지는 무시해야 한다.
        val image: IntArray = blankImage()
        paint(image, SHORTAGE_GREY, 840, 850, 870, 860) // 300px, 위 칸
        paint(image, SHORTAGE_GREY, 840, 1000, 870, 1010) // 300px, 아래 칸
        assertEquals(SoldOutSignal(false, false), classify(image))
    }

    @Test
    fun `입석+좌석의 어두운 글자는 매진이 아니다`() {
        val image: IntArray = blankImage()
        paint(image, STANDING_DARK, 915, 850, 1030, 860)
        assertEquals(SoldOutSignal(false, false), classify(image))
    }

    @Test
    fun `잡티 수준의 빨강은 임계값 미만이라 무시한다`() {
        val image: IntArray = blankImage()
        for (i: Int in 0 until SoldOutPixelClassifier.MIN_PIXELS - 1) {
            image[(850 + i / 100) * WIDTH + 900 + (i % 100)] = SOLD_OUT_RED
        }
        assertEquals(SoldOutSignal(false, false), classify(image))
    }

    @Test
    fun `행이 화면 밖으로 잘려도 예외 없이 판정한다`() {
        val clipped = PixelRect(45, HEIGHT - 40, 1035, HEIGHT + 240)
        assertEquals(SoldOutSignal(false, false), classify(blankImage(), clipped))
    }

    @Test
    fun `읽는 띠는 행 우측 20퍼센트로 한정된다`() {
        val strip: PixelRect = SoldOutPixelClassifier.stripRect(ROW, WIDTH, HEIGHT)
        assertEquals(837, strip.left)
        assertEquals(1035, strip.right)
        assertEquals(141, SoldOutPixelClassifier.splitYInStrip(ROW, strip))
    }

    @Test
    fun `한 행을 읽는 데 필요한 버퍼는 화면 전체의 5퍼센트 미만이다`() {
        // 화면 전체를 IntArray로 뜨다가 OOM으로 앱이 죽은 적이 있다(실기). 그 회귀 방지.
        val strip: PixelRect = SoldOutPixelClassifier.stripRect(ROW, WIDTH, HEIGHT)
        val stripPixels: Int = strip.width * strip.height
        assertTrue(
            "띠 $stripPixels px 는 전체 ${WIDTH * HEIGHT} px 의 5% 미만이어야 한다",
            stripPixels * 20 < WIDTH * HEIGHT,
        )
    }
}
