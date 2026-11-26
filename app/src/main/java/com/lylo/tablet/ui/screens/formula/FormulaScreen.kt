package com.lylo.tablet.ui.screens.formula

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lylo.tablet.R
import com.lylo.tablet.data.ShareableFormulaCodec
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.components.Navbar
import com.lylo.tablet.ui.theme.LuxuryTitleStyle
import com.lylo.tablet.ui.theme.LyloAccentTan
import com.lylo.tablet.ui.theme.LyloBackgroundLight
import com.lylo.tablet.ui.theme.LyloPrimary

/**
 * Portage de src/app/formula/page.tsx — page atteinte via le deep link lylo://formula?data=...
 * (scanné depuis le QR code affiché dans /recommendations ou /quiz/results). Décode le payload
 * base64url et affiche la formule en lecture seule, sans session active.
 */
@Composable
fun FormulaScreen(encodedData: String?, onNavigateHome: () -> Unit) {
    val strings = LocalStringResolver.current
    val formula = remember(encodedData) { encodedData?.let { ShareableFormulaCodec.decode(it) } }

    Box(modifier = Modifier.fillMaxSize().background(LyloBackgroundLight)) {
        // Halos décoratifs — portage des deux div flous de formula/page.tsx (~L110-113)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 24.dp)
                .size(256.dp)
                .blur(90.dp)
                .background(LyloPrimary.copy(alpha = 0.05f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-24).dp)
                .size(256.dp)
                .blur(90.dp)
                .background(LyloAccentTan.copy(alpha = 0.05f), CircleShape),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Navbar(showActions = false, transparent = true, onLogoClick = onNavigateHome)

            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                if (formula == null) {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp).padding(24.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.85f)).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(strings.get(R.string.recommendations_no_formulas), style = MaterialTheme.typography.titleMedium, color = LyloPrimary)
                        Text(
                            strings.get(R.string.recommendations_invalid_qr),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LyloPrimary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                } else {
                    Column(modifier = Modifier.widthIn(max = 480.dp).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            strings.get(R.string.quiz_results_title_chosen),
                            style = MaterialTheme.typography.headlineSmall,
                            color = LyloPrimary,
                        )
                        Text(
                            strings.get(R.string.quiz_results_subtitle_chosen),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LyloPrimary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .padding(20.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                Text("Formule recommandée", style = MaterialTheme.typography.labelSmall, color = LyloPrimary.copy(alpha = 0.7f))
                                Text(formula.profile, style = LuxuryTitleStyle.copy(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)), modifier = Modifier.padding(top = 4.dp))
                                Text("${strings.get(R.string.formula_version)} ${formula.size.wire}", style = MaterialTheme.typography.bodySmall, color = LyloPrimary.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp))
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SharedNoteList(strings.get(R.string.recommendations_note_labels_top), formula.top)
                                SharedNoteList(strings.get(R.string.recommendations_note_labels_heart), formula.heart)
                                SharedNoteList(strings.get(R.string.recommendations_note_labels_base), formula.base)
                                SharedNoteList(strings.get(R.string.recommendations_note_labels_boosters), formula.boosters)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedNoteList(label: String, notes: List<com.lylo.tablet.data.model.FormulaNote>) {
    if (notes.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFCFAF8))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LyloPrimary, modifier = Modifier.padding(bottom = 6.dp))
        notes.forEach { note ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(note.name, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4F443E))
                Text("${note.ml} ml", style = MaterialTheme.typography.bodyMedium, color = LyloPrimary)
            }
        }
    }
}
