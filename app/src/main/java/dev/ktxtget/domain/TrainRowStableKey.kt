package dev.ktxtget.domain

object TrainRowStableKey {
    private val TRAIN_NUMBER: Regex = Regex("""\d{3,5}""")

    fun fromRawTexts(texts: List<String>): String {
        for (line: String in texts) {
            val m: MatchResult? = TRAIN_NUMBER.find(line)
            if (m != null) {
                return m.value
            }
        }
        val joined: String = texts.joinToString("")
        val fallback: MatchResult? = TRAIN_NUMBER.find(joined)
        if (fallback != null) {
            return fallback.value
        }
        return joined.take(32).ifEmpty { "row" }
    }
}
