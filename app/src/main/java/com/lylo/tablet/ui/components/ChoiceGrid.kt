package com.lylo.tablet.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lylo.tablet.BuildConfig
import com.lylo.tablet.data.model.Choice
import kotlin.math.ceil

/**
 * Portage de src/components/interaction/ChoiceGrid.tsx — grille 3 colonnes de ChoiceCard,
 * filtrant les hiddenChoices (déjà sélectionnées en mode voix) et gérant la sélection multiple
 * (jusqu'à 2 cartes) en mode clic.
 *
 * Portage fidèle de `max-h-[52vh]` (CSS Grid) : la grille entière ne doit JAMAIS scroller — on
 * calcule donc la taille des cartes à partir de l'espace réellement disponible (BoxWithConstraints)
 * plutôt que de les laisser en aspect-ratio 1:1 non contraint dans un LazyVerticalGrid (qui
 * scrollait dès que le nombre de lignes dépassait la hauteur visible). Jusqu'à 3 colonnes, autant
 * de lignes que nécessaire réparties dans la hauteur disponible.
 *
 * NOTE : la logique de sélection à 2 clics + submitClickAnswer() est gérée par l'appelant
 * (InteractionScreen) qui tient l'état `clickSelected`, car ChoiceGrid ici reste un composant
 * d'affichage pur piloté par état — fidèle à l'esprit du composant React qui, lui, gérait cet
 * état localement ; on l'a remonté d'un cran pour simplifier le partage avec le SessionViewModel.
 */
@Composable
fun ChoiceGrid(
    choices: List<Choice>,
    hiddenChoices: List<String>,
    selectedLabels: List<String>,
    clickable: Boolean,
    modifier: Modifier = Modifier,
    /** Pastille numérotée sur chaque carte (mode vocal : l'utilisateur peut dire « le 4 »). */
    showNumbers: Boolean = false,
    onSelect: (String) -> Unit,
) {
    // Portage de ChoiceGrid.tsx normalize() : supprime les diacritiques (accents) avant
    // comparaison, pour que "Épicé" et "epice" soient reconnus comme identiques.
    fun normalize(s: String): String {
        val decomposed = java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{Mn}+"), "").lowercase()
    }
    // Le numéro reste celui d'origine (1…n) même quand des cartes sont masquées.
    val visible = choices
        .mapIndexed { i, choice -> choice to (i + 1) }
        .filter { (choice, _) -> hiddenChoices.none { normalize(it) == normalize(choice.label) } }

    val columns = 3
    val spacing = 12.dp
    val maxCardSize = 170.dp
    val rows = ceil(visible.size / columns.toFloat()).toInt().coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardWidth = (maxWidth - spacing * (columns - 1)) / columns
        val cardHeight = (maxHeight - spacing * (rows - 1)) / rows
        // Carte carrée bornée par la plus petite des deux dimensions disponibles (garantit
        // qu'aucune ligne ne déborde jamais de la hauteur allouée) ET par un plafond fixe
        // (maxCardSize) pour que les cartes restent compactes même sur un grand écran — la
        // grille entière est ensuite centrée dans l'espace disponible.
        val cardSize = minOf(cardWidth, cardHeight, maxCardSize)

        // Lignes complètes, dernière ligne centrée (5 cartes = 3 + 2 centrées).
        Column(verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally) {
            visible.chunked(columns).forEach { rowChoices ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    rowChoices.forEach { (choice, number) ->
                        ChoiceCard(
                            name = choice.label,
                            number = if (showNumbers) number else null,
                            imageUrl = choice.image.takeIf { it.isNotBlank() }?.let { img ->
                                if (img.startsWith("http")) img else "${BuildConfig.API_BASE_URL}$img"
                            },
                            selected = selectedLabels.contains(choice.label),
                            clickable = clickable,
                            onSelect = onSelect,
                            modifier = Modifier.width(cardSize).height(cardSize),
                        )
                    }
                }
            }
        }
    }
}
