package com.lylo.tablet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lylo.tablet.R
import com.lylo.tablet.data.model.FormulaNote
import com.lylo.tablet.data.model.FormulaSizes
import com.lylo.tablet.data.model.SizeOption
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.LuxuryTitleStyle
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloSecondary
import com.lylo.tablet.ui.theme.cardShadow

enum class FormulaCardVariant { DEFAULT, COMPARISON }

private const val MAX_NOTES = 3

/** Portage de src/components/recommendations/FormulaCard.tsx. */
@Composable
fun FormulaCard(
    name: String,
    sizes: FormulaSizes,
    modifier: Modifier = Modifier,
    variant: FormulaCardVariant = FormulaCardVariant.DEFAULT,
    selectedSize: SizeOption? = null,
    onSelectedSizeChange: (SizeOption) -> Unit = {},
    /** Agrandit le texte des notes — écrans plein page où la carte n'est pas contrainte (/formulas). */
    large: Boolean = false,
) {
    val strings = LocalStringResolver.current
    var internalSize by remember { mutableStateOf(SizeOption.ML30) }
    val activeSize = selectedSize ?: internalSize
    val sizeData = sizes.forOption(activeSize)
    val isComparison = variant == FormulaCardVariant.COMPARISON
    val shape = RoundedCornerShape(16.dp)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, LyloSecondary.copy(alpha = 0.3f), shape)
            .cardShadow(shape)
            .padding(if (isComparison) 16.dp else 12.dp),
    ) {
        if (isComparison) {
            Text(
                text = strings.get(R.string.formula_recommended_formula),
                style = MaterialTheme.typography.labelSmall,
                color = LyloPrimary.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = name,
            style = LuxuryTitleStyle.copy(fontWeight = FontWeight.Bold, fontSize = if (isComparison) 22.sp else 18.sp),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
            textAlign = TextAlign.Center,
        )

        val notesModifier = if (isComparison) {
            Modifier.weight(1f, fill = false).verticalScroll(scrollState)
        } else {
            Modifier
        }
        Column(modifier = notesModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NoteList(strings.get(R.string.recommendations_note_labels_top), sizeData.topNotes, isComparison, large)
            NoteList(strings.get(R.string.recommendations_note_labels_heart), sizeData.heartNotes, isComparison, large)
            NoteList(strings.get(R.string.recommendations_note_labels_base), sizeData.baseNotes, isComparison, large)
            NoteList(strings.get(R.string.recommendations_note_labels_boosters), sizeData.boosters, isComparison, large)
        }

        SizeToggle(
            selected = activeSize,
            onSelect = { size ->
                if (selectedSize == null) internalSize = size
                onSelectedSizeChange(size)
            },
        )
    }
}

@Composable
private fun NoteList(label: String, notes: List<FormulaNote>, isComparison: Boolean, large: Boolean = false) {
    if (notes.isEmpty()) return
    val visible = notes.take(MAX_NOTES)

    Column(
        modifier = if (isComparison) {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFCFAF8))
                .border(1.dp, LyloPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        } else {
            Modifier.fillMaxWidth()
        },
    ) {
        Text(
            text = label,
            style = if (large) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
            color = LyloPrimary,
            modifier = Modifier.padding(bottom = if (isComparison) 8.dp else if (large) 6.dp else 2.dp),
        )
        visible.forEach { note ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = if (isComparison) 4.dp else if (large) 6.dp else 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = note.name,
                    style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (isComparison) Color(0xFF4F443E) else Color(0xFF4B5563),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = "${com.lylo.tablet.util.formatMl(note.ml)} ml",
                    style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (isComparison) LyloPrimary else LyloPrimary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
