package dev.ktxtget.domain

/**
 * 화면 좌표 사각형 (JVM 단위 테스트에서 쓰려고 android.graphics.Rect 대신 둔다).
 */
data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}
