package dev.ktxtget.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ktxtget.R
import dev.ktxtget.runtime.MacroLogLine
import dev.ktxtget.runtime.MacroRuntimeLog
import dev.ktxtget.ui.theme.DisplayFamily
import dev.ktxtget.ui.theme.Ink
import dev.ktxtget.ui.theme.InkMuted
import dev.ktxtget.ui.theme.InkSecondary
import dev.ktxtget.ui.theme.MonoFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 예매 진행과 오류처럼 눈에 띄어야 하는 로그 종류. */
private val EMPHASIZED_KINDS: Set<String> = setOf("reserve", "ticket", "error")

@Composable
fun LogScreen(onBack: () -> Unit) {
    val lines: List<MacroLogLine> by MacroRuntimeLog.lines.collectAsStateWithLifecycle()
    val formatter = rememberLogTimeFormatter()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item {
            LogHeader(onBack = onBack, onClear = { MacroRuntimeLog.clear() })
            Text(
                text = stringResource(R.string.log_retention_note),
                style = CaptionStyle,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SectionRule()
        }
        items(lines.asReversed()) { line: MacroLogLine ->
            LogRow(line = line, formatter = formatter)
        }
    }
}

@Composable
private fun LogHeader(onBack: () -> Unit, onClear: () -> Unit) {
    val backLabel: String = stringResource(R.string.log_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(LineWidth, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "←", fontSize = 16.sp, color = Ink)
                }
            }
            Text(
                text = stringResource(R.string.log_screen_title),
                fontFamily = DisplayFamily,
                fontSize = 38.sp,
                letterSpacing = (-1).sp,
                color = Ink,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        UnderlinedLink(text = stringResource(R.string.log_clear_button), onClick = onClear)
    }
}

@Composable
fun rememberLogTimeFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
}

/** 시각 · 종류 · 내용 한 줄. 홈의 최근 기록과 기록 화면이 같이 쓴다. */
@Composable
fun LogRow(line: MacroLogLine, formatter: SimpleDateFormat) {
    val emphasized: Boolean = line.kind in EMPHASIZED_KINDS
    Row(modifier = Modifier.padding(vertical = 11.dp)) {
        Text(
            text = formatter.format(Date(line.timestampMs)),
            fontFamily = MonoFamily,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            color = if (emphasized) Ink else InkSecondary,
            modifier = Modifier
                .width(78.dp)
                .padding(top = 1.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = line.kind.uppercase(Locale.ROOT),
                fontFamily = MonoFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = if (emphasized) Ink else InkMuted,
            )
            Text(
                text = line.detail,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
                color = Ink,
            )
        }
    }
    RowDivider()
}
