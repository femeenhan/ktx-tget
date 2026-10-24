package dev.ktxtget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SageScheme =
    lightColorScheme(
        primary = Ink,
        onPrimary = Sage,
        secondary = Ink,
        onSecondary = Sage,
        background = Sage,
        onBackground = Ink,
        surface = Sage,
        onSurface = Ink,
        surfaceVariant = Sage,
        onSurfaceVariant = InkSecondary,
        outline = Ink,
        outlineVariant = Hairline,
    )

@Composable
fun KtxtgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SageScheme,
        typography = Typography,
        content = content,
    )
}
