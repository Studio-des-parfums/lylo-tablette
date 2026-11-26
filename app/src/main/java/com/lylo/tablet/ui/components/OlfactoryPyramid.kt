package com.lylo.tablet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lylo.tablet.R
import com.lylo.tablet.data.model.FormulaNote
import com.lylo.tablet.data.model.FormulaSize
import com.lylo.tablet.i18n.LocalStringResolver
import com.lylo.tablet.ui.theme.BrandTextStyle
import com.lylo.tablet.ui.theme.LyloPrimary
import com.lylo.tablet.ui.theme.LyloSecondary
import com.lylo.tablet.util.formatMl

enum class NoteType(val wire: String) { TOP("top"), HEART("heart"), BASE("base") }

/** Étage de la pyramide : tranche d'un même triangle (polygone en fractions de largeur/hauteur). */
private data class Tier(
    val type: NoteType,
    val polygon: List<Pair<Float, Float>>,
    /** Fraction de la largeur de la pyramide entre le bord de la colonne et le bord oblique de
     * l'étage (mesuré à mi-hauteur) : les traits fléchés s'enfoncent jusque-là. */
    val inset: Float,
    val labelRes: Int,
)

private val TIERS = listOf(
    Tier(NoteType.TOP, listOf(0.5f to 0f, 0.66667f to 1f, 0.33333f to 1f), 5f / 12f, R.string.pyramid_top),
    Tier(NoteType.HEART, listOf(0.33333f to 0f, 0.66667f to 0f, 0.83333f to 1f, 0.16667f to 1f), 3f / 12f, R.string.pyramid_heart),
    Tier(NoteType.BASE, listOf(0.16667f to 0f, 0.83333f to 0f, 1f to 1f, 0f to 1f), 1f / 12f, R.string.pyramid_base),
)

private val CONNECTOR_GAP = 6.dp
private val PYRAMID_WIDTH = 240.dp
private val TIER_HEIGHT = 96.dp

private fun polygonShape(points: List<Pair<Float, Float>>): Shape = GenericShape { size, _ ->
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(x * size.width, y * size.height) else lineTo(x * size.width, y * size.height)
    }
    close()
}

/**
 * Portage de src/components/recommendations/OlfactoryPyramid.tsx : pyramide olfactive en 3 étages
 * (tête / cœur / fond). Les notes de chaque étage sont réparties à gauche et à droite (1re à
 * gauche, 2e à droite…), reliées à l'étage par un trait fléché. Une note qui a des alternatives est
 * cliquable et ouvre la personnalisation côté parent.
 */
@Composable
fun OlfactoryPyramid(
    sizeData: FormulaSize,
    modifier: Modifier = Modifier,
    onNoteEdit: ((NoteType, FormulaNote) -> Unit)? = null,
    activeNoteName: String? = null,
) {
    val strings = LocalStringResolver.current
    val notesByType = mapOf(
        NoteType.TOP to sizeData.topNotes,
        NoteType.HEART to sizeData.heartNotes,
        NoteType.BASE to sizeData.baseNotes,
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TIERS.forEach { tier ->
            val notes = notesByType[tier.type].orEmpty()
            val left = notes.filterIndexed { i, _ -> i % 2 == 0 }
            val right = notes.filterIndexed { i, _ -> i % 2 == 1 }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(TIER_HEIGHT)) {
                val sideWidth = (maxWidth - PYRAMID_WIDTH) / 2
                // Le trait déborde dans la colonne centrale jusqu'au bord oblique de l'étage.
                val reach = sideWidth + PYRAMID_WIDTH * tier.inset - CONNECTOR_GAP

                SideNotes(
                    notes = left,
                    noteType = tier.type,
                    isStart = true,
                    maxNotesWidth = sideWidth,
                    onNoteEdit = onNoteEdit,
                    activeNoteName = activeNoteName,
                    modifier = Modifier.align(Alignment.CenterStart).width(reach),
                )
                SideNotes(
                    notes = right,
                    noteType = tier.type,
                    isStart = false,
                    maxNotesWidth = sideWidth,
                    onNoteEdit = onNoteEdit,
                    activeNoteName = activeNoteName,
                    modifier = Modifier.align(Alignment.CenterEnd).width(reach),
                )

                val surface = when (tier.type) {
                    NoteType.TOP -> LyloSecondary
                    NoteType.HEART -> LyloPrimary.copy(alpha = 0.65f)
                    NoteType.BASE -> LyloPrimary
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1f)
                        .width(PYRAMID_WIDTH)
                        .fillMaxHeight()
                        .clip(polygonShape(tier.polygon))
                        .background(surface)
                        .padding(bottom = if (tier.type == NoteType.TOP) 8.dp else 0.dp),
                    contentAlignment = if (tier.type == NoteType.TOP) Alignment.BottomCenter else Alignment.Center,
                ) {
                    Text(
                        strings.get(tier.labelRes).uppercase(),
                        style = BrandTextStyle.copy(fontSize = 12.sp, letterSpacing = 2.4.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (tier.type == NoteType.TOP) LyloPrimary else Color.White,
                    )
                }
            }
        }

        if (sizeData.boosters.isNotEmpty()) {
            Text(
                text = strings.get(R.string.recommendations_note_labels_boosters).uppercase() + "  " +
                    sizeData.boosters.joinToString(" · ") { "${it.name} ${formatMl(it.ml)} ml" },
                style = MaterialTheme.typography.labelSmall,
                color = LyloPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SideNotes(
    notes: List<FormulaNote>,
    noteType: NoteType,
    isStart: Boolean,
    maxNotesWidth: Dp,
    onNoteEdit: ((NoteType, FormulaNote) -> Unit)?,
    activeNoteName: String?,
    modifier: Modifier = Modifier,
) {
    if (notes.isEmpty()) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val chips = @Composable {
            Column(
                modifier = Modifier.widthIn(max = maxNotesWidth),
                horizontalAlignment = if (isStart) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                notes.forEach { note ->
                    NoteChip(note, noteType, onNoteEdit, active = activeNoteName == note.name)
                }
            }
        }
        if (isStart) {
            chips()
            Connector(pointsToEnd = true, modifier = Modifier.weight(1f).padding(start = 8.dp))
        } else {
            Connector(pointsToEnd = false, modifier = Modifier.weight(1f).padding(end = 8.dp))
            chips()
        }
    }
}

/** Trait fléché vers l'étage (la flèche pointe vers la pyramide). */
@Composable
private fun Connector(pointsToEnd: Boolean, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val lineColor = LyloPrimary.copy(alpha = 0.4f)
    val arrowColor = LyloPrimary.copy(alpha = 0.6f)
    Canvas(modifier = modifier.height(8.dp)) {
        val y = size.height / 2
        // En RTL, Row inverse l'ordre visuel : la flèche doit toujours pointer vers la pyramide.
        val towardsRight = pointsToEnd != rtl
        val arrowW = 6.dp.toPx()
        val arrowH = 4.dp.toPx()
        if (towardsRight) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width - arrowW, y), strokeWidth = 1.dp.toPx())
            drawPath(Path().apply {
                moveTo(size.width, y); lineTo(size.width - arrowW, y - arrowH); lineTo(size.width - arrowW, y + arrowH); close()
            }, arrowColor)
        } else {
            drawLine(lineColor, Offset(arrowW, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            drawPath(Path().apply {
                moveTo(0f, y); lineTo(arrowW, y - arrowH); lineTo(arrowW, y + arrowH); close()
            }, arrowColor)
        }
    }
}

@Composable
private fun NoteChip(
    note: FormulaNote,
    noteType: NoteType,
    onNoteEdit: ((NoteType, FormulaNote) -> Unit)?,
    active: Boolean,
) {
    val editable = onNoteEdit != null && note.alternatives.isNotEmpty()
    val shape = RoundedCornerShape(999.dp)
    val bg = when {
        active -> LyloPrimary
        editable -> Color.White
        else -> Color.White.copy(alpha = 0.8f)
    }
    val borderColor = when {
        active -> LyloPrimary
        editable -> LyloPrimary.copy(alpha = 0.25f)
        else -> LyloPrimary.copy(alpha = 0.15f)
    }
    Row(
        modifier = Modifier
            .then(if (editable && !active) Modifier.shadow(1.dp, shape) else Modifier)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .then(if (editable) Modifier.clickable { onNoteEdit?.invoke(noteType, note) } else Modifier)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            note.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (active) Color.White else Color(0xFF4F443E),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            "${formatMl(note.ml)} ml",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else LyloPrimary,
            maxLines = 1,
        )
        if (editable) {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = if (active) Color.White else LyloPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
