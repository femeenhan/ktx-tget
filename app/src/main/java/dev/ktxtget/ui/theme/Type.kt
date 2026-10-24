package dev.ktxtget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.ktxtget.R

/** 앱 이름 같은 큰 라틴 제목. 한글은 시스템 글꼴로 대체된다. */
@OptIn(ExperimentalTextApi::class)
val DisplayFamily: FontFamily = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
)

/** 시각·간격·열차번호처럼 자리를 맞춰야 하는 숫자. */
val MonoFamily: FontFamily = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

val Typography: Typography = Typography()
