package com.sleepclimate.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepclimate.app.ui.theme.SC

/** 차량 터치 기준 큰 버튼 (높이 72dp) */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SC.Accent,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = SC.SurfaceHigh,
            disabledContentColor = SC.TextSecondary
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 22.sp)
    }
}

@Composable
fun BigOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SC.TextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(2.dp, SC.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** 선택 칩 한 줄 — 슬라이더 대신 큰 터치 영역 */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(if (isSelected) SC.Accent else SC.SurfaceHigh, label = "chip")
            Box(
                Modifier
                    .weight(1f)
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) Color.White else SC.TextPrimary
                )
            }
        }
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
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 44.sp,
            color = valueColor
        )
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(SC.SurfaceHigh)
            .border(2.dp, SC.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = SC.TextPrimary)
    }
}

/** 상단 상태 표시용 알약 모양 배지 */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** 카드 컨테이너 */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = SC.Border,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .clip(shape)
            .background(SC.Surface)
            .border(1.5.dp, borderColor, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(24.dp)
    ) { content() }
}

/** 라벨 + 큰 값 타일 */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = SC.TextPrimary) {
    Panel(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = SC.TextSecondary)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        }
    }
}

/** 섹션 제목 + 내용 */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, trailing: String? = null, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = SC.TextSecondary, modifier = Modifier.weight(1f))
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = SC.Accent)
        }
        content()
    }
}

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
                visual(Modifier.weight(1.1f).fillMaxHeight())
                controls(Modifier.weight(1f).fillMaxHeight(), true)
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                visual(Modifier.fillMaxWidth().height(340.dp))
                controls(Modifier.fillMaxWidth(), false)
            }
        }
    }
}
