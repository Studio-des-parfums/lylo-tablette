package com.lylo.tablet.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

// ── Portage direct des types définis dans src/context/SessionContext.tsx ───

enum class SessionState {
    IDLE, CONNECTING, COLLECTING_PROFILE, QUESTIONNAIRE, GENERATING_FORMULAS,
    COMPLETED, CUSTOMIZATION, STANDBY, AWAITING_CONFIRMATION, ASKING_INTENSITY, ASKING_PERFUME_NAME;

    companion object {
        fun fromWire(value: String): SessionState = when (value) {
            "idle" -> IDLE
            "connecting" -> CONNECTING
            "collecting_profile" -> COLLECTING_PROFILE
            "questionnaire" -> QUESTIONNAIRE
            "generating_formulas" -> GENERATING_FORMULAS
            "completed" -> COMPLETED
            "customization" -> CUSTOMIZATION
            "standby" -> STANDBY
            "awaiting_confirmation" -> AWAITING_CONFIRMATION
            "asking_intensity" -> ASKING_INTENSITY
            "asking_perfume_name" -> ASKING_PERFUME_NAME
            else -> IDLE
        }
    }
}

enum class AgentState {
    SPEAKING, LISTENING, THINKING, IDLE, INITIALIZING;

    companion object {
        fun fromWire(value: String): AgentState = when (value) {
            "speaking" -> SPEAKING
            "listening" -> LISTENING
            "thinking" -> THINKING
            "idle" -> IDLE
            "initializing" -> INITIALIZING
            else -> IDLE
        }
    }
}

@Serializable
data class ProfileUpdate(
    val field: String,
    val value: String,
    @SerialName("profile_complete") val profileComplete: Boolean = false,
    @SerialName("missing_fields") val missingFields: List<String> = emptyList(),
)

data class AnswerSaved(
    val questionId: Int,
    val top2: List<String>,
    val bottom2: List<String>,
)

@Serializable
data class FormulaNote(
    val name: String,
    val ml: Double,
    val family: String? = null,
    val description: String? = null,
    @SerialName("note_type") val noteType: String? = null,
    val priority: String? = null,
    // Jusqu'à 2 alternatives proposées par le LLM au moment de la génération — permet de
    // remplacer cette note dans l'écran de personnalisation sans repasser par une recherche.
    val alternatives: List<FormulaNote> = emptyList(),
)

@Serializable
data class FormulaSize(
    @SerialName("target_ml") val targetMl: Double,
    @SerialName("top_notes") val topNotes: List<FormulaNote> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<FormulaNote> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<FormulaNote> = emptyList(),
    val boosters: List<FormulaNote> = emptyList(),
)

@Serializable
data class FormulaSizes(
    @SerialName("10ml") val ml10: FormulaSize,
    @SerialName("30ml") val ml30: FormulaSize,
    @SerialName("50ml") val ml50: FormulaSize,
) {
    fun forOption(option: SizeOption): FormulaSize = when (option) {
        SizeOption.ML10 -> ml10
        SizeOption.ML30 -> ml30
        SizeOption.ML50 -> ml50
    }
}

/**
 * Formule générée (mode vocal et quiz visuel). [raw] conserve l'objet JSON reçu du back tel quel :
 * le web renvoie la formule complète (perfume_name, session_id, champs moodboard…) aux endpoints
 * save / replace-note / similar-perfumes — on fait de même via [toJson] pour ne rien perdre.
 */
@Serializable
data class Formula(
    val profile: String,
    val description: String = "",
    val score: Int = 0,
    @SerialName("formula_type") val formulaType: String? = null,
    @SerialName("top_notes") val topNotes: List<String> = emptyList(),
    @SerialName("heart_notes") val heartNotes: List<String> = emptyList(),
    @SerialName("base_notes") val baseNotes: List<String> = emptyList(),
    val sizes: FormulaSizes,
    @SerialName("session_id") val sessionId: String? = null,
    // Visuel d'ambiance généré à partir des notes (voir moodboard_service côté back) — absent
    // tant que la génération en arrière-plan n'est pas terminée.
    @SerialName("moodboard_image_url") val moodboardImageUrl: String? = null,
    @Transient val raw: JsonObject? = null,
) {
    fun toJson(): JsonObject = raw ?: FormulaJson.json.encodeToJsonElement(this).jsonObject

    companion object {
        fun parse(element: JsonElement): Formula? = runCatching {
            FormulaJson.json.decodeFromJsonElement<Formula>(element).copy(raw = element.jsonObject)
        }.getOrNull()

        fun parseList(element: JsonElement?): List<Formula> =
            (element as? kotlinx.serialization.json.JsonArray)?.mapNotNull { parse(it) } ?: emptyList()
    }
}

object FormulaJson {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
}

/**
 * Portage de src/lib/answerFormat.ts : format de réponse du questionnaire actif (renvoyé par le
 * back : /api/questions et la session). Pilote le nombre de choix « le plus » / « le moins »,
 * l'étape « le moins » et les libellés, déjà traduits par le back dans la langue de la session.
 */
@Serializable
data class AnswerFormat(
    @SerialName("positive_label") val positiveLabel: String = "",
    @SerialName("negative_label") val negativeLabel: String = "",
    @SerialName("positive_count") val positiveCount: Int = 2,
    @SerialName("positive_justify") val positiveJustify: Boolean = true,
    @SerialName("negative_count") val negativeCount: Int = 2, // 0 = pas d'étape « le moins »
    @SerialName("negative_justify") val negativeJustify: Boolean = true,
) {
    companion object {
        /** Format historique Lylo : 2 préférés + 2 moins aimés (libellés remplacés par ceux du back). */
        val DEFAULT = AnswerFormat()

        fun parse(element: JsonElement?): AnswerFormat =
            if (element is JsonObject) runCatching { FormulaJson.json.decodeFromJsonElement<AnswerFormat>(element) }.getOrDefault(DEFAULT)
            else DEFAULT
    }

    /** Consigne de sélection : « Vos préférés · 2 réponses » — portage de selectionHint(). */
    fun selectionHint(positive: Boolean, countOne: String, countMany: String): String {
        val n = if (positive) positiveCount else negativeCount
        val label = if (positive) positiveLabel else negativeLabel
        val count = (if (n == 1) countOne else countMany).replace("{n}", n.toString())
        return if (label.isNotEmpty()) "$label · $count" else count
    }
}

enum class SizeOption(val wire: String) {
    ML10("10ml"), ML30("30ml"), ML50("50ml");

    companion object {
        fun fromWire(value: String): SizeOption = entries.firstOrNull { it.wire == value } ?: ML30
    }
}

@Serializable
data class Choice(
    val label: String,
    val image: String = "",
)

@Serializable
data class Question(
    val id: Int,
    val question: String,
    val choices: List<Choice> = emptyList(),
)

data class TranscriptMessage(
    val id: String,
    val sender: TranscriptSender,
    val text: String,
    val timestamp: Long,
)

enum class TranscriptSender { AGENT, USER }

data class PendingClickAnswer(
    val type: String, // "questionnaire_top_2" | "questionnaire_bottom_2"
    val questionId: Int,
    val values: List<String>,
    val top2: List<String>? = null,
)

data class ConfirmationData(
    val questionId: Int,
    val top2: List<String>,
    val bottom2: List<String>,
)

data class SessionData(
    val sessionId: String,
    val roomName: String,
    val token: String,
    val livekitUrl: String,
    val identity: String,
)
