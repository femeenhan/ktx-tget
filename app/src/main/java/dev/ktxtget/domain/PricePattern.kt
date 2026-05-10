package dev.ktxtget.domain

object PricePattern {
    /** Matches fare labels like 23,700원 */
    val KOREAN_WON: Regex = Regex("""\d{1,3}(,\d{3})*원""")
}
