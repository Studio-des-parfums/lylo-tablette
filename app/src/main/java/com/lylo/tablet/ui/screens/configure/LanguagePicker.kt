package com.lylo.tablet.ui.screens.configure

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lylo.tablet.R
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.i18n.LocalStringResolver

private data class LangEntry(val lang: AppLanguage, val native: String, val region: String, val flag: String)

// Même liste et même ordre que LANGUAGES dans LanguagePicker.tsx (langues vocales de l'avatar).
private val LANGUAGES = listOf(
    LangEntry(AppLanguage.FR, "Français", "France", "🇫🇷"),
    LangEntry(AppLanguage.EN, "English", "United Kingdom", "🇬🇧"),
    LangEntry(AppLanguage.ES, "Español", "España", "🇪🇸"),
    LangEntry(AppLanguage.DE, "Deutsch", "Deutschland", "🇩🇪"),
    LangEntry(AppLanguage.AR, "العربية", "العربية", "🇸🇦"),
    LangEntry(AppLanguage.RU, "Русский", "Россия", "🇷🇺"),
)

/** Portage de src/components/configure/LanguagePicker.tsx. */
@Composable
fun LanguagePicker(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current
    var open by remember { mutableStateOf(false) }
    val currentEntry = LANGUAGES.first { it.lang == current }

    Column(modifier = modifier) {
        SectionHeader(Icons.Default.RecordVoiceOver, strings.get(R.string.configure_language_title))

        // Écart volontaire par rapport au web (qui utilise un dégradé amber/jaune pour ce
        // bouton précis) : couleur primary marron standard de l'app, comme partout ailleurs.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(com.lylo.tablet.ui.theme.LyloPrimary)
                .border(2.dp, com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(currentEntry.flag, style = MaterialTheme.typography.headlineSmall)
                Column {
                    Text(currentEntry.native, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text(currentEntry.region, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.get(R.string.configure_language_change), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFDFAF7))
                    .padding(28.dp),
            ) {
                Text(
                    strings.get(R.string.configure_language_modal_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = com.lylo.tablet.ui.theme.LyloPrimary,
                )
                Text(
                    strings.get(R.string.configure_language_modal_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )

                Column(
                    modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LANGUAGES.forEach { entry ->
                        val isSelected = entry.lang == current
                        val lyloPrimary = com.lylo.tablet.ui.theme.LyloPrimary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) lyloPrimary else Color.White.copy(alpha = 0.5f))
                                .border(2.dp, if (isSelected) lyloPrimary else lyloPrimary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .clickable { onSelect(entry.lang); open = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val textColor = if (isSelected) Color.White else lyloPrimary
                            Text(entry.flag, style = MaterialTheme.typography.headlineSmall)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.native, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = textColor)
                                Text(entry.region, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White.copy(alpha = 0.65f) else lyloPrimary.copy(alpha = 0.45f))
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                Text(
                    "${LANGUAGES.size} ${strings.get(R.string.configure_language_available)}".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.3f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )

                Text(
                    strings.get(R.string.configure_language_close),
                    style = MaterialTheme.typography.labelMedium,
                    color = com.lylo.tablet.ui.theme.LyloPrimary.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .clickable { open = false },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
