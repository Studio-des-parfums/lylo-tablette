package com.lylo.tablet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary

/** Portage de src/components/livekit/GeneratingLoader.tsx. */
@Composable
fun GeneratingLoader(modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LyloBackgroundLight.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val transition = rememberInfiniteTransition(label = "generating")
            val bounce by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
                label = "bounce",
            )

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(LyloPrimary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(LyloPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = null,
                        tint = LyloPrimary,
                        modifier = Modifier
                            .offset(y = (-6f * bounce).dp)
                            .size(48.dp),
                    )
                }
            }

            Text(
                text = strings.get(R.string.generating_title),
                style = MaterialTheme.typography.headlineSmall,
                color = LyloPrimary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = strings.get(R.string.generating_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = LyloPrimary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 280.dp),
            )

            Row(modifier = Modifier.padding(top = 24.dp)) {
                repeat(3) { index ->
                    val dotTransition = rememberInfiniteTransition(label = "dot$index")
                    val scale by dotTransition.animateFloat(
                        initialValue = 0.7f, targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            tween(700, delayMillis = index * 150, easing = LinearEasing),
                            RepeatMode.Reverse,
                        ),
                        label = "dotScale$index",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size((8 * scale).dp)
                            .clip(CircleShape)
                            .background(LyloPrimary),
                    )
                }
            }
        }
    }
}
