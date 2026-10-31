package dev.ktxtget.domain

/**
 * 열차 목록 행에서 「매진」 라벨을 픽셀로 판정한다.
 *
 * 코레일톡 7.x는 목록 행을 접근성 트리에 `Button text='' content-desc='' NAF=true` 인 빈 리프로만
 * 노출해서(실기 검증), 화면 캡처가 좌석 상태를 읽는 유일한 경로다.
 *
 * 판정 규칙 — SM-F731N(1080x2640) / 코레일톡 7.0.2 스크린샷 10개 행으로 검증:
 * - 매진 글자색은 [isSoldOutRed] 범위(7.0.2·7.1.0 실측색 모두 포함). 「M 5%적립」 배지(212,95,8)·「할인15%」 초록(0,165,98)과 겹치지 않는다.
 * - 행의 우측 [VALUE_COLUMN_LEFT_RATIO] 구간만 본다. 「마감임박」 모래시계 이모지는 매진과
 *   **완전히 같은 색(255,83,89)** 이라 색으로는 구분할 수 없고, 클래스 라벨 왼쪽에 있다는
 *   위치로만 걸러진다. 이 구간을 넓히면 오검출이 생긴다.
 * - 행을 상하로 이등분해 위=일반실, 아래=특실·우등실로 본다. 적립 배지 유무로 셀 높이가 달라져도
 *   두 셀이 각각 위/아래 절반 안에 머문다.
 *
 * 검증 시 매진 셀은 1000px 이상, 비매진 셀은 0px이 나왔다. [MIN_PIXELS]는 그 사이의 여유값이다.
 * - 「좌석 부족」(인원수보다 남은 좌석이 적음)은 회색 [isUnavailableGrey] 글자다. 매진과 번갈아 뜨면
 *   전이로 오인돼 탭·시트 닫기를 반복하므로, 이 글자도 「살 수 없음」=매진으로 친다.
 *   같은 회색인 「일반실/특실」 라벨 끝이 띠에 조금 걸치므로(실측 196~257px) 임계값을
 *   [MIN_GREY_PIXELS]로 따로 높게 둔다. 좌석 부족 셀은 1050~1065px(코레일톡 7.1.0 실측).
 *
 * **화면 전체가 아니라 [valueColumnLeft] 오른쪽 좁은 띠만 읽는다.** 전체를 IntArray로 뜨면
 * 1080x2640 기준 한 번에 11MB라 몇 주기 만에 힙이 터진다(실기에서 실제로 OOM 크래시 발생).
 */
object SoldOutPixelClassifier {

    /** 행 폭 기준, 값 컬럼이 시작하는 비율. 이보다 왼쪽(클래스 라벨·마감임박 이모지)은 보지 않는다. */
    const val VALUE_COLUMN_LEFT_RATIO: Double = 0.80

    /** 한 셀을 매진으로 볼 최소 픽셀 수. */
    const val MIN_PIXELS: Int = 150

    /** 한 셀을 「좌석 부족」으로 볼 최소 회색 픽셀 수. 라벨이 새어 들어오는 양보다 넉넉히 위. */
    const val MIN_GREY_PIXELS: Int = 600

    /** [row] 안에서 좌석 상태 글자가 있는 구간의 왼쪽 x. */
    fun valueColumnLeft(row: PixelRect): Int {
        return row.left + (row.width * VALUE_COLUMN_LEFT_RATIO).toInt()
    }

    /**
     * [row]에서 실제로 읽어야 할 띠. 이미지 밖으로 나간 부분은 잘라낸다.
     * 캡처 경로와 테스트가 같은 계산을 쓰도록 여기에 둔다.
     */
    fun stripRect(row: PixelRect, imageWidth: Int, imageHeight: Int): PixelRect {
        val left: Int = valueColumnLeft(row).coerceIn(0, imageWidth)
        val top: Int = row.top.coerceIn(0, imageHeight)
        val right: Int = minOf(row.right, imageWidth).coerceAtLeast(left)
        val bottom: Int = minOf(row.bottom, imageHeight).coerceAtLeast(top)
        return PixelRect(left, top, right, bottom)
    }

    /** 띠 안에서 일반실(위)과 특실·우등실(아래)을 가르는 y. */
    fun splitYInStrip(row: PixelRect, strip: PixelRect): Int {
        return (row.top + row.height / 2) - strip.top
    }

    /**
     * 매진 글자색 판정. 버전마다 실측색이 다르다:
     * - 코레일톡 7.0.2: (255, 83, 89)
     * - 코레일톡 7.1.0: (223, 27, 53) — 더 진하고 어둡다.
     *
     * 특정 색 하나가 아니라 「채도 높은 빨강」 범위로 본다. 초록(할인/운임 안내)은 r 조건에서,
     * 주황 적립 배지(212,95,8)는 g·b 차이 조건에서 걸러진다. 7.0.2 값만 보던 옛 범위(g 56~124)는
     * 7.1.0 색을 통째로 놓쳐 모든 행이 「매진 아님」으로 읽혔다(실기).
     */
    fun isSoldOutRed(argb: Int): Boolean {
        val r: Int = (argb shr 16) and 0xFF
        val g: Int = (argb shr 8) and 0xFF
        val b: Int = argb and 0xFF
        if (r <= 200) {
            return false
        }
        if (g >= 130 || b >= 140) {
            return false
        }
        if (r - g <= 100) {
            return false
        }
        return kotlin.math.abs(g - b) < 40
    }

    /**
     * 「좌석 부족」 글자색 판정. 코레일톡 7.1.0 실측 (112,113,119), 2026-10-31 SM-F731N.
     * 「입석+좌석」·가격은 훨씬 어두운 (16,19,28)이라 겹치지 않는다.
     */
    fun isUnavailableGrey(argb: Int): Boolean {
        val r: Int = (argb shr 16) and 0xFF
        val g: Int = (argb shr 8) and 0xFF
        val b: Int = argb and 0xFF
        return r in 100..124 && g in 101..125 && b in 107..131
    }

    /**
     * 한 행의 값 컬럼 띠를 보고 위/아래 칸의 매진 여부를 판정한다.
     * 「좌석 부족」 회색도 매진으로 센다 — [isUnavailableGrey] 참고.
     *
     * [strip]은 [stripWidth] 폭의 ARGB 배열(행 우선)이고, 앞의 [stripWidth] * [splitY] 개가
     * 일반실 칸, 나머지가 특실·우등실 칸이다. [strip]은 재사용 버퍼일 수 있으므로
     * [stripWidth] * [stripHeight] 개까지만 읽는다.
     */
    fun classifyStrip(
        strip: IntArray,
        stripWidth: Int,
        stripHeight: Int,
        splitY: Int,
    ): SoldOutSignal {
        if (stripWidth <= 0 || stripHeight <= 0) {
            return SoldOutSignal(generalSoldOut = false, premiumSoldOut = false)
        }
        val boundary: Int = splitY.coerceIn(0, stripHeight)
        val total: Int = minOf(stripWidth * stripHeight, strip.size)
        val splitIndex: Int = minOf(stripWidth * boundary, total)
        var upperRed: Int = 0
        var lowerRed: Int = 0
        var upperGrey: Int = 0
        var lowerGrey: Int = 0
        for (i: Int in 0 until total) {
            val argb: Int = strip[i]
            if (isSoldOutRed(argb)) {
                if (i < splitIndex) upperRed++ else lowerRed++
            } else if (isUnavailableGrey(argb)) {
                if (i < splitIndex) upperGrey++ else lowerGrey++
            }
        }
        return SoldOutSignal(
            generalSoldOut = upperRed >= MIN_PIXELS || upperGrey >= MIN_GREY_PIXELS,
            premiumSoldOut = lowerRed >= MIN_PIXELS || lowerGrey >= MIN_GREY_PIXELS,
        )
    }
}
