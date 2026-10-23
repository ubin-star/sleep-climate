package com.sleepclimate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepclimate.app.ui.theme.SC

private val ButtonShape = RoundedCornerShape(16.dp)

/** 주요 동작 버튼 — 화면당 하나만 강조색으로 */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SC.Accent,
    enabled: Boolean = true
) {
    Box(
        modifier
            .height(68.dp)
            .clip(ButtonShape)
            .background(if (enabled) color else SC.SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Color.White else SC.TextTertiary)
    }
}

/** 보조 동작 버튼 — 테두리 없는 톤 버튼 */
@Composable
fun BigOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SC.TextPrimary,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .height(68.dp)
            .clip(ButtonShape)
            .background(SC.SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) LineIcon(icon, tint = color, size = 22.dp)
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** 원형 뒤로 가기 버튼 */
@Composable
fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier.size(56.dp).clip(CircleShape).background(SC.SurfaceHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { LineIcon(LineIcons.Back, size = 26.dp) }
}

/** 선택 칩 한 줄 — 선택된 항목은 밝은 면으로 반전 */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    // 좁은 화면에서는 한 줄에 4개씩 — 글자가 줄바꿈되지 않도록
    val perRow = if (isCompactWidth()) 4 else options.size
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(perRow).forEach { row ->
            ChoiceRowLine(row, perRow, selected, onSelect)
        }
    }
}

@Composable
private fun <T> ChoiceRowLine(
    options: List<Pair<T, String>>,
    slots: Int,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(if (isSelected) SC.TextPrimary else SC.SurfaceHigh, label = "chip")
            val fg by animateColorAsState(if (isSelected) SC.Background else SC.TextSecondary, label = "chipText")
            Box(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = fg, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        repeat(slots - options.size) { Box(Modifier.weight(1f)) }
    }
}

/** − 값 + 스테퍼 */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    valueColor: Color = SC.TextPrimary
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", onMinus)
        Text(
            value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displaySmall,
            color = valueColor
        )
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(72.dp).clip(CircleShape).background(SC.SurfaceHigh).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 34.sp, fontWeight = FontWeight.Light, color = SC.TextPrimary)
    }
}

/** 상단 상태 표시 — 아이콘 + 짧은 텍스트, 테두리·배경 없음 */
@Composable
fun StatusItem(icon: ImageVector, text: String, color: Color = SC.TextSecondary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LineIcon(icon, tint = color, size = 22.dp)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** 면 컨테이너 — 기본은 테두리 없음, 강조가 필요할 때만 [accent] */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .clip(shape)
            .background(SC.Surface)
            .let { if (accent != null) it.border(1.dp, accent.copy(alpha = 0.5f), shape) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(24.dp)
    ) { content() }
}

/** 작은 라벨 */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = SC.TextSecondary) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

/** 라벨 + 얇은 큰 값 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = SC.TextPrimary) {
    Panel(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Caption(label)
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontSize = if (isCompactWidth()) 28.sp else 40.sp,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

/** 라벨 + 값 (패널 없이) */
@Composable
fun InfoValue(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = SC.TextPrimary) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Caption(label)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
    }
}

/** 섹션 제목 + 내용 */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, trailing: String? = null, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Caption(title, Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = SC.TextSecondary)
        }
        content()
    }
}

/** 비활성 표현 */
fun Modifier.dimmedIf(disabled: Boolean): Modifier = alpha(if (disabled) 0.35f else 1f)

/**
 * 가로 와이드 화면(차량 중앙 디스플레이 전체 화면)이면 좌우 2분할,
 * 좁은 화면(Pleos Connect 2앱 분할 화면 등)이면 위아래로 쌓고 전체 스크롤.
 *
 * [controls]에 전달되는 Boolean은 wide 여부 — wide일 때만 패널 자체 스크롤을 사용한다.
 */
@Composable
fun AdaptiveSplit(
    visual: @Composable (Modifier) -> Unit,
    controls: @Composable (Modifier, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= 900.dp) {
            Row(Modifier.fillMaxSize()) {
                visual(Modifier.weight(1.15f).fillMaxHeight().padding(end = 32.dp, bottom = 12.dp))
                controls(Modifier.weight(1f).fillMaxHeight(), true)
            }
        } else {
            val visualHeight = if (maxWidth < 600.dp) 300.dp else 380.dp
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                visual(Modifier.fillMaxWidth().height(visualHeight).padding(bottom = 20.dp))
                controls(Modifier.fillMaxWidth().padding(bottom = 24.dp), false)
            }
        }
    }
}

/** 폭이 좁은 화면(폰·분할 화면)인지 */
@Composable
fun isCompactWidth(): Boolean = LocalConfiguration.current.screenWidthDp < 760

/** 화면 바깥 여백 — 차량 와이드 화면은 넉넉하게, 좁은 화면은 촘촘하게 */
@Composable
fun Modifier.screenPadding(): Modifier =
    if (isCompactWidth()) padding(horizontal = 18.dp, vertical = 12.dp) else padding(horizontal = 40.dp, vertical = 28.dp)
