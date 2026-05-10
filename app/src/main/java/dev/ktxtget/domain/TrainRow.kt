package dev.ktxtget.domain

/**
 * One visible train row snapshot from the Korail list (strings only; safe for JVM tests).
 */
data class TrainRow(
    val stableKey: String,
    val rawLineTexts: List<String>,
    val generalSeatCellText: String,
    val firstClassSeatCellText: String,
)
