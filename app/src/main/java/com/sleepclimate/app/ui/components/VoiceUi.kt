package com.sleepclimate.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.sleepclimate.app.voice.VoiceState
import com.sleepclimate.app.ui.theme.SC

/** 화면 상단 상태줄의 마이크 버튼 — AppNavigation에서 제공 */
data class VoiceControl(val listening: Boolean, val onClick: () -> Unit)

val LocalVoiceControl = compositionLocalOf<VoiceControl?> { null }

@Composable
fun VoiceButton(listening: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val scale by pulse.animateFloat(1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "micScale")
    Box(
        Modifier
            .size(56.dp)
            .scale(if (listening) scale else 1f)
            .clip(CircleShape)
            .background(if (listening) SC.Accent else SC.SurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LineIcon(LineIcons.Mic, size = 26.dp)
    }
}

/** 화면 하단 음성 패널 — 듣는 중 / 인식한 문장 / 안내 문구 */
@Composable
fun VoiceOverlay(state: VoiceState, engines: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = state !is VoiceState.Idle,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
    ) {
        val (caption, text, color) = when (state) {
            is VoiceState.Listening -> Triple("듣고 있어요", state.partial.ifBlank { "말씀해 주세요" }, SC.Accent)
            is VoiceState.Heard -> Triple("인식", "“${state.text}”", SC.TextPrimary)
            is VoiceState.Speaking -> Triple("SLEEP CLIMATE", state.text, SC.TextPrimary)
            is VoiceState.Error -> Triple("음성", state.message, SC.Warn)
            VoiceState.Idle -> Triple("", "", SC.TextPrimary)
        }
        val compact = isCompactWidth()
        Row(
            Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = if (compact) 16.dp else 32.dp)
                .widthIn(min = if (compact) 0.dp else 520.dp, max = 1100.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SC.SurfaceHigh.copy(alpha = 0.96f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VoiceLevel(active = state is VoiceState.Listening || state is VoiceState.Speaking, color = color)
            Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Caption(if (engines.isBlank()) caption else "$caption · $engines", color = SC.TextSecondary)
                Text(text, style = MaterialTheme.typography.titleLarge, color = color)
            }
        }
    }
}

/** 말하는 중·듣는 중을 나타내는 막대 */
@Composable
private fun VoiceLevel(active: Boolean, color: androidx.compose.ui.graphics.Color) {
    val t = rememberInfiniteTransition(label = "level")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(0, 180, 90, 270).forEach { offset ->
            val h by t.animateFloat(
                initialValue = 10f, targetValue = 34f,
                animationSpec = infiniteRepeatable(tween(520, delayMillis = offset), RepeatMode.Reverse), label = "bar$offset"
            )
            Box(
                Modifier
                    .size(width = 5.dp, height = (if (active) h else 10f).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
