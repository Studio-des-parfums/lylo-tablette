package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloSecondary
import com.lylo.tablet.ui.theme.StoneGray400

/** Portage de src/components/recommendations/SizeToggle.tsx. */
@Composable
fun SizeToggle(
    selected: SizeOption,
    onSelect: (SizeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current

    // Écart volontaire par rapport au web (qui utilise justify-between, label à gauche/boutons à
    // droite) : label au-dessus, boutons dessous, le tout collé à gauche de la carte.
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = strings.get(R.string.recommendations_select_size),
            style = MaterialTheme.typography.labelSmall,
            color = StoneGray400,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            SizeOption.entries.forEach { option ->
                val isSelected = option == selected
                Text(
                    text = option.wire,
                    color = if (isSelected) Color.White else Color(0xFF6B7280),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) LyloPrimary else LyloSecondary.copy(alpha = 0.1f))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
