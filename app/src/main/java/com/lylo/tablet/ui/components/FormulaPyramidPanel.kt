package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lylo.tablet.R
import com.lylo.tablet.data.Brand
import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.FormulaNote
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.data.remote.ApiClient
import com.lylo.tablet.data.remote.ReplaceNoteRequest
import com.lylo.tablet.data.remote.SimilarPerfume
import com.lylo.tablet.data.remote.SimilarPerfumesFormula
import com.lylo.tablet.data.remote.SimilarPerfumesRequest
import com.lylo.tablet.i18n.AppLanguage
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.BrandTextStyle
import com.lylo.tablet.ui.theme.LuxuryTitleStyle
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.util.formatMl
import kotlinx.coroutines.launch

/** Recherche des parfums du commerce ressemblants (POST /api/formulas/similar-perfumes). */
suspend fun fetchSimilarPerfumes(formula: Formula, language: AppLanguage): List<SimilarPerfume>? = try {
    val res = ApiClient.service.similarPerfumes(
        SimilarPerfumesRequest(
            formula = SimilarPerfumesFormula(
                profile = formula.profile,
                description = formula.description.ifEmpty { null },
                topNotes = formula.topNotes,
                heartNotes = formula.heartNotes,
                baseNotes = formula.baseNotes,
            ),
            language = language.code,
        ),
    )
    if (res.isSuccessful) res.body()?.similarPerfumes ?: emptyList() else null
} catch (_: Exception) {
    null
}

/**
 * Portage de src/components/recommendations/FormulaPyramidPanel.tsx : écran final d'une formule,
 * commun au parcours visuel (quiz) et vocal — nom + parfums ressemblants, pyramide olfactive
 * (notes cliquables → remplacement en modale) et taille.
 */
@Composable
fun FormulaPyramidPanel(
    formula: Formula,
    selectedSize: SizeOption,
    onSelectedSizeChange: (SizeOption) -> Unit,
    language: AppLanguage,
    onFormulaChange: (Formula) -> Unit,
    modifier: Modifier = Modifier,
    /** Référence en base : le remplacement de note y est répercuté. */
    reference: String? = null,
    /** Mode vocal : session en cours, tenue à jour pour l'agent lors d'un remplacement. */
    sessionId: String? = null,
    /** Cache des parfums ressemblants géré par le parent ; à défaut, le panneau gère le sien. */
    similarPerfumes: SimilarPerfumesState? = null,
) {
    val strings = LocalStringResolver.current
    val scope = rememberCoroutineScope()
    var activeNote by remember { mutableStateOf<Pair<NoteType, FormulaNote>?>(null) }

    var ownSimilar by remember(formula.profile) { mutableStateOf<List<SimilarPerfume>?>(null) }
    var ownSimilarLoading by remember(formula.profile) { mutableStateOf(false) }
    var ownSimilarError by remember(formula.profile) { mutableStateOf(false) }

    val similar = similarPerfumes ?: SimilarPerfumesState(
        cached = ownSimilar,
        onFetch = {
            ownSimilarLoading = true
            ownSimilarError = false
            scope.launch {
                val result = fetchSimilarPerfumes(formula, language)
                if (result == null) ownSimilarError = true else ownSimilar = result
                ownSimilarLoading = false
            }
        },
        loading = ownSimilarLoading,
        error = ownSimilarError,
    )

    val sizeData = formula.sizes.forOption(selectedSize)

    Column(
        modifier = modifier.widthIn(max = 768.dp).fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                formula.profile,
                style = LuxuryTitleStyle.copy(fontSize = 20.sp, color = LyloPrimary),
                textAlign = TextAlign.Center,
            )
            SimilarPerfumesButton(similar)
        }
        OlfactoryPyramid(
            sizeData = sizeData,
            onNoteEdit = { type, note -> activeNote = type to note },
            activeNoteName = activeNote?.second?.name,
        )
        Text(
            strings.get(R.string.pyramid_hint),
            style = MaterialTheme.typography.labelSmall,
            color = LyloPrimary.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Box(modifier = Modifier.widthIn(max = 320.dp)) {
            SizeToggle(
                selected = selectedSize,
                onSelect = { size -> onSelectedSizeChange(size); activeNote = null },
            )
        }
    }

    // Remplacement d'une note : ouvert au clic sur une note, refermé après remplacement.
    activeNote?.let { (noteType, note) ->
        Dialog(onDismissRequest = { activeNote = null }) {
            Box(
                modifier = Modifier
                    .widthIn(max = 384.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, LyloPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
            ) {
                FormulaPersonalizer(
                    formula = formula,
                    language = language,
                    noteType = noteType,
                    note = note,
                    reference = reference,
                    sessionId = sessionId,
                    onFormulaChange = { updated -> onFormulaChange(updated); activeNote = null },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 40.dp),
                )
                IconButton(
                    onClick = { activeNote = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = strings.get(R.string.send_mail_close), tint = LyloPrimary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * Portage de src/components/recommendations/FormulaPersonalizer.tsx : affiche, pour la note
 * sélectionnée, ses alternatives (jusqu'à 2, proposées par le LLM à la génération). Cliquer une
 * alternative appelle POST /api/formulas/replace-note (recalcule le booster) et remonte la formule
 * mise à jour au parent.
 */
@Composable
private fun FormulaPersonalizer(
    formula: Formula,
    language: AppLanguage,
    noteType: NoteType,
    note: FormulaNote,
    reference: String?,
    sessionId: String?,
    onFormulaChange: (Formula) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStringResolver.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun replace(newNote: String) {
        pending = true
        error = false
        scope.launch {
            try {
                val res = ApiClient.service.replaceNote(
                    ReplaceNoteRequest(
                        formula = formula.toJson(),
                        noteType = noteType.wire,
                        oldNote = note.name,
                        newNote = newNote,
                        language = language.code,
                        reference = reference?.takeIf { it.isNotEmpty() },
                        sessionId = sessionId?.takeIf { it.isNotEmpty() },
                        brand = Brand.active.id,
                    ),
                )
                val updated = if (res.isSuccessful) res.body()?.get("formula")?.let { Formula.parse(it) } else null
                if (updated == null) error = true else onFormulaChange(updated)
            } catch (_: Exception) {
                error = true
            } finally {
                pending = false
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text(
                strings.get(R.string.formula_personalizer_current_note).uppercase(),
                style = BrandTextStyle.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                color = LyloPrimary.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LyloPrimary.copy(alpha = 0.05f))
                    .border(1.dp, LyloPrimary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(note.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = LyloPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text("${formatMl(note.ml)} ml", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = LyloPrimary)
            }
        }

        if (note.alternatives.isNotEmpty()) {
            Column {
                Text(
                    strings.get(R.string.formula_personalizer_alternatives).uppercase(),
                    style = BrandTextStyle.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                    color = LyloPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.alternatives.forEach { alt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFCFAF8))
                                .border(2.dp, LyloPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .alpha(if (pending) 0.4f else 1f)
                                .clickable(enabled = !pending) { replace(alt.name) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                if (pending) {
                                    CircularProgressIndicator(color = LyloPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = LyloPrimary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    alt.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4F443E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 10.dp),
                                )
                            }
                            Text("${formatMl(alt.ml)} ml", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LyloPrimary)
                        }
                    }
                }
            }
        } else {
            Text(
                strings.get(R.string.formula_personalizer_no_alternatives),
                style = MaterialTheme.typography.labelSmall,
                color = LyloPrimary.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (error) {
            Text(
                strings.get(R.string.formula_personalizer_error),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFEF4444),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
