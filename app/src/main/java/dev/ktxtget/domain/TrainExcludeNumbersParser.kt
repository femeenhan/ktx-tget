package dev.ktxtget.domain

/**
 * 사용자가 입력한 제외 열차번호를 파싱한다.
 *
 * 코레일 열차번호는 3자리(001, 121, 705)와 4자리(4031, 4135)가 섞여 있다.
 * 표기상의 앞자리 0은 무시하고 숫자 값으로 맞춘다 — "7", "007" 둘 다 열차 007과 매칭된다.
 * (이 정규화 덕분에 예전에 3자리로 저장해 둔 설정도 그대로 동작한다.)
 */
object TrainExcludeNumbersParser {
    private const val MAX_DIGITS: Int = 4

    fun parseCommaSeparated(input: String): Set<String> {
        val out: MutableSet<String> = mutableSetOf()
        for (segment: String in input.split(",")) {
            val normalized: String? = normalizeToken(segment.trim())
            if (normalized != null) {
                out.add(normalized)
            }
        }
        return out
    }

    /** 숫자 토큰을 앞자리 0 없는 형태로. 숫자가 아니거나 너무 길면 null. */
    fun normalizeToken(token: String): String? {
        if (token.isEmpty()) {
            return null
        }
        if (!token.all { ch: Char -> ch.isDigit() }) {
            return null
        }
        if (token.length > MAX_DIGITS) {
            return null
        }
        return token.trimStart('0').ifEmpty { "0" }
    }
}
