package com.lylo.tablet.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lylo.tablet.data.LocalPrefs
import com.lylo.tablet.data.model.Formula
import com.lylo.tablet.data.model.FormulaJson
import com.lylo.tablet.data.model.ParticipantResult
import com.lylo.tablet.i18n.LanguageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Portage de src/app/quiz/results/page.tsx (QuizResultsPage + SoloResults + MultiResults).
 * Détecte le mode (solo/duo) via "participant_colors", lit quiz_formulas / quiz_multi_results
 * (formules brutes, telles que renvoyées par le back) et pilote l'enchaînement des personnes en
 * duo. Le parcours d'une personne (choix → pyramide → personnalisation → actions) est porté par
 * FormulaJourney (QuizResultsScreen.kt), comme le composant React du même nom.
 */
class QuizResultsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = LocalPrefs(application)
    val language = LanguageStore.resolveStoredLanguage(prefs).also { LanguageStore.persistLanguage(prefs, it) }

    val isMulti: Boolean = run {
        val raw = prefs.getString(LocalPrefs.KEY_PARTICIPANT_COLORS)
        if (raw.isNullOrBlank()) false
        else runCatching { (FormulaJson.json.parseToJsonElement(raw) as JsonArray).size > 1 }.getOrElse { false }
    }

    // ── Solo ─────────────────────────────────────────────────────────────
    /** null = rien en mémoire (retour au quiz, comme router.push("/quiz") côté web). */
    val soloFormulas: List<Formula>? = if (!isMulti) {
        prefs.getString(LocalPrefs.KEY_QUIZ_FORMULAS)?.let { raw ->
            runCatching { Formula.parseList(FormulaJson.json.parseToJsonElement(raw)) }.getOrNull()
        }
    } else null

    // ── Duo ──────────────────────────────────────────────────────────────
    val multiParticipants: List<ParticipantResult> = if (isMulti) {
        runCatching {
            val raw = prefs.getString(LocalPrefs.KEY_QUIZ_MULTI_RESULTS) ?: return@runCatching emptyList()
            (FormulaJson.json.parseToJsonElement(raw) as JsonArray).map { el ->
                val obj = el.jsonObject
                ParticipantResult(
                    color = obj["color"]?.jsonPrimitive?.content ?: "",
                    formulas = Formula.parseList(obj["formulas"]),
                )
            }
        }.getOrNull() ?: emptyList()
    } else emptyList()

    private val _currentIdx = MutableStateFlow(0)
    val currentIdx: StateFlow<Int> = _currentIdx.asStateFlow()

    val hasResults: Boolean get() = if (isMulti) multiParticipants.isNotEmpty() else !soloFormulas.isNullOrEmpty()

    /** Duo : « Passer à la personne suivante » / « Terminer » (la dernière personne quitte). */
    fun nextParticipant(): Boolean {
        val next = _currentIdx.value + 1
        return if (next < multiParticipants.size) {
            _currentIdx.value = next
            true
        } else {
            false
        }
    }

    /** « Retour à l'accueil » / « Terminer » : purge les résultats en mémoire, comme le web. */
    fun clearResults() {
        if (isMulti) {
            prefs.remove(LocalPrefs.KEY_QUIZ_MULTI_RESULTS)
            prefs.remove(LocalPrefs.KEY_PARTICIPANT_COLORS)
        } else {
            prefs.remove(LocalPrefs.KEY_QUIZ_FORMULAS)
        }
    }

    val recapEmail: String? get() = prefs.getString(LocalPrefs.KEY_RECAP_EMAIL)
    val recapName: String? get() = prefs.getString(LocalPrefs.KEY_RECAP_NAME)
}
