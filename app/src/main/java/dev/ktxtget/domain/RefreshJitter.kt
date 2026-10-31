package dev.ktxtget.domain

import kotlin.random.Random

/**
 * 새로고침 간격에 무작위 흔들림을 준다.
 *
 * 설정값 그대로 2.5초마다 정확히 새로고침하면 서버 로그에 기계 같은 등간격이 남는다.
 * 사람이 연타하는 간격은 들쭉날쭉하므로, 주기마다 [MIN_RATIO]~[MAX_RATIO] 배 사이를 고르게 뽑는다.
 * 위쪽으로 더 넓게 두어 평균은 설정값보다 약간 길다 — 사용자가 고른 간격이 「최소 안전선」이라는
 * 뜻을 지키기 위해서다.
 */
object RefreshJitter {
    const val MIN_RATIO: Double = 0.85
    const val MAX_RATIO: Double = 1.35

    fun jittered(baseMs: Long, random: Random = Random.Default): Long {
        if (baseMs <= 0L) {
            return 0L
        }
        val ratio: Double = MIN_RATIO + random.nextDouble() * (MAX_RATIO - MIN_RATIO)
        return (baseMs * ratio).toLong()
    }
}
