package dev.ktxtget.domain

/**
 * 코레일톡이 자동화·과도한 요청을 의심할 때 띄우는 안내 문구를 알아본다.
 *
 * 정확한 문구는 코레일이 바꿀 수 있어 핵심 낱말로 본다. 하나라도 보이면 매크로를 즉시 멈추고
 * [PAUSE_MS] 동안 쉰다. 오탐으로 멈추는 쪽이 계정 제재보다 싸다.
 */
object AbnormalAccessDetector {
    /** 감지 뒤 쉬는 시간. */
    const val PAUSE_MS: Long = 10 * 60_000L

    private val KEYWORDS: List<String> = listOf(
        "비정상적인 접근",
        "비정상 접근",
        "비정상적 접근",
        "비정상적인 요청",
        "자동화 프로그램",
        "매크로",
        "과도한 요청",
        "과도한 접근",
        "부정 이용",
        "부정한 방법",
        "접근이 차단",
        "이용이 제한",
    )

    /** 화면 문구들 가운데 경고로 보이는 첫 문구(60자 이내로 자름). 없으면 null. */
    fun match(texts: List<String>): String? {
        for (raw: String in texts) {
            val line: String = raw.replace('\n', ' ').trim()
            if (line.isEmpty()) continue
            val compact: String = line.replace(" ", "")
            val hit: Boolean = KEYWORDS.any { keyword: String -> compact.contains(keyword.replace(" ", "")) }
            if (hit) return line.take(60)
        }
        return null
    }
}
