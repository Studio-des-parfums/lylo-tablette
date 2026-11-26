package com.lylo.tablet.ui.screens.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloTextDark

/** Portage commun des sections de ConfigPanel : titre + icône + option "info" en tooltip. */
@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    infoText: String? = null,
) {
    var showInfo by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = LyloTextDark, modifier = Modifier.padding(end = 2.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = LyloTextDark, fontWeight = FontWeight.Bold)
            if (infoText != null) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "info",
                    tint = LyloTextDark,
                    modifier = Modifier.clickable { showInfo = !showInfo },
                )
            }
        }
        if (showInfo && infoText != null) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, LyloPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text(infoText, style = MaterialTheme.typography.bodySmall, color = LyloPrimary.copy(alpha = 0.8f))
            }
        }
    }
}

/** Une "pill" radio-like utilisée par tous les sélecteurs de ConfigPanel (persona/depth/mode/input). */
@Composable
fun SelectorPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) LyloPrimary else Color.White.copy(alpha = 0.5f))
            .border(2.dp, if (selected) LyloPrimary else LyloPrimary.copy(alpha = 0.1f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.White else LyloPrimary.copy(alpha = 0.6f), modifier = Modifier.padding(end = 6.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else LyloPrimary.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun PersonaSelector(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    Column(modifier = modifier) {
        SectionHeader(Icons.Default.Psychology, strings.get(R.string.configure_persona_title))
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorPill(strings.get(R.string.configure_male), value == "male", { onChange("male") }, Modifier.weight(1f), Icons.Default.Male)
            SelectorPill(strings.get(R.string.configure_female), value == "female", { onChange("female") }, Modifier.weight(1f), Icons.Default.Female)
        }
    }
}

@Composable
fun DepthSelector(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    // Nombre de questions imposé par la marque (un seul choix affiché), sinon 1 / 4 / 12.
    val fixed = com.lylo.tablet.data.Brand.active.fixedQuestionCount
    val depths = if (fixed != null) listOf(fixed) else listOf("1", "4", "12")
    androidx.compose.runtime.LaunchedEffect(fixed, value) {
        if (fixed != null && value != fixed) onChange(fixed)
    }
    Column(modifier = modifier) {
        SectionHeader(
            Icons.Default.QueryStats,
            strings.get(R.string.configure_question_count_title),
            infoText = strings.get(R.string.configure_depth_info),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            depths.forEach { depth ->
                SelectorPill(depth, value == depth, { onChange(depth) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ModeSelector(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    val guidedDesc = strings.get(R.string.configure_mode_guided) + " : " + strings.get(R.string.configure_mode_guided_desc)
    val discoveryDesc = strings.get(R.string.configure_mode_discovery) + " : " + strings.get(R.string.configure_mode_discovery_desc)
    Column(modifier = modifier) {
        SectionHeader(
            Icons.Default.Tune,
            strings.get(R.string.configure_mode_title),
            infoText = "$guidedDesc\n\n$discoveryDesc",
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SelectorPill(strings.get(R.string.configure_mode_guided), value == "guided", { onChange("guided") }, Modifier.weight(1f), Icons.Default.Route)
            SelectorPill(strings.get(R.string.configure_mode_discovery), value == "discovery", { onChange("discovery") }, Modifier.weight(1f), Icons.Default.Explore)
        }
    }
}

@Composable
fun InputModeSelector(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStringResolver.current
    Column(modifier = modifier) {
        SectionHeader(
            Icons.Default.SettingsInputComponent,
            strings.get(R.string.configure_input_mode_title),
            // Écart volontaire par rapport au web : le mode "Hybride" (click) est masqué sur
            // tablette, seuls Voix et Visuel restent proposés — voir configure_input_mode_info_tablet.
            infoText = strings.get(R.string.configure_input_mode_info_tablet),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectorPill(strings.get(R.string.configure_input_mode_voice), value == "voice", { onChange("voice") }, Modifier.weight(1f))
            SelectorPill(strings.get(R.string.configure_input_mode_quiz), value == "quiz", { onChange("quiz") }, Modifier.weight(1f))
        }
    }
}
