package dev.ktxtget.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ktxtget.BuildConfig
import dev.ktxtget.R
import dev.ktxtget.ui.theme.DisplayFamily
import dev.ktxtget.ui.theme.Ink
import dev.ktxtget.ui.theme.InkMuted

/** 앱 안에서 읽는 사용 안내. 내용은 [GUIDE_SECTIONS]. */
@Composable
fun GuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        item { GuideHeader(onBack = onBack) }
        items(GUIDE_SECTIONS) { section: GuideSection ->
            GuideSectionBlock(
                section = section,
                onAction = { action: GuideAction ->
                    when (action) {
                        GuideAction.DISPLAY -> runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                },
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.guide_web_note), style = CaptionStyle)
            Spacer(modifier = Modifier.height(6.dp))
            UnderlinedLink(
                text = stringResource(R.string.guide_web_link),
                onClick = { openUrl(context, BuildConfig.API_BASE_URL + "/guide") },
            )
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun GuideHeader(onBack: () -> Unit) {
    val backLabel: String = stringResource(R.string.log_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
            text = stringResource(R.string.guide_screen_title),
            fontFamily = DisplayFamily,
            fontSize = 38.sp,
            letterSpacing = (-1).sp,
            color = Ink,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun GuideSectionBlock(section: GuideSection, onAction: (GuideAction) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 26.dp)) {
        Text(text = section.title, fontSize = 18.sp, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        SectionRule()
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.lines.forEach { line: String ->
                Row {
                    Text(text = "·", fontSize = 15.sp, lineHeight = 22.sp, color = InkMuted)
                    Text(
                        text = line,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Ink,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        section.callout?.let { callout: String ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = callout,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBorder(color = InkMuted, width = LineWidth, cornerRadius = 16.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        section.action?.let { action: GuideAction ->
            Spacer(modifier = Modifier.height(12.dp))
            LineButton(
                text = stringResource(
                    when (action) {
                        GuideAction.DISPLAY -> R.string.guide_open_display_settings
                    },
                ),
                onClick = { onAction(action) },
            )
        }
    }
}
