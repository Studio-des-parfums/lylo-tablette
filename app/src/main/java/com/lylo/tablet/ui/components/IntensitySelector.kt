package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary

private data class IntensityLevel(val labelRes: Int, val dotSize: androidx.compose.ui.unit.Dp)

private val LEVELS = listOf(
    IntensityLevel(R.string.intensity_light, 10.dp),
    IntensityLevel(R.string.intensity_moderate, 14.dp),
    IntensityLevel(R.string.intensity_intense, 18.dp),
)

/** Portage de src/components/interaction/IntensitySelector.tsx. */
@Composable
fun IntensitySelector(modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(
            text = strings.get(R.string.intensity_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = LyloPrimary.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LEVELS.forEach { level ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f))
                        .border(1.dp, LyloPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(level.dotSize)
                            .clip(CircleShape)
                            .background(LyloPrimary.copy(alpha = 0.1f)),
                    )
                    Text(
                        text = strings.get(level.labelRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LyloPrimary,
                    )
                }
            }
        }
    }
}
