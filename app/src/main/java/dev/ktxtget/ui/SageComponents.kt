package dev.ktxtget.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ktxtget.R
import dev.ktxtget.ui.theme.Hairline
import dev.ktxtget.ui.theme.Ink
import dev.ktxtget.ui.theme.InkSecondary
import dev.ktxtget.ui.theme.MonoFamily
import dev.ktxtget.ui.theme.Sage

/** 시안 C의 선 굵기. 카드·알약·스위치 윤곽이 모두 이 값을 쓴다. */
val LineWidth: Dp = 1.25.dp

val CardShape: Shape = RoundedCornerShape(28.dp)

val LabelStyle: TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
val CaptionStyle: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, color = InkSecondary)

@Composable
fun OutlineCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .border(BorderStroke(LineWidth, Ink), CardShape)
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, description: String? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = title, style = LabelStyle)
        if (description != null) {
            Text(text = description, style = CaptionStyle)
        }
    }
}

/** 섹션 머리 아래에 긋는 굵은 선. 그 아래 줄들은 [RowDivider]로 나눈다. */
@Composable
fun SectionRule() {
    HorizontalDivider(thickness = LineWidth, color = Ink)
}

@Composable
fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = Hairline)
}

/** 참고 디자인의 ON/OFF 스위치. 켜지면 먹색으로 채우고, 꺼지면 윤곽만 남긴다. */
@Composable
fun OnOffSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val label: String = stringResource(if (checked) R.string.switch_on else R.string.switch_off)
    Row(
        modifier = modifier
            .size(width = 64.dp, height = 32.dp)
            .clip(CircleShape)
            .background(if (checked) Ink else Color.Transparent)
            .border(LineWidth, Ink, CircleShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val knob: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .size(if (checked) 26.dp else 24.dp)
                    .clip(CircleShape)
                    .background(if (checked) Sage else Ink),
            )
        }
        val text: @Composable () -> Unit = {
            Text(
                text = label,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                color = if (checked) Sage else Ink,
                modifier = Modifier.padding(horizontal = 5.dp),
            )
        }
        if (checked) {
            text()
            knob()
        } else {
            knob()
            text()
        }
    }
}

/** 표처럼 가는 선으로 나뉜 설정 줄. 줄 전체가 스위치 역할을 한다. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = Ink,
            )
            if (description != null) {
                Text(text = description, style = CaptionStyle)
            }
        }
        OnOffSwitch(checked = checked)
    }
    RowDivider()
}

@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Sage),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 36.dp),
        shape = CircleShape,
        border = BorderStroke(LineWidth, Ink),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** 비활성 상태를 표시하는 점선 윤곽. */
fun Modifier.dashedBorder(color: Color, width: Dp, cornerRadius: Dp): Modifier = drawBehind {
    val stroke: Float = width.toPx()
    val inset: Float = stroke / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}
