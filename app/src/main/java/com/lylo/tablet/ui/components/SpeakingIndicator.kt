package com.lylo.tablet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.AgentState
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

/** Portage de src/components/interaction/SpeakingIndicator.tsx. */
@Composable
fun SpeakingIndicator(
    agentState: AgentState,
    agentName: String,
    clickSelectionMode: String?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current

    val (icon, label, border, bg, contentColor, animated) = when {
        clickSelectionMode != null -> IndicatorSpec(
            Icons.Default.TouchApp,
            strings.get(R.string.interaction_click_select_hint),
            LyloPrimary.copy(alpha = 0.3f), Color.White.copy(alpha = 0.5f), LyloPrimary, false,
        )
        agentState == AgentState.INITIALIZING -> IndicatorSpec(
            null, strings.get(R.string.interaction_connecting),
            LyloPrimary.copy(alpha = 0.1f), Color.White.copy(alpha = 0.5f), LyloPrimary, true,
        )
        agentState == AgentState.SPEAKING -> IndicatorSpec(
            null, strings.get(R.string.interaction_agent_speaking).replace("{name}", agentName),
            LyloPrimary.copy(alpha = 0.3f), Color.White.copy(alpha = 0.5f), LyloPrimary, true,
        )
        agentState == AgentState.THINKING -> IndicatorSpec(
            null, strings.get(R.string.interaction_agent_thinking),
            LyloPrimary.copy(alpha = 0.1f), Color.White.copy(alpha = 0.5f), LyloPrimary.copy(alpha = 0.6f), true,
        )
        agentState == AgentState.LISTENING -> IndicatorSpec(
            Icons.Default.Mic, strings.get(R.string.interaction_agent_listening),
            LyloPrimary.copy(alpha = 0.4f), LyloPrimary.copy(alpha = 0.05f), LyloPrimary, false,
        )
        else -> IndicatorSpec(
            Icons.Default.MicOff, strings.get(R.string.interaction_agent_idle),
            LyloPrimary.copy(alpha = 0.1f), Color.White.copy(alpha = 0.3f), LyloPrimary.copy(alpha = 0.4f), false,
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        } else if (animated) {
            PulsingDots(color = contentColor)
        }
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class IndicatorSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val label: String,
    val border: Color,
    val bg: Color,
    val contentColor: Color,
    val animated: Boolean,
)

/** Portage du composant Dots (3 points qui "bounce") utilisé dans plusieurs indicateurs. */
@Composable
fun PulsingDots(color: Color = LyloPrimary, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val transition = rememberInfiniteTransition(label = "dot$index")
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 750, delayMillis = index * 150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dotOffset$index",
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
