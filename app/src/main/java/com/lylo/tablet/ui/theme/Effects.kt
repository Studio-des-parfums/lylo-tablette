package com.lylo.tablet.ui.theme

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * .glass-panel (globals.css) :
 *   background: rgba(255,255,255,0.7); backdrop-filter: blur(10px);
 *   border: 1px solid rgba(153,111,86,0.1)
 *
 * Note : Compose (hors Android 12+ RenderEffect) n'a pas de vrai "backdrop-filter" qui floute
 * ce qui est DERRIÈRE la vue. On approxime avec un fond blanc translucide + un léger blur du
 * contenu du panneau lui-même, ce qui donne un rendu très proche à l'œil sur les fonds
 * relativement unis utilisés dans l'app (dégradés doux / images de fond).
 */
fun Modifier.glassPanel(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(RadiusLg),
    borderColor: Color = LyloPrimary.copy(alpha = 0.1f),
): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.7f), shape)
    .border(1.dp, borderColor, shape)

/** .glass-nav : blur(12px) + background-color: rgba(247,247,246,0.8) */
fun Modifier.glassNav(): Modifier = this
    .background(LyloBackgroundLight.copy(alpha = 0.8f))

/** .card-shadow : box-shadow: 0 10px 30px -10px rgba(154,111,86,0.2) */
fun Modifier.cardShadow(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(RadiusLg),
): Modifier = this.shadow(
    elevation = 14.dp,
    shape = shape,
    ambientColor = LyloPrimary.copy(alpha = 0.20f),
    spotColor = LyloPrimary.copy(alpha = 0.20f),
)

/** .ai-glow : box-shadow: 0 0 40px 10px rgba(153,111,86,0.15) — halo diffus autour de l'avatar */
fun Modifier.aiGlow(): Modifier = this.drawBehind {
    val glowColor = LyloPrimary.copy(alpha = 0.15f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, glowColor.copy(alpha = 0f)),
            radius = size.minDimension / 2f + 40.dp.toPx(),
        ),
        radius = size.minDimension / 2f + 40.dp.toPx(),
        center = Offset(size.width / 2f, size.height / 2f),
    )
}

/**
 * .mic-pulse (globals.css @keyframes pulse-ring) :
 *   0%   { transform: scale(1);    box-shadow: 0 0 0 0 rgba(153,111,86,0.6) }
 *   70%  { transform: scale(1.05); box-shadow: 0 0 0 12px rgba(153,111,86,0) }
 *   100% { transform: scale(1);    box-shadow: 0 0 0 0 rgba(153,111,86,0) }
 *   animation: 2s infinite
 *
 * Composable autonome dessinant l'anneau qui pulse derrière son `content` (ex: bouton micro).
 */
@Composable
fun MicPulseRing(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = LyloPrimary,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mic-pulse-progress",
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        // Anneau : scale 1 → 1.05 sur les 70% premiers, box-shadow qui s'étend et s'estompe
        val scale = 1f + (if (progress < 0.7f) (progress / 0.7f) * 0.05f else 0.05f)
        val ringAlpha = (0.6f * (1f - progress)).coerceIn(0f, 0.6f)
        val ringSpread = 12.dp * progress

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawBehind {
                    drawCircle(
                        color = color.copy(alpha = ringAlpha),
                        radius = this.size.minDimension / 2f + ringSpread.toPx(),
                    )
                },
        )
        content()
    }
}
