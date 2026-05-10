package dev.ktxtget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme =
    lightColorScheme(
        primary = PrimaryGreen,
        onPrimary = OnPrimaryWhite,
    )

@Composable
fun KtxtgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = Typography,
        content = content,
    )
}
