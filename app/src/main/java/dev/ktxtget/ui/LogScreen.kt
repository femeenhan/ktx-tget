package dev.ktxtget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ktxtget.R
import dev.ktxtget.runtime.MacroLogLine
import dev.ktxtget.runtime.MacroRuntimeLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen() {
    val lines: List<MacroLogLine> by MacroRuntimeLog.lines.collectAsStateWithLifecycle()
    val formatter = rememberFormatter()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.log_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Button(
            onClick = { MacroRuntimeLog.clear() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.log_clear_button))
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(lines.asReversed()) { line: MacroLogLine ->
                LogLineItem(line = line, formatter = formatter)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun rememberFormatter(): SimpleDateFormat {
    return remember {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    }
}

@Composable
private fun LogLineItem(line: MacroLogLine, formatter: SimpleDateFormat) {
    val time: String = formatter.format(Date(line.timestampMs))
    Text(
        text = "[$time] ${line.kind} ${line.detail}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    )
}
