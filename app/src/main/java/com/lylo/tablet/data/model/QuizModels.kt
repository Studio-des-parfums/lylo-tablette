package com.lylo.tablet.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Portage des types locaux de src/app/quiz/page.tsx ───────────────────────

@Serializable
data class QuizAnswer(
    @SerialName("question_id") val questionId: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("top_2") val top2: List<String>,
    @SerialName("bottom_2") val bottom2: List<String>,
    @SerialName("positive_label") val positiveLabel: String = "",
    @SerialName("negative_label") val negativeLabel: String = "",
)

enum class Gender(val wire: String) { HOMME("homme"), FEMME("femme"), NONE("") }

data class QuizProfile(
    val gender: Gender = Gender.NONE,
    val age: String = "",
    val pregnant: Boolean? = null,
    val hasDislikedNotes: Boolean? = null,
    val dislikedNotes: String = "",
    val hasAllergies: Boolean? = null,
    val allergies: String = "",
)

enum class QuizStep { PROFILE, QUESTIONNAIRE }
enum class QuizPhase { TOP2, BOTTOM2, CONFIRM, INTENSITY, PERFUME_NAME }
enum class ProfileStep { GENDER, AGE, PREGNANT, DISLIKES, DISLIKES_DETAIL, ALLERGIES, ALLERGIES_DETAIL }

/** Préférence d'intensité du parfum, demandée après le questionnaire — miroir de
 * FormulaType dans src/app/quiz/page.tsx. `wire` est la valeur envoyée au backend. */
enum class FormulaType(val wire: String) { FRAIS("frais"), MIX("mix"), PUISSANT("puissant") }

data class ParticipantQuizState(
    val color: String,
    val profile: QuizProfile = QuizProfile(),
    val answers: List<QuizAnswer> = emptyList(),
    val formulaType: FormulaType? = null,
    val perfumeName: String = "",
)

// ── Résultat de génération de formule(s) — src/app/quiz/results/page.tsx ────

data class ParticipantResult(
    val color: String,
    val formulas: List<Formula>,
)
