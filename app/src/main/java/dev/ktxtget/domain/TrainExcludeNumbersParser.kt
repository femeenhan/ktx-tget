package dev.ktxtget.domain

/**
 * Parses user input for excluded Korail train numbers (stable keys are 3-digit style, e.g. 082, 116).
 * Accepts only digit tokens up to 3 chars per segment; left-pads with zeros (82 → 082).
 */
object TrainExcludeNumbersParser {
    private const val WIDTH: Int = 3

    fun parseCommaSeparated(input: String): Set<String> {
        val segments: List<String> = input.split(",")
        val out: MutableSet<String> = mutableSetOf()
        for (segment: String in segments) {
            val normalized: String? = normalizeToken(segment.trim())
            if (normalized != null) {
                out.add(normalized)
            }
        }
        return out
    }

    fun normalizeToken(token: String): String? {
        if (token.isEmpty()) {
            return null
        }
        if (!token.all { ch: Char -> ch.isDigit() }) {
            return null
        }
        if (token.length > WIDTH) {
            return null
        }
        return token.padStart(WIDTH, '0')
    }
}
