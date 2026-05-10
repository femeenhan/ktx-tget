package dev.ktxtget.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Ring-buffered log lines for the in-app Log screen (thread-safe append).
 */
object MacroRuntimeLog {
    private const val MAX_LINES: Int = 200
    private val lock: Any = Any()
    private val _lines: MutableStateFlow<List<MacroLogLine>> =
        MutableStateFlow(emptyList())
    val lines: StateFlow<List<MacroLogLine>> = _lines.asStateFlow()

    fun emit(kind: String, detail: String) {
        val line: MacroLogLine = MacroLogLine(
            timestampMs = System.currentTimeMillis(),
            kind = kind,
            detail = detail,
        )
        synchronized(lock) {
            _lines.update { current: List<MacroLogLine> ->
                (current + line).takeLast(MAX_LINES)
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            _lines.value = emptyList()
        }
    }
}

data class MacroLogLine(
    val timestampMs: Long,
    val kind: String,
    val detail: String,
)
